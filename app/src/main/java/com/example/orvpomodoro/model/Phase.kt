package com.example.orvpomodoro.model

enum class Phase(val label: String) {
    FOCUS("Escenario Principal"),
    SHORT_BREAK("Zona Segura"),
    LONG_BREAK("Descanso de Constelaciones")
}

data class Durations(
    val focusMin: Int = 25,
    val shortBreakMin: Int = 5,
    val longBreakMin: Int = 15
) {
    fun minutesOf(phase: Phase): Int = when (phase) {
        Phase.FOCUS -> focusMin
        Phase.SHORT_BREAK -> shortBreakMin
        Phase.LONG_BREAK -> longBreakMin
    }

    fun secondsOf(phase: Phase): Int = minutesOf(phase) * 60
}