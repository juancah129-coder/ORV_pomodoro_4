package com.example.orvpomodoro.model

/** Un pulso: vibra [ms] milisegundos y luego descansa [gapAfterMs]. strength va de 1 a 255. */
data class Pulse(
    val ms: Long,
    val gapAfterMs: Long = 0L,
    val strength: Int = 255
)

/**
 * Para añadir un nivel nuevo, agrega una línea aquí. Se mostrará solo en los ajustes.
 * Ejemplo: LATIDO("Latido", listOf(Pulse(120, 100, 200), Pulse(220, strength = 255)))
 */
enum class VibrationLevel(val label: String, val pulses: List<Pulse>) {
    OFF("Apagada", emptyList()),
    LOW("Baja", listOf(Pulse(ms = 200, strength = 80))),
    MEDIUM("Media", listOf(Pulse(300, 150, 160), Pulse(300, strength = 160))),
    HIGH("Alta", listOf(Pulse(500, 150, 255), Pulse(500, 150, 255), Pulse(500, strength = 255)))
}