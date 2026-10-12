package dev.mafhper.softpuk.model

enum class BreachCode(val label: String) {
    C1C("1C"),
    C55("55"),
    C7A("7A"),
    CBD("BD"),
    CE9("E9"),
    CFF("FF");

    fun next(): BreachCode = entries[(ordinal + 1) % entries.size]
}

data class Daemon(
    val name: String,
    val codes: List<BreachCode>,
    val enabled: Boolean = true,
)

data class BreachPuzzle(
    val matrix: List<List<BreachCode>>,
    val bufferSize: Int,
    val daemons: List<Daemon>,
) {
    val rows: Int get() = matrix.size
    val cols: Int get() = matrix.first().size
}

/**
 * true se [seq] aparece como sequência CONTÍGUA (sem quebras) em algum ponto.
 * É a semântica do jogo (confirmada no modelo do repo yet3: busca de substring
 * com reset no erro — códigos certos em sequência, sem intervalo).
 */
fun List<BreachCode>.containsRun(seq: List<BreachCode>): Boolean {
    if (seq.isEmpty()) return true
    if (seq.size > size) return false
    for (start in 0..size - seq.size) {
        var ok = true
        for (j in seq.indices) {
            if (this[start + j] != seq[j]) {
                ok = false
                break
            }
        }
        if (ok) return true
    }
    return false
}

/** Menor prefixo (em nº de cliques, 1-based) que já contém [seq]; null se nunca. */
fun List<BreachCode>.firstRunCompletionStep(seq: List<BreachCode>): Int? {
    for (k in seq.size..size) {
        if (subList(0, k).containsRun(seq)) return k
    }
    return null
}

object DemoPuzzle {
    val matrix: List<List<BreachCode>> = listOf(
        listOf(BreachCode.CBD, BreachCode.CE9, BreachCode.C1C, BreachCode.CBD, BreachCode.CBD),
        listOf(BreachCode.C1C, BreachCode.C1C, BreachCode.CBD, BreachCode.C55, BreachCode.C55),
        listOf(BreachCode.CBD, BreachCode.CBD, BreachCode.C55, BreachCode.C1C, BreachCode.CBD),
        listOf(BreachCode.C1C, BreachCode.C55, BreachCode.CE9, BreachCode.CBD, BreachCode.C55),
        listOf(BreachCode.C55, BreachCode.C1C, BreachCode.CBD, BreachCode.CFF, BreachCode.CE9),
    )

    val daemons: List<Daemon> = listOf(
        Daemon("DATAMINE_V1", listOf(BreachCode.C1C, BreachCode.C55)),
        Daemon("DATAMINE_V2", listOf(BreachCode.C55, BreachCode.CBD, BreachCode.C55)),
        Daemon("DATAMINE_V3", listOf(BreachCode.CBD, BreachCode.C55, BreachCode.C1C)),
    )

    const val bufferSize: Int = 6
}
