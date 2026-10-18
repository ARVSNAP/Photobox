package com.example.photobooth.ui.components

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.photobooth.model.LayoutCardCustomization
import com.example.photobooth.ui.screens.LayoutOptionItem

@Composable
fun LayoutCardVisualPreview(
    layout: LayoutOptionItem,
    customization: LayoutCardCustomization?,
    modifier: Modifier = Modifier
) {
    val style = customization?.visualStyle ?: "default"
    val colorFilter = customization?.colorFilter ?: "none"

    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            layout.customImagePath != null -> {
                AsyncImage(
                    model = java.io.File(layout.customImagePath),
                    contentDescription = layout.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            style == "custom_image" && customization?.customImageUri != null -> {
                AsyncImage(
                    model = Uri.parse(customization.customImageUri),
                    contentDescription = layout.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            style == "blueprint" -> {
                BlueprintGridGraphic(layout = layout)
            }
            style == "neon_glow" -> {
                NeonGlowGraphic(layout = layout)
            }
            style == "polaroid" -> {
                PolaroidRetroGraphic(layout = layout)
            }
            style == "monolith" -> {
                MonolithGraphic(layout = layout)
            }
            else -> {
                // Default photo mockup
                if (layout.imageRes != 0) {
                    Image(
                        painter = painterResource(id = layout.imageRes),
                        contentDescription = layout.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LayoutWireframeVisual(
                        layout = layout,
                        accentColor = Color(0xFFA855F7),
                        frameBg = Color(0x33A855F7)
                    )
                }
            }
        }

        // Color filter tint overlay if configured
        when (colorFilter) {
            "rose" -> Box(modifier = Modifier.fillMaxSize().background(Color(0x33FF007F)))
            "cyan" -> Box(modifier = Modifier.fillMaxSize().background(Color(0x3300F0FF)))
            "amber" -> Box(modifier = Modifier.fillMaxSize().background(Color(0x33F59E0B)))
            "noir" -> Box(modifier = Modifier.fillMaxSize().background(Color(0x44000000)))
        }
    }
}

@Composable
private fun BlueprintGridGraphic(layout: LayoutOptionItem) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Grid pattern background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 20.dp.toPx()
            val lineColor = Color(0x2200F0FF)
            for (x in 0..(size.width / step).toInt()) {
                drawLine(lineColor, Offset(x * step, 0f), Offset(x * step, size.height), strokeWidth = 1f)
            }
            for (y in 0..(size.height / step).toInt()) {
                drawLine(lineColor, Offset(0f, y * step), Offset(size.width, y * step), strokeWidth = 1f)
            }
        }
        LayoutWireframeVisual(layout = layout, accentColor = Color(0xFF00F0FF), frameBg = Color(0x3300F0FF))
    }
}

@Composable
private fun NeonGlowGraphic(layout: LayoutOptionItem) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LayoutWireframeVisual(
            layout = layout,
            accentColor = Color(0xFFFF007F),
            frameBg = Color(0x44FF007F)
        )
    }
}

@Composable
private fun PolaroidRetroGraphic(layout: LayoutOptionItem) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LayoutWireframeVisual(
            layout = layout,
            accentColor = Color(0xFFFDE047),
            frameBg = Color(0x33FDE047)
        )
    }
}

@Composable
private fun MonolithGraphic(layout: LayoutOptionItem) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LayoutWireframeVisual(
            layout = layout,
            accentColor = Color(0xFFE2E8F0),
            frameBg = Color(0x22FFFFFF)
        )
    }
}

@Composable
private fun LayoutWireframeVisual(
    layout: LayoutOptionItem,
    accentColor: Color,
    frameBg: Color
) {
    Box(
        modifier = Modifier
            .size(width = 86.dp, height = 100.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.5.dp, accentColor, RoundedCornerShape(8.dp))
            .background(Color(0x44000000))
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            layout.customImagePath != null || layout.id.startsWith("custom_layout_") -> {
                when (layout.photoCount) {
                    1 -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(4.dp))
                                .background(frameBg)
                                .border(1.dp, accentColor, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    2 -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(3.dp)).background(frameBg).border(1.dp, accentColor, RoundedCornerShape(3.dp)))
                            Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(3.dp)).background(frameBg).border(1.dp, accentColor, RoundedCornerShape(3.dp)))
                        }
                    }
                    3 -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(frameBg)
                                        .border(1.dp, accentColor, RoundedCornerShape(3.dp))
                                )
                            }
                        }
                    }
                    8 -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            repeat(4) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(frameBg).border(0.8.dp, accentColor, RoundedCornerShape(2.dp)))
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(frameBg).border(0.8.dp, accentColor, RoundedCornerShape(2.dp)))
                                }
                            }
                        }
                    }
                    else -> { // Default 4 shots
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(frameBg).border(1.dp, accentColor, RoundedCornerShape(3.dp)))
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(frameBg).border(1.dp, accentColor, RoundedCornerShape(3.dp)))
                            }
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(frameBg).border(1.dp, accentColor, RoundedCornerShape(3.dp)))
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(frameBg).border(1.dp, accentColor, RoundedCornerShape(3.dp)))
                            }
                        }
                    }
                }
            }
            layout.id == "Single Shot" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(4.dp))
                        .background(frameBg)
                        .border(1.dp, accentColor, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            layout.id == "2x2 Grid" -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(frameBg).border(1.dp, accentColor, RoundedCornerShape(3.dp)))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(frameBg).border(1.dp, accentColor, RoundedCornerShape(3.dp)))
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(frameBg).border(1.dp, accentColor, RoundedCornerShape(3.dp)))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(frameBg).border(1.dp, accentColor, RoundedCornerShape(3.dp)))
                    }
                }
            }
            layout.id == "3-Photo Strip" -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(3.dp))
                                .background(frameBg)
                                .border(1.dp, accentColor, RoundedCornerShape(3.dp))
                        )
                    }
                }
            }
            layout.id == "4-Photo Grid" -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(2.dp))
                                .background(frameBg)
                                .border(0.8.dp, accentColor, RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
            layout.id == "Boothlab 8-Frame" -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    repeat(4) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(frameBg).border(0.8.dp, accentColor, RoundedCornerShape(2.dp)))
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(frameBg).border(0.8.dp, accentColor, RoundedCornerShape(2.dp)))
                        }
                    }
                }
            }
            else -> { // GIF / Boomerang
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(4.dp))
                        .background(frameBg)
                        .border(1.dp, accentColor, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
