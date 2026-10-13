package dev.mafhper.softpuk.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mafhper.softpuk.state.PuzzleState
import dev.mafhper.softpuk.ui.components.BottomActionBar
import dev.mafhper.softpuk.ui.components.DaemonPanel
import dev.mafhper.softpuk.ui.components.MatrixGrid

@Composable
fun ConfirmScreen(
    state: PuzzleState,
    onBack: () -> Unit,
    onSolve: () -> Unit,
) {
    val isPortrait =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        // Ajustes FIXOS no topo: o canvas central fica só com a grade e os daemons.
        ConfirmControlsBar(state)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = "CONFIRMAR LEITURA",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Toque numa célula para trocar o código; ajuste os daemons abaixo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))

            MatrixGrid(
                matrix = state.matrix,
                onCellClick = { r, c -> state.cycleCell(r, c) },
            )

            Spacer(Modifier.height(18.dp))
            Text(
                text = "DAEMONS",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.daemons.forEachIndexed { i, daemon ->
                    DaemonPanel(
                        name = daemon.name,
                        codes = daemon.codes.map { it.label },
                        checked = daemon.enabled,
                        dim = !daemon.enabled,
                        onToggle = { state.toggleDaemon(i) },
                        onCodeClick = { j -> state.cycleDaemonCode(i, j) },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            // Em paisagem as ações ficam no fim do conteúdo (como sempre);
            // em retrato, na barra fixa do rodapé.
            if (!isPortrait) {
                Row {
                    OutlinedButton(onClick = onBack) {
                        Text("VOLTAR")
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = onSolve, modifier = Modifier.weight(1f)) {
                        Text("RESOLVER")
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }

        if (isPortrait) {
            BottomActionBar(
                primaryLabel = "RESOLVER",
                onBack = onBack,
                onPrimary = onSolve,
            )
        }
    }
}

/**
 * Barra superior: um RESUMO compacto de uma linha (o que quase nunca muda fica só
 * visível); os ajustes abrem no toque em AJUSTAR e voltam a se esconder no OCULTAR.
 */
@Composable
private fun ConfirmControlsBar(state: PuzzleState) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "GRADE ${state.matrix.size}x${state.matrix.maxOf { it.size }} · " +
                    "BUFFER ${state.bufferSize} · DAEMONS ${state.daemons.size}",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (expanded) "OCULTAR" else "AJUSTAR",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        if (expanded) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ControlGroup("GRADE") {
                    CompactButton(
                        label = "5x5",
                        selected = state.matrix.size == 5,
                        onClick = { state.setMatrixSize(5) },
                    )
                    Spacer(Modifier.width(4.dp))
                    CompactButton(
                        label = "6x6",
                        selected = state.matrix.size == 6,
                        onClick = { state.setMatrixSize(6) },
                    )
                }
                ControlGroup("BUFFER") {
                    CompactButton(
                        label = "-",
                        enabled = state.bufferSize > PuzzleState.MIN_BUFFER_SIZE,
                        onClick = { state.updateBufferSize(state.bufferSize - 1) },
                    )
                    StepperValue(state.bufferSize)
                    CompactButton(
                        label = "+",
                        enabled = state.bufferSize < PuzzleState.MAX_BUFFER_SIZE,
                        onClick = { state.updateBufferSize(state.bufferSize + 1) },
                    )
                }
                ControlGroup("DAEMONS") {
                    CompactButton(
                        label = "-",
                        enabled = state.daemons.size > 2,
                        onClick = { state.setDaemonCount(state.daemons.size - 1) },
                    )
                    StepperValue(state.daemons.size)
                    CompactButton(
                        label = "+",
                        enabled = state.daemons.size < 4,
                        onClick = { state.setDaemonCount(state.daemons.size + 1) },
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    }
}

@Composable
private fun ControlGroup(label: String, content: @Composable RowScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        content()
    }
}

@Composable
private fun RowScope.CompactButton(
    label: String,
    enabled: Boolean = true,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val modifier = Modifier.size(width = 44.dp, height = 40.dp)
    val padding = PaddingValues(0.dp)
    if (selected) {
        Button(onClick = onClick, modifier = modifier, contentPadding = padding) {
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            contentPadding = padding,
            enabled = enabled,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun RowScope.StepperValue(value: Int) {
    Text(
        text = value.toString(),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .widthIn(min = 26.dp)
            .padding(horizontal = 2.dp),
    )
}
