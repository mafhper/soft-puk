package dev.mafhper.softpuk

import dev.mafhper.softpuk.model.BreachCode
import dev.mafhper.softpuk.solver.BreachParser
import dev.mafhper.softpuk.solver.ParsedCell
import dev.mafhper.softpuk.solver.ParsedPuzzle
import dev.mafhper.softpuk.solver.isPlausible
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BreachParserTest {

    @Test
    fun fuzzyMapsToNearestCode() {
        assertEquals(BreachCode.C1C, BreachParser.codeFor("1C"))
        assertEquals(BreachCode.C1C, BreachParser.codeFor("10")) // leitura típica sob glare
        assertEquals(BreachCode.CBD, BreachParser.codeFor("8D"))
        assertEquals(BreachCode.CE9, BreachParser.codeFor("E8"))
        assertEquals(BreachCode.C55, BreachParser.codeFor("55"))
        assertNull(BreachParser.codeFor("ZZ"))
        assertNull(BreachParser.codeFor("QW"))
    }

    @Test
    fun chromeWordsAreNotCodeTokens() {
        // "TEMPO RESTANTE DA BRECHA" e "MATRIZ DE CÓDIGOS" — títulos do jogo.
        assertFalse("DA normalizaria para 7A", BreachParser.looksLikeCodeToken("DA"))
        assertFalse("DE viraria célula no overlay", BreachParser.looksLikeCodeToken("DE"))
        assertTrue(BreachParser.looksLikeCodeToken("7A"))
        assertTrue(BreachParser.looksLikeCodeToken("BD"))
        assertTrue(BreachParser.looksLikeCodeToken("55"))
        assertTrue(BreachParser.looksLikeCodeToken("E9"))
        assertFalse(BreachParser.looksLikeCodeToken("ZZ"))
        assertFalse(BreachParser.looksLikeCodeToken("12"))
    }

    @Test
    fun parsesMatrixRowsAndDaemons() {
        val cells = mutableListOf<ParsedCell>()

        val matrixCodes = listOf(
            listOf("1C", "1C", "1C", "1C", "1C"),
            listOf("E9", "E9", "E9", "E9", "1C"),
            listOf("1C", "1C", "BD", "1C", "1C"),
            listOf("E9", "1C", "55", "55", "E9"),
            listOf("BD", "BD", "E9", "55", "1C"),
        )
        matrixCodes.forEachIndexed { r, row ->
            row.forEachIndexed { c, token ->
                cells.add(
                    ParsedCell(
                        text = token,
                        centerX = 100f + c * 80f + if (c % 2 == 0) 2f else -2f,
                        centerY = 100f + r * 50f + if (r % 2 == 0) 3f else -3f,
                        regionLeft = true,
                    ),
                )
            }
        }

        val daemonRows = listOf(
            listOf("BD", "E9"),
            listOf("1C", "1C"),
            listOf("1C", "E9", "E9", "E9"),
        )
        daemonRows.forEachIndexed { r, row ->
            row.forEachIndexed { c, token ->
                cells.add(
                    ParsedCell(
                        text = token,
                        centerX = 900f + c * 80f,
                        centerY = 120f + r * 60f,
                        regionLeft = false,
                    ),
                )
            }
        }

        // ruído que não mapeia para nenhum código — deve sumir
        cells.add(ParsedCell("ZZ", 500f, 600f, regionLeft = true))

        val parsed = BreachParser.parse(cells, rowTolerance = 25f)
        requireNotNull(parsed)
        assertEquals(5, parsed.matrix.size)
        assertEquals(5, parsed.matrix[0].size)
        assertEquals(BreachCode.CBD, parsed.matrix[2][2])
        assertEquals(BreachCode.C55, parsed.matrix[3][2])
        assertEquals(3, parsed.daemons.size)
        assertEquals(listOf(BreachCode.CBD, BreachCode.CE9), parsed.daemons[0])
        assertEquals(4, parsed.daemons[2].size)
    }

    @Test
    fun returnsNullWhenNothingParsable() {
        assertNull(BreachParser.parse(emptyList(), rowTolerance = 25f))
        assertNull(
            BreachParser.parse(
                listOf(ParsedCell("ZZ", 10f, 10f, regionLeft = true)),
                rowTolerance = 25f,
            ),
        )
    }

    @Test
    fun raggedRowsArePadded() {
        val cells = listOf(
            ParsedCell("1C", 100f, 100f, regionLeft = true),
            ParsedCell("1C", 180f, 100f, regionLeft = true),
            ParsedCell("E9", 100f, 150f, regionLeft = true),
            ParsedCell("E9", 180f, 150f, regionLeft = true),
            ParsedCell("BD", 260f, 150f, regionLeft = true),
        )
        val parsed = BreachParser.parse(cells, rowTolerance = 25f)
        requireNotNull(parsed)
        assertEquals(3, parsed.matrix[0].size)
        assertEquals(3, parsed.matrix[1].size)
    }

    @Test
    fun plausibilityRejectsWideOrEmptyReads() {
        fun puzzle(
            width: Int,
            rows: Int = 5,
            daemons: List<List<BreachCode>> = listOf(listOf(BreachCode.C1C)),
        ) = ParsedPuzzle(
            matrix = List(rows) { List(width) { BreachCode.C1C } },
            daemons = daemons,
        )

        assertTrue(puzzle(6).isPlausible())
        assertTrue(puzzle(5).isPlausible())
        assertFalse("7 colunas é artefato de rastreio", puzzle(7).isPlausible())
        assertFalse("7 linhas é artefato de rastreio", puzzle(5, rows = 7).isPlausible())
        assertFalse("sem daemons não confirma", puzzle(6, daemons = emptyList()).isPlausible())
        assertFalse("1 linha não é grade", puzzle(5, rows = 1).isPlausible())
    }
}
