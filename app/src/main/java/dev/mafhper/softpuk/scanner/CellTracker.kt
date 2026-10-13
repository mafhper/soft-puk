package dev.mafhper.softpuk.scanner

import kotlin.math.abs

/** Uma célula lida, com a caixa (em coordenadas da imagem) para desenhar sobre o preview. */
internal data class OcrLine(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val locked: Boolean,
)

/** Código detectado num quadro, com a caixa e a região (matriz à esquerda / sequências à direita). */
internal data class CellDetection(
    val text: String,
    val cx: Float,
    val cy: Float,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val isLeftRegion: Boolean,
)

/** Leituras idênticas consecutivas para TRAVAR uma célula (≈ ~3 s no ritmo atual). */
private const val LOCK_HITS = 5

/** Quadros sem ver a célula até ela sumir do DESENHO (a leitura continua guardada). */
private const val HIDE_AFTER_FRAMES = 8L

/** Quadros sem ver a célula até DESCARTA-LA de vez (travadas inclusas). */
private const val DROP_AFTER_FRAMES = 20L

/** Leituras divergentes CONSECUTIVAS na mesma posição para uma travada ceder o lugar. */
private const val CONFLICT_HITS = 3

/**
 * Rastreia células entre quadros.
 * - Leitura igual N vezes consecutivas na mesma posição TRAVA a célula.
 * - Célula casa por POSIÇÃO (tolerância): no máximo UMA célula por posição. Uma
 *   travada que recebe conteúdo divergente por [CONFLICT_HITS] quadros seguidos
 *   CEDE o lugar (antes, a divergência criava uma segunda célula no mesmo lugar —
 *   era isso que produzia grade de mais de 6 colunas na troca de puzzle).
 * - A posição da célula acompanha o quadro (o overlay anda junto com a tela).
 * - Sem ser vista: some do desenho (~2 s) e é descartada (~5 s) — travadas inclusas.
 */
internal class CellTracker {
    private class Cell(
        var text: String,
        var hits: Int,
        var cx: Float,
        var cy: Float,
        var left: Int,
        var top: Int,
        var right: Int,
        var bottom: Int,
        val isLeftRegion: Boolean,
        var locked: Boolean,
        var lastSeen: Long,
        var conflicts: Int,
    )

    private val cells = mutableListOf<Cell>()
    private var frame = 0L

    /** Descarta TODAS as células (recuperação manual: leitura presa/misturada). */
    fun reset() {
        cells.clear()
        frame = 0
    }

    fun update(
        detections: List<CellDetection>,
        tolX: Float,
        tolY: Float,
    ): Pair<List<OcrLine>, List<OcrLine>> {
        frame++
        // IMPORTANTE: `used` acompanha o tamanho de `cells` (que cresce no loop).
        val used = MutableList(cells.size) { false }
        for (d in detections) {
            var bestIndex = -1
            var bestDist = Float.MAX_VALUE
            for (i in cells.indices) {
                if (used[i]) continue
                val c = cells[i]
                if (c.isLeftRegion != d.isLeftRegion) continue
                val dx = abs(c.cx - d.cx)
                val dy = abs(c.cy - d.cy)
                if (dx < tolX && dy < tolY) {
                    val dist = dx + dy
                    if (dist < bestDist) {
                        bestDist = dist
                        bestIndex = i
                    }
                }
            }
            if (bestIndex >= 0) {
                used[bestIndex] = true
                val c = cells[bestIndex]
                c.lastSeen = frame
                // posição SEMPRE acompanha o quadro; conteúdo só muda enquanto solta —
                // ou quando uma travada acumula evidência de que o conteúdo mudou.
                c.cx = d.cx
                c.cy = d.cy
                c.left = d.left
                c.top = d.top
                c.right = d.right
                c.bottom = d.bottom
                if (!c.locked) {
                    if (d.text == c.text) {
                        c.hits++
                    } else {
                        c.text = d.text
                        c.hits = 1
                    }
                    c.conflicts = 0
                    if (c.hits >= LOCK_HITS) c.locked = true
                } else if (d.text == c.text) {
                    c.conflicts = 0
                } else {
                    c.conflicts++
                    if (c.conflicts >= CONFLICT_HITS) {
                        // cede o lugar: conteúdo novo entra solto e precisa re-travar
                        c.text = d.text
                        c.hits = 1
                        c.locked = false
                        c.conflicts = 0
                    }
                }
            } else {
                used.add(true)
                cells.add(
                    Cell(
                        text = d.text,
                        hits = 1,
                        cx = d.cx,
                        cy = d.cy,
                        left = d.left,
                        top = d.top,
                        right = d.right,
                        bottom = d.bottom,
                        isLeftRegion = d.isLeftRegion,
                        locked = false,
                        lastSeen = frame,
                        conflicts = 0,
                    ),
                )
            }
        }

        // desenha só o que foi visto recentemente; o resto some do overlay.
        val visible = cells.filter { frame - it.lastSeen <= HIDE_AFTER_FRAMES }
        // descarta o que sumiu por completo (a leitura não é eterna).
        cells.removeAll { frame - it.lastSeen > DROP_AFTER_FRAMES }

        val left = visible
            .filter { it.isLeftRegion }
            .map { OcrLine(it.text, it.left, it.top, it.right, it.bottom, it.locked) }
            .sortedBy { it.top }
        val right = visible
            .filter { !it.isLeftRegion }
            .map { OcrLine(it.text, it.left, it.top, it.right, it.bottom, it.locked) }
            .sortedBy { it.top }
        return left to right
    }
}
