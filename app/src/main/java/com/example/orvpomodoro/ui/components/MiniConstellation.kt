package com.example.orvpomodoro.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.example.orvpomodoro.engine.PomodoroEngine
import com.example.orvpomodoro.ui.theme.OrvPomodoroTheme
import com.example.orvpomodoro.ui.theme.Vacio

/** Contenido de la ventana flotante: lee el estado directamente del motor. */
@Composable
fun MiniOverlayContent() {
    val s by PomodoroEngine.state.collectAsState()
    val constellation = s.constellation
    val elapsed = (1f - s.remainingSeconds / s.totalSeconds.toFloat()).coerceIn(0f, 1f)
    val shape = CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp)

    OrvPomodoroTheme {
        Box(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Vacio.copy(alpha = 0.85f))
                .border(1.dp, constellation.color.copy(alpha = 0.7f), shape)
                .padding(8.dp)
        ) {
            MiniConstellation(
                elapsed = elapsed,
                points = constellation.points,
                color = constellation.color,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun MiniConstellation(
    elapsed: Float,
    points: List<Offset>,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = elapsed,
        animationSpec = tween(1000, easing = LinearEasing),
        label = "miniElapsed"
    )
    val pulse by rememberInfiniteTransition(label = "miniPulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "miniPulse"
    )

    Canvas(modifier) {
        val side = minOf(size.width, size.height)
        val ox = (size.width - side) / 2f
        val oy = (size.height - side) / 2f
        val px = points.map { Offset(ox + it.x * side, oy + it.y * side) }
        val segments = px.size - 1
        val position = animated.coerceIn(0f, 1f) * segments
        val full = position.toInt().coerceAtMost(segments)
        val partial = position - full

        for (i in 0 until segments) {
            drawLine(color.copy(alpha = 0.14f), px[i], px[i + 1], strokeWidth = 1.dp.toPx())
        }
        for (i in 0 until full) {
            drawLine(color, px[i], px[i + 1], strokeWidth = 1.5f.dp.toPx(), cap = StrokeCap.Round)
        }
        var head = px[full]
        if (full < segments) {
            head = lerp(px[full], px[full + 1], partial)
            drawLine(color, px[full], head, strokeWidth = 1.5f.dp.toPx(), cap = StrokeCap.Round)
        }

        px.forEachIndexed { i, p ->
            if (i <= full) {
                drawCircle(color.copy(alpha = 0.18f), radius = 7.dp.toPx(), center = p)
                drawCircle(color, radius = 3.dp.toPx(), center = p)
            } else {
                drawCircle(Color.White.copy(alpha = 0.35f), radius = 2.dp.toPx(), center = p)
            }
        }

        if (elapsed < 0.999f) {
            drawCircle(color.copy(alpha = 0.25f), radius = (6f + 4f * pulse).dp.toPx(), center = head)
            drawCircle(Color.White, radius = 3.dp.toPx(), center = head)
        }
    }
}