package com.example.photobooth.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.photobooth.model.*
import com.example.ui.theme.*
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.graphics.Brush

@Composable
fun BrandingFrameEditorDialog(
    config: BrandingFrameConfig,
    onConfigChanged: (BrandingFrameConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var activeTab by remember { mutableIntStateOf(1) } // 1: Canvas

    // Frame States
    var selectedStyle by remember { mutableStateOf(config.style) }
    var borderStyle by remember { mutableStateOf(config.borderStyle) }
    var customBorderColorHex by remember { mutableStateOf(config.customBorderColorHex) }
    var cornerRadiusDp by remember { mutableFloatStateOf(config.photoCornerRadiusDp.toFloat()) }
    var borderThicknessDp by remember { mutableFloatStateOf(config.photoBorderThicknessDp.toFloat()) }
    var headerTitle by remember { mutableStateOf(config.headerTitle) }
    var subtitle by remember { mutableStateOf(config.subtitle) }

    // Canvas States
    val context = LocalContext.current
    var canvasBgType by remember { mutableStateOf(config.canvasBgType) }
    var customBgHex by remember { mutableStateOf(config.customBgColorHex) }
    var gradientPreset by remember { mutableStateOf(config.canvasGradientPreset) }
    var canvasPattern by remember { mutableStateOf(config.canvasPattern) }
    var customTextHex by remember { mutableStateOf(config.customTextColorHex) }
    var sidePaddingDp by remember { mutableFloatStateOf(config.canvasSidePaddingDp.toFloat()) }
    var photoGapDp by remember { mutableFloatStateOf(config.canvasPhotoGapDp.toFloat()) }
    var customBgImageUri by remember { mutableStateOf(config.customBgImageUri) }
    var uploadedBgImageUris by remember { mutableStateOf(config.uploadedBgImageUris) }

    val pickBgImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedUri = com.example.photobooth.utils.BrandingFrameStorage.saveUploadedBgImage(context, uri)
            if (savedUri != null) {
                val uriStr = savedUri.toString()
                val currentList = uploadedBgImageUris.toMutableList()
                if (!currentList.contains(uriStr)) {
                    currentList.add(0, uriStr)
                }
                uploadedBgImageUris = currentList
                customBgImageUri = uriStr
                canvasBgType = CanvasBgType.IMAGE
                Toast.makeText(context, "Gambar background berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Gagal memproses gambar background!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Sticker States
    var showSticker by remember { mutableStateOf(config.showSticker) }
    var selectedSticker by remember { mutableStateOf(config.selectedSticker) }
    var stickerPosition by remember { mutableStateOf(config.stickerPosition) }
    var customStickerText by remember { mutableStateOf(config.customStickerText) }
    var selectedEmoji by remember { mutableStateOf(config.emojiSticker) }

    // Custom Logo States
    var showCustomLogo by remember { mutableStateOf(config.showCustomLogo) }
    var customLogoImageUri by remember { mutableStateOf(config.customLogoImageUri) }
    var customLogoPosition by remember { mutableStateOf(config.customLogoPosition) }
    var customLogoScalePercent by remember { mutableFloatStateOf(config.customLogoScalePercent.toFloat()) }
    var uploadedLogoImageUris by remember { mutableStateOf(config.uploadedLogoImageUris) }

    val pickLogoImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedUri = com.example.photobooth.utils.BrandingFrameStorage.saveUploadedLogoImage(context, uri)
            if (savedUri != null) {
                val uriStr = savedUri.toString()
                val currentList = uploadedLogoImageUris.toMutableList()
                if (!currentList.contains(uriStr)) {
                    currentList.add(0, uriStr)
                }
                uploadedLogoImageUris = currentList
                customLogoImageUri = uriStr
                showCustomLogo = true
                Toast.makeText(context, "Logo kustom berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Gagal memproses gambar logo!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val colorSwatches = listOf(
        "#FFFFFF", "#0F172A", "#070B14", "#FFFBEB", "#121212", "#050505",
        "#EC4899", "#8B5CF6", "#06B6D4", "#10B981", "#F59E0B", "#EF4444"
    )

    val emojiPresets = listOf("🎉", "✨", "👑", "❤️", "🔥", "📸", "🕶️", "💖", "🌟", "🎈", "🍿", "🚀")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = NeonCyan.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, NeonCyan)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.padding(8.dp).size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "PENGATURAN CANVAS STUDIO",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Atur warna latar belakang, gradasi, pola & gambar kustom foto",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf("Frame Style", "Background", "Badge Sticker", "Logo Icon")
                    tabs.forEachIndexed { index, label ->
                        val isSelected = activeTab == index
                        Button(
                            onClick = { activeTab = index },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) NeonCyan else Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Content Area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (activeTab) {
                        // TAB 0: FRAME CUSTOMIZATION
                        0 -> {
                            // 1. Frame Style Theme Presets
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("FRAME THEME PRESETS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    FrameStyle.entries.forEach { style ->
                                        val isSelected = style == selectedStyle
                                        val parsedBg = try { Color(android.graphics.Color.parseColor(style.defaultBgHex)) } catch (_: Exception) { Color.Gray }
                                        val parsedText = try { Color(android.graphics.Color.parseColor(style.defaultTextHex)) } catch (_: Exception) { Color.White }

                                        Surface(
                                            modifier = Modifier
                                                .width(130.dp)
                                                .clickable {
                                                    selectedStyle = style
                                                    customBgHex = style.defaultBgHex
                                                    customTextHex = style.defaultTextHex
                                                },
                                            shape = RoundedCornerShape(14.dp),
                                            color = parsedBg,
                                            border = BorderStroke(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) NeonCyan else Color(0x44FFFFFF)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = style.badgeTag,
                                                    color = parsedText,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    letterSpacing = 1.sp
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = style.displayName,
                                                    color = parsedText,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = NeonCyan,
                                                        modifier = Modifier.padding(top = 2.dp).size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. Frame Border Style Selection
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("FRAME BORDER STYLE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FrameBorderStyle.entries.forEach { border ->
                                        val isSelected = border == borderStyle
                                        Button(
                                            onClick = { borderStyle = border },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) NeonRose else Color(0x331E293B)
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                        ) {
                                            Text(border.displayName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }

                            // 3. Photo Card Corner & Border Sliders
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("CARD CORNER RADIUS & STROKE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Corner: ${cornerRadiusDp.toInt()} dp", color = TextMuted, fontSize = 11.sp)
                                        Slider(
                                            value = cornerRadiusDp,
                                            onValueChange = { cornerRadiusDp = it },
                                            valueRange = 0f..32f,
                                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Border Thickness: ${borderThicknessDp.toInt()} dp", color = TextMuted, fontSize = 11.sp)
                                        Slider(
                                            value = borderThicknessDp,
                                            onValueChange = { borderThicknessDp = it },
                                            valueRange = 0f..12f,
                                            colors = SliderDefaults.colors(thumbColor = NeonRose, activeTrackColor = NeonRose)
                                        )
                                    }
                                }
                            }

                            // 4. Header & Subtitle text inputs
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("HEADER TITLE & SUBTITLE STAMP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = headerTitle,
                                        onValueChange = { headerTitle = it },
                                        label = { Text("Header Title", color = TextMuted, fontSize = 10.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = NeonCyan,
                                            unfocusedBorderColor = Color(0x44FFFFFF)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = subtitle,
                                        onValueChange = { subtitle = it },
                                        label = { Text("Subtitle", color = TextMuted, fontSize = 10.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = NeonCyan,
                                            unfocusedBorderColor = Color(0x44FFFFFF)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // TAB 1: CANVAS CUSTOMIZATION
                        1 -> {
                            // 1. Canvas Background Type
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("CANVAS BACKGROUND TYPE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CanvasBgType.entries.forEach { bgType ->
                                        val isSelected = bgType == canvasBgType
                                        Button(
                                            onClick = { canvasBgType = bgType },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) NeonCyan else Color(0x331E293B)
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                bgType.displayName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.Black else Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            // Solid Swatches or Custom Image UI
                            if (canvasBgType == CanvasBgType.IMAGE) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "PILIHAN GAMBAR BACKGROUND (${uploadedBgImageUris.size} Tersimpan)",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )

                                        if (!customBgImageUri.isNullOrBlank()) {
                                            TextButton(
                                                onClick = { customBgImageUri = null },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = null,
                                                    tint = NeonRose,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text("Hapus Pilihan", color = NeonRose, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // Grid of Image Tiles
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val allTiles = listOf("UPLOAD_NEW") + uploadedBgImageUris
                                        val chunkedRows = allTiles.chunked(3)

                                        chunkedRows.forEach { rowItems ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                rowItems.forEach { tileUri ->
                                                    if (tileUri == "UPLOAD_NEW") {
                                                        // Add New Image Tile
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(72.dp)
                                                                .clip(RoundedCornerShape(12.dp))
                                                                .background(Color(0x221E293B))
                                                                .border(
                                                                    BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(NeonCyan, NeonRose))),
                                                                    RoundedCornerShape(12.dp)
                                                                )
                                                                .clickable { pickBgImageLauncher.launch("image/*") },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Column(
                                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                                verticalArrangement = Arrangement.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Add,
                                                                    contentDescription = "Upload",
                                                                    tint = NeonCyan,
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                                Spacer(modifier = Modifier.height(2.dp))
                                                                Text(
                                                                    text = "Unggah +",
                                                                    fontSize = 10.sp,
                                                                    color = Color.White,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        // Uploaded Image Tile
                                                        val isSelected = customBgImageUri == tileUri
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(72.dp)
                                                                .clip(RoundedCornerShape(12.dp))
                                                                .background(Color(0xFF1E293B))
                                                                .border(
                                                                    BorderStroke(
                                                                        width = if (isSelected) 2.5.dp else 1.dp,
                                                                        color = if (isSelected) NeonCyan else Color(0x33FFFFFF)
                                                                    ),
                                                                    RoundedCornerShape(12.dp)
                                                                )
                                                                .clickable { customBgImageUri = tileUri }
                                                        ) {
                                                            AsyncImage(
                                                                model = Uri.parse(tileUri),
                                                                contentDescription = "Background Tile",
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier.fillMaxSize()
                                                            )

                                                            // Selection badge
                                                            if (isSelected) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .align(Alignment.TopEnd)
                                                                        .padding(4.dp)
                                                                        .size(18.dp)
                                                                        .clip(CircleShape)
                                                                        .background(NeonCyan),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Check,
                                                                        contentDescription = null,
                                                                        tint = Color.Black,
                                                                        modifier = Modifier.size(12.dp)
                                                                    )
                                                                }
                                                            }

                                                            // Delete from list button
                                                            Box(
                                                                modifier = Modifier
                                                                    .align(Alignment.TopStart)
                                                                    .padding(4.dp)
                                                                    .size(18.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Color(0xAA000000))
                                                                    .clickable {
                                                                        val updated = uploadedBgImageUris.filter { it != tileUri }
                                                                        uploadedBgImageUris = updated
                                                                        if (customBgImageUri == tileUri) {
                                                                            customBgImageUri = updated.firstOrNull()
                                                                        }
                                                                    },
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Delete,
                                                                    contentDescription = "Delete",
                                                                    tint = NeonRose,
                                                                    modifier = Modifier.size(11.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                if (rowItems.size < 3) {
                                                    repeat(3 - rowItems.size) {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Text("PILIHAN WARNA CANVAS (GRID)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)

                                    val chunkedColors = colorSwatches.chunked(6)
                                    chunkedColors.forEach { rowColors ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            rowColors.forEach { hex ->
                                                val isSelected = customBgHex.equals(hex, ignoreCase = true)
                                                val parsedColor = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Gray }

                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(parsedColor)
                                                        .border(
                                                            width = if (isSelected) 2.5.dp else 1.dp,
                                                            color = if (isSelected) NeonCyan else Color(0x33FFFFFF),
                                                            shape = CircleShape
                                                        )
                                                        .clickable { customBgHex = hex },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = if (hex == "#FFFFFF" || hex == "#FFFBEB") Color.Black else Color.White,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. Canvas Margins & Photo Gaps Sliders
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("CANVAS PADDING & PHOTO GAP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Side Padding: ${sidePaddingDp.toInt()} dp", color = TextMuted, fontSize = 11.sp)
                                        Slider(
                                            value = sidePaddingDp,
                                            onValueChange = { sidePaddingDp = it },
                                            valueRange = 20f..100f,
                                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Photo Gap: ${photoGapDp.toInt()} dp", color = TextMuted, fontSize = 11.sp)
                                        Slider(
                                            value = photoGapDp,
                                            onValueChange = { photoGapDp = it },
                                            valueRange = 10f..60f,
                                            colors = SliderDefaults.colors(thumbColor = NeonRose, activeTrackColor = NeonRose)
                                        )
                                    }
                                }
                            }
                        }

                        // TAB 2: STICKERS & BADGES
                        2 -> {
                            // 1. Enable Sticker Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("SHOW BADGE STICKER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (showSticker) {
                                        Button(
                                            onClick = {
                                                showSticker = false
                                                selectedSticker = StickerPreset.NONE
                                                customStickerText = ""
                                                Toast.makeText(context, "Sticker / Preset Logo berhasil dihapus!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF007F)),
                                            border = BorderStroke(1.dp, NeonRose),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Hapus 🗑️", fontSize = 10.sp, color = NeonRose, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Switch(
                                        checked = showSticker,
                                        onCheckedChange = { showSticker = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = Color(0x3300F0FF))
                                    )
                                }
                            }

                            if (showSticker) {
                                // 2. Sticker Presets
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("STICKER PRESET BADGES", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        StickerPreset.entries.filter { it != StickerPreset.NONE }.forEach { sticker ->
                                            val isSelected = sticker == selectedSticker
                                            val parsedStickerColor = try { Color(android.graphics.Color.parseColor(sticker.primaryColorHex)) } catch (_: Exception) { Color.Red }

                                            Button(
                                                onClick = { selectedSticker = sticker },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isSelected) parsedStickerColor else Color(0x331E293B)
                                                ),
                                                shape = RoundedCornerShape(14.dp),
                                                border = BorderStroke(1.dp, if (isSelected) NeonCyan else Color(0x33FFFFFF))
                                            ) {
                                                Text(
                                                    text = sticker.displayName,
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }

                                // 3. Emoji Sticker Selector
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("DECORATIVE EMOJI STICKER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        emojiPresets.forEach { emoji ->
                                            val isSelected = emoji == selectedEmoji
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) NeonRose else Color(0x331E293B))
                                                    .border(BorderStroke(1.dp, if (isSelected) NeonCyan else Color(0x22FFFFFF)), CircleShape)
                                                    .clickable { selectedEmoji = emoji },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(emoji, fontSize = 18.sp)
                                            }
                                        }
                                    }
                                }

                                // 4. Custom Text Input if Custom Sticker selected
                                if (selectedSticker == StickerPreset.CUSTOM) {
                                    OutlinedTextField(
                                        value = customStickerText,
                                        onValueChange = { customStickerText = it },
                                        label = { Text("Custom Sticker Text (e.g. Happy 21st Arvin!)", color = TextMuted, fontSize = 10.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = NeonCyan,
                                            unfocusedBorderColor = Color(0x44FFFFFF)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // 5. Sticker Placement Position
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("STICKER PLACEMENT POSITION", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        StickerPosition.entries.forEach { pos ->
                                            val isSelected = pos == stickerPosition
                                            Button(
                                                onClick = { stickerPosition = pos },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isSelected) NeonCyan else Color(0x331E293B)
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    pos.displayName,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.Black else Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // TAB 3: LOGO KUSTOM
                        3 -> {
                            // 1. Enable Logo Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("AKTIFKAN LOGO KUSTOM", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (showCustomLogo) {
                                        Button(
                                            onClick = {
                                                showCustomLogo = false
                                                customLogoImageUri = null
                                                Toast.makeText(context, "Logo kustom dinonaktifkan!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF007F)),
                                            border = BorderStroke(1.dp, NeonRose),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Reset Logo 🗑️", fontSize = 10.sp, color = NeonRose, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Switch(
                                        checked = showCustomLogo,
                                        onCheckedChange = { showCustomLogo = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = Color(0x3300F0FF))
                                    )
                                }
                            }

                            if (showCustomLogo) {
                                // 2. Upload Logo Section
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("PILIH ATAU UNGGAH LOGO BARU", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Button(
                                            onClick = { pickLogoImageLauncher.launch("image/*") },
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Unggah Logo", fontSize = 11.sp, color = Color.White)
                                        }
                                    }

                                    if (uploadedLogoImageUris.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(100.dp)
                                                .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                                .border(BorderStroke(1.dp, Color(0x22FFFFFF)), RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Belum ada logo yang diunggah.\nSilakan klik tombol Unggah Logo di atas.", color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            uploadedLogoImageUris.forEach { logoUri ->
                                                val isSelected = customLogoImageUri == logoUri
                                                Box(
                                                    modifier = Modifier
                                                        .size(80.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(Color(0xFF1E293B))
                                                        .border(
                                                            BorderStroke(
                                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                                color = if (isSelected) NeonCyan else Color(0x33FFFFFF)
                                                            ),
                                                            RoundedCornerShape(12.dp)
                                                        )
                                                        .clickable { customLogoImageUri = logoUri }
                                                ) {
                                                    AsyncImage(
                                                        model = Uri.parse(logoUri),
                                                        contentDescription = "Custom Logo Item",
                                                        contentScale = ContentScale.Fit,
                                                        modifier = Modifier.fillMaxSize().padding(6.dp)
                                                    )

                                                    if (isSelected) {
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.TopEnd)
                                                                .padding(4.dp)
                                                                .size(16.dp)
                                                                .clip(CircleShape)
                                                                .background(NeonCyan),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = Color.Black,
                                                                modifier = Modifier.size(10.dp)
                                                            )
                                                        }
                                                    }

                                                    // Delete button
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopStart)
                                                            .padding(4.dp)
                                                            .size(16.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xAA000000))
                                                            .clickable {
                                                                val updated = uploadedLogoImageUris.filter { it != logoUri }
                                                                uploadedLogoImageUris = updated
                                                                if (customLogoImageUri == logoUri) {
                                                                    customLogoImageUri = updated.firstOrNull()
                                                                }
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(10.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // 3. Logo Scale/Size Slider
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("UKURAN SKALA LOGO: ${customLogoScalePercent.toInt()}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Slider(
                                        value = customLogoScalePercent,
                                        onValueChange = { customLogoScalePercent = it },
                                        valueRange = 5f..40f,
                                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // 4. Logo Placement Position
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("POSISI PENEMPATAN LOGO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        StickerPosition.entries.forEach { pos ->
                                            val isSelected = pos == customLogoPosition
                                            Button(
                                                onClick = { customLogoPosition = pos },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isSelected) NeonCyan else Color(0x331E293B)
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    pos.displayName,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.Black else Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = TextMuted, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            val updated = config.copy(
                                style = selectedStyle,
                                borderStyle = borderStyle,
                                customBorderColorHex = customBorderColorHex,
                                photoCornerRadiusDp = cornerRadiusDp.toInt(),
                                photoBorderThicknessDp = borderThicknessDp.toInt(),
                                headerTitle = headerTitle,
                                subtitle = subtitle,
                                canvasBgType = canvasBgType,
                                customBgColorHex = customBgHex,
                                canvasGradientPreset = gradientPreset,
                                canvasPattern = canvasPattern,
                                customTextColorHex = customTextHex,
                                canvasSidePaddingDp = sidePaddingDp.toInt(),
                                canvasPhotoGapDp = photoGapDp.toInt(),
                                customBgImageUri = customBgImageUri,
                                uploadedBgImageUris = uploadedBgImageUris,
                                showSticker = showSticker,
                                selectedSticker = selectedSticker,
                                stickerPosition = stickerPosition,
                                customStickerText = customStickerText,
                                emojiSticker = selectedEmoji,
                                showCustomLogo = showCustomLogo,
                                customLogoImageUri = customLogoImageUri,
                                customLogoPosition = customLogoPosition,
                                customLogoScalePercent = customLogoScalePercent.toInt(),
                                uploadedLogoImageUris = uploadedLogoImageUris
                            )
                            onConfigChanged(updated)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("APPLY DESIGN", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
