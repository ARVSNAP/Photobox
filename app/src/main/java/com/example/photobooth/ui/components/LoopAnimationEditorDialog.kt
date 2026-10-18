package com.example.photobooth.ui.components

import android.net.Uri
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.scale
import androidx.compose.animation.animateColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.photobooth.model.LoopAnimationConfig
import com.example.photobooth.utils.LoopAnimationStorage
import com.example.ui.theme.*

@Composable
fun LoopAnimationEditorDialog(
    config: LoopAnimationConfig,
    onConfigChanged: (LoopAnimationConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var isEnabled by remember(config) { mutableStateOf(config.isEnabled) }
    var selectedPreset by remember(config) { mutableStateOf(config.animationPreset) }
    // Check for previously saved permanent loop file if current config doesn't have one
    val initialPermanentFile = remember { LoopAnimationStorage.getSavedCustomLoopFile(context) }
    var externalFileUri by remember(config) {
        mutableStateOf(config.externalFileUri ?: initialPermanentFile?.first?.toString())
    }
    var externalFileType by remember(config) {
        mutableStateOf(config.externalFileType ?: initialPermanentFile?.second)
    }
    var customSpeed by remember(config) { mutableStateOf(config.customSpeed) }
    var selectedScaleMode by remember(config) { mutableStateOf(config.scaleMode) }

    // Custom Coil GIF Loader
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

    // Media Pickers with permanent internal storage copying
    val pickGifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val permanentUri = LoopAnimationStorage.saveExternalFilePermanently(context, uri, "gif")
            if (permanentUri != null) {
                externalFileUri = permanentUri.toString()
                externalFileType = "gif"
                selectedPreset = "Custom Loop"
                Toast.makeText(context, "Loop GIF berhasil disimpan sebagai preset permanen!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Gagal menyimpan file GIF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val pickMp4Launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val permanentUri = LoopAnimationStorage.saveExternalFilePermanently(context, uri, "mp4")
            if (permanentUri != null) {
                externalFileUri = permanentUri.toString()
                externalFileType = "mp4"
                selectedPreset = "Custom Loop"
                Toast.makeText(context, "Loop MP4 video berhasil disimpan sebagai preset permanen!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Gagal menyimpan file MP4", Toast.LENGTH_SHORT).show()
            }
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
                border = BorderStroke(1.dp, NeonRose.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.85f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "LOOP ANIMATION MANAGER",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Add beautiful looping background motions to welcome screen",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Content Scroll Block
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Live Preview Panel
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
                            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isEnabled) {
                                    if (selectedPreset.startsWith("Custom") && externalFileUri != null) {
                                        if (externalFileType == "mp4") {
                                            Mp4LoopPlayer(
                                                uriString = externalFileUri!!,
                                                scaleMode = selectedScaleMode,
                                                speed = customSpeed,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            // GIF via Coil
                                            val gifScale = when (selectedScaleMode) {
                                                "fill" -> androidx.compose.ui.layout.ContentScale.FillBounds
                                                "crop" -> androidx.compose.ui.layout.ContentScale.Crop
                                                else -> androidx.compose.ui.layout.ContentScale.Fit
                                            }
                                            AsyncImage(
                                                model = Uri.parse(externalFileUri),
                                                contentDescription = "Custom GIF Loop Preview",
                                                imageLoader = gifImageLoader,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = gifScale
                                            )
                                        }
                                    } else {
                                        // Preset preview generator
                                        LoopPresetRenderer(preset = selectedPreset)
                                    }
                                } else {
                                    Text(
                                        text = "Loop Animation is Disabled",
                                        color = TextMuted,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Overlay Label
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(10.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xCC000000))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "LIVE PREVIEW",
                                        color = NeonRose,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }

                        // 2. Enable/Disable Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x22FFFFFF))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable Loop Motion Effect",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Toggle background ambient animations on/off",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { isEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NeonRose,
                                    checkedTrackColor = NeonRose.copy(alpha = 0.5f),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0x33FFFFFF)
                                )
                            )
                        }

                        // 3. Preset Selector Section
                        if (isEnabled) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "CHOOSE AMBIENT PRESET",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    if (externalFileUri != null) {
                                        Text(
                                            text = "1 Preset Kustom Tersimpan",
                                            color = Color(0xFF34D399),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                val presetsRow1 = listOf("Neon Pulse", "Disco Stars")
                                val presetsRow2 = listOf("Retro Scanline", "Custom Loop")

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    presetsRow1.forEach { preset ->
                                        val isSel = selectedPreset == preset
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSel) NeonRose.copy(alpha = 0.2f) else Color(0x11FFFFFF))
                                                .border(
                                                    BorderStroke(
                                                        1.5.dp,
                                                        if (isSel) NeonRose else Color(0x22FFFFFF)
                                                    ), RoundedCornerShape(12.dp)
                                                )
                                                .clickable { selectedPreset = preset }
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = preset,
                                                color = if (isSel) Color.White else TextMuted,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    presetsRow2.forEach { preset ->
                                        val isSel = if (preset == "Custom Loop") {
                                            selectedPreset.startsWith("Custom")
                                        } else {
                                            selectedPreset == preset
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isSel) {
                                                        if (preset == "Custom Loop") Color(0x3310B981) else NeonRose.copy(alpha = 0.2f)
                                                    } else Color(0x11FFFFFF)
                                                )
                                                .border(
                                                    BorderStroke(
                                                        1.5.dp,
                                                        if (isSel) {
                                                            if (preset == "Custom Loop") Color(0xFF10B981) else NeonRose
                                                        } else Color(0x22FFFFFF)
                                                    ), RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    selectedPreset = preset
                                                    if (preset == "Custom Loop" && externalFileUri == null) {
                                                        Toast.makeText(context, "Silakan unggah GIF/MP4 di bawah untuk mengaktifkan preset kustom ini", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                .padding(vertical = 12.dp, horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                if (preset == "Custom Loop" && externalFileUri != null) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = Color(0xFF34D399),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                                Text(
                                                    text = if (preset == "Custom Loop" && externalFileUri != null) "Custom (Permanen)" else preset,
                                                    color = if (isSel) Color.White else TextMuted,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 4. Custom Loop File Picker & Permanent Preset Management
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF)),
                                border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "UNGGAH PRESET PERMANEN (.GIF / .MP4)",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0x2610B981),
                                            border = BorderStroke(1.dp, Color(0x6610B981))
                                        ) {
                                            Text(
                                                text = "PERMANEN",
                                                color = Color(0xFF34D399),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Animasi yang Anda unggah otomatis disimpan permanen di memori aplikasi sehingga tetap aktif dan tidak hilang saat aplikasi dikeluarkan.",
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = { pickGifLauncher.launch("image/gif") },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x331E293B)),
                                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Unggah GIF", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = { pickMp4Launcher.launch("video/mp4") },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x331E293B)),
                                            border = BorderStroke(1.dp, NeonRose.copy(alpha = 0.6f)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(imageVector = Icons.Default.Movie, contentDescription = null, tint = NeonRose, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Unggah MP4", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (externalFileUri != null) {
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0x2210B981)),
                                            border = BorderStroke(1.dp, Color(0x4410B981)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (externalFileType == "mp4") Icons.Default.Movie else Icons.Default.Star,
                                                            contentDescription = null,
                                                            tint = Color(0xFF34D399),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Text(
                                                            text = "Preset Aktif: Custom (${externalFileType?.uppercase() ?: "GIF"})",
                                                            color = Color.White,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Text(
                                                        text = "Tersimpan permanen di memori & tidak akan hilang saat keluar",
                                                        color = Color(0xFF86EFAC),
                                                        fontSize = 10.sp
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        LoopAnimationStorage.deleteSavedCustomLoop(context)
                                                        externalFileUri = null
                                                        externalFileType = null
                                                        selectedPreset = "Neon Pulse"
                                                        Toast.makeText(context, "Preset kustom dihapus dan direset", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Hapus Preset Permanen",
                                                        tint = Color(0xFFFCA5A5),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Scale mode options for Full screen & Uncropped
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "MODE TAMPILAN (SKALA VIDEO / GIF):",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val modes = listOf(
                                                Triple("fit", "Fit (Utuh)", "Tidak terpotong"),
                                                Triple("fill", "Penuh (Fill)", "Isi layar"),
                                                Triple("crop", "Zoom (Crop)", "Potong tepi")
                                            )
                                            modes.forEach { (modeKey, modeTitle, modeDesc) ->
                                                val isSel = selectedScaleMode == modeKey
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(if (isSel) NeonRose.copy(alpha = 0.25f) else Color(0x11FFFFFF))
                                                        .border(
                                                            BorderStroke(
                                                                1.5.dp,
                                                                if (isSel) NeonRose else Color(0x22FFFFFF)
                                                            ), RoundedCornerShape(10.dp)
                                                        )
                                                        .clickable { selectedScaleMode = modeKey }
                                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(
                                                            text = modeTitle,
                                                            color = if (isSel) Color.White else TextMuted,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            textAlign = TextAlign.Center
                                                        )
                                                        Text(
                                                            text = modeDesc,
                                                            color = if (isSel) Color(0xFF34D399) else TextMuted.copy(alpha = 0.7f),
                                                            fontSize = 8.5.sp,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Batal")
                        }

                        Button(
                            onClick = {
                                val finalPreset = if (selectedPreset.startsWith("Custom")) "Custom Loop" else selectedPreset
                                val newConfig = LoopAnimationConfig(
                                    isEnabled = isEnabled,
                                    animationPreset = finalPreset,
                                    externalFileUri = externalFileUri,
                                    externalFileType = externalFileType,
                                    customSpeed = customSpeed,
                                    customPresetName = if (finalPreset == "Custom Loop") "Custom Loop" else finalPreset,
                                    scaleMode = selectedScaleMode
                                )
                                LoopAnimationStorage.saveLoopConfig(context, newConfig)
                                onConfigChanged(newConfig)
                                Toast.makeText(context, "Preset loop animasi berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan Loop")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoopPresetRenderer(
    modifier: Modifier = Modifier,
    preset: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "preset_loop_render")

    when (preset) {
        "Disco Stars" -> {
            val starCount = 30
            val alphas = (0 until starCount).map { i ->
                infiniteTransition.animateFloat(
                    initialValue = 0.1f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween((800 + i * 150) % 1500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "star_alpha_$i"
                )
            }
            Canvas(modifier = modifier.fillMaxSize().background(Color(0xFF030712))) {
                val rand = java.util.Random(42)
                for (i in 0 until starCount) {
                    val x = rand.nextFloat() * size.width
                    val y = rand.nextFloat() * size.height
                    val radius = rand.nextFloat() * 4.dp.toPx() + 2f
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
                    animation = tween(2500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "scanline_offset"
            )
            Canvas(modifier = modifier.fillMaxSize().background(Color(0xFF090514))) {
                // Background Grid
                val colSpacing = 24.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(Color(0x1A00F5FF), Offset(x, 0f), Offset(x, size.height), 1f)
                    x += colSpacing
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(Color(0x1A00F5FF), Offset(0f, y), Offset(size.width, y), 1f)
                    y += colSpacing
                }

                // Laser scanline horizontal line
                val scanY = scanlineOffset * size.height
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color(0xFFFF007F), Color.Transparent)
                    ),
                    start = Offset(0f, scanY),
                    end = Offset(size.width, scanY),
                    strokeWidth = 3.dp.toPx()
                )
            }
        }
        else -> {
            // Neon Pulse
            val scalePulse by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "neon_scale"
            )
            val glowColor by infiniteTransition.animateColor(
                initialValue = Color(0xFFFF007F),
                targetValue = Color(0xFF00F5FF),
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "neon_color"
            )
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color(0xFF05030A)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(scalePulse)
                        .blur(16.dp)
                        .background(glowColor.copy(alpha = 0.4f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .border(BorderStroke(2.dp, glowColor), CircleShape)
                )
            }
        }
    }
}
