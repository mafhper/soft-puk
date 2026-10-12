package dev.mafhper.softpuk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mafhper.softpuk.state.PuzzleState
import dev.mafhper.softpuk.ui.components.MatrixGrid
import dev.mafhper.softpuk.ui.theme.SignalYellow

@Composable
fun SolutionScreen(
    state: PuzzleState,
    onBack: () -> Unit,
    onNewScan: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(
            text = "SOLUÇÃO",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        val solution = state.solution
        if (solution == null || solution.picks.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Nenhum daemon alcançável com esta configuração. Volte e ajuste a " +
                    "grade, o buffer ou as sequências.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val enabledCount = state.daemons.count { it.enabled }
            var step by remember(solution) { mutableIntStateOf(solution.picks.size) }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        RoundedCornerShape(6.dp),
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DeckCell(
                    label = "DAEMONS",
                    value = "${solution.achieved.size}/$enabledCount",
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(48.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                )
                DeckCell(
                    label = "CLIQUES",
                    value = "${solution.picks.size}/${state.bufferSize}",
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            ) {
                OutlinedButton(onClick = { if (step > 1) step-- }, enabled = step > 1) {
                    Text("<")
                }
                Text(
                    text = "P A S S O  $step / ${solution.picks.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                OutlinedButton(
                    onClick = { if (step < solution.picks.size) step++ },
                    enabled = step < solution.picks.size,
                ) {
                    Text(">")
                }
            }

            Spacer(Modifier.height(12.dp))
            MatrixGrid(
                matrix = state.matrix,
                order = solution.picks,
                step = step,
                nextAccent = SignalYellow,
            )

            Spacer(Modifier.height(12.dp))
            Text(
                text = "B U F F E R",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 3.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                for (i in 0 until state.bufferSize) {
                    val slotState = when {
                        i < step -> BufferSlotState.DONE
                        i == step && step < solution.picks.size -> BufferSlotState.NEXT
                        i < solution.picks.size -> BufferSlotState.FUTURE
                        else -> BufferSlotState.EMPTY
                    }
                    val label = if (i < solution.buffer.size && slotState != BufferSlotState.EMPTY) {
                        solution.buffer[i].label
                    } else {
                        "·"
                    }
                    BufferSlot(label = label, state = slotState)
                    Spacer(Modifier.width(4.dp))
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "D A E M O N S",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 3.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            state.daemons.forEachIndexed { i, daemon ->
                val done = i in solution.achieved &&
                    (solution.firstStep[i] ?: Int.MAX_VALUE) <= step
                val color = when {
                    done -> MaterialTheme.colorScheme.primary
                    daemon.enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.outline
                }
                Row {
                    Text(
                        text = if (done) "[x] " else "[ ] ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = color,
                    )
                    Text(
                        text = daemon.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = color,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = daemon.codes.joinToString(" ") { it.label },
                        style = MaterialTheme.typography.bodyMedium,
                        color = color,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Row {
            OutlinedButton(onClick = onBack) {
                Text("VOLTAR")
            }
            Spacer(Modifier.width(12.dp))
            Button(onClick = onNewScan, modifier = Modifier.weight(1f)) {
                Text("NOVO SCAN")
            }
        }
    }
}

@Composable
private fun DeckCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 3.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

private enum class BufferSlotState { DONE, NEXT, FUTURE, EMPTY }

@Composable
private fun BufferSlot(label: String, state: BufferSlotState) {
    val borderColor = when (state) {
        BufferSlotState.DONE -> MaterialTheme.colorScheme.primary
        BufferSlotState.NEXT -> SignalYellow
        BufferSlotState.FUTURE -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        BufferSlotState.EMPTY -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    }
    val textColor = when (state) {
        BufferSlotState.DONE -> MaterialTheme.colorScheme.onSurface
        BufferSlotState.NEXT -> SignalYellow
        BufferSlotState.FUTURE -> MaterialTheme.colorScheme.onSurfaceVariant
        BufferSlotState.EMPTY -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderWidth = if (state == BufferSlotState.NEXT) 2.dp else 1.dp
    Box(
        modifier = Modifier
            .size(width = 46.dp, height = 38.dp)
            .border(borderWidth, borderColor, RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
        )
    }
}
