package com.example.orvpomodoro.model

data class PomodoroState(
    val phase: Phase = Phase.FOCUS,
    val durations: Durations = Durations(),
    val remainingSeconds: Int = Durations().secondsOf(Phase.FOCUS),
    val totalSeconds: Int = Durations().secondsOf(Phase.FOCUS),
    val isRunning: Boolean = false,
    val isCelebrating: Boolean = false,
    /** Hora de fin según SystemClock.elapsedRealtime(). 0 si no está corriendo. */
    val endAtMs: Long = 0L,
    val completedScenarios: Int = 0,
    val streak: Int = 0,
    val coins: Int = 0,
    val shields: Int = 0,
    val blessingActive: Boolean = false,
    val miniEnabled: Boolean = true,
    val vibrationLevel: VibrationLevel = VibrationLevel.MEDIUM,
    val markedStars: Set<Int> = emptySet(),      // ← NUEVO (índices, desde 0)
    val ownedIds: Set<String> = Cosmetics.defaultOwned,
    val equippedTheme: String = Themes.STAR_STREAM,
    val equippedPalette: String = Palettes.ORIGINAL,
    val equippedFlare: String = Flares.CLASSIC,
    val message: String = "[Un nuevo escenario está por comenzar.]"
) {
    /** La constelación que toca ahora: depende de los minutos, la temática y la paleta. */
    val constellation: Constellation
        get() = Cosmetics.constellation(equippedTheme, equippedPalette, durations.minutesOf(phase))

    val flareStyle: FlareStyle
        get() = Cosmetics.flare(equippedFlare).style

    fun isOwned(id: String) = id in ownedIds
    fun isEquipped(id: String) = id == equippedTheme || id == equippedPalette || id == equippedFlare

    /** null = se puede comprar un cosmético; si no, el motivo. */
    fun buyLock(id: String): String? {
        val cost = Cosmetics.costOf(id) ?: return "No disponible"
        return if (coins < cost) "Faltan ${cost - coins}" else null
    }

    /** null = se puede comprar un objeto; si no, el motivo (se muestra en el botón). */
    fun lockReason(item: ShopItem): String? = when {
        coins < item.cost -> "Faltan ${item.cost - coins}"
        item == ShopItem.EXTEND_BREAK && phase == Phase.FOCUS -> "Solo en descansos"
        item == ShopItem.EXTEND_BREAK && isCelebrating -> "Espera un momento"
        item == ShopItem.BLESSING && blessingActive -> "Ya activa"
        item == ShopItem.SHIELD && shields >= Economy.MAX_SHIELDS -> "Máximo ${Economy.MAX_SHIELDS}"
        else -> null
    }
}