package dev.mafhper.softpuk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mafhper.softpuk.state.PuzzleState
import dev.mafhper.softpuk.ui.components.MatrixGrid

@Composable
fun ConfirmScreen(
    state: PuzzleState,
    onBack: () -> Unit,
    onSolve: () -> Unit,
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
            text = "CONFIRMAR LEITURA",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Toque numa célula para trocar o código. Ajuste também tamanho da grade, " +
                "buffer e daemons.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        MatrixGrid(matrix = state.matrix, onCellClick = { r, c -> state.cycleCell(r, c) })

        Spacer(Modifier.height(12.dp))
        Text(
            text = "GRADE",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        Row {
            OutlinedButton(onClick = { state.setMatrixSize(5) }, enabled = state.matrix.size != 5) {
                Text("5x5")
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { state.setMatrixSize(6) }, enabled = state.matrix.size != 6) {
                Text("6x6")
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "BUFFER: ${state.bufferSize}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.width(12.dp))
            OutlinedButton(onClick = { state.updateBufferSize(state.bufferSize - 1) }) {
                Text("-")
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { state.updateBufferSize(state.bufferSize + 1) }) {
                Text("+")
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "DAEMONS",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        state.daemons.forEachIndexed { i, daemon ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { state.toggleDaemon(i) }) {
                    Text(
                        text = if (daemon.enabled) "[x]" else "[ ]",
                        color = if (daemon.enabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                Text(
                    text = daemon.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.width(8.dp))
                daemon.codes.forEachIndexed { j, code ->
                    CodeChip(code.label) { state.cycleDaemonCode(i, j) }
                    Spacer(Modifier.width(4.dp))
                }
            }
        }
        Row {
            OutlinedButton(
                onClick = { state.setDaemonCount(state.daemons.size - 1) },
                enabled = state.daemons.size > 2,
            ) {
                Text("- DAEMON")
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = { state.setDaemonCount(state.daemons.size + 1) },
                enabled = state.daemons.size < 4,
            ) {
                Text("+ DAEMON")
            }
        }

        Spacer(Modifier.height(20.dp))
        Row {
            OutlinedButton(onClick = onBack) {
                Text("VOLTAR")
            }
            Spacer(Modifier.width(12.dp))
            Button(onClick = onSolve, modifier = Modifier.weight(1f)) {
                Text("RESOLVER")
            }
        }
    }
}

@Composable
private fun CodeChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
