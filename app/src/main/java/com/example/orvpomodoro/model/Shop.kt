package com.example.orvpomodoro.model

enum class ShopItem(val label: String, val description: String, val cost: Int) {
    EXTEND_BREAK(
        "Prórroga de descanso",
        "Suma 5 minutos al descanso actual.",
        60
    ),
    BLESSING(
        "Bendición de la constelación",
        "Duplica las monedas del próximo escenario superado.",
        150
    ),
    SHIELD(
        "Escudo de escenario",
        "Si abandonas un escenario, protege tu racha una vez. Máximo 3.",
        90
    )
}

object Economy {
    const val COINS_PER_MINUTE = 4
    const val STREAK_FOR_LONG_BREAK = 4
    const val MAX_SHIELDS = 3

    fun scenarioReward(focusMin: Int): Int = focusMin * COINS_PER_MINUTE
}