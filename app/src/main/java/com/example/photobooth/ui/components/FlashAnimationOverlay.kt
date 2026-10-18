package com.example.photobooth.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.photobooth.model.FlashAnimationConfig
import com.example.photobooth.model.FlashStyle

@Composable
fun FlashAnimationOverlay(
    isTriggered: Boolean,
    config: FlashAnimationConfig,
    modifier: Modifier = Modifier
) {
    val flashColor = Color(config.colorHex)

    val transition = updateTransition(targetState = isTriggered, label = "FlashTransition")

    when (config.style) {
        FlashStyle.SOLID_BURST -> {
            val alpha by transition.animateFloat(
                transitionSpec = {
                    if (targetState) {
                        tween(durationMillis = (config.durationMillis * 0.2f).toInt(), easing = FastOutLinearInEasing)
                    } else {
                        tween(durationMillis = config.durationMillis, easing = LinearOutSlowInEasing)
                    }
                },
                label = "solid_burst_alpha"
            ) { triggered ->
                if (triggered) config.maxIntensity else 0f
            }

            if (alpha > 0f) {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .background(flashColor.copy(alpha = alpha))
                )
            }
        }

        FlashStyle.RING_RIPPLE -> {
            val progress by transition.animateFloat(
                transitionSpec = {
                    tween(durationMillis = config.durationMillis, easing = FastOutSlowInEasing)
                },
                label = "ring_progress"
            ) { triggered ->
                if (triggered) 1f else 0f
            }

            if (progress > 0f) {
                Canvas(modifier = modifier.fillMaxSize()) {
                    val maxRadius = kotlin.math.hypot(size.width, size.height) / 1.5f
                    val currentRadius = maxRadius * progress
                    val alpha = (1f - progress) * config.maxIntensity

                    // Outer pulse gradient
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                flashColor.copy(alpha = alpha),
                                flashColor.copy(alpha = alpha * 0.5f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = currentRadius
                        ),
                        center = center,
                        radius = currentRadius
                    )

                    // Sharp concentric ring
                    drawCircle(
                        color = flashColor.copy(alpha = alpha),
                        center = center,
                        radius = currentRadius * 0.9f,
                        style = Stroke(width = 16.dp.toPx() * (1f - progress))
                    )
                }
            }
        }

        FlashStyle.NEON_EDGE -> {
            val borderAlpha by transition.animateFloat(
                transitionSpec = {
                    tween(durationMillis = config.durationMillis, easing = FastOutSlowInEasing)
                },
                label = "neon_edge_alpha"
            ) { triggered ->
                if (triggered) 1f else 0f
            }

            if (borderAlpha > 0f) {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .background(flashColor.copy(alpha = 0.25f * borderAlpha * config.maxIntensity))
                        .border(
                            width = (24 * borderAlpha).dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    flashColor.copy(alpha = borderAlpha * config.maxIntensity),
                                    flashColor.copy(alpha = 0.7f * borderAlpha * config.maxIntensity),
                                    flashColor.copy(alpha = borderAlpha * config.maxIntensity)
                                )
                            ),
                            shape = RoundedCornerShape(0.dp)
                        )
                )
            }
        }

        FlashStyle.DOUBLE_STROBE -> {
            val strobeAlpha by transition.animateFloat(
                transitionSpec = {
                    keyframes {
                        durationMillis = config.durationMillis
                        0f at 0
                        config.maxIntensity at (durationMillis * 0.15).toInt()
                        0.1f at (durationMillis * 0.35).toInt()
                        config.maxIntensity at (durationMillis * 0.55).toInt()
                        0f at durationMillis
                    }
                },
                label = "strobe_alpha"
            ) { triggered ->
                if (triggered) config.maxIntensity else 0f
            }

            if (strobeAlpha > 0f) {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .background(flashColor.copy(alpha = strobeAlpha))
                )
            }
        }

        FlashStyle.SOFT_DIFFUSE -> {
            val diffuseAlpha by transition.animateFloat(
                transitionSpec = {
                    tween(durationMillis = config.durationMillis, easing = LinearOutSlowInEasing)
                },
                label = "diffuse_alpha"
            ) { triggered ->
                if (triggered) 1f else 0f
            }

            if (diffuseAlpha > 0f) {
                Canvas(modifier = modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                flashColor.copy(alpha = config.maxIntensity * diffuseAlpha),
                                flashColor.copy(alpha = config.maxIntensity * 0.75f * diffuseAlpha),
                                flashColor.copy(alpha = 0.2f * diffuseAlpha)
                            ),
                            center = center,
                            radius = size.maxDimension / 1.2f
                        )
                    )
                }
            }
        }
    }
}
