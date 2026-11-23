package com.example.orvpomodoro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orvpomodoro.model.PomodoroState
import com.example.orvpomodoro.model.VibrationLevel
import com.example.orvpomodoro.ui.theme.Abismo
import com.example.orvpomodoro.ui.theme.HiloDorado
import com.example.orvpomodoro.ui.theme.Papel
import androidx.compose.runtime.remember

@Composable
fun MarkDialog(
    state: PomodoroState,
    accent: Color,
    onToggle: (Int) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val constellation = state.constellation
    val points = constellation.points
    val n = points.size
    val segments = n - 1
    val total = state.totalSeconds
    val elapsed = (total - state.remainingSeconds).coerceAtLeast(0)
    val marks = state.markedStars
    val haptic = LocalHapticFeedback.current
    val markColor = remember(accent) { markColorFor(accent) }

    // La estrella i ya se alcanzó cuando elapsed / total >= i / segments
    fun reached(i: Int) = elapsed * segments >= i * total
    // Solo las intermedias que aún no se alcanzan: la primera es el inicio, la última el final
    fun selectable(i: Int) = i in 1..(n - 2) && !reached(i)
    fun timeOf(i: Int): String {
        val sec = (i * total.toLong() / segments).toInt()
        return "%d:%02d".format(sec / 60, sec % 60)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp),
        containerColor = Abismo,
        title = {
            Column {
                Text("Marcar estrellas", color = Papel)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append(constellation.name)
                        constellation.detail?.let { append(", ").append(it) }
                        append(", $n estrellas")
                    },
                    color = accent,
                    fontFamily = FontFamily.Serif,
                    fontSize = 15.sp
                )
                Text(
                    "${state.phase.label}, ${total / 60} min",
                    color = Papel.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Toca una estrella y el teléfono vibrará cuando el trazo llegue a ella. " +
                            "La primera es el inicio y la última el final, que ya vibra al terminar.",
                    color = Papel.copy(alpha = 0.85f),
                    fontFamily = FontFamily.Serif,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                StarPicker(
                    points = points,
                    color = accent,
                    markColor = markColor,          // ← NUEVO
                    marks = marks,
                    isReached = { reached(it) },
                    isSelectable = { selectable(it) },
                    onTap = { i ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggle(i)
                    },
                    description = "Constelación con $n estrellas, ${marks.size} marcadas",
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )

                if (marks.isEmpty()) {
                    Text("Aún no hay marcas.", color = Papel.copy(alpha = 0.65f), fontSize = 14.sp)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        marks.sorted().forEach { i ->
                            Text(
                                "Estrella ${i + 1} de $n: a los ${timeOf(i)}",
                                color = markColor,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                if (state.vibrationLevel == VibrationLevel.OFF) {
                    Text(
                        "La vibración está apagada: las marcas no vibrarán. Actívala en Tiempos.",
                        color = HiloDorado,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Listo", color = accent) }
        },
        dismissButton = {
            TextButton(onClick = onClear, enabled = marks.isNotEmpty()) {
                Text(
                    "Quitar todas",
                    color = if (marks.isNotEmpty()) Papel.copy(alpha = 0.8f) else Papel.copy(alpha = 0.3f)
                )
            }
        }
    )
}

@Composable
private fun StarPicker(
    points: List<Offset>,
    color: Color,
    markColor: Color,
    marks: Set<Int>,
    isReached: (Int) -> Boolean,
    isSelectable: (Int) -> Boolean,
    onTap: (Int) -> Unit,
    description: String,
    modifier: Modifier = Modifier
) {
    // Se tocan estrellas marcadas (para quitarlas) y las que aún se pueden marcar
    val currentTappable by rememberUpdatedState { i: Int -> isSelectable(i) || i in marks }
    val currentTap by rememberUpdatedState(onTap)
    val currentPoints by rememberUpdatedState(points)

    Canvas(
        modifier
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                detectTapGestures { tap ->
                    // El dibujo ocupa un cuadrado centrado
                    val side = minOf(size.width, size.height).toFloat()
                    val ox = (size.width - side) / 2f
                    val oy = (size.height - side) / 2f
                    val px = currentPoints.map { Offset(ox + it.x * side, oy + it.y * side) }

                    val nearest = px.indices.minByOrNull { (px[it] - tap).getDistance() }
                    if (nearest != null &&
                        (px[nearest] - tap).getDistance() <= 36.dp.toPx() &&
                        currentTappable(nearest)
                    ) {
                        currentTap(nearest)
                    }
                }
            }
    ) {
        val side = minOf(size.width, size.height)
        val ox = (size.width - side) / 2f
        val oy = (size.height - side) / 2f
        val px = points.map { Offset(ox + it.x * side, oy + it.y * side) }

        for (i in 0 until px.lastIndex) {
            drawLine(
                color = color.copy(alpha = 0.35f),
                start = px[i],
                end = px[i + 1],
                strokeWidth = 1.5f.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        px.forEachIndexed { i, p ->
            when {
                i in marks -> {
                    drawCircle(markColor.copy(alpha = 0.25f), radius = 18.dp.toPx(), center = p)
                    drawCircle(markColor, radius = 6.dp.toPx(), center = p)
                    drawCircle(markColor, radius = 12.dp.toPx(), center = p, style = Stroke(width = 2.dp.toPx()))
                }
                isReached(i) -> {
                    drawCircle(color, radius = 4.dp.toPx(), center = p)
                }
                isSelectable(i) -> {
                    drawCircle(
                        Color.White.copy(alpha = 0.7f), radius = 10.dp.toPx(), center = p,
                        style = Stroke(width = 1.5f.dp.toPx())
                    )
                    drawCircle(Color.White.copy(alpha = 0.7f), radius = 3.dp.toPx(), center = p)
                }
                else -> {
                    drawCircle(Color.White.copy(alpha = 0.35f), radius = 3.dp.toPx(), center = p)
                }
            }
        }
    }
}