package com.example.orvpomodoro.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

enum class CosmeticKind { THEME, PALETTE, FLARE }

enum class FlareStyle { FOUR_POINT, EIGHT_POINT, RINGS, SPARKS }

class Palette(
    val id: String,
    val name: String,
    val description: String,
    val cost: Int,
    /** 7 colores, de la constelación más pequeña a la más grande. null = usa los de la temática. */
    val colors: List<Color>?
)

class Flare(
    val id: String,
    val name: String,
    val description: String,
    val cost: Int,
    val style: FlareStyle
)

object Palettes {
    const val ORIGINAL = "palette_original"

    val original = Palette(
        ORIGINAL, "Colores originales",
        "Cada constelación conserva su propio color.", 0, null
    )
    val embers = Palette(
        "palette_embers", "Brasas",
        "Del durazno al carmesí, como una hoguera que crece.", 150,
        listOf(
            Color(0xFFFFD6A0), Color(0xFFFFB86B), Color(0xFFFF9150), Color(0xFFFF7043),
            Color(0xFFFF5252), Color(0xFFE8456B), Color(0xFFFF7FA8)
        )
    )
    val ice = Palette(
        "palette_ice", "Hielo",
        "Del blanco escarcha al violeta profundo.", 150,
        listOf(
            Color(0xFFE6F6FF), Color(0xFFC2E8FF), Color(0xFF9DD9FF), Color(0xFF7AC8FF),
            Color(0xFF5FB4FF), Color(0xFF8CA3FF), Color(0xFFAE92FF)
        )
    )
    val aurora = Palette(
        "palette_aurora", "Aurora",
        "Verdes y violetas que se encienden en el cielo.", 200,
        listOf(
            Color(0xFFA6FFDA), Color(0xFF7CF0C9), Color(0xFF5EE6B0), Color(0xFF7BD7FF),
            Color(0xFF9EB4FF), Color(0xFFC09BFF), Color(0xFFFF9BE6)
        )
    )

    val all = listOf(original, embers, ice, aurora)
}

object Flares {
    const val CLASSIC = "flare_classic"

    val classic = Flare(CLASSIC, "Estrella clásica", "Destello de 4 puntas.", 0, FlareStyle.FOUR_POINT)
    val octagram = Flare("flare_octagram", "Estrella de 8 puntas", "Dos estrellas cruzadas que giran.", 120, FlareStyle.EIGHT_POINT)
    val ripples = Flare("flare_ripples", "Ondas", "Anillos que se expanden desde la estrella.", 120, FlareStyle.RINGS)
    val sparks = Flare("flare_sparks", "Chispas", "Ocho chispas que salen disparadas.", 180, FlareStyle.SPARKS)

    val all = listOf(classic, octagram, ripples, sparks)
}

object Cosmetics {
    val themes: List<ConstellationTheme> get() = Themes.all
    val palettes: List<Palette> get() = Palettes.all
    val flares: List<Flare> get() = Flares.all

    /** Lo que el jugador tiene desde el principio. */
    val defaultOwned = setOf(Themes.STAR_STREAM, Palettes.ORIGINAL, Flares.CLASSIC)

    fun theme(id: String) = Themes.all.firstOrNull { it.id == id } ?: Themes.starStream
    fun palette(id: String) = Palettes.all.firstOrNull { it.id == id } ?: Palettes.original
    fun flare(id: String) = Flares.all.firstOrNull { it.id == id } ?: Flares.classic

    /** 0 a 6, de menos a más estrellas. */


    fun constellation(themeId: String, paletteId: String, minutes: Int): Constellation {
        val theme = theme(themeId)
        val tier = theme.tierFor(minutes)
        val base = theme.tiers[tier]
        val colors = palette(paletteId).colors ?: return base

        // Estira los 7 colores de la paleta sobre las constelaciones de la temática
        val pos = if (theme.tiers.size <= 1) 0f
        else tier * (colors.size - 1f) / (theme.tiers.size - 1f)
        val i = pos.toInt().coerceAtMost(colors.size - 2)
        val color = lerp(colors[i], colors[i + 1], pos - i)

        return Constellation(base.name, color, base.points, base.detail)
    }
    fun kindOf(id: String): CosmeticKind? = when {
        Themes.all.any { it.id == id } -> CosmeticKind.THEME
        Palettes.all.any { it.id == id } -> CosmeticKind.PALETTE
        Flares.all.any { it.id == id } -> CosmeticKind.FLARE
        else -> null
    }

    fun costOf(id: String): Int? = when (kindOf(id)) {
        CosmeticKind.THEME -> theme(id).cost
        CosmeticKind.PALETTE -> palette(id).cost
        CosmeticKind.FLARE -> flare(id).cost
        null -> null
    }

    fun nameOf(id: String): String? = when (kindOf(id)) {
        CosmeticKind.THEME -> theme(id).name
        CosmeticKind.PALETTE -> palette(id).name
        CosmeticKind.FLARE -> flare(id).name
        null -> null
    }
}