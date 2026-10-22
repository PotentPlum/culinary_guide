package nl.potentplum.mise.domain

/**
 * The 5-step score scale of the evaluation sheet (spec §6.12): Mislukt · Matig · Oké · Goed · Top, stored as 2/4/6/8/10
 * in the 1–10 fields so the thresholds of §11.6 and the badges keep working.
 */
object ScoreSteps {
    const val COUNT = 5

    /** The stored value for a step index (0 = Mislukt … 4 = Top). */
    fun valueOf(step: Int): Int {
        require(step in 0 until COUNT) { "step must be 0..${COUNT - 1}, was $step" }
        return (step + 1) * 2
    }

    /** The step shown for a stored value; older values map to the nearest step (1–2 Mislukt … 9–10 Top). */
    fun stepOf(value: Int): Int = (value.coerceIn(1, 10) + 1) / 2 - 1
}
