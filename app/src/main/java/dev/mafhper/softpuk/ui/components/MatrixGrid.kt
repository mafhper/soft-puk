package dev.mafhper.softpuk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mafhper.softpuk.model.BreachCode

private enum class CellState { PLAIN, DONE, NEXT, FUTURE }

/** Em telas largas a célula para de crescer (mesma densidade do desenho original). */
private val MaxCellWidth = 56.dp

/** Respiro entre células (a célula carrega metade em cada lado). */
private val CellGap = 2.dp

@Composable
fun MatrixGrid(
    matrix: List<List<BreachCode>>,
    modifier: Modifier = Modifier,
    order: List<Pair<Int, Int>> = emptyList(),
    step: Int = Int.MAX_VALUE,
    nextAccent: Color? = null,
    onCellClick: ((Int, Int) -> Unit)? = null,
) {
    val cols = matrix.maxOfOrNull { it.size } ?: 0
    if (matrix.isEmpty() || cols == 0) return

    // A célula encolhe para a grade INTEIRA caber na largura disponível — em
    // retrato, 6 colunas deixavam de fechar a tela e a última coluna sumia.
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val cellWidth = minOf(MaxCellWidth, maxWidth / cols - CellGap * 2)
            .coerceAtLeast(24.dp)
        val cellHeight = cellWidth * 48f / 56f
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            matrix.forEachIndexed { r, row ->
                Row {
                    row.forEachIndexed { c, code ->
                        val position = order
                            .indexOfFirst { it.first == r && it.second == c }
                            .takeIf { it >= 0 }
                            ?.plus(1)
                        val state = when {
                            position == null -> CellState.PLAIN
                            position <= step -> CellState.DONE
                            position == step + 1 && nextAccent != null -> CellState.NEXT
                            else -> CellState.FUTURE
                        }
                        val clickHandler: (() -> Unit)? = onCellClick?.let { cb -> { cb(r, c) } }
                        MatrixCell(
                            code = code,
                            number = position,
                            state = state,
                            nextAccent = nextAccent ?: MaterialTheme.colorScheme.primary,
                            width = cellWidth,
                            height = cellHeight,
                            onClick = clickHandler,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatrixCell(
    code: BreachCode,
    number: Int?,
    state: CellState,
    nextAccent: Color,
    width: Dp,
    height: Dp,
    onClick: (() -> Unit)?,
) {
    val primary = MaterialTheme.colorScheme.primary
    val borderColor = when (state) {
        CellState.PLAIN -> MaterialTheme.colorScheme.outline
        CellState.DONE -> primary
        CellState.NEXT -> nextAccent
        CellState.FUTURE -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    }
    val borderWidth = if (state == CellState.NEXT) 2.dp else 1.dp
    val numberColor = when (state) {
        CellState.PLAIN -> Color.Transparent
        CellState.DONE -> primary
        CellState.NEXT -> nextAccent
        CellState.FUTURE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val codeColor = when (state) {
        CellState.FUTURE -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    val backgroundTint = if (state == CellState.NEXT) {
        nextAccent.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    // Tipografia acompanha a célula (em telas estreitas o par número+código ainda cabe).
    val codeSize = (width.value * 16f / 56f).coerceIn(11f, 16f).sp
    val numberSize = (width.value * 11f / 56f).coerceIn(8f, 11f).sp
    Box(
        modifier = Modifier
            .padding(CellGap)
            .size(width = width, height = height)
            .border(borderWidth, borderColor, RoundedCornerShape(3.dp))
            .background(backgroundTint, RoundedCornerShape(3.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (number != null && state != CellState.PLAIN) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = numberSize,
                    color = numberColor,
                )
            }
            Text(
                text = code.label,
                style = MaterialTheme.typography.titleMedium,
                fontSize = codeSize,
                color = codeColor,
            )
        }
    }
}
