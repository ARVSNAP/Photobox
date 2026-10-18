package com.example.photobooth.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.photobooth.model.FLASH_COLOR_PRESETS
import com.example.photobooth.model.FlashAnimationConfig
import com.example.photobooth.model.FlashStyle
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FlashEditorDialog(
    config: FlashAnimationConfig,
    onConfigChanged: (FlashAnimationConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStyle by remember(config) { mutableStateOf(config.style) }
    var selectedColorHex by remember(config) { mutableStateOf(config.colorHex) }
    var selectedDuration by remember(config) { mutableStateOf(config.durationMillis) }
    var selectedIntensity by remember(config) { mutableStateOf(config.maxIntensity) }
    var playSound by remember(config) { mutableStateOf(config.playSoundEffect) }

    // Live preview testing state inside dialog
    var isTestFlashing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val currentTempConfig = remember(selectedStyle, selectedColorHex, selectedDuration, selectedIntensity, playSound) {
        FlashAnimationConfig(
            style = selectedStyle,
            colorHex = selectedColorHex,
            durationMillis = selectedDuration,
            maxIntensity = selectedIntensity,
            playSoundEffect = playSound
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000)),
            contentAlignment = Alignment.Center
        ) {
            // Screen Flash Test Overlay behind/over preview
            if (isTestFlashing) {
                FlashAnimationOverlay(
                    isTriggered = isTestFlashing,
                    config = currentTempConfig,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, NeonRose.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.90f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Dialog Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(NeonRose.copy(alpha = 0.2f))
                                    .border(BorderStroke(1.dp, NeonRose), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = NeonRose,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Kustomisasi Efek Flash Layar",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Atur animasi, warna cahaya, durasi & intensitas blitz",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x331E293B))
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0x22FFFFFF))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Scrollable Configuration Body
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // 1. Gaya Animasi Flash (Flash Style)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(imageVector = Icons.Default.Flare, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                Text(text = "GAYA ANIMASI KILATAN", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                FlashStyle.values().forEach { style ->
                                    val isSelected = style == selectedStyle
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) NeonRose.copy(alpha = 0.2f) else Color(0x331E293B)
                                        ),
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) NeonRose else Color(0x22FFFFFF)
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedStyle = style }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = style.displayName,
                                                    color = if (isSelected) Color.White else TextPrimary,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                    fontSize = 15.sp
                                                )
                                                Text(
                                                    text = style.description,
                                                    color = TextMuted,
                                                    fontSize = 12.sp
                                                )
                                            }

                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(NeonRose),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Warna Cahaya Flash (Color Swatches)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(imageVector = Icons.Default.LightMode, contentDescription = null, tint = Color(0xFFFFB703), modifier = Modifier.size(18.dp))
                                Text(text = "WARNA TINT KILATAN", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                items(FLASH_COLOR_PRESETS) { colorOption ->
                                    val isSelected = colorOption.colorHex == selectedColorHex
                                    val color = Color(colorOption.colorHex)

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable { selectedColorHex = colorOption.colorHex }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .border(
                                                    width = if (isSelected) 3.dp else 1.dp,
                                                    color = if (isSelected) NeonRose else Color(0x55FFFFFF),
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = if (colorOption.colorHex == 0xFFFFFFFFL || colorOption.colorHex == 0xFFFFE5B4L) Color.Black else Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = colorOption.name,
                                            color = if (isSelected) Color.White else TextMuted,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Durasi / Kecepatan Animasi
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                    Text(text = "DURASI KILATAN", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "${selectedDuration}ms",
                                    color = NeonCyan,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val presets = listOf(
                                    "Cepat (150ms)" to 150,
                                    "Normal (300ms)" to 300,
                                    "Sinematik (500ms)" to 500,
                                    "Panjang (800ms)" to 800
                                )
                                presets.forEach { (label, ms) ->
                                    val isSelected = selectedDuration == ms
                                    Button(
                                        onClick = { selectedDuration = ms },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) NeonCyan else Color(0x331E293B)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = label.split(" ")[0],
                                            color = if (isSelected) Color.Black else TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // 4. Intensitas / Kecerahan Flash
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = NeonRose, modifier = Modifier.size(18.dp))
                                    Text(text = "INTENSITAS CAHAYA", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "${(selectedIntensity * 100).toInt()}%",
                                    color = NeonRose,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Slider(
                                value = selectedIntensity,
                                onValueChange = { selectedIntensity = it },
                                valueRange = 0.4f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = NeonRose,
                                    activeTrackColor = NeonRose,
                                    inactiveTrackColor = Color(0x33FFFFFF)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0x22FFFFFF))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Bottom Action Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Test Button
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    isTestFlashing = true
                                    delay(selectedDuration.toLong() + 100L)
                                    isTestFlashing = false
                                }
                            },
                            border = BorderStroke(1.5.dp, NeonCyan),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("UJI KILATAN (TEST)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        // Save Button
                        Button(
                            onClick = {
                                onConfigChanged(currentTempConfig)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(50.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SIMPAN PENGATURAN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
