package dev.mafhper.softpuk.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Painel de daemon usado nas telas de Confirmação e Solução — MESMO componente,
 * MESMO desenho nas duas (titulo + conteudo):
 *
 *   ┌─────────────────────────────┐
 *   │ [x] DATAMINE_V3             │  <- cabeçalho (estado + nome)
 *   │  1C  7A  BD  1C             │  <- sequência em chips uniformes
 *   └─────────────────────────────┘
 *
 * - [checked]: `[x]` ligado/concluído; `[ ]` desligado/pendente.
 * - [done]: concluído no passo atual (verde), usado na Solução.
 * - [dim]: daemon fora do jogo (não habilitado) — apagado nas duas telas.
 * - [onToggle]/[onCodeClick]: só a Confirmação passa callbacks (edição);
 *   na Solução o painel é passivo.
 */
@Composable
fun DaemonPanel(
    name: String,
    codes: List<String>,
    checked: Boolean,
    modifier: Modifier = Modifier,
    done: Boolean = false,
    dim: Boolean = false,
    onToggle: (() -> Unit)? = null,
    onCodeClick: ((Int) -> Unit)? = null,
) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val indicatorColor = when {
        dim -> outline
        checked || done -> primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val nameColor = when {
        dim -> outline
        done -> primary
        else -> MaterialTheme.colorScheme.onBackground
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = outline.copy(alpha = if (dim) 0.3f else 0.55f),
                shape = RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = if (onToggle != null) {
                Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
            } else {
                Modifier.fillMaxWidth()
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (checked) "[x]" else "[ ]",
                style = MaterialTheme.typography.labelLarge,
                color = indicatorColor,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall.copy(letterSpacing = 1.sp),
                color = nameColor,
            )
        }
        Spacer(Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            codes.forEachIndexed { j, label ->
                DaemonCodeChip(
                    label = label,
                    textColor = when {
                        dim -> outline
                        done || onCodeClick != null -> primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    borderColor = when {
                        done -> primary
                        dim -> outline.copy(alpha = 0.3f)
                        else -> outline.copy(alpha = 0.55f)
                    },
                    onClick = onCodeClick?.let { cb -> { cb(j) } },
                )
            }
        }
    }
}

@Composable
private fun DaemonCodeChip(
    label: String,
    textColor: Color,
    borderColor: Color,
    onClick: (() -> Unit)?,
) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 40.dp, minHeight = 30.dp)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            maxLines = 1,
            softWrap = false,
        )
    }
}
