package dev.mafhper.softpuk

import dev.mafhper.softpuk.model.BreachCode
import dev.mafhper.softpuk.model.DEFAULT_BUFFER_SIZE
import dev.mafhper.softpuk.model.DemoPuzzle
import dev.mafhper.softpuk.state.PuzzleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PuzzleStateTest {

    @Test
    fun startsAtDefaultFourSlotBuffer() {
        assertEquals(4, DEFAULT_BUFFER_SIZE)
        assertEquals(DEFAULT_BUFFER_SIZE, PuzzleState().bufferSize)
    }

    @Test
    fun loadingNewPuzzleResetsBufferToDefault() {
        val state = PuzzleState()
        state.updateBufferSize(8)

        state.loadParsed(
            matrix = DemoPuzzle.matrix,
            daemons = listOf(listOf(BreachCode.C1C, BreachCode.C55)),
        )

        assertEquals(DEFAULT_BUFFER_SIZE, state.bufferSize)
    }

    @Test
    fun loadDemoResetsGridBufferDaemonsAndSolution() {
        val state = PuzzleState()
        state.setMatrixSize(6)
        state.updateBufferSize(9)
        state.setDaemonCount(2)
        state.cycleCell(0, 0)
        state.solve()

        state.loadDemo()

        assertEquals(DemoPuzzle.matrix, state.matrix)
        assertEquals(DemoPuzzle.daemons, state.daemons)
        assertEquals(DEFAULT_BUFFER_SIZE, state.bufferSize)
        assertNull(state.solution)
    }

    @Test
    fun demoPuzzleClosesAllDaemonsWithinDefaultBuffer() {
        val state = PuzzleState()

        state.solve()

        assertEquals(DEFAULT_BUFFER_SIZE, state.bufferSize)
        val solution = requireNotNull(state.solution)
        assertEquals(3, solution.achieved.size)
    }
}
