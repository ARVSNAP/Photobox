package com.example.photobooth.ui.components

import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.photobooth.model.LoopAnimationConfig

@Composable
fun LoopAnimationRenderer(
    modifier: Modifier = Modifier,
    config: LoopAnimationConfig
) {
    if (!config.isEnabled) return

    val context = LocalContext.current

    // GIF Image Loader
    val gifImageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (config.animationPreset.startsWith("Custom") && config.externalFileUri != null) {
            if (config.externalFileType == "mp4") {
                Mp4LoopPlayer(
                    uriString = config.externalFileUri,
                    scaleMode = config.scaleMode,
                    speed = config.customSpeed,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // GIF
                val gifContentScale = when (config.scaleMode) {
                    "fill" -> androidx.compose.ui.layout.ContentScale.FillBounds
                    "crop" -> androidx.compose.ui.layout.ContentScale.Crop
                    else -> androidx.compose.ui.layout.ContentScale.Fit
                }
                AsyncImage(
                    model = Uri.parse(config.externalFileUri),
                    contentDescription = "Custom GIF Loop Background",
                    imageLoader = gifImageLoader,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = gifContentScale,
                    alpha = 0.85f
                )
            }
        } else {
            // Render built-in presets
            val infiniteTransition = rememberInfiniteTransition(label = "preset_background_loop")

            when (config.animationPreset) {
                "Disco Stars" -> {
                    val starCount = 35
                    val alphas = (0 until starCount).map { i ->
                        infiniteTransition.animateFloat(
                            initialValue = 0.1f,
                            targetValue = 0.9f,
                            animationSpec = infiniteRepeatable(
                                animation = tween((900 + i * 160) % 1800, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "star_bg_alpha_$i"
                        )
                    }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val rand = java.util.Random(100)
                        for (i in 0 until starCount) {
                            val x = rand.nextFloat() * size.width
                            val y = rand.nextFloat() * size.height
                            val radius = rand.nextFloat() * 5.dp.toPx() + 3f
                            drawCircle(
                                color = Color.White.copy(alpha = alphas[i].value),
                                radius = radius,
                                center = Offset(x, y)
                            )
                        }
                    }
                }
                "Retro Scanline" -> {
                    val scanlineOffset by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(4000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "scanline_bg_offset"
                    )
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Background Grid
                        val colSpacing = 32.dp.toPx()
                        var x = 0f
                        while (x < size.width) {
                            drawLine(Color(0x0C00F5FF), Offset(x, 0f), Offset(x, size.height), 1f)
                            x += colSpacing
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(Color(0x0C00F5FF), Offset(0f, y), Offset(size.width, y), 1f)
                            y += colSpacing
                        }

                        // Glowing sweep line
                        val scanY = scanlineOffset * size.height
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, Color(0x33FF007F), Color.Transparent)
                            ),
                            start = Offset(0f, scanY),
                            end = Offset(size.width, scanY),
                            strokeWidth = 4.dp.toPx()
                        )
                    }
                }
                else -> {
                    // Neon Pulse
                    val scalePulse by infiniteTransition.animateFloat(
                        initialValue = 0.7f,
                        targetValue = 1.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2500, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "neon_bg_scale"
                    )
                    val glowColor by infiniteTransition.animateColor(
                        initialValue = Color(0xFFFF007F),
                        targetValue = Color(0xFF00F5FF),
                        animationSpec = infiniteRepeatable(
                            animation = tween(4000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "neon_bg_color"
                    )
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .scale(scalePulse)
                                .blur(48.dp)
                                .background(glowColor.copy(alpha = 0.15f), CircleShape)
                        )
                    }
                }
            }
        }
    }
}
