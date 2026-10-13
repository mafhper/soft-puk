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

/**
 * Buffer padrão do app: em uso real o dono só encontrou puzzles de 4 slots.
 * Continua ajustável na tela de confirmação — é um default, não um limite.
 */
const val DEFAULT_BUFFER_SIZE = 4

object DemoPuzzle {
    // Exemplo trabalhado do buffer 4: o caminho (0,0)→(1,0)→(1,2)→(3,2) produz
    // [1C,55,BD,FF] e fecha os TRÊS daemons dentro do buffer — é a demonstração
    // de que o objetivo "máximo de daemons" funciona no default.
    val matrix: List<List<BreachCode>> = listOf(
        listOf(BreachCode.C1C, BreachCode.CBD, BreachCode.C55, BreachCode.CE9, BreachCode.CBD),
        listOf(BreachCode.C55, BreachCode.C1C, BreachCode.CBD, BreachCode.CFF, BreachCode.C55),
        listOf(BreachCode.CBD, BreachCode.C55, BreachCode.CE9, BreachCode.C1C, BreachCode.CBD),
        listOf(BreachCode.CE9, BreachCode.CBD, BreachCode.CFF, BreachCode.C55, BreachCode.C1C),
        listOf(BreachCode.C55, BreachCode.CE9, BreachCode.C1C, BreachCode.CBD, BreachCode.C55),
    )

    val daemons: List<Daemon> = listOf(
        Daemon("DATAMINE_V1", listOf(BreachCode.CBD, BreachCode.CFF)),
        Daemon("DATAMINE_V2", listOf(BreachCode.C1C, BreachCode.C55, BreachCode.CBD)),
        // V3 com 4 códigos, como no jogo real — exercita o layout de sequência longa.
        Daemon(
            "DATAMINE_V3",
            listOf(BreachCode.C1C, BreachCode.C55, BreachCode.CBD, BreachCode.CFF),
        ),
    )

    const val bufferSize: Int = DEFAULT_BUFFER_SIZE
}
