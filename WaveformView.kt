package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JarvisAqua
import com.example.ui.theme.JarvisCyan
import kotlin.math.sin

@Composable
fun WaveformView(
    isActive: Boolean = true,
    barCount: Int = 24,
    color: Color = JarvisCyan,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveformTransition")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveformPhase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val width = size.width
        val height = size.height
        val barWidth = (width / (barCount * 1.5f)).coerceAtLeast(3.dp.toPx())
        val spacing = (width - (barCount * barWidth)) / (barCount - 1).coerceAtLeast(1)

        for (i in 0 until barCount) {
            val x = i * (barWidth + spacing)
            val normalizedIndex = i.toFloat() / barCount

            val wave = if (isActive) {
                val s1 = sin(phase + normalizedIndex * 4 * Math.PI).toFloat()
                val s2 = sin(phase * 1.5f + normalizedIndex * 2 * Math.PI).toFloat()
                ((s1 + s2) / 2f + 1f) / 2f // normalize between 0.1 and 1.0
            } else {
                0.15f
            }

            val barHeight = (height * 0.15f + height * 0.75f * wave).coerceIn(4.dp.toPx(), height)
            val y = (height - barHeight) / 2f

            drawRoundRect(
                color = if (isActive) color else JarvisAqua.copy(alpha = 0.3f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
