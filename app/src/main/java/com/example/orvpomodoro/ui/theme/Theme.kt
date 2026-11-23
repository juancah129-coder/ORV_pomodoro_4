package com.example.orvpomodoro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val OrvColors = darkColorScheme(
    primary = TintaSistema,
    secondary = HiloDorado,
    tertiary = Nebulosa,
    background = Vacio,
    surface = Abismo,
    onPrimary = Vacio,
    onBackground = Papel,
    onSurface = Papel
)

@Composable
fun OrvPomodoroTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = OrvColors, content = content)
}