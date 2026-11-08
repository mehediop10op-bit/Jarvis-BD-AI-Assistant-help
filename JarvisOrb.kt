package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisAqua
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisPurple
import kotlin.math.cos
import kotlin.math.sin

enum class OrbState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING
}

@Composable
fun JarvisOrb(
    state: OrbState = OrbState.IDLE,
    audioLevel: Float = 0f,
    size: Dp = 180.dp,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "JarvisOrbTransition")

    // Continuous rotation for HUD rings
    val rotationClockwise by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationClockwise"
    )

    val rotationCounterClockwise by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationCounterClockwise"
    )

    // Pulse core
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Dynamic state colors
    val targetCoreColor = when (state) {
        OrbState.IDLE -> JarvisCyan
        OrbState.LISTENING -> JarvisAmber
        OrbState.PROCESSING -> JarvisPurple
        OrbState.SPEAKING -> JarvisGreen
    }

    val animatedCoreColor by animateColorAsState(targetValue = targetCoreColor, label = "coreColor")
    val animatedOuterColor by animateColorAsState(
        targetValue = when (state) {
            OrbState.IDLE -> JarvisAqua
            OrbState.LISTENING -> Color(0xFFFB8500)
            OrbState.PROCESSING -> Color(0xFFB5179E)
            OrbState.SPEAKING -> Color(0xFF2EC4B6)
        },
        label = "outerColor"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clickable(onClick = onClick)
            .testTag("jarvis_orb_button")
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension / 2.2f

            // Audio level amplification
            val audioMod = (audioLevel.coerceIn(0f, 10f) / 10f) * (baseRadius * 0.25f)
            val currentRadius = baseRadius * pulseScale + audioMod

            // 1. Outermost glow wave
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedCoreColor.copy(alpha = 0.35f),
                        animatedOuterColor.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = currentRadius * 1.3f
                ),
                radius = currentRadius * 1.3f,
                center = center
            )

            // 2. Outer segmented HUD Ring (rotating clockwise)
            val outerDash = PathEffect.dashPathEffect(floatArrayOf(30f, 15f, 10f, 15f), rotationClockwise)
            drawCircle(
                color = animatedOuterColor.copy(alpha = 0.7f),
                radius = baseRadius * 0.95f,
                center = center,
                style = Stroke(width = 3.dp.toPx(), pathEffect = outerDash)
            )

            // 3. Middle HUD Ring (rotating counter-clockwise with fine dashes)
            val middleDash = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), rotationCounterClockwise)
            drawCircle(
                color = animatedCoreColor.copy(alpha = 0.5f),
                radius = baseRadius * 0.75f,
                center = center,
                style = Stroke(width = 2.dp.toPx(), pathEffect = middleDash)
            )

            // 4. Inner Ring with 4 radial locator notches
            drawCircle(
                color = animatedCoreColor.copy(alpha = 0.6f),
                radius = baseRadius * 0.55f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            for (i in 0 until 4) {
                val angle = Math.toRadians((rotationClockwise + i * 90).toDouble())
                val startX = center.x + (baseRadius * 0.50f * cos(angle)).toFloat()
                val startY = center.y + (baseRadius * 0.50f * sin(angle)).toFloat()
                val endX = center.x + (baseRadius * 0.60f * cos(angle)).toFloat()
                val endY = center.y + (baseRadius * 0.60f * sin(angle)).toFloat()
                drawLine(
                    color = animatedCoreColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.5.dp.toPx()
                )
            }

            // 5. Central Reactor Glowing Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        animatedCoreColor,
                        animatedOuterColor.copy(alpha = 0.4f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 0.45f
                ),
                radius = baseRadius * 0.45f,
                center = center
            )

            // 6. Center-most bright point
            drawCircle(
                color = Color.White,
                radius = baseRadius * 0.12f,
                center = center
            )
        }
    }
}
