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

/** Maior grade do jogo (Breach Protocol) — o que passar disso é artefato de leitura. */
const val MAX_GRID_SIZE = 6

/**
 * Leitura plausível para confirmar: grade de 2..6 linhas/colunas com daemons.
 * Blinda a confirmação contra artefatos de rastreio (ex.: coluna duplicada na
 * troca de puzzle, que produzia grade de 7+ colunas).
 */
fun ParsedPuzzle.isPlausible(): Boolean =
    matrix.size in 2..MAX_GRID_SIZE &&
        matrix.all { row -> row.size in 2..MAX_GRID_SIZE } &&
        daemons.isNotEmpty()

/**
 * Reconstrói matriz e daemons a partir das células TRAVADAS (Fatia 3):
 * agrupa por proximidade vertical (linhas), ordena por X dentro da linha e
 * normaliza cada token contra os 6 códigos do jogo com tolerância de 1 edição.
 */
object BreachParser {

    /**
     * Palavras do chrome do jogo (PT) que passam no filtro hexadecimal mas não são
     * códigos: "TEMPO RESTANTE DA BRECHA" ("DA") e "MATRIZ DE CÓDIGOS" ("DE").
     * Sem o filtro, "DA" normaliza para 7A (1 edição) e suja grade/overlay.
     */
    private val NON_CODE_WORDS = setOf("DA", "DE")

    /** Token com cara de código do jogo: par hex com letra (ou 55), sem as palavras do chrome. */
    fun looksLikeCodeToken(token: String): Boolean =
        token.length == 2 &&
            (token == "55" || token.any { it in 'A'..'F' }) &&
            token !in NON_CODE_WORDS

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
