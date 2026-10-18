package com.example.photobooth.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MotionPhotosOn
import android.net.Uri
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.photobooth.hardware.gphoto2.LibGPhoto2Engine
import com.example.photobooth.hardware.CameraXPreviewView
import com.example.photobooth.hardware.UsbOtgCameraManager
import com.example.photobooth.hardware.capturePhotoFromCameraX
import com.example.photobooth.model.BoothManagerConfig
import com.example.photobooth.model.FlashAnimationConfig
import com.example.photobooth.model.WatermarkConfig
import com.example.photobooth.ui.components.FlashAnimationOverlay
import com.example.photobooth.ui.components.FlashEditorDialog
import com.example.photobooth.ui.components.GifProgressDialog
import com.example.photobooth.ui.components.WatermarkEditorDialog
import com.example.photobooth.utils.GifComposer
import com.example.photobooth.utils.WatermarkHelper
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LivePreviewScreen(
    layoutType: String,
    flashConfig: FlashAnimationConfig = FlashAnimationConfig(),
    onFlashConfigChanged: (FlashAnimationConfig) -> Unit = {},
    watermarkConfig: WatermarkConfig = WatermarkConfig(),
    onWatermarkConfigChanged: (WatermarkConfig) -> Unit = {},
    managerConfig: BoothManagerConfig = BoothManagerConfig(),
    onPhotosComplete: (List<Bitmap>) -> Unit
) {
    val context = LocalContext.current
    val isGifMode = remember(layoutType) { layoutType == "GIF / Boomerang" }

    val isUsbCameraConnected by UsbOtgCameraManager.isUsbCameraConnected.collectAsState()
    val activeUsbDevice by UsbOtgCameraManager.activeCameraDevice.collectAsState()

    val activeGPhoto2Camera by LibGPhoto2Engine.activeCamera.collectAsState()

    var isExternalUsb by remember(managerConfig.cameraFacing, isUsbCameraConnected, activeGPhoto2Camera) {
        mutableStateOf(
            managerConfig.cameraFacing == "Kamera Eksternal USB OTG" ||
                    managerConfig.cameraFacing == "USB OTG" ||
                    managerConfig.cameraFacing == "DSLR libgphoto2" ||
                    (managerConfig.autoSwitchToUsbCamera && isUsbCameraConnected)
        )
    }

    val totalPhotos = remember(layoutType) {
        when (layoutType) {
            "Single Shot" -> 1
            "3-Photo Strip" -> 3
            "Boothlab 8-Frame", "Boothlab 4", "Boothlab Frame" -> 8
            "GIF / Boomerang" -> 8
            else -> {
                if (layoutType.startsWith("custom_layout_")) {
                    com.example.photobooth.utils.CustomLayoutStorage.getCustomLayouts(context)
                        .find { it.id == layoutType }?.photoCount ?: 4
                } else {
                    4
                }
            }
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    var isFlashOn by remember { mutableStateOf(false) }
    var showGrid by remember { mutableStateOf(true) }
    var isSessionStarted by remember { mutableStateOf(false) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    var currentPhotoIndex by remember { mutableStateOf(0) }
    var countdownSeconds by remember { mutableIntStateOf(3) }
    var countdown by remember { mutableStateOf(3) }
    var isCountingDown by remember { mutableStateOf(true) }
    var isBurstRecording by remember { mutableStateOf(false) }
    var triggerFlashAnimation by remember { mutableStateOf(false) }
    var showFlashEditorDialog by remember { mutableStateOf(false) }
    var showWatermarkEditorDialog by remember { mutableStateOf(false) }
    var brightnessLevel by remember { mutableFloatStateOf(0f) }
    var showBrightnessPanel by remember { mutableStateOf(false) }

    // GIF Processing & Progress Animation Dialog State
    var showGifProgressDialog by remember { mutableStateOf(false) }
    var gifProgressVal by remember { mutableStateOf(0f) }
    var gifStatusMsg by remember { mutableStateOf("Initializing motion engine...") }

    val capturedList = remember { mutableStateListOf<Bitmap>() }
    val coroutineScope = rememberCoroutineScope()

    // Helper to apply watermark and brightness to captured photo
    val processCapturedPhoto: (Bitmap) -> Bitmap = { raw ->
        val brightened = if (brightnessLevel != 0f) {
            adjustBitmapBrightness(raw, brightnessLevel)
        } else {
            raw
        }
        if (watermarkConfig.isEnabled) {
            WatermarkHelper.applyWatermark(brightened, watermarkConfig)
        } else {
            brightened
        }
    }

    // Handle photo capture timer sequence
    LaunchedEffect(currentPhotoIndex, hasCameraPermission, flashConfig, isSessionStarted) {
        if (!hasCameraPermission) return@LaunchedEffect
        if (!isSessionStarted) return@LaunchedEffect

        if (isGifMode) {
            // GIF BURST MODE: 1 Single Countdown, followed by rapid 8-frame burst capture
            if (currentPhotoIndex == 0 && isCountingDown) {
                countdown = countdownSeconds
                while (countdown > 0) {
                    delay(1000)
                    countdown--
                }
                isCountingDown = false
                isBurstRecording = true

                // Rapidly capture all 8 burst frames
                for (frameIdx in 0 until totalPhotos) {
                    currentPhotoIndex = frameIdx

                    // Fast blitz flash
                    triggerFlashAnimation = true
                    delay(100)
                    triggerFlashAnimation = false

                    var captured = false
                    capturePhotoFromCameraX(
                        context = context,
                        imageCapture = imageCapture,
                        isExternalUsb = isExternalUsb,
                        onSuccess = { bitmap ->
                            capturedList.add(processCapturedPhoto(bitmap))
                            captured = true
                        },
                        onError = {
                            val fallback = if (isExternalUsb) {
                                UsbOtgCameraManager.createUsbCameraPreviewFrame(
                                    deviceLabel = activeUsbDevice?.productName ?: "USB OTG BURST FRAME ${frameIdx + 1}"
                                )
                            } else {
                                createSimulatedCameraFrame(frameIdx)
                            }
                            capturedList.add(processCapturedPhoto(fallback))
                            captured = true
                        }
                    )

                    var waitCount = 0
                    while (!captured && waitCount < 8) {
                        delay(50)
                        waitCount++
                    }

                    if (!captured) {
                        val fallback = if (isExternalUsb) {
                            UsbOtgCameraManager.createUsbCameraPreviewFrame(
                                deviceLabel = activeUsbDevice?.productName ?: "USB OTG BURST FRAME ${frameIdx + 1}"
                            )
                        } else {
                            createSimulatedCameraFrame(frameIdx)
                        }
                        capturedList.add(processCapturedPhoto(fallback))
                    }

                    delay(160) // High-speed interval between burst frames
                }

                isBurstRecording = false

                // Show high-tech GIF Generation Progress Animation Dialog
                showGifProgressDialog = true
                coroutineScope.launch {
                    GifComposer.createAnimatedGif(
                        frames = capturedList.toList(),
                        filter = "Normal",
                        watermarkText = "PHOTO BOOTH GIF",
                        watermarkConfig = watermarkConfig,
                        isBoomerang = true,
                        delayMs = 140
                    ) { progress, status ->
                        gifProgressVal = progress
                        gifStatusMsg = status
                    }

                    delay(600)
                    showGifProgressDialog = false
                    onPhotosComplete(capturedList.toList())
                }
            }
        } else {
            // STANDARD MULTI-SHOT PHOTO SEQUENCE (Single, 2x2, 3-Strip, 4-Grid)
            if (currentPhotoIndex < totalPhotos) {
                isCountingDown = true
                countdown = countdownSeconds

                while (countdown > 0) {
                    delay(1000)
                    countdown--
                }

                // Trigger dynamic customized screen flash effect
                triggerFlashAnimation = true
                delay(flashConfig.durationMillis.toLong())
                triggerFlashAnimation = false

                isCountingDown = false

                // Capture photo
                var captured = false
                capturePhotoFromCameraX(
                    context = context,
                    imageCapture = imageCapture,
                    isExternalUsb = isExternalUsb,
                    onSuccess = { bitmap ->
                        capturedList.add(processCapturedPhoto(bitmap))
                        captured = true
                    },
                    onError = {
                        val fallback = if (isExternalUsb) {
                            UsbOtgCameraManager.createUsbCameraPreviewFrame(
                                deviceLabel = activeUsbDevice?.productName ?: "USB OTG WEBCAM"
                            )
                        } else {
                            createSimulatedCameraFrame(currentPhotoIndex)
                        }
                        capturedList.add(processCapturedPhoto(fallback))
                        captured = true
                    }
                )

                var waitCount = 0
                while (!captured && waitCount < 15) {
                    delay(100)
                    waitCount++
                }

                if (!captured) {
                    val fallback = if (isExternalUsb) {
                        UsbOtgCameraManager.createUsbCameraPreviewFrame(
                            deviceLabel = activeUsbDevice?.productName ?: "USB OTG WEBCAM"
                        )
                    } else {
                        createSimulatedCameraFrame(currentPhotoIndex)
                    }
                    capturedList.add(processCapturedPhoto(fallback))
                }

                delay(800)

                if (currentPhotoIndex + 1 < totalPhotos) {
                    currentPhotoIndex++
                } else {
                    onPhotosComplete(capturedList.toList())
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkObsidian)
    ) {
        if (!hasCameraPermission) {
            // Permission Grant Modern UI
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkObsidian),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(420.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0x331E293B))
                        .border(BorderStroke(1.dp, Color(0x33FFFFFF)), RoundedCornerShape(24.dp))
                        .padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Permission",
                        tint = NeonRose,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera Access Required",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To capture high-resolution photos for your booth session, please enable camera access.",
                        color = TextMuted,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("ENABLE CAMERA ACCESS", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // LIVE CAMERA PREVIEW VIEW
            Box(modifier = Modifier.fillMaxSize()) {
                CameraXPreviewView(
                    lensFacing = lensFacing,
                    isExternalUsb = isExternalUsb,
                    usbDeviceName = activeUsbDevice?.productName ?: "USB OTG Camera",
                    exposureIndex = (brightnessLevel / 10f).toInt(),
                    onImageCaptureReady = { capture -> imageCapture = capture },
                    modifier = Modifier.fillMaxSize()
                )

                // Real-time Live Screen Brightness Tint Overlay
                if (brightnessLevel > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = (brightnessLevel / 140f).coerceIn(0f, 0.45f)))
                    )
                } else if (brightnessLevel < 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = ((-brightnessLevel) / 100f).coerceIn(0f, 0.65f)))
                    )
                }
            }

            // Optional Grid Lines Overlay
            if (showGrid) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val gridColor = Color(0x33FFFFFF)
                    // Rule of thirds
                    drawLine(gridColor, Offset(w / 3, 0f), Offset(w / 3, h), strokeWidth = 2f)
                    drawLine(gridColor, Offset(w * 2 / 3, 0f), Offset(w * 2 / 3, h), strokeWidth = 2f)
                    drawLine(gridColor, Offset(0f, h / 3), Offset(w, h / 3), strokeWidth = 2f)
                    drawLine(gridColor, Offset(0f, h * 2 / 3), Offset(w, h * 2 / 3), strokeWidth = 2f)
                }
            }

            // DYNAMIC CUSTOMIZABLE SCREEN FLASH ANIMATION OVERLAY
            FlashAnimationOverlay(
                isTriggered = triggerFlashAnimation,
                config = flashConfig,
                modifier = Modifier.fillMaxSize()
            )

            // Top Status Bar Overlay - Modernized Transparent Glassmorphism
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x77000000))
                            .border(BorderStroke(1.dp, Color(0x22FFFFFF)), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isExternalUsb) Color(0xFF10B981) else if (isGifMode) NeonCyan else NeonRose)
                        )
                        Text(
                            text = if (isExternalUsb) {
                                val dslr = activeGPhoto2Camera?.brand?.displayName ?: "DSLR"
                                if (isGifMode) "$dslr libgphoto2 • FRAME ${currentPhotoIndex + 1}/$totalPhotos"
                                else "$dslr libgphoto2 • SHOT ${currentPhotoIndex + 1}/$totalPhotos"
                            } else if (isGifMode) {
                                if (isBurstRecording) "FRAME ${currentPhotoIndex + 1}/$totalPhotos"
                                else "GIF BURST"
                            } else {
                                "SHOT ${currentPhotoIndex + 1}/$totalPhotos"
                            },
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    // Camera quick controls with transparent modern styling
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Brightness Toggle Button (with level badge)
                        IconButton(
                            onClick = { showBrightnessPanel = !showBrightnessPanel },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (showBrightnessPanel || brightnessLevel != 0f) NeonCyan.copy(alpha = 0.35f) else Color(0x77000000))
                                .border(
                                    width = if (showBrightnessPanel || brightnessLevel != 0f) 1.5.dp else 1.dp,
                                    color = if (showBrightnessPanel || brightnessLevel != 0f) NeonCyan else Color(0x22FFFFFF),
                                    shape = CircleShape
                                )
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = "Brightness",
                                tint = if (showBrightnessPanel || brightnessLevel != 0f) NeonCyan else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Countdown Timer Selector
                        IconButton(
                            onClick = {
                                countdownSeconds = when (countdownSeconds) {
                                    3 -> 5
                                    5 -> 10
                                    else -> 3
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x77000000))
                                .border(BorderStroke(1.dp, Color(0x22FFFFFF)), CircleShape)
                                .size(36.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "${countdownSeconds}s",
                                    color = NeonRose,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // USB OTG Camera Toggle
                        IconButton(
                            onClick = {
                                isExternalUsb = !isExternalUsb
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isExternalUsb) NeonCyan.copy(alpha = 0.35f) else Color(0x77000000))
                                .border(
                                    width = if (isExternalUsb) 1.5.dp else 1.dp,
                                    color = if (isExternalUsb) NeonCyan else Color(0x22FFFFFF),
                                    shape = CircleShape
                                )
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Usb,
                                contentDescription = "USB OTG Camera",
                                tint = if (isExternalUsb) NeonCyan else if (isUsbCameraConnected) Color(0xFF10B981) else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { showGrid = !showGrid },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x77000000))
                                .border(BorderStroke(1.dp, Color(0x22FFFFFF)), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridOn,
                                contentDescription = "Grid",
                                tint = if (showGrid) NeonCyan else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { isFlashOn = !isFlashOn },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x77000000))
                                .border(BorderStroke(1.dp, Color(0x22FFFFFF)), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Flash",
                                tint = if (isFlashOn) Color.Yellow else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (isExternalUsb) {
                                    isExternalUsb = false
                                } else {
                                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                                        CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x77000000))
                                .border(BorderStroke(1.dp, Color(0x22FFFFFF)), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Transparent Brightness Adjustment Overlay Panel (Transparan Sesuai Permintaan)
                AnimatedVisibility(
                    visible = showBrightnessPanel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x66000000), // Transparent glass background
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WbSunny,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "BRIGHTNESS CAMERA",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = if (brightnessLevel > 0) "+${brightnessLevel.toInt()}" else "${brightnessLevel.toInt()}",
                                        color = NeonCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (brightnessLevel != 0f) {
                                        Text(
                                            text = "RESET",
                                            color = NeonRose,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { brightnessLevel = 0f }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { showBrightnessPanel = false },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Interactive Brightness Slider
                            Slider(
                                value = brightnessLevel,
                                onValueChange = { brightnessLevel = it },
                                valueRange = -50f..50f,
                                colors = SliderDefaults.colors(
                                    thumbColor = NeonCyan,
                                    activeTrackColor = NeonCyan,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Quick preset brightness buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(-20f to "🌙 Gelap", 0f to "✨ Normal", 20f to "☀️ Cerah", 40f to "⚡ Glow").forEach { (level, label) ->
                                    val isSelected = brightnessLevel == level
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) NeonCyan else Color(0x44000000),
                                        border = BorderStroke(1.dp, if (isSelected) NeonCyan else Color(0x33FFFFFF)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { brightnessLevel = level }
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Customizer Quick Bar with transparent frosted style
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Watermark Quick Edit Button
                    Button(
                        onClick = { showWatermarkEditorDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x66000000)),
                        border = BorderStroke(1.dp, if (watermarkConfig.isEnabled) NeonCyan else Color(0x33FFFFFF)),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Edit Watermark",
                            tint = if (watermarkConfig.isEnabled) NeonCyan else TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (watermarkConfig.isEnabled) "WM: ${watermarkConfig.corner.shortLabel}" else "WM: Off",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Flash Animation Style Quick Edit Button
                    Button(
                        onClick = { showFlashEditorDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x66000000)),
                        border = BorderStroke(1.dp, NeonRose.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Edit Flash",
                            tint = Color(flashConfig.colorHex),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "FX: ${flashConfig.style.displayName}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Center Countdown / Burst HUD Overlay
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isSessionStarted) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.08f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseScale"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier
                            .width(340.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color(0xEE0F172A)) // Sleek dark card background
                            .border(BorderStroke(1.5.dp, Brush.linearGradient(listOf(NeonCyan, NeonRose))), RoundedCornerShape(28.dp))
                            .padding(28.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = NeonRose.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, NeonRose.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "KAMERA SIAP 📸",
                                color = NeonRose,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                letterSpacing = 1.2.sp
                            )
                        }

                        Text(
                            text = managerConfig.eventName.ifBlank { "VIP PHOTO BOOTH" },
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Text(
                            text = "Siapkan gaya terbaik Anda, lalu ketuk tombol di bawah untuk memulai sesi foto!",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Large Pulsating Start Button
                        Button(
                            onClick = { isSessionStarted = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .scale(pulseScale)
                                .background(
                                    Brush.horizontalGradient(listOf(NeonRose, NeonCyan)),
                                    shape = RoundedCornerShape(20.dp)
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Camera,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "MULAI SEKARANG ⚡",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                } else if (isCountingDown) {
                    val scale by animateFloatAsState(
                        targetValue = when (countdown) {
                            3 -> 1.2f
                            2 -> 1.2f
                            1 -> 1.2f
                            else -> 1.0f
                        },
                        animationSpec = tween(durationMillis = 300)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Top preparation badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.85f),
                            border = BorderStroke(1.5.dp, if (isGifMode) NeonCyan else NeonRose)
                        ) {
                            Text(
                                text = if (isGifMode) {
                                    "SIAP-SIAP BURST VIDEO 🎬"
                                } else {
                                    "SIAP-SIAP FOTO KE-${currentPhotoIndex + 1} / $totalPhotos 📸"
                                },
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                letterSpacing = 1.sp
                            )
                        }

                        // Central Countdown Circle with Glow
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(180.dp)
                                .scale(scale)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.75f))
                                .border(
                                    BorderStroke(
                                        width = 6.dp,
                                        brush = Brush.sweepGradient(
                                            colors = if (isGifMode) {
                                                listOf(NeonCyan, Color.Transparent, NeonCyan)
                                            } else {
                                                listOf(NeonRose, Color.Transparent, NeonRose)
                                            }
                                        )
                                    ),
                                    shape = CircleShape
                                )
                        ) {
                            // Circular Indicator Background Ring
                            CircularProgressIndicator(
                                progress = countdown / 3f,
                                modifier = Modifier.fillMaxSize().padding(6.dp),
                                color = if (isGifMode) NeonCyan else NeonRose,
                                strokeWidth = 6.dp,
                                trackColor = Color.White.copy(alpha = 0.15f)
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "$countdown",
                                    color = Color.White,
                                    fontSize = 84.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-2).sp
                                )
                            }
                        }

                        // Bottom Action Subtext
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.8f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                        ) {
                            Text(
                                text = "POSE SEKARANG! ✨",
                                color = if (isGifMode) NeonCyan else NeonRose,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                letterSpacing = 1.5.sp
                            )
                        }
                    }
                } else if (isBurstRecording) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(30.dp))
                            .background(
                                Brush.horizontalGradient(listOf(NeonCyan, NeonRose))
                            )
                            .padding(horizontal = 32.dp, vertical = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MotionPhotosOn,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "BURST RECORDING! (${currentPhotoIndex + 1}/$totalPhotos)",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(30.dp))
                            .background(NeonRose)
                            .padding(horizontal = 32.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = "SMILE! 📸",
                            color = Color.White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Bottom Logo Branding in Live Preview Screen
            if (capturedList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0x77000000))
                                .border(BorderStroke(1.5.dp, Color(0x33FFFFFF)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!managerConfig.welcomeCustomLogoUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                                        .data(Uri.parse(managerConfig.welcomeCustomLogoUri))
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Live Booth Logo",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Camera,
                                    contentDescription = "Live Booth Logo",
                                    tint = NeonRose,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Text(
                            text = managerConfig.eventName.ifBlank { "VIP PHOTO BOOTH" },
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                }
            }

            // Bottom Live Thumbnails Strip
            if (capturedList.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(20.dp)
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xDD0F172A))
                            .padding(12.dp)
                    ) {
                        itemsIndexed(capturedList) { index, bitmap ->
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        BorderStroke(2.dp, if (isGifMode) NeonCyan else NeonRose),
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Captured $index",
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (isGifMode) NeonCyan else NeonRose),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // GIF Generation Progress Animation Dialog
        if (showGifProgressDialog) {
            GifProgressDialog(
                progress = gifProgressVal,
                statusMessage = gifStatusMsg,
                sampleFrames = capturedList.toList()
            )
        }

        // Flash Editor Dialog Modal
        if (showFlashEditorDialog) {
            FlashEditorDialog(
                config = flashConfig,
                onConfigChanged = onFlashConfigChanged,
                onDismiss = { showFlashEditorDialog = false }
            )
        }

        // Watermark Editor Dialog Modal
        if (showWatermarkEditorDialog) {
            WatermarkEditorDialog(
                config = watermarkConfig,
                onConfigChanged = onWatermarkConfigChanged,
                onDismiss = { showWatermarkEditorDialog = false }
            )
        }
    }
}

private fun createSimulatedCameraFrame(index: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(1200, 900, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)

    // High quality studio photo frame background
    val bgColors = intArrayOf(
        android.graphics.Color.parseColor("#1E1B4B"),
        android.graphics.Color.parseColor("#0F172A")
    )
    val gradient = android.graphics.LinearGradient(
        0f, 0f, 1200f, 900f,
        bgColors, null, android.graphics.Shader.TileMode.CLAMP
    )
    val bgPaint = Paint().apply { shader = gradient }
    canvas.drawRect(0f, 0f, 1200f, 900f, bgPaint)

    // Draw stylized motion subject with shifting angle / position
    val circlePaint = Paint().apply {
        color = when (index % 4) {
            0 -> android.graphics.Color.parseColor("#FF2A5F")
            1 -> android.graphics.Color.parseColor("#00E5FF")
            2 -> android.graphics.Color.parseColor("#8A2BE2")
            else -> android.graphics.Color.parseColor("#10B981")
        }
        isAntiAlias = true
    }
    val offsetX = (index * 40f) - 140f
    canvas.drawCircle(600f + offsetX, 400f, 180f, circlePaint)

    val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 54f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText("MOTION FRAME #${index + 1}", 600f, 720f, textPaint)

    return bitmap
}

private fun adjustBitmapBrightness(bitmap: Bitmap, value: Float): Bitmap {
    val dest = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(dest)
    val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        val cm = android.graphics.ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, value,
            0f, 1f, 0f, 0f, value,
            0f, 0f, 1f, 0f, value,
            0f, 0f, 0f, 1f, 0f
        ))
        colorFilter = android.graphics.ColorMatrixColorFilter(cm)
    }
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return dest
}
