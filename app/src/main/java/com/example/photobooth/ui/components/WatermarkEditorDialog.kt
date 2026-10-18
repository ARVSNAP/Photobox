package com.example.photobooth.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.photobooth.model.WatermarkConfig
import com.example.photobooth.model.WatermarkCorner
import com.example.photobooth.model.WatermarkPreset
import com.example.photobooth.utils.WatermarkHelper
import com.example.ui.theme.*

@Composable
fun WatermarkEditorDialog(
    config: WatermarkConfig,
    onConfigChanged: (WatermarkConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var isEnabled by remember(config) { mutableStateOf(config.isEnabled) }
    var selectedCorner by remember(config) { mutableStateOf(config.corner) }
    var selectedPreset by remember(config) { mutableStateOf(config.preset) }
    var customBitmap by remember(config) { mutableStateOf(config.customBitmap) }
    var customFileName by remember(config) { mutableStateOf(config.customFileName) }
    var selectedScale by remember(config) { mutableStateOf(config.scalePercent) }
    var selectedOpacity by remember(config) { mutableStateOf(config.opacity) }
    var selectedMargin by remember(config) { mutableStateOf(config.marginPercent) }

    // Launcher for selecting a custom PNG from device storage
    val pickPngLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = WatermarkHelper.decodePngFromUri(context, uri)
            if (bitmap != null) {
                customBitmap = bitmap
                selectedPreset = WatermarkPreset.CUSTOM_UPLOAD
                customFileName = uri.lastPathSegment?.substringAfterLast('/') ?: "custom_watermark.png"
                Toast.makeText(context, "PNG Watermark berhasil dimuat!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Gagal memuat format gambar PNG", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val currentTempConfig = remember(isEnabled, selectedCorner, selectedPreset, customBitmap, customFileName, selectedScale, selectedOpacity, selectedMargin) {
        WatermarkConfig(
            isEnabled = isEnabled,
            corner = selectedCorner,
            preset = selectedPreset,
            customBitmap = customBitmap,
            customFileName = customFileName,
            scalePercent = selectedScale,
            opacity = selectedOpacity,
            marginPercent = selectedMargin
        )
    }

    // Sample preview background bitmap with live watermark applied
    val sampleWatermarkedBitmap = remember(currentTempConfig) {
        val sampleBg = createSamplePhotoBitmap()
        if (isEnabled) {
            WatermarkHelper.applyWatermark(sampleBg, currentTempConfig)
        } else {
            sampleBg
        }
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
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.92f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Header
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
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = 0.2f))
                                    .border(BorderStroke(1.dp, NeonCyan), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Pengaturan Watermark PNG Otomatis",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Cap logo/branding transparan otomatis di sudut setiap foto hasil jepretan",
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

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0x22FFFFFF))
                    Spacer(modifier = Modifier.height(14.dp))

                    // 2-Column Split: Controls on Left, Live Preview on Right
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Left Column: Scrollable Settings
                        Column(
                            modifier = Modifier
                                .weight(1.3f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            // 1. Enable / Disable Master Toggle
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isEnabled) NeonCyan.copy(alpha = 0.15f) else Color(0x331E293B)
                                ),
                                border = BorderStroke(1.dp, if (isEnabled) NeonCyan else Color(0x22FFFFFF)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
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
                                            text = if (isEnabled) "Watermark Aktif" else "Watermark Nonaktif",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = if (isEnabled) "PNG otomatis ditempelkan ke setiap jepretan foto & GIF" else "Foto disimpan polos tanpa logo",
                                            color = TextMuted,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Switch(
                                        checked = isEnabled,
                                        onCheckedChange = { isEnabled = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = NeonCyan
                                        )
                                    )
                                }
                            }

                            // 2. Corner Placement (Visual 2x2 Grid)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Place, contentDescription = null, tint = NeonRose, modifier = Modifier.size(18.dp))
                                    Text(text = "POSISI SUDUT (CORNER)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CornerButton(
                                        title = "Kiri Atas",
                                        subtitle = "Top-Left",
                                        corner = WatermarkCorner.TOP_LEFT,
                                        isSelected = selectedCorner == WatermarkCorner.TOP_LEFT,
                                        modifier = Modifier.weight(1f)
                                    ) { selectedCorner = WatermarkCorner.TOP_LEFT }

                                    CornerButton(
                                        title = "Kanan Atas",
                                        subtitle = "Top-Right",
                                        corner = WatermarkCorner.TOP_RIGHT,
                                        isSelected = selectedCorner == WatermarkCorner.TOP_RIGHT,
                                        modifier = Modifier.weight(1f)
                                    ) { selectedCorner = WatermarkCorner.TOP_RIGHT }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CornerButton(
                                        title = "Kiri Bawah",
                                        subtitle = "Bottom-Left",
                                        corner = WatermarkCorner.BOTTOM_LEFT,
                                        isSelected = selectedCorner == WatermarkCorner.BOTTOM_LEFT,
                                        modifier = Modifier.weight(1f)
                                    ) { selectedCorner = WatermarkCorner.BOTTOM_LEFT }

                                    CornerButton(
                                        title = "Kanan Bawah",
                                        subtitle = "Bottom-Right",
                                        corner = WatermarkCorner.BOTTOM_RIGHT,
                                        isSelected = selectedCorner == WatermarkCorner.BOTTOM_RIGHT,
                                        modifier = Modifier.weight(1f)
                                    ) { selectedCorner = WatermarkCorner.BOTTOM_RIGHT }
                                }
                            }

                            // 3. Watermark Source: Upload Custom PNG & Presets
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                    Text(text = "PILIHAN LOGO / LOGOMARK PNG", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }

                                // Upload Custom PNG Box
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selectedPreset == WatermarkPreset.CUSTOM_UPLOAD) NeonRose.copy(alpha = 0.2f) else Color(0x331E293B)
                                    ),
                                    border = BorderStroke(
                                        width = if (selectedPreset == WatermarkPreset.CUSTOM_UPLOAD) 1.5.dp else 1.dp,
                                        color = if (selectedPreset == WatermarkPreset.CUSTOM_UPLOAD) NeonRose else Color(0x22FFFFFF)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedPreset = WatermarkPreset.CUSTOM_UPLOAD
                                            pickPngLauncher.launch("image/png")
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0x33FFFFFF)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (customBitmap != null) {
                                                    Image(
                                                        bitmap = customBitmap!!.asImageBitmap(),
                                                        contentDescription = "Custom PNG",
                                                        modifier = Modifier.fillMaxSize().padding(4.dp)
                                                    )
                                                } else {
                                                    Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, tint = NeonRose)
                                                }
                                            }

                                            Column {
                                                Text(
                                                    text = if (customBitmap != null) "Kustom PNG: ${customFileName ?: "Terpasang"}" else "Upload File PNG Kustom Operator",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = if (customBitmap != null) "Klik untuk mengganti file gambar PNG" else "Pilih file PNG transparan dari penyimpanan perangkat",
                                                    color = TextMuted,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        if (customBitmap != null) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Button(
                                                    onClick = {
                                                        customBitmap = null
                                                        customFileName = null
                                                        selectedPreset = WatermarkPreset.STUDIO_PRO
                                                        Toast.makeText(context, "Logo kustom berhasil dihapus!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF007F)),
                                                    border = BorderStroke(1.dp, NeonRose),
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                                ) {
                                                    Text("HAPUS 🗑️", fontSize = 11.sp, color = NeonRose, fontWeight = FontWeight.Bold)
                                                }

                                                Button(
                                                    onClick = {
                                                        selectedPreset = WatermarkPreset.CUSTOM_UPLOAD
                                                        pickPngLauncher.launch("image/png")
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text("GANTI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    selectedPreset = WatermarkPreset.CUSTOM_UPLOAD
                                                    pickPngLauncher.launch("image/png")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text("PILIH PNG", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                // Built-in Preset Badges Carousel
                                Text(text = "Atau Pilih Preset Studio Bawaan:", color = TextMuted, fontSize = 12.sp)

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                ) {
                                    val presets = listOf(
                                        WatermarkPreset.STUDIO_PRO,
                                        WatermarkPreset.EVENT_VIP,
                                        WatermarkPreset.NEON_PARTY,
                                        WatermarkPreset.RETRO_STAMP
                                    )
                                    items(presets) { preset ->
                                        val isSelected = (selectedPreset == preset)
                                        val presetBitmap = remember(preset) { WatermarkHelper.generatePresetWatermark(preset) }

                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color(0x331E293B)
                                            ),
                                            border = BorderStroke(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) NeonCyan else Color(0x22FFFFFF)
                                            ),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .width(130.dp)
                                                .clickable { selectedPreset = preset }
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(54.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0x33000000)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Image(
                                                        bitmap = presetBitmap.asImageBitmap(),
                                                        contentDescription = preset.displayName,
                                                        modifier = Modifier.fillMaxSize().padding(4.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                Text(
                                                    text = preset.displayName,
                                                    color = if (isSelected) Color.White else TextPrimary,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 4. Scale / Size Slider
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(imageVector = Icons.Default.PhotoSizeSelectLarge, contentDescription = null, tint = NeonRose, modifier = Modifier.size(16.dp))
                                        Text(text = "UKURAN RELATIF (SCALE)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = "${(selectedScale * 100).toInt()}% Lebar Foto",
                                        color = NeonRose,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Slider(
                                    value = selectedScale,
                                    onValueChange = { selectedScale = it },
                                    valueRange = 0.08f..0.35f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = NeonRose,
                                        activeTrackColor = NeonRose,
                                        inactiveTrackColor = Color(0x33FFFFFF)
                                    )
                                )
                            }

                            // 5. Opacity / Transparency Slider
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(imageVector = Icons.Default.Opacity, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                        Text(text = "OPASITAS / TRANSPARANSI", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = "${(selectedOpacity * 100).toInt()}%",
                                        color = NeonCyan,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Slider(
                                    value = selectedOpacity,
                                    onValueChange = { selectedOpacity = it },
                                    valueRange = 0.20f..1.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = NeonCyan,
                                        activeTrackColor = NeonCyan,
                                        inactiveTrackColor = Color(0x33FFFFFF)
                                    )
                                )
                            }
                        }

                        // Right Column: Live Interactive Watermark Preview
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0x331E293B)),
                            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                    Text(text = "Live Watermark Preview", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(BorderStroke(2.dp, if (isEnabled) NeonCyan.copy(alpha = 0.6f) else Color(0x22FFFFFF)), RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = sampleWatermarkedBitmap.asImageBitmap(),
                                        contentDescription = "Sample Watermark Preview",
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    if (!isEnabled) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0x99000000)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "WATERMARK NONAKTIF",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Logo otomatis tertempel pada posisi ${selectedCorner.displayName} dengan opasitas ${(selectedOpacity * 100).toInt()}%",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0x22FFFFFF))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Dialog Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text("BATAL", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                onConfigChanged(currentTempConfig)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1.4f)
                                .height(50.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SIMPAN PENGATURAN WATERMARK", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CornerButton(
    title: String,
    subtitle: String,
    corner: WatermarkCorner,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color(0x331E293B)
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) NeonCyan else Color(0x22FFFFFF)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    color = if (isSelected) Color.White else TextPrimary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp
                )
                Text(text = subtitle, color = TextMuted, fontSize = 10.sp)
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(NeonCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

/**
 * Creates a synthetic stylish photo for real-time watermark preview in the dialog.
 */
private fun createSamplePhotoBitmap(): android.graphics.Bitmap {
    val width = 600
    val height = 450
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    // Gradient background representing a photobooth scene
    val shader = android.graphics.LinearGradient(
        0f, 0f, width.toFloat(), height.toFloat(),
        intArrayOf(
            android.graphics.Color.parseColor("#312E81"),
            android.graphics.Color.parseColor("#4C1D95"),
            android.graphics.Color.parseColor("#831843")
        ),
        null,
        android.graphics.Shader.TileMode.CLAMP
    )
    val bgPaint = android.graphics.Paint().apply { this.shader = shader }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Silhouette portrait art
    val facePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#FBBF24")
        isAntiAlias = true
    }
    canvas.drawCircle(width / 2f, height / 2f - 40f, 75f, facePaint)

    val bodyPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#60A5FA")
        isAntiAlias = true
    }
    canvas.drawRoundRect(
        android.graphics.RectF(width / 2f - 110f, height / 2f + 50f, width / 2f + 110f, height.toFloat()),
        40f, 40f, bodyPaint
    )

    // Camera viewfinder grid
    val gridPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#44FFFFFF")
        strokeWidth = 2f
    }
    canvas.drawLine(width / 3f, 0f, width / 3f, height.toFloat(), gridPaint)
    canvas.drawLine(2 * width / 3f, 0f, 2 * width / 3f, height.toFloat(), gridPaint)
    canvas.drawLine(0f, height / 3f, width.toFloat(), height / 3f, gridPaint)
    canvas.drawLine(0f, 2 * height / 3f, width.toFloat(), 2 * height / 3f, gridPaint)

    return bitmap
}
