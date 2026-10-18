package com.example.photobooth.ui.screens

import android.graphics.Bitmap
import com.example.photobooth.model.LoopAnimationConfig
import com.example.photobooth.ui.components.LoopAnimationRenderer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Palette
import com.example.photobooth.model.*
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import com.example.photobooth.ui.components.BrandingFrameEditorDialog
import com.example.photobooth.ui.components.GifProgressDialog
import com.example.photobooth.ui.components.PrintPaperSizeDialog
import com.example.photobooth.ui.components.WatermarkEditorDialog
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.content.Intent
import com.example.photobooth.hardware.PrintController
import com.example.photobooth.utils.CloudinaryUploader
import com.example.photobooth.utils.QrCodeGenerator
import com.example.photobooth.utils.PhotoShareHelper
import com.example.photobooth.utils.GifComposer
import com.example.photobooth.utils.PhotoComposer
import com.example.photobooth.utils.WatermarkHelper
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image

@Composable
fun PhotoEditorScreen(
    photos: List<Bitmap>,
    layoutType: String,
    watermarkConfig: WatermarkConfig = WatermarkConfig(),
    onWatermarkConfigChanged: (WatermarkConfig) -> Unit = {},
    brandingFrameConfig: BrandingFrameConfig = BrandingFrameConfig(),
    onBrandingFrameConfigChanged: (BrandingFrameConfig) -> Unit = {},
    flashConfig: FlashAnimationConfig = FlashAnimationConfig(),
    managerConfig: BoothManagerConfig = BoothManagerConfig(),
    onPhotoReplaced: (Int, Bitmap) -> Unit = { _, _ -> },
    onPhotosRearranged: (List<Bitmap>) -> Unit = {},
    onRetakeAll: () -> Unit = {},
    onFinishSession: () -> Unit = {},
    onEditCompleted: (Bitmap, ByteArray?) -> Unit = { _, _ -> }
) {
    var currentPhotosList by remember(photos) { mutableStateOf(photos) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val printController = remember { PrintController(context) }

    // Print & Share & Cloudinary states
    var printCopies by remember { mutableIntStateOf(1) }
    var isUploadingCloudinary by remember { mutableStateOf(false) }
    var cloudinaryUrl by remember { mutableStateOf<String?>(null) }
    var uploadStatusMessage by remember { mutableStateOf<String?>(null) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showConfirmFinishDialog by remember { mutableStateOf(false) }
    var showPrintPaperDialog by remember { mutableStateOf(false) }
    val pickBgImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedUri = com.example.photobooth.utils.BrandingFrameStorage.saveUploadedBgImage(context, uri)
            if (savedUri != null) {
                val uriStr = savedUri.toString()
                val currentList = brandingFrameConfig.uploadedBgImageUris.toMutableList()
                if (!currentList.contains(uriStr)) {
                    currentList.add(0, uriStr)
                }
                onBrandingFrameConfigChanged(
                    brandingFrameConfig.copy(
                        canvasBgType = CanvasBgType.IMAGE,
                        customBgImageUri = uriStr,
                        uploadedBgImageUris = currentList
                    )
                )
                Toast.makeText(context, "Gambar background berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Gagal memproses gambar background!", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val isGifMode = remember(layoutType) { layoutType == "GIF / Boomerang" }
    var selectedFilter by remember { mutableStateOf("Normal") }
    var brandingText by remember { mutableStateOf(brandingFrameConfig.headerTitle) }
    var isBoomerang by remember { mutableStateOf(true) }
    var frameDelayMs by remember { mutableStateOf(140) }
    var selectedLoopPreset by remember { mutableStateOf("Neon Pulse") }
    var isPlaying by remember { mutableStateOf(true) }
    var showWatermarkDialog by remember { mutableStateOf(false) }
    var showFrameEditorDialog by remember { mutableStateOf(false) }

    // GIF Generation Progress state
    var isGeneratingGif by remember { mutableStateOf(false) }
    var gifProgress by remember { mutableStateOf(0f) }
    var gifStatusMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // Pre-calculate filtered photo sequence for smooth animated preview
    val filteredPhotos = remember(currentPhotosList, selectedFilter, watermarkConfig) {
        currentPhotosList.map { raw ->
            val filtered = PhotoComposer.applyColorFilter(raw, selectedFilter)
            if (watermarkConfig.isEnabled) {
                WatermarkHelper.applyWatermark(filtered, watermarkConfig)
            } else {
                filtered
            }
        }
    }

    // Sequence for player (boomerang or forward)
    val playbackSequence = remember(filteredPhotos, isBoomerang) {
        if (isBoomerang && filteredPhotos.size > 2) {
            val reversed = filteredPhotos.subList(1, filteredPhotos.size - 1).reversed()
            filteredPhotos + reversed
        } else {
            filteredPhotos
        }
    }

    // Live frame animator for GIF mode
    var activeFrameIndex by remember { mutableStateOf(0) }
    LaunchedEffect(isPlaying, playbackSequence, frameDelayMs) {
        if (isGifMode && isPlaying && playbackSequence.isNotEmpty()) {
            while (true) {
                delay(frameDelayMs.toLong())
                activeFrameIndex = (activeFrameIndex + 1) % playbackSequence.size
            }
        }
    }

    // Composed static bitmap for print / contact sheet
    val composedBitmap = remember(currentPhotosList, selectedFilter, brandingText, watermarkConfig, brandingFrameConfig, layoutType, context) {
        PhotoComposer.composePhotoStrip(
            photos = currentPhotosList,
            filter = selectedFilter,
            overlayText = brandingText,
            watermarkConfig = watermarkConfig,
            frameConfig = brandingFrameConfig,
            layoutType = layoutType,
            context = context
        )
    }

    // Trigger Cloudinary upload for the current composed image
    LaunchedEffect(composedBitmap) {
        if (CloudinaryUploader.isConfigured()) {
            isUploadingCloudinary = true
            uploadStatusMessage = "Mengunggah ke Cloudinary..."
            try {
                val stream = java.io.ByteArrayOutputStream()
                composedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)
                val bytes = stream.toByteArray()
                val url = CloudinaryUploader.uploadToCloudinary(bytes, isGif = false)
                if (url != null) {
                    cloudinaryUrl = url
                    uploadStatusMessage = "Foto tersinkron Cloudinary!"
                } else {
                    uploadStatusMessage = "Upload gagal, QR lokal siap."
                }
            } catch (e: Exception) {
                uploadStatusMessage = e.message ?: "Upload gagal."
            } finally {
                isUploadingCloudinary = false
            }
        } else {
            uploadStatusMessage = "Cloudinary offline / lokal."
        }
    }

    val qrCodeBitmap = remember(cloudinaryUrl) {
        val targetUrl = cloudinaryUrl ?: "https://res.cloudinary.com/photobooth/download/${System.currentTimeMillis()}"
        QrCodeGenerator.generateQrCode(targetUrl)
    }

    var studioSubTab by remember { mutableIntStateOf(0) } // 0: Frame, 1: Canvas, 2: Stiker

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF030712))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isGifMode) "GIF & PHOTO STUDIO" else "PHOTO EDITOR STUDIO",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x331E293B),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NeonRose)
                        )
                        Text(
                            text = "${photos.size} SHOTS",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Main Scrollable Area for Preview and All Bottom Controls
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Photo Preview Strip Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x221E293B))
                    .border(
                        BorderStroke(1.dp, if (isGifMode) NeonRose.copy(alpha = 0.5f) else Color(0x33FFFFFF)),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isGifMode && playbackSequence.isNotEmpty()) {
                    val currentFrame = playbackSequence.getOrNull(activeFrameIndex) ?: playbackSequence[0]
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(currentFrame.width.toFloat() / currentFrame.height.toFloat())
                            .clip(RoundedCornerShape(16.dp))
                            .border(BorderStroke(2.dp, NeonRose), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = currentFrame.asImageBitmap(),
                            contentDescription = "Animated Frame",
                            modifier = Modifier.fillMaxSize()
                        )

                        // Live loop animation preset overlay on the editor preview
                        if (selectedLoopPreset != "None") {
                            LoopAnimationRenderer(
                                config = LoopAnimationConfig(
                                    isEnabled = true,
                                    animationPreset = selectedLoopPreset
                                ),
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Event Branding Watermark Overlay on frame preview
                        if (brandingText.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(Color(0x88000000))
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = brandingText,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                            }
                        }

                        // Top Frame Counter Badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xCC000000))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "FRAME ${activeFrameIndex + 1}/${playbackSequence.size}",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Bottom Play/Pause overlay button
                        IconButton(
                            onClick = { isPlaying = !isPlaying },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xAA000000))
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    Image(
                        bitmap = composedBitmap.asImageBitmap(),
                        contentDescription = "Composed Photo",
                        modifier = Modifier.fillMaxHeight()
                    )
                }
            }

            // =========================================================================
            // FITUR FRAME, CANVAS & STIKER (LANGSUNG DI BAWAH PHOTO PREVIEW)
            // =========================================================================
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header Studio Bar with Title & "Edit Detail" Modal Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "CANVAS & BACKGROUND",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Kustomisasi warna & gambar latar belakang foto",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Tombol untuk membuka dialog pengaturan lengkap
                        Button(
                            onClick = { showFrameEditorDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Edit Detail",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // CANVAS BACKGROUND CUSTOMIZATION
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Background Type Switcher
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CanvasBgType.entries.forEach { bgType ->
                                        val isSelected = bgType == brandingFrameConfig.canvasBgType
                                        Button(
                                            onClick = {
                                                onBrandingFrameConfigChanged(
                                                    brandingFrameConfig.copy(canvasBgType = bgType)
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) NeonCyan else Color(0x22FFFFFF)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(28.dp)
                                        ) {
                                            Text(
                                                text = bgType.displayName,
                                                fontSize = 10.sp,
                                                color = if (isSelected) Color.Black else Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (brandingFrameConfig.canvasBgType == CanvasBgType.IMAGE) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "PILIHAN GAMBAR BACKGROUND (${brandingFrameConfig.uploadedBgImageUris.size} Tersimpan):",
                                                color = TextMuted,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )

                                            if (!brandingFrameConfig.customBgImageUri.isNullOrBlank()) {
                                                TextButton(
                                                    onClick = {
                                                        onBrandingFrameConfigChanged(
                                                            brandingFrameConfig.copy(customBgImageUri = null)
                                                        )
                                                    },
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

                                        // Grid Layout of Image Options
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val allTiles = listOf("UPLOAD_NEW") + brandingFrameConfig.uploadedBgImageUris
                                            val chunkedRows = allTiles.chunked(3)

                                            chunkedRows.forEach { rowItems ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    rowItems.forEach { tileUri ->
                                                        if (tileUri == "UPLOAD_NEW") {
                                                            // Add / Upload New Image Button Tile
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
                                                            val isSelected = brandingFrameConfig.customBgImageUri == tileUri
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
                                                                    .clickable {
                                                                        onBrandingFrameConfigChanged(
                                                                            brandingFrameConfig.copy(customBgImageUri = tileUri)
                                                                        )
                                                                    }
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

                                                                // Delete from saved list button
                                                                Box(
                                                                    modifier = Modifier
                                                                        .align(Alignment.TopStart)
                                                                        .padding(4.dp)
                                                                        .size(18.dp)
                                                                        .clip(CircleShape)
                                                                        .background(Color(0xAA000000))
                                                                        .clickable {
                                                                            val updated = brandingFrameConfig.uploadedBgImageUris.filter { it != tileUri }
                                                                            val newCurrent = if (brandingFrameConfig.customBgImageUri == tileUri) {
                                                                                updated.firstOrNull()
                                                                            } else {
                                                                                brandingFrameConfig.customBgImageUri
                                                                            }
                                                                            onBrandingFrameConfigChanged(
                                                                                brandingFrameConfig.copy(
                                                                                    uploadedBgImageUris = updated,
                                                                                    customBgImageUri = newCurrent
                                                                                )
                                                                            )
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

                                                    // Fill remaining empty columns in row if row is not full
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
                                    // Solid Color Swatches in a 2-Row Grid
                                    val quickSwatches = listOf(
                                        "#FFFFFF", "#0F172A", "#FFFBEB", "#121212",
                                        "#EC4899", "#8B5CF6", "#06B6D4", "#10B981",
                                        "#F59E0B", "#EF4444", "#64748B", "#FDE047"
                                    )
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "PILIHAN WARNA CANVAS (GRID):",
                                            color = TextMuted,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        val chunkedColors = quickSwatches.chunked(6)
                                        chunkedColors.forEach { rowColors ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                rowColors.forEach { hex ->
                                                    val isSelected = brandingFrameConfig.customBgColorHex.equals(hex, ignoreCase = true)
                                                    val parsed = try {
                                                        Color(android.graphics.Color.parseColor(hex))
                                                    } catch (_: Exception) {
                                                        Color.Gray
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(CircleShape)
                                                            .background(parsed)
                                                            .border(
                                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                                color = if (isSelected) NeonCyan else Color(0x33FFFFFF),
                                                                shape = CircleShape
                                                            )
                                                            .clickable {
                                                                onBrandingFrameConfigChanged(
                                                                    brandingFrameConfig.copy(customBgColorHex = hex)
                                                                )
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isSelected) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = if (hex == "#FFFFFF" || hex == "#FFFBEB" || hex == "#FDE047") Color.Black else Color.White,
                                                                modifier = Modifier.size(16.dp)
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
            }

            // GIF Mode Controls Bar (Boomerang Toggle, Speed, and Loop FX selector)
            if (isGifMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x331E293B))
                        .border(BorderStroke(1.dp, Color(0x33FFFFFF)), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Boomerang Loop Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Loop, contentDescription = null, tint = NeonRose)
                            Text(text = "Boomerang Loop:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = isBoomerang,
                                onClick = { isBoomerang = true },
                                label = { Text("Ping-Pong") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonRose,
                                    selectedLabelColor = Color.White
                                )
                            )
                            FilterChip(
                                selected = !isBoomerang,
                                onClick = { isBoomerang = false },
                                label = { Text("Forward") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Playback Speed Selector
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = NeonCyan)
                            Text(text = "Speed:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Fast" to 90, "Normal" to 140, "Slow" to 220).forEach { (label, delay) ->
                                val isSelected = frameDelayMs == delay
                                Button(
                                    onClick = { frameDelayMs = delay },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) NeonCyan else Color(0x22FFFFFF)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Loop Animation FX Selector (Neon Pulse, Disco Stars, Retro Scanline)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = NeonRose)
                            Text(text = "Loop FX Overlay:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("None", "Neon Pulse", "Disco Stars", "Retro Scanline").forEach { preset ->
                                val isSelected = selectedLoopPreset == preset
                                Button(
                                    onClick = { selectedLoopPreset = preset },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) NeonRose else Color(0x22FFFFFF)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = preset,
                                        color = if (isSelected) Color.White else Color(0xFFCCCCCC),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }



            // Filter Selection Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.ColorLens, contentDescription = null, tint = NeonRose)
                Text(text = "STUDIO FILTERS:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                val filters = listOf("Normal", "B&W Studio", "Sepia Dream", "Vintage Film", "Cyberpunk", "Pop Art", "Soft Portrait", "Cool Drama")
                filters.forEach { filter ->
                    val isSelected = filter == selectedFilter
                    Button(
                        onClick = { selectedFilter = filter },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) NeonRose else Color(0x331E293B)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (isSelected) NeonRose else Color(0x33FFFFFF))
                    ) {
                        Text(
                            text = filter,
                            color = Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Spacer(modifier = Modifier.height(6.dp))

            // ==========================================
            // OPSI CETAK, QR KODE CLOUDINARY, WHATSAPP & EMAIL
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x331E293B))
                    .border(BorderStroke(1.dp, Color(0x33FFFFFF)), RoundedCornerShape(20.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Bar: Label & Cloudinary Sync Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "OPSI CETAK & BAGIKAN",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Cloudinary Status Badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (cloudinaryUrl != null) NeonCyan.copy(alpha = 0.15f) else Color(0x22FFFFFF),
                        border = BorderStroke(1.dp, if (cloudinaryUrl != null) NeonCyan else Color(0x33FFFFFF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isUploadingCloudinary) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(10.dp),
                                    strokeWidth = 2.dp,
                                    color = NeonCyan
                                )
                                Text(
                                    text = "Cloudinary...",
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                            } else if (cloudinaryUrl != null) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(NeonCyan)
                                )
                                Text(
                                    text = "Cloudinary Aktif",
                                    color = NeonCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = "QR Lokal",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                // 1. OPSI CETAK (Copies Counter + Cetak Foto Button atau Proses GIF)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isGifMode) {
                        Button(
                            onClick = {
                                isGeneratingGif = true
                                gifProgress = 0f
                                gifStatusMessage = "Memulai mesin animasi GIF..."
                                coroutineScope.launch {
                                    try {
                                        val gifBytes = GifComposer.createAnimatedGif(
                                            frames = currentPhotosList,
                                            filter = selectedFilter,
                                            watermarkText = brandingText,
                                            watermarkConfig = watermarkConfig,
                                            isBoomerang = isBoomerang,
                                            delayMs = frameDelayMs,
                                            loopPreset = selectedLoopPreset
                                        ) { progress, status ->
                                            gifProgress = progress
                                            gifStatusMessage = status
                                        }
                                        onEditCompleted(composedBitmap, gifBytes)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        isGeneratingGif = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MovieFilter,
                                contentDescription = "Proses GIF",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PROSES & BAGIKAN GIF",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        // Copy Counter
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0x22FFFFFF),
                            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 6.dp)
                            ) {
                                IconButton(
                                    onClick = { if (printCopies > 1) printCopies-- },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Kurang Lembar",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "$printCopies Eks",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                IconButton(
                                    onClick = { if (printCopies < 10) printCopies++ },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Tambah Lembar",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Primary Print Button
                        Button(
                            onClick = {
                                showPrintPaperDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "Cetak",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CETAK FOTO",
                                color = Color.Black,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // 2. OPSI QR KODE CLOUDINARY, WHATSAPP & EMAIL
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // QR Code Cloudinary Button
                    Button(
                        onClick = {
                            showQrDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "QR Kode",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "QR KODE",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // WhatsApp Share Button
                    Button(
                        onClick = {
                            PhotoShareHelper.shareToWhatsApp(context, composedBitmap, cloudinaryUrl)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "WhatsApp",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "WHATSAPP",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // Email Share Button
                    Button(
                        onClick = {
                            PhotoShareHelper.shareToEmail(context, composedBitmap, cloudinaryUrl)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "EMAIL",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Selesai Sesi / Sesi Baru Button
                OutlinedButton(
                    onClick = {
                        showConfirmFinishDialog = true
                    },
                    border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selesai Sesi",
                        tint = NeonRose,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SELESAI SESI & MULAI SESI BARU",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }

    // GIF Generation Progress Animation Dialog
    if (isGeneratingGif) {
        GifProgressDialog(
            progress = gifProgress,
            statusMessage = gifStatusMessage,
            sampleFrames = currentPhotosList
        )
    }

    // Watermark Editor Dialog Modal
    if (showWatermarkDialog) {
        WatermarkEditorDialog(
            config = watermarkConfig,
            onConfigChanged = onWatermarkConfigChanged,
            onDismiss = { showWatermarkDialog = false }
        )
    }

    // Branding Frame Customizer Dialog Modal
    if (showFrameEditorDialog) {
        BrandingFrameEditorDialog(
            config = brandingFrameConfig,
            onConfigChanged = onBrandingFrameConfigChanged,
            onDismiss = { showFrameEditorDialog = false }
        )
    }

    // Print Paper Size Dialog Modal
    if (showPrintPaperDialog) {
        PrintPaperSizeDialog(
            composedBitmap = composedBitmap,
            layoutType = layoutType,
            initialCopies = printCopies,
            onDismiss = { showPrintPaperDialog = false },
            onPrintConfirmed = { copies, scale, color, orientation, paperSizeName ->
                showPrintPaperDialog = false
                printCopies = copies
                try {
                    printController.printWithAndroidPrintHelper(
                        bitmap = composedBitmap,
                        jobName = "PhotoBooth_${paperSizeName.replace(" ", "_")}_${System.currentTimeMillis()}",
                        scaleModeVal = scale,
                        colorModeVal = color,
                        orientationVal = orientation
                    )
                    Toast.makeText(
                        context,
                        "Mengirim dokumen cetak ($paperSizeName, $copies Rangkap) ke Printer...",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal mengirim dokumen cetak: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Cloudinary QR Code Dialog Modal
    if (showQrDialog) {
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "QR Code Cloudinary",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Pindai dengan kamera ponsel untuk mengunduh foto langsung via Cloudinary",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    // QR Code Display Card
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .size(220.dp)
                            .padding(4.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp)
                        ) {
                            Image(
                                bitmap = qrCodeBitmap.asImageBitmap(),
                                contentDescription = "Cloudinary QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Cloudinary Upload Status
                    if (isUploadingCloudinary) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = NeonCyan,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Mengunggah foto ke Cloudinary...",
                                color = NeonCyan,
                                fontSize = 12.sp
                            )
                        }
                    } else if (cloudinaryUrl != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0x2210B981),
                            border = BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Tersinkron dengan Cloudinary!",
                                    color = Color(0xFF10B981),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Copy Link & Open Link Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(cloudinaryUrl ?: ""))
                                    Toast.makeText(context, "Tautan berhasil disalin ke papan klip!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Salin",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Salin Link",
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }

                            Button(
                                onClick = {
                                    try {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(cloudinaryUrl))
                                        context.startActivity(browserIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Gagal membuka browser", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInBrowser,
                                    contentDescription = "Buka",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Buka Link",
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        Text(
                            text = uploadStatusMessage ?: "QR code lokal siap dipindai.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showQrDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("TUTUP", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Confirm Finish Session Dialog Modal
    if (showConfirmFinishDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmFinishDialog = false },
            title = {
                Text(
                    text = "Selesaikan Sesi Foto?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Pastikan Anda sudah mencetak foto atau menyimpan salinan via WhatsApp, Email, atau QR Code sebelum menyelesaikan sesi.",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmFinishDialog = false
                        onFinishSession()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("SELESAI & MENU UTAMA", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmFinishDialog = false }) {
                    Text("KEMBALI", color = Color.White)
                }
            },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(18.dp)
        )
    }
}
