package com.example.orvpomodoro.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class Star(val x: Float, val y: Float, val size: Float, val offset: Float)

@Composable
fun StarField(modifier: Modifier = Modifier) {
    val stars = remember {
        List(80) {
            Star(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat(),
                offset = Random.nextFloat() * 2f * PI.toFloat()
            )
        }
    }

    val t by rememberInfiniteTransition(label = "stars").animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing)),
        label = "t"
    )

    Canvas(modifier) {
        stars.forEach { s ->
            val twinkle = 0.55f + 0.45f * sin(t + s.offset)
            drawCircle(
                color = Color.White.copy(alpha = (0.10f + s.size * 0.35f) * twinkle),
                radius = (0.8f + s.size * 1.6f).dp.toPx(),
                center = Offset(s.x * size.width, s.y * size.height)
            )
        }
    }
}