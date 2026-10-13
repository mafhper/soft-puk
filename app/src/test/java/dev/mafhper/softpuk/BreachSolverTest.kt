package dev.mafhper.softpuk

import dev.mafhper.softpuk.model.BreachCode
import dev.mafhper.softpuk.model.BreachPuzzle
import dev.mafhper.softpuk.model.Daemon
import dev.mafhper.softpuk.model.DemoPuzzle
import dev.mafhper.softpuk.model.containsRun
import dev.mafhper.softpuk.solver.BreachSolver
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BreachSolverTest {

    // ---------- oráculo independente (força bruta) ----------

    private fun containsRunOracle(buffer: List<BreachCode>, seq: List<BreachCode>): Boolean {
        if (seq.size > buffer.size) return false
        return (0..buffer.size - seq.size).any { start ->
            seq.indices.all { j -> buffer[start + j] == seq[j] }
        }
    }

    private fun allBuffers(matrix: List<List<BreachCode>>, bufferSize: Int): List<List<BreachCode>> {
        val rows = matrix.size
        val cols = matrix.first().size
        val out = mutableListOf<List<BreachCode>>()
        val buffer = ArrayList<BreachCode>()
        val used = HashSet<Int>()

        fun dfs(rowMode: Boolean, line: Int) {
            out.add(buffer.toList())
            if (buffer.size >= bufferSize) return
            if (rowMode) {
                for (c in 0 until cols) {
                    val idx = line * cols + c
                    if (idx in used) continue
                    buffer.add(matrix[line][c])
                    used.add(idx)
                    dfs(rowMode = false, line = c)
                    used.remove(idx)
                    buffer.removeAt(buffer.size - 1)
                }
            } else {
                for (r in 0 until rows) {
                    val idx = r * cols + line
                    if (idx in used) continue
                    buffer.add(matrix[r][line])
                    used.add(idx)
                    dfs(rowMode = true, line = r)
                    used.remove(idx)
                    buffer.removeAt(buffer.size - 1)
                }
            }
        }

        dfs(rowMode = true, line = 0)
        return out
    }

    /**
     * Objetivo do solver: MÁXIMO DE DAEMONS primeiro; empate pelo peso 2^índice
     * (o último vale mais). O conjunto atingido identifica (quantidade, peso) de
     * forma única (soma de potências de 2 = bitmask).
     */
    private fun oracleBest(puzzle: BreachPuzzle, enabled: Set<Int>): Set<Int> {
        val byObjective = compareBy<Set<Int>>(
            { set -> set.size },
            { set -> set.sumOf { 1 shl it } },
        )
        return allBuffers(puzzle.matrix, puzzle.bufferSize)
            .map { buffer ->
                enabled.filter { containsRunOracle(buffer, puzzle.daemons[it].codes) }.toSet()
            }
            .maxWith(byObjective)
    }

    /** Valida as regras de seleção: 1ª na linha 0; alterna coluna/linha; sem reuso; ≤ buffer. */
    private fun validatePicks(
        matrix: List<List<BreachCode>>,
        bufferSize: Int,
        picks: List<Pair<Int, Int>>,
    ): Boolean {
        if (picks.size > bufferSize) return false
        val used = HashSet<Pair<Int, Int>>()
        var rowMode = true
        var line = 0
        for ((r, c) in picks) {
            if (r !in matrix.indices || c !in matrix[0].indices) return false
            if (!used.add(r to c)) return false
            if (rowMode && r != line) return false
            if (!rowMode && c != line) return false
            rowMode = !rowMode
            line = if (rowMode) r else c
        }
        return true
    }

    // ---------- testes ----------

    @Test
    fun demoPuzzleAchievesAllDaemons() {
        val puzzle = BreachPuzzle(DemoPuzzle.matrix, DemoPuzzle.bufferSize, DemoPuzzle.daemons)
        val sol = BreachSolver.solve(puzzle)
        assertEquals(7, sol.score)
        assertEquals(setOf(0, 1, 2), sol.achieved)
        assertTrue(validatePicks(puzzle.matrix, puzzle.bufferSize, sol.picks))
        assertEquals(sol.picks.map { puzzle.matrix[it.first][it.second] }, sol.buffer)
    }

    @Test
    fun containsRunRequiresContiguity() {
        val a = listOf(BreachCode.C1C, BreachCode.C1C, BreachCode.C1C)
        assertTrue(a.containsRun(listOf(BreachCode.C1C, BreachCode.C1C)))
        val b = listOf(BreachCode.C1C, BreachCode.C55, BreachCode.C1C)
        assertFalse(b.containsRun(listOf(BreachCode.C1C, BreachCode.C1C)))
    }

    @Test
    fun subsequenceIsNotEnough() {
        val matrix = listOf(
            listOf(BreachCode.C1C, BreachCode.C55),
            listOf(BreachCode.C55, BreachCode.C1C),
        )
        val impossible = BreachPuzzle(
            matrix,
            4,
            listOf(Daemon("D", listOf(BreachCode.C1C, BreachCode.C1C))),
        )
        assertEquals(0, BreachSolver.solve(impossible).score)

        val possible = BreachPuzzle(
            matrix,
            4,
            listOf(Daemon("E", listOf(BreachCode.C55, BreachCode.C1C))),
        )
        assertTrue(BreachSolver.solve(possible).score > 0)
    }

    @Test
    fun weightsPreferTheLastDaemonOnConflict() {
        val matrix = listOf(
            listOf(BreachCode.C1C, BreachCode.C55),
            listOf(BreachCode.C55, BreachCode.C1C),
        )
        val daemons = listOf(
            Daemon("V1", listOf(BreachCode.C1C, BreachCode.C55)), // peso 1
            Daemon("V2", listOf(BreachCode.C55, BreachCode.C1C)), // peso 2
        )
        val sol = BreachSolver.solve(BreachPuzzle(matrix, 2, daemons))
        assertEquals(2, sol.score)
        assertEquals(setOf(1), sol.achieved)
    }

    @Test
    fun maxDaemonsCountBeatsSingleHeavierDaemon() {
        // Buffer 3 comporta UM caminho que completa V1+V2 (2 daemons, nota 1+2=3)
        // e outro que completa só V3 (1 daemon, nota 4).
        // Máximo de daemons primeiro → V1+V2 vence, mesmo com nota menor.
        val matrix = listOf(
            listOf(BreachCode.CBD, BreachCode.C7A, BreachCode.C1C),
            listOf(BreachCode.C55, BreachCode.C1C, BreachCode.CE9),
            listOf(BreachCode.C7A, BreachCode.CBD, BreachCode.C55),
        )
        val daemons = listOf(
            Daemon("V1", listOf(BreachCode.C1C, BreachCode.C55)),
            Daemon("V2", listOf(BreachCode.C55, BreachCode.CBD)),
            Daemon("V3", listOf(BreachCode.CBD, BreachCode.C55, BreachCode.C1C)),
        )
        val sol = BreachSolver.solve(BreachPuzzle(matrix, 3, daemons))
        assertEquals(setOf(0, 1), sol.achieved)
        assertEquals(3, sol.score)
        assertEquals(listOf(BreachCode.C1C, BreachCode.C55, BreachCode.CBD), sol.buffer)
    }

    @Test
    fun bufferTooSmallCannotComplete() {
        val matrix = listOf(
            listOf(BreachCode.C1C, BreachCode.C55),
            listOf(BreachCode.C55, BreachCode.C1C),
        )
        val daemon = Daemon("D", listOf(BreachCode.C55, BreachCode.C1C, BreachCode.C55))
        assertEquals(0, BreachSolver.solve(BreachPuzzle(matrix, 2, listOf(daemon))).score)
    }

    @Test
    fun disabledDaemonsAreIgnored() {
        val puzzle = BreachPuzzle(
            DemoPuzzle.matrix,
            DemoPuzzle.bufferSize,
            DemoPuzzle.daemons.map { it.copy(enabled = false) },
        )
        val sol = BreachSolver.solve(puzzle, enabled = emptySet())
        assertEquals(0, sol.score)
        assertTrue(sol.picks.isEmpty())
    }

    @Test
    fun solverMatchesBruteForceOracleOnRandomCases() {
        val rnd = Random(42)
        repeat(60) { case ->
            val n = if (rnd.nextBoolean()) 5 else 6
            val matrix = List(n) { List(n) { BreachCode.entries[rnd.nextInt(BreachCode.entries.size)] } }
            val bufferSize = 4 + rnd.nextInt(5) // 4..8
            val daemons = List(2 + rnd.nextInt(3)) { i ->
                Daemon(
                    "D$i",
                    List(2 + rnd.nextInt(3)) { BreachCode.entries[rnd.nextInt(BreachCode.entries.size)] },
                )
            }
            val puzzle = BreachPuzzle(matrix, bufferSize, daemons)
            val enabled = daemons.indices.toSet()
            val sol = BreachSolver.solve(puzzle, enabled)
            val expectedBest = oracleBest(puzzle, enabled)
            assertEquals("achieved divergente no caso $case", expectedBest, sol.achieved)
            assertEquals(
                "score divergente no caso $case",
                expectedBest.sumOf { 1 shl it },
                sol.score,
            )
            assertTrue("picks invalidos no caso $case", validatePicks(matrix, bufferSize, sol.picks))
        }
    }
}
