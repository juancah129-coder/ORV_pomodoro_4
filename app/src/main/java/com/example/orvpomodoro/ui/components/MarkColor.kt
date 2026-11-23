package com.example.orvpomodoro.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Color que contrasta con el de la constelación: el opuesto en el círculo cromático,
 * siempre saturado y con el brillo al máximo para que destaque sobre el fondo oscuro.
 */
fun markColorFor(accent: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(accent.toArgb(), hsv)
    hsv[0] = (hsv[0] + 180f) % 360f          // tono opuesto
    hsv[1] = hsv[1].coerceAtLeast(0.85f)     // aunque la constelación sea pálida, la marca es viva
    hsv[2] = 1f
    return Color(android.graphics.Color.HSVToColor(hsv))
}