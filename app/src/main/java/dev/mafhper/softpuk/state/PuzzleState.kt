package dev.mafhper.softpuk.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.mafhper.softpuk.model.BreachCode
import dev.mafhper.softpuk.model.BreachPuzzle
import dev.mafhper.softpuk.model.Daemon
import dev.mafhper.softpuk.model.DemoPuzzle
import dev.mafhper.softpuk.solver.BreachSolution
import dev.mafhper.softpuk.solver.BreachSolver

/**
 * Estado editável do puzzle (Fatia 1: entrada manual). Simples de propósito —
 * um holder de estado do Compose; a câmera (Fatias 2–3) vai preencher o mesmo estado.
 */
class PuzzleState {
    var matrix by mutableStateOf(DemoPuzzle.matrix)
        private set

    var bufferSize by mutableStateOf(DemoPuzzle.bufferSize)
        private set

    var daemons by mutableStateOf(DemoPuzzle.daemons)
        private set

    var solution by mutableStateOf<BreachSolution?>(null)
        private set

    fun cycleCell(row: Int, col: Int) {
        matrix = matrix.mapIndexed { r, rowCodes ->
            if (r != row) {
                rowCodes
            } else {
                rowCodes.mapIndexed { c, code ->
                    if (c != col) code else code.next()
                }
            }
        }
    }

    fun setMatrixSize(size: Int) {
        if (size == matrix.size) return
        matrix = List(size) { r ->
            List(size) { c ->
                matrix.getOrNull(r)?.getOrNull(c) ?: BreachCode.C1C
            }
        }
    }

    fun updateBufferSize(size: Int) {
        bufferSize = size.coerceIn(4, 10)
    }

    fun cycleDaemonCode(di: Int, ci: Int) {
        daemons = daemons.mapIndexed { i, daemon ->
            if (i != di) {
                daemon
            } else {
                daemon.copy(
                    codes = daemon.codes.mapIndexed { j, code ->
                        if (j != ci) code else code.next()
                    },
                )
            }
        }
    }

    fun toggleDaemon(di: Int) {
        daemons = daemons.mapIndexed { i, daemon ->
            if (i != di) daemon else daemon.copy(enabled = !daemon.enabled)
        }
    }

    fun setDaemonCount(count: Int) {
        val n = count.coerceIn(2, 4)
        daemons = when {
            n == daemons.size -> daemons
            n < daemons.size -> daemons.take(n)
            else -> daemons + (daemons.size until n).map { i ->
                Daemon("DATAMINE_V${i + 1}", listOf(BreachCode.C1C, BreachCode.C55))
            }
        }
    }

    /** Carrega um puzzle lido pela mira (Fatia 3) e deixa pronto para confirmar/editar. */
    fun loadParsed(matrix: List<List<BreachCode>>, daemons: List<List<BreachCode>>) {
        if (matrix.isNotEmpty()) {
            this.matrix = matrix
        }
        if (daemons.isNotEmpty()) {
            this.daemons = daemons.mapIndexed { i, codes ->
                Daemon("DATAMINE_V${i + 1}", codes)
            }
        }
        solution = null
    }

    fun solve() {
        val enabled = daemons.indices.filter { daemons[it].enabled }.toSet()
        solution = BreachSolver.solve(
            puzzle = BreachPuzzle(matrix = matrix, bufferSize = bufferSize, daemons = daemons),
            enabled = enabled,
        )
    }
}
