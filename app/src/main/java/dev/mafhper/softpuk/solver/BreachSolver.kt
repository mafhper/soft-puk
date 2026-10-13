package dev.mafhper.softpuk.solver

import dev.mafhper.softpuk.model.BreachCode
import dev.mafhper.softpuk.model.BreachPuzzle
import dev.mafhper.softpuk.model.containsRun
import dev.mafhper.softpuk.model.firstRunCompletionStep

data class BreachSolution(
    val picks: List<Pair<Int, Int>>,
    val buffer: List<BreachCode>,
    val achieved: Set<Int>,
    val firstStep: Map<Int, Int>,
    val score: Int,
)

/**
 * Busca exaustiva pelas regras do jogo (modeladas no repo yet3, spec do projeto):
 * - 1ª escolha em qualquer coluna da LINHA 0;
 * - depois alterna coluna/linha, presa à última célula escolhida; sem reutilizar célula;
 * - no máximo [BreachPuzzle.bufferSize] escolhas.
 *
 * Objetivo: maximizar o NÚMERO de daemons completados dentro do buffer
 * ("o máximo de datamine que o buffer comporta"). Empate desempata pelo peso
 * 2^índice ("o último vale mais", comparação binária do conjunto); depois menos
 * cliques; depois ordem lexicográfica determinística.
 *
 * Espaço de busca: ≤ ~10^5 caminhos no pior caso (6×6, buffer 10) — roda em ms.
 */
object BreachSolver {

    fun solve(
        puzzle: BreachPuzzle,
        enabled: Set<Int> = defaultEnabled(puzzle),
    ): BreachSolution {
        val rows = puzzle.rows
        val cols = puzzle.cols
        val targets = enabled.sorted().map { it to puzzle.daemons[it].codes }

        var bestCount = -1
        var bestScore = -1
        var bestLen = Int.MAX_VALUE
        var bestPicks: List<Pair<Int, Int>> = emptyList()
        var bestBuffer: List<BreachCode> = emptyList()

        val picks = ArrayList<Pair<Int, Int>>()
        val used = HashSet<Int>()

        fun evaluate() {
            val buffer = picks.map { puzzle.matrix[it.first][it.second] }
            var count = 0
            var score = 0
            for ((idx, codes) in targets) {
                if (buffer.containsRun(codes)) {
                    count++
                    score += 1 shl idx
                }
            }
            val len = picks.size
            val better = count > bestCount ||
                (count == bestCount && score > bestScore) ||
                (count == bestCount && score == bestScore && len < bestLen) ||
                (count == bestCount && score == bestScore && len == bestLen &&
                    lexLess(picks, bestPicks))
            if (better) {
                bestCount = count
                bestScore = score
                bestLen = len
                bestPicks = picks.toList()
                bestBuffer = buffer
            }
        }

        fun dfs(rowMode: Boolean, line: Int) {
            evaluate()
            if (picks.size >= puzzle.bufferSize) return
            if (rowMode) {
                for (c in 0 until cols) {
                    val idx = line * cols + c
                    if (idx in used) continue
                    picks.add(line to c)
                    used.add(idx)
                    dfs(rowMode = false, line = c)
                    used.remove(idx)
                    picks.removeAt(picks.size - 1)
                }
            } else {
                for (r in 0 until rows) {
                    val idx = r * cols + line
                    if (idx in used) continue
                    picks.add(r to line)
                    used.add(idx)
                    dfs(rowMode = true, line = r)
                    used.remove(idx)
                    picks.removeAt(picks.size - 1)
                }
            }
        }

        dfs(rowMode = true, line = 0)

        val achieved = targets
            .map { it.first }
            .filter { bestBuffer.containsRun(puzzle.daemons[it].codes) }
            .toSet()
        val firstStep = achieved.associateWith { idx ->
            bestBuffer.firstRunCompletionStep(puzzle.daemons[idx].codes) ?: bestBuffer.size
        }

        return BreachSolution(
            picks = bestPicks,
            buffer = bestBuffer,
            achieved = achieved,
            firstStep = firstStep,
            score = bestScore.coerceAtLeast(0),
        )
    }

    private fun defaultEnabled(puzzle: BreachPuzzle): Set<Int> =
        puzzle.daemons.indices.filter { puzzle.daemons[it].enabled }.toSet()

    private fun lexLess(a: List<Pair<Int, Int>>, b: List<Pair<Int, Int>>): Boolean {
        val n = minOf(a.size, b.size)
        for (i in 0 until n) {
            if (a[i].first != b[i].first) return a[i].first < b[i].first
            if (a[i].second != b[i].second) return a[i].second < b[i].second
        }
        return a.size < b.size
    }
}
