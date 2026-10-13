package dev.mafhper.softpuk

import dev.mafhper.softpuk.scanner.CellDetection
import dev.mafhper.softpuk.scanner.CellTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CellTrackerTest {

    private val tolX = 40f
    private val tolY = 40f

    private fun det(text: String, cx: Float, cy: Float, left: Boolean = true) = CellDetection(
        text = text,
        cx = cx,
        cy = cy,
        left = (cx - 20f).toInt(),
        top = (cy - 10f).toInt(),
        right = (cx + 20f).toInt(),
        bottom = (cy + 10f).toInt(),
        isLeftRegion = left,
    )

    /** Grade de 6 colunas com códigos próprios (simula um puzzle na mira). */
    private fun row(codes: List<String>): List<CellDetection> =
        codes.mapIndexed { i, code -> det(code, 100f + i * 90f, 100f) }

    @Test
    fun cellLocksAfterFiveIdenticalReads() {
        val tracker = CellTracker()
        var lines = tracker.update(listOf(det("BD", 100f, 100f)), tolX, tolY).first
        assertFalse(lines[0].locked)
        repeat(4) { lines = tracker.update(listOf(det("BD", 100f, 100f)), tolX, tolY).first }
        assertTrue(lines[0].locked)
    }

    @Test
    fun briefMissKeepsLockedCellVisible() {
        val tracker = CellTracker()
        repeat(6) { tracker.update(listOf(det("BD", 100f, 100f)), tolX, tolY) }
        tracker.update(emptyList(), tolX, tolY)
        val lines = tracker.update(listOf(det("BD", 100f, 100f)), tolX, tolY).first
        assertEquals(1, lines.size)
        assertTrue(lines[0].locked)
    }

    @Test
    fun alternatingNoiseDoesNotBreakLock() {
        val tracker = CellTracker()
        repeat(6) { tracker.update(listOf(det("BD", 100f, 100f)), tolX, tolY) }
        var lines = tracker.update(listOf(det("BD", 100f, 100f)), tolX, tolY).first
        assertEquals("BD", lines[0].text)
        // "8D" é leitura típica sob glare — alternada, nunca vence 3 conflitos seguidos.
        repeat(4) {
            tracker.update(listOf(det("8D", 100f, 100f)), tolX, tolY)
            lines = tracker.update(listOf(det("BD", 100f, 100f)), tolX, tolY).first
        }
        assertEquals("BD", lines[0].text)
        assertTrue(lines[0].locked)
    }

    @Test
    fun conflictingReadAtSamePositionReplacesInsteadOfDuplicating() {
        val tracker = CellTracker()
        repeat(6) { tracker.update(listOf(det("BD", 100f, 100f)), tolX, tolY) }

        // Leitura divergente e CONSECUTIVA na mesma posição (troca de puzzle/glare forte):
        // a célula cede o lugar — nunca vira uma segunda coluna no mesmo lugar.
        var lines = tracker.update(listOf(det("1C", 100f, 100f)), tolX, tolY).first
        repeat(2) { lines = tracker.update(listOf(det("1C", 100f, 100f)), tolX, tolY).first }

        assertEquals(1, lines.size)
        assertEquals("1C", lines[0].text)

        // Conteúdo adotado precisa de NOVA evidência para travar.
        repeat(5) { lines = tracker.update(listOf(det("1C", 100f, 100f)), tolX, tolY).first }
        assertTrue(lines[0].locked)
    }

    @Test
    fun puzzleSwitchNeverYieldsMoreThanSixColumns() {
        val tracker = CellTracker()
        val puzzleA = listOf("1C", "55", "BD", "E9", "FF", "7A")
        val puzzleB = listOf("FF", "BD", "1C", "7A", "55", "E9")

        repeat(6) { tracker.update(row(puzzleA), tolX, tolY) }

        var maxColumns = 0
        repeat(10) {
            val lines = tracker.update(row(puzzleB), tolX, tolY).first
            maxColumns = maxOf(maxColumns, lines.size)
        }

        assertEquals("troca de puzzle não pode duplicar colunas", 6, maxColumns)
        val final = tracker.update(row(puzzleB), tolX, tolY).first
        assertEquals(puzzleB, final.map { it.text })
    }

    @Test
    fun resetDropsEverythingIncludingLocks() {
        val tracker = CellTracker()
        repeat(6) { tracker.update(listOf(det("BD", 100f, 100f)), tolX, tolY) }

        tracker.reset()

        val empty = tracker.update(emptyList(), tolX, tolY).first
        assertEquals(0, empty.size)
        // a célula reaparece SOLTA: a limpeza apaga a memória, não trava nada.
        val fresh = tracker.update(listOf(det("BD", 100f, 100f)), tolX, tolY).first
        assertEquals(1, fresh.size)
        assertFalse(fresh[0].locked)
    }
}
