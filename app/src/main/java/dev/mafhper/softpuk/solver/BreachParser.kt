package dev.mafhper.softpuk.solver

import dev.mafhper.softpuk.model.BreachCode
import kotlin.math.abs

/** Célula travada pronta para virar puzzle (coordenadas em pixels da imagem). */
data class ParsedCell(
    val text: String,
    val centerX: Float,
    val centerY: Float,
    val regionLeft: Boolean,
)

data class ParsedPuzzle(
    val matrix: List<List<BreachCode>>,
    val daemons: List<List<BreachCode>>,
)

/**
 * Reconstrói matriz e daemons a partir das células TRAVADAS (Fatia 3):
 * agrupa por proximidade vertical (linhas), ordena por X dentro da linha e
 * normaliza cada token contra os 6 códigos do jogo com tolerância de 1 edição.
 */
object BreachParser {

    fun codeFor(token: String): BreachCode? {
        val t = token.uppercase()
        BreachCode.entries.firstOrNull { it.label == t }?.let { return it }
        val best = BreachCode.entries.minByOrNull { hamming(it.label, t) } ?: return null
        return best.takeIf { hamming(it.label, t) <= 1 }
    }

    fun parse(cells: List<ParsedCell>, rowTolerance: Float): ParsedPuzzle? {
        val left = rows(cells.filter { it.regionLeft }, rowTolerance)
        val right = rows(cells.filter { !it.regionLeft }, rowTolerance)

        val matrix = left
            .map { row -> row.mapNotNull { codeFor(it.text) } }
            .filter { it.size >= 2 }
        if (matrix.isEmpty()) return null
        val width = matrix.maxOf { it.size }
        val padded = matrix.map { row -> row + List(width - row.size) { BreachCode.C1C } }

        val daemons = right
            .map { row -> row.mapNotNull { codeFor(it.text) } }
            .filter { it.size >= 2 }

        return ParsedPuzzle(matrix = padded, daemons = daemons)
    }

    private fun rows(cells: List<ParsedCell>, rowTolerance: Float): List<List<ParsedCell>> {
        if (cells.isEmpty()) return emptyList()
        val sorted = cells.sortedBy { it.centerY }
        val rows = mutableListOf<MutableList<ParsedCell>>()
        for (cell in sorted) {
            val row = rows.lastOrNull()
            val ref = row?.lastOrNull()
            if (row != null && ref != null && abs(cell.centerY - ref.centerY) <= rowTolerance) {
                row.add(cell)
            } else {
                rows.add(mutableListOf(cell))
            }
        }
        return rows.map { row -> row.sortedBy { c -> c.centerX } }
    }

    private fun hamming(a: String, b: String): Int {
        if (a.length != b.length) return maxOf(a.length, b.length)
        var d = 0
        for (i in a.indices) if (a[i] != b[i]) d++
        return d
    }
}
