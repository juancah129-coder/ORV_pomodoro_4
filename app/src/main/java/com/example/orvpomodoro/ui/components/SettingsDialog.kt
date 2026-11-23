package com.example.orvpomodoro.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orvpomodoro.model.Constellation
import com.example.orvpomodoro.model.Cosmetics
import com.example.orvpomodoro.model.Durations
import com.example.orvpomodoro.model.Economy
import com.example.orvpomodoro.model.Phase
import com.example.orvpomodoro.ui.theme.Abismo
import com.example.orvpomodoro.ui.theme.HiloDorado
import com.example.orvpomodoro.ui.theme.Papel
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import com.example.orvpomodoro.model.VibrationLevel
import com.example.orvpomodoro.ui.theme.Vacio

// ---------- Datos auxiliares de cada fase ----------

private fun Phase.maxMinutes(): Int = when (this) {
    Phase.FOCUS -> 90
    Phase.SHORT_BREAK -> 30
    Phase.LONG_BREAK -> 45
}

private fun Phase.kind(): String = when (this) {
    Phase.FOCUS -> "Foco"
    Phase.SHORT_BREAK -> "Descanso corto"
    Phase.LONG_BREAK -> "Descanso largo"
}

private fun Durations.withMinutes(phase: Phase, minutes: Int): Durations = when (phase) {
    Phase.FOCUS -> copy(focusMin = minutes)
    Phase.SHORT_BREAK -> copy(shortBreakMin = minutes)
    Phase.LONG_BREAK -> copy(longBreakMin = minutes)
}

// ---------- Diálogo ----------

@Composable
fun SettingsDialog(
    current: Durations,
    themeId: String,
    paletteId: String,
    locked: Boolean,                         // true = temporizador corriendo: los tiempos no se tocan
    miniEnabled: Boolean,
    onMiniEnabledChange: (Boolean) -> Unit,
    vibrationLevel: VibrationLevel,                       // ← NUEVO
    onVibrationLevelChange: (VibrationLevel) -> Unit,     // ← NUEVO
    initialPhase: Phase = Phase.FOCUS,
    onDismiss: () -> Unit,
    onSave: (Durations) -> Unit
) {
    var draft by remember { mutableStateOf(current) }
    val phases = Phase.entries
    var index by remember { mutableIntStateOf(phases.indexOf(initialPhase)) }

    val phase = phases[index]
    val maxMinutes = phase.maxMinutes()
    val minutes = draft.minutesOf(phase).coerceIn(1, maxMinutes)

    val constellation = Cosmetics.constellation(themeId, paletteId, minutes)
    val color by animateColorAsState(
        targetValue = constellation.color,
        animationSpec = tween(400),
        label = "settingsColor"
    )

    fun setMinutes(value: Int) {
        if (locked) return
        draft = draft.withMinutes(phase, value.coerceIn(1, maxMinutes))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp),
        containerColor = Abismo,
        title = { Text("Ajustar tiempos", color = Papel) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PhaseSelector(
                    phase = phase,
                    onPrev = { index = (index - 1 + phases.size) % phases.size },
                    onNext = { index = (index + 1) % phases.size }
                )

                MinutesDial(
                    minutes = minutes,
                    maxMinutes = maxMinutes,
                    color = if (locked) color.copy(alpha = 0.5f) else color,
                    enabled = !locked,
                    onChange = ::setMinutes,
                    modifier = Modifier
                        .widthIn(max = 260.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                ) {
                    ConstellationPreview(
                        constellation = constellation,
                        color = color,
                        modifier = Modifier.size(80.dp)
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$minutes",
                            style = TextStyle(
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Light,
                                fontFeatureSettings = "tnum",
                                color = color
                            )
                        )
                        Text(
                            text = " min",
                            color = color,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val buttonColors = ButtonDefaults.textButtonColors(
                        contentColor = color,
                        disabledContentColor = Papel.copy(alpha = 0.3f)
                    )
                    TextButton(
                        onClick = { setMinutes(minutes - 1) },
                        enabled = !locked && minutes > 1,
                        colors = buttonColors
                    ) { Text("−1 min") }
                    TextButton(
                        onClick = { setMinutes(minutes + 1) },
                        enabled = !locked && minutes < maxMinutes,
                        colors = buttonColors
                    ) { Text("+1 min") }
                }

                Text(
                    text = buildString {
                        append(constellation.name)
                        constellation.detail?.let { append(", ").append(it) }
                        append(", ${constellation.points.size} estrellas")
                    },
                    color = color,
                    fontFamily = FontFamily.Serif,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )

                if (phase == Phase.FOCUS) {
                    Text(
                        text = "Recompensa: ${Economy.scenarioReward(minutes)} monedas",
                        color = HiloDorado,
                        fontFamily = FontFamily.Serif,
                        fontSize = 16.sp
                    )
                } else {
                    Text(
                        text = "Los descansos no dan monedas",
                        color = Papel.copy(alpha = 0.65f),
                        fontSize = 14.sp
                    )
                }

                if (locked) {
                    Text(
                        text = "Los tiempos se pueden cambiar con el temporizador en pausa.",
                        color = Papel.copy(alpha = 0.65f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // Separador y switch de la mini ventana
                Box(
                    Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Papel.copy(alpha = 0.12f))
                )
                MiniSwitchRow(checked = miniEnabled, color = color, onChange = onMiniEnabledChange)
                VibrationRow(level = vibrationLevel, color = color, onChange = onVibrationLevelChange)

            }

        },
        confirmButton = {
            if (locked) {
                TextButton(onClick = onDismiss) { Text("Cerrar", color = color) }
            } else {
                TextButton(onClick = { onSave(draft) }) { Text("Guardar", color = color) }
            }
        },
        dismissButton = {
            if (!locked) {
                TextButton(onClick = onDismiss) { Text("Cancelar", color = Papel.copy(alpha = 0.8f)) }
            }
        }
    )
}

// ---------- Switch de la mini ventana ----------

@Composable
private fun MiniSwitchRow(checked: Boolean, color: Color, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Mini constelación flotante",
                color = Papel,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Muestra tu avance sobre otras apps cuando sales de esta.",
                color = Papel.copy(alpha = 0.7f),
                fontFamily = FontFamily.Serif,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            modifier = Modifier.semantics { contentDescription = "Mini constelación flotante" },
            colors = SwitchDefaults.colors(
                checkedThumbColor = color,
                checkedTrackColor = color.copy(alpha = 0.35f),
                checkedBorderColor = color,
                uncheckedThumbColor = Papel.copy(alpha = 0.7f),
                uncheckedTrackColor = Papel.copy(alpha = 0.12f),
                uncheckedBorderColor = Papel.copy(alpha = 0.4f)
            )
        )
    }
}
// ---------- Vibración al terminar ----------

@Composable
private fun VibrationRow(level: VibrationLevel, color: Color, onChange: (VibrationLevel) -> Unit) {
    val shape = CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Text(
            "Vibración al terminar",
            color = Papel,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Al tocar una opción vibra, para que sientas la intensidad.",
            color = Papel.copy(alpha = 0.7f),
            fontFamily = FontFamily.Serif,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            VibrationLevel.entries.forEach { option ->
                val selected = option == level
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(shape)
                        .background(if (selected) color else Color.Transparent)
                        .border(1.dp, if (selected) color else Papel.copy(alpha = 0.35f), shape)
                        .selectable(
                            selected = selected,
                            onClick = { onChange(option) },
                            role = Role.RadioButton
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option.label,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false,
                        color = if (selected) Vacio else Papel,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
// ---------- Selector de arriba (flechas + nombre) ----------

@Composable
private fun PhaseSelector(phase: Phase, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChevronButton(left = true, description = "Ajuste anterior", onClick = onPrev)

        Column(
            modifier = Modifier
                .weight(1f)
                .height(76.dp),   // alto fijo: el diálogo no salta entre nombres cortos y largos
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = phase.label,
                color = Papel,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = phase.kind(),
                color = Papel.copy(alpha = 0.7f),
                fontFamily = FontFamily.Serif,
                fontSize = 14.sp
            )
        }

        ChevronButton(left = false, description = "Ajuste siguiente", onClick = onNext)
    }
}

@Composable
private fun ChevronButton(left: Boolean, description: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = description }
    ) {
        Canvas(Modifier.size(20.dp)) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                if (left) {
                    moveTo(w * 0.70f, h * 0.10f)
                    lineTo(w * 0.25f, h * 0.50f)
                    lineTo(w * 0.70f, h * 0.90f)
                } else {
                    moveTo(w * 0.30f, h * 0.10f)
                    lineTo(w * 0.75f, h * 0.50f)
                    lineTo(w * 0.30f, h * 0.90f)
                }
            }
            drawPath(
                path = path,
                color = Papel.copy(alpha = 0.9f),
                style = Stroke(width = 2.5f.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

// ---------- Anillo de minutos ----------

/**
 * Convierte un toque en minutos. El tope del anillo (arriba) es el punto cero y
 * se avanza en sentido horario. Devuelve null si el toque cae en el centro.
 */
private fun minutesFromPosition(pos: Offset, size: IntSize, max: Int): Int? {
    val dx = pos.x - size.width / 2f
    val dy = pos.y - size.height / 2f
    if (hypot(dx, dy) < minOf(size.width, size.height) * 0.2f) return null

    var degrees = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 90.0
    if (degrees < 0) degrees += 360.0
    return (degrees / 360.0 * max).roundToInt().coerceIn(1, max)
}

@Composable
private fun MinutesDial(
    minutes: Int,
    maxMinutes: Int,
    color: Color,
    enabled: Boolean,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val currentMinutes by rememberUpdatedState(minutes)
    val currentOnChange by rememberUpdatedState(onChange)
    val currentEnabled by rememberUpdatedState(enabled)
    val haptic = LocalHapticFeedback.current

    fun apply(value: Int) {
        if (!currentEnabled) return
        if (value != currentMinutes) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        currentOnChange(value)
    }

    Box(
        modifier = modifier
            .semantics { contentDescription = "Selector de minutos: $minutes de $maxMinutes" }
            .pointerInput(maxMinutes) {
                detectTapGestures { pos ->
                    minutesFromPosition(pos, size, maxMinutes)?.let { apply(it) }
                }
            }
            .pointerInput(maxMinutes) {
                detectDragGestures(
                    onDragStart = { pos ->
                        minutesFromPosition(pos, size, maxMinutes)?.let { apply(it) }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val raw = minutesFromPosition(change.position, size, maxMinutes)
                            ?: return@detectDragGestures
                        val old = currentMinutes
                        // Evita el salto de 90 a 1 al cruzar el tope del anillo
                        val fixed = if (abs(raw - old) > maxMinutes / 2) {
                            if (old > maxMinutes / 2) maxMinutes else 1
                        } else raw
                        apply(fixed)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val radius = minOf(size.width, size.height) / 2f - 24.dp.toPx()
            val c = center
            val fraction = minutes / maxMinutes.toFloat()

            // Marcas alrededor: cada 5 min (descansos) o cada 10 (escenario)
            val step = if (maxMinutes <= 45) 5 else 10
            var m = step
            while (m <= maxMinutes) {
                val a = Math.toRadians(m.toDouble() / maxMinutes * 360.0 - 90.0)
                val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
                drawLine(
                    color = Papel.copy(alpha = 0.3f),
                    start = c + dir * (radius + stroke / 2f + 4.dp.toPx()),
                    end = c + dir * (radius + stroke / 2f + 9.dp.toPx()),
                    strokeWidth = 1.5f.dp.toPx(),
                    cap = StrokeCap.Round
                )
                m += step
            }

            // Pista y progreso
            drawCircle(color.copy(alpha = 0.18f), radius = radius, center = c, style = Stroke(stroke))
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = fraction * 360f,
                useCenter = false,
                topLeft = Offset(c.x - radius, c.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )

            // Perilla
            val angle = Math.toRadians(fraction * 360.0 - 90.0)
            val thumb = c + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * radius
            drawCircle(color.copy(alpha = 0.25f), radius = 16.dp.toPx(), center = thumb)
            drawCircle(color, radius = 11.dp.toPx(), center = thumb)
            drawCircle(Color.White, radius = 5.dp.toPx(), center = thumb)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

// ---------- Vista previa de la constelación ----------

@Composable
private fun ConstellationPreview(
    constellation: Constellation,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val px = constellation.points.map { Offset(it.x * size.width, it.y * size.height) }
        for (i in 0 until px.lastIndex) {
            drawLine(
                color = color.copy(alpha = 0.6f),
                start = px[i],
                end = px[i + 1],
                strokeWidth = 1.5f.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        px.forEach { p ->
            drawCircle(color.copy(alpha = 0.18f), radius = 6.dp.toPx(), center = p)
            drawCircle(color, radius = 2.5f.dp.toPx(), center = p)
        }
    }
}