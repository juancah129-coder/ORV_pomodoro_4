package com.example.orvpomodoro.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.orvpomodoro.model.FlareStyle
import kotlin.math.cos
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Duraciones de los destellos (en milisegundos)
private const val FLARE_MS = 900
private const val CELEBRATION_MS = 1400

@Composable
fun ConstellationProgress(
    elapsed: Float,              // 0f = recién empezado, 1f = completo
    points: List<Offset>,        // puntos normalizados (0..1), mínimo 2
    color: Color,
    description: String,
    celebrate: Boolean = false,  // true = destello final de todas las estrellas
    flareStyle: FlareStyle = FlareStyle.FOUR_POINT,
    marks: Set<Int> = emptySet(),                      // ← NUEVO
    modifier: Modifier = Modifier
) {
    val segments = points.size - 1

    // Avanza suave, pero retrocede de golpe (reinicio o nueva fase)
    val progress = remember { Animatable(elapsed) }
    LaunchedEffect(elapsed) {
        if (elapsed < progress.value) progress.snapTo(elapsed)
        else progress.animateTo(elapsed, tween(1000, easing = LinearEasing))
    }

    // Un valor 0..1 por estrella: 0 = sin destello, 0..1 = destellando
    val flares = remember(points) { List(points.size) { Animatable(0f) } }

    // 1) Destello individual: cuando el trazo alcanza cada estrella
    LaunchedEffect(points) {
        var last = -1
        snapshotFlow {
            (progress.value.coerceIn(0f, 1f) * segments).toInt().coerceAtMost(segments)
        }.collect { full ->
            if (full < last) {
                // El trazo retrocedió: apaga los destellos de las estrellas perdidas
                flares.forEachIndexed { i, f -> if (i > full) f.snapTo(0f) }
                last = -1
            }
            for (i in (last + 1)..full) {
                launch {
                    flares[i].snapTo(0f)
                    flares[i].animateTo(1f, tween(FLARE_MS, easing = FastOutSlowInEasing))
                }
            }
            last = full
        }
    }

    // 2) Destello final: todas las estrellas a la par, cuando el trazo ya terminó
    LaunchedEffect(celebrate, points) {
        if (celebrate) {
            snapshotFlow { progress.value }.first { it >= 0.999f }
            coroutineScope {
                flares.forEach { f ->
                    launch {
                        f.snapTo(0f)
                        f.animateTo(1f, tween(CELEBRATION_MS, easing = FastOutSlowInEasing))
                    }
                }
            }
        }
    }

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "pulse"
    )
    val markColor = remember(color) { markColorFor(color) }

    Canvas(modifier.semantics { contentDescription = description }) {
        val side = minOf(size.width, size.height)
        val ox = (size.width - side) / 2f
        val oy = (size.height - side) / 2f
        val px = points.map { Offset(ox + it.x * side, oy + it.y * side) }
        val p = progress.value.coerceIn(0f, 1f)
        val position = p * segments
        val full = position.toInt().coerceAtMost(segments)
        val partial = position - full

        // Camino que falta
        for (i in 0 until segments) {
            drawLine(color.copy(alpha = 0.14f), px[i], px[i + 1], strokeWidth = 1.dp.toPx())
        }
        // Tramos completos
        for (i in 0 until full) {
            drawLine(color, px[i], px[i + 1], strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }
        // Tramo en curso
        var head = px[full]
        if (full < segments) {
            head = lerp(px[full], px[full + 1], partial)
            drawLine(color, px[full], head, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }

        // Estrellas (apagadas o encendidas)
        px.forEachIndexed { i, pt ->
            if (i <= full) {
                drawCircle(color.copy(alpha = 0.15f), radius = 11.dp.toPx(), center = pt)
                drawCircle(color, radius = 4.dp.toPx(), center = pt)
            } else {
                drawCircle(Color.White.copy(alpha = 0.35f), radius = 3.dp.toPx(), center = pt)
            }
        }

        // Marcas: color contrastante con la constelación, con halo, anillo y punto
        marks.forEach { i ->
            if (i in px.indices) {
                drawCircle(markColor.copy(alpha = 0.25f), radius = 14.dp.toPx(), center = px[i])
                drawCircle(
                    color = markColor,
                    radius = 9.dp.toPx(),
                    center = px[i],
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(markColor, radius = 4.dp.toPx(), center = px[i])
            }
        }

        // Destellos de 4 puntas, encima de todo
        px.forEachIndexed { i, pt ->
            drawFlare(flareStyle, pt, flares[i].value, color)
        }

        // Cabeza que avanza (se oculta al terminar para no competir con el destello final)
        if (p < 0.999f) {
            drawCircle(color.copy(alpha = 0.25f), radius = (9f + 7f * pulse).dp.toPx(), center = head)
            drawCircle(Color.White, radius = 4.5f.dp.toPx(), center = head)
        }
    }
}

/** v va de 0 a 1. La intensidad sube rápido hasta v = 0.25 y luego se apaga. */
/** v va de 0 a 1. La intensidad sube rápido hasta v = 0.25 y luego se apaga. */
internal fun DrawScope.drawFlare(style: FlareStyle, center: Offset, v: Float, color: Color) {
    if (v <= 0f || v >= 1f) return

    val e = sin(PI.toFloat() * sqrt(v))
    val radius = (6f + 24f * e).dp.toPx()

    // Halo suave, común a todos los estilos
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.55f * e), Color.Transparent),
            center = center,
            radius = radius * 1.4f
        ),
        radius = radius * 1.4f,
        center = center
    )

    when (style) {
        FlareStyle.FOUR_POINT -> {
            rotate(degrees = 45f * v, pivot = center) {
                drawPath(fourPointStar(center, radius), color.copy(alpha = e))
            }
        }
        FlareStyle.EIGHT_POINT -> {
            rotate(degrees = 45f * v, pivot = center) {
                drawPath(fourPointStar(center, radius), color.copy(alpha = e))
            }
            rotate(degrees = 45f + 45f * v, pivot = center) {
                drawPath(fourPointStar(center, radius * 0.62f), color.copy(alpha = e * 0.9f))
            }
        }
        FlareStyle.RINGS -> {
            for (k in 0..2) {
                val p = v - k * 0.18f
                if (p in 0f..1f) {
                    drawCircle(
                        color = color.copy(alpha = (1f - p) * 0.9f),
                        radius = (4f + 34f * p).dp.toPx(),
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
        FlareStyle.SPARKS -> {
            for (k in 0 until 8) {
                val a = Math.toRadians(k * 45.0 + 14.0 * v)
                val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
                drawLine(
                    color = color.copy(alpha = 1f - v),
                    start = center + dir * (4f + 12f * v).dp.toPx(),
                    end = center + dir * (10f + 28f * v).dp.toPx(),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }

    // Núcleo blanco
    if (style == FlareStyle.FOUR_POINT || style == FlareStyle.EIGHT_POINT) {
        drawPath(fourPointStar(center, radius * 0.5f), Color.White.copy(alpha = e))
    } else {
        drawCircle(Color.White.copy(alpha = e), radius = (2.5f + 3f * e).dp.toPx(), center = center)
    }
}
/** Estrella cóncava de 4 puntas: cuatro curvas que se hunden hacia el centro. */
private fun fourPointStar(c: Offset, r: Float): Path = Path().apply {
    moveTo(c.x, c.y - r)
    quadraticBezierTo(c.x, c.y, c.x + r, c.y)
    quadraticBezierTo(c.x, c.y, c.x, c.y + r)
    quadraticBezierTo(c.x, c.y, c.x - r, c.y)
    quadraticBezierTo(c.x, c.y, c.x, c.y - r)
    close()
}