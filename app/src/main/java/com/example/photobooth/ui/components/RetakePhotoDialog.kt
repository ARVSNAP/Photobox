package com.example.photobooth.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.photobooth.hardware.CameraXPreviewView
import com.example.photobooth.hardware.UsbOtgCameraManager
import com.example.photobooth.hardware.capturePhotoFromCameraX
import com.example.photobooth.model.BoothManagerConfig
import com.example.photobooth.model.FlashAnimationConfig
import com.example.photobooth.model.WatermarkConfig
import com.example.photobooth.utils.WatermarkHelper
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RetakePhotoDialog(
    slotIndex: Int,
    slotTitle: String = "Slot #${slotIndex + 1}",
    layoutType: String,
    currentPhoto: Bitmap?,
    flashConfig: FlashAnimationConfig = FlashAnimationConfig(),
    watermarkConfig: WatermarkConfig = WatermarkConfig(),
    managerConfig: BoothManagerConfig = BoothManagerConfig(),
    onPhotoRetaken: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isUsbCameraConnected by UsbOtgCameraManager.isUsbCameraConnected.collectAsState()
    val activeUsbDevice by UsbOtgCameraManager.activeCameraDevice.collectAsState()

    var isExternalUsb by remember {
        mutableStateOf(
            managerConfig.cameraFacing == "USB OTG" ||
                    managerConfig.cameraFacing == "Kamera Eksternal USB OTG" ||
                    managerConfig.cameraFacing == "DSLR libgphoto2" ||
                    (managerConfig.autoSwitchToUsbCamera && isUsbCameraConnected)
        )
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

    var lensFacing by remember {
        mutableStateOf(
            if (managerConfig.cameraFacing == "Back DSLR / Webcam" || managerConfig.cameraFacing == "Back DSLR") {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
        )
    }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var showGrid by remember { mutableStateOf(true) }
    var useCountdown by remember { mutableStateOf(true) }
    var countdownValue by remember { mutableIntStateOf(0) }
    var isCountingDown by remember { mutableStateOf(false) }
    var triggerFlash by remember { mutableStateOf(false) }

    // Staged newly captured photo
    var newCapturedPhoto by remember { mutableStateOf<Bitmap?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    fun takePhotoNow() {
        if (isCapturing || isCountingDown) return

        coroutineScope.launch {
            if (useCountdown) {
                isCountingDown = true
                countdownValue = 3
                while (countdownValue > 0) {
                    delay(1000)
                    countdownValue--
                }
                isCountingDown = false
            }

            // Flash effect
            triggerFlash = true
            delay(flashConfig.durationMillis.toLong().coerceAtLeast(150))
            triggerFlash = false

            isCapturing = true

            capturePhotoFromCameraX(
                context = context,
                imageCapture = imageCapture,
                isExternalUsb = isExternalUsb,
                onSuccess = { bitmap ->
                    val processed = if (watermarkConfig.isEnabled) {
                        WatermarkHelper.applyWatermark(bitmap, watermarkConfig)
                    } else {
                        bitmap
                    }
                    newCapturedPhoto = processed
                    isCapturing = false
                },
                onError = {
                    val fallback = if (isExternalUsb) {
                        UsbOtgCameraManager.createUsbCameraPreviewFrame(
                            deviceLabel = activeUsbDevice?.productName ?: "USB OTG RETAKE"
                        )
                    } else {
                        createSimulatedRetakeFrame(slotIndex)
                    }
                    val processed = if (watermarkConfig.isEnabled) {
                        WatermarkHelper.applyWatermark(fallback, watermarkConfig)
                    } else {
                        fallback
                    }
                    newCapturedPhoto = processed
                    isCapturing = false
                }
            )
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isCapturing && !isCountingDown) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF0030712))
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .widthIn(max = 520.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.5.dp, NeonRose.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Bar
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
                                shape = RoundedCornerShape(10.dp),
                                color = NeonRose.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, NeonRose)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = NeonRose,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "FOTO ULANG",
                                        color = NeonRose,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = slotTitle,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tata Letak: $layoutType",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Main View Area: Either Live Camera Preview OR Comparison of New vs Old
                    if (newCapturedPhoto == null) {
                        // ==========================================
                        // LIVE RETAKE CAMERA VIEW
                        // ==========================================
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black)
                                .border(BorderStroke(1.5.dp, if (isExternalUsb) NeonCyan else NeonRose), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (hasCameraPermission) {
                                CameraXPreviewView(
                                    lensFacing = lensFacing,
                                    isExternalUsb = isExternalUsb,
                                    usbDeviceName = activeUsbDevice?.productName ?: "USB OTG Camera",
                                    exposureIndex = 0,
                                    onImageCaptureReady = { capture -> imageCapture = capture },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Izin Kamera Diperlukan",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Button(
                                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Izinkan Kamera", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Flash Animation Overlay
                            FlashAnimationOverlay(
                                isTriggered = triggerFlash,
                                config = flashConfig,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Rule of Thirds Grid Overlay
                            if (showGrid && hasCameraPermission) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                        val strokeW = 1.dp.toPx()
                                        val gridColor = Color(0x44FFFFFF)
                                        // Vertical lines
                                        drawLine(gridColor, androidx.compose.ui.geometry.Offset(size.width / 3f, 0f), androidx.compose.ui.geometry.Offset(size.width / 3f, size.height), strokeW)
                                        drawLine(gridColor, androidx.compose.ui.geometry.Offset(size.width * 2f / 3f, 0f), androidx.compose.ui.geometry.Offset(size.width * 2f / 3f, size.height), strokeW)
                                        // Horizontal lines
                                        drawLine(gridColor, androidx.compose.ui.geometry.Offset(0f, size.height / 3f), androidx.compose.ui.geometry.Offset(size.width, size.height / 3f), strokeW)
                                        drawLine(gridColor, androidx.compose.ui.geometry.Offset(0f, size.height * 2f / 3f), androidx.compose.ui.geometry.Offset(size.width, size.height * 2f / 3f), strokeW)
                                    }
                                }
                            }

                            // Top Quick Controls Overlay (Flip, USB, Grid, Timer)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Source Badge
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xCC000000)
                                ) {
                                    Text(
                                        text = if (isExternalUsb) "USB OTG" else if (lensFacing == CameraSelector.LENS_FACING_FRONT) "FRONT" else "REAR",
                                        color = if (isExternalUsb) NeonCyan else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Countdown toggle
                                    IconButton(
                                        onClick = { useCountdown = !useCountdown },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (useCountdown) NeonRose.copy(alpha = 0.4f) else Color(0x99000000))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = "Countdown",
                                            tint = if (useCountdown) NeonRose else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Grid toggle
                                    IconButton(
                                        onClick = { showGrid = !showGrid },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (showGrid) NeonCyan.copy(alpha = 0.4f) else Color(0x99000000))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GridOn,
                                            contentDescription = "Grid",
                                            tint = if (showGrid) NeonCyan else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // USB toggle
                                    IconButton(
                                        onClick = { isExternalUsb = !isExternalUsb },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (isExternalUsb) NeonCyan.copy(alpha = 0.4f) else Color(0x99000000))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Usb,
                                            contentDescription = "USB",
                                            tint = if (isExternalUsb) NeonCyan else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Switch Camera
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
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x99000000))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Cameraswitch,
                                            contentDescription = "Switch Camera",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Center Countdown HUD
                            if (isCountingDown) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xCC000000))
                                        .border(BorderStroke(3.dp, NeonRose), CircleShape)
                                ) {
                                    Text(
                                        text = "$countdownValue",
                                        color = Color.White,
                                        fontSize = 54.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        // Bottom Action Controls: Shutter Button & Original Photo Reference
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail of current photo to be replaced
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (currentPhoto != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(BorderStroke(1.dp, Color(0x44FFFFFF)), RoundedCornerShape(8.dp))
                                    ) {
                                        Image(
                                            bitmap = currentPhoto.asImageBitmap(),
                                            contentDescription = "Foto Sebelumnya",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Column {
                                        Text("Foto Sekarang", color = TextMuted, fontSize = 10.sp)
                                        Text("Akan diganti", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Capture / Retake Button
                            Button(
                                onClick = { takePhotoNow() },
                                enabled = !isCapturing && !isCountingDown,
                                colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Camera,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (useCountdown) "Jepret (3s)" else "Jepret Sekarang",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        // ==========================================
                        // RESULT COMPARISON VIEW (NEW vs OLD)
                        // ==========================================
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Bandingkan Hasil Jepretan Baru:",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Previous photo (if exists)
                                if (currentPhoto != null) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Foto Lama",
                                            color = TextMuted,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(BorderStroke(1.dp, Color(0x33FFFFFF)), RoundedCornerShape(12.dp))
                                        ) {
                                            Image(
                                                bitmap = currentPhoto.asImageBitmap(),
                                                contentDescription = "Foto Lama",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }

                                // Newly Captured Photo
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Foto Baru ✨",
                                            color = NeonCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(BorderStroke(2.dp, NeonCyan), RoundedCornerShape(12.dp))
                                    ) {
                                        Image(
                                            bitmap = newCapturedPhoto!!.asImageBitmap(),
                                            contentDescription = "Foto Baru",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }

                            Surface(
                                color = Color(0x2210B981),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Foto berhasil diambil! Pilih 'Pasang ke Layout' untuk menerapkan.",
                                        color = Color.White,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Decision Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Retake again button
                                OutlinedButton(
                                    onClick = { newCapturedPhoto = null },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0x66FFFFFF)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Jepret Ulang", color = Color.White, fontSize = 12.sp)
                                }

                                // Apply to slot button
                                Button(
                                    onClick = {
                                        newCapturedPhoto?.let { photo ->
                                            onPhotoRetaken(photo)
                                            Toast.makeText(
                                                context,
                                                "$slotTitle berhasil diperbarui!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            onDismiss()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Pasang ke Layout",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
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

private fun createSimulatedRetakeFrame(slotIndex: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(1200, 900, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)

    val gradient = android.graphics.LinearGradient(
        0f, 0f, 1200f, 900f,
        intArrayOf(
            android.graphics.Color.parseColor("#0F172A"),
            android.graphics.Color.parseColor("#FF2A5F")
        ),
        null,
        android.graphics.Shader.TileMode.CLAMP
    )
    val paint = Paint().apply { shader = gradient }
    canvas.drawRect(0f, 0f, 1200f, 900f, paint)

    val circlePaint = Paint().apply {
        color = android.graphics.Color.parseColor("#00F0FF")
        isAntiAlias = true
    }
    canvas.drawCircle(600f, 400f, 170f, circlePaint)

    val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 50f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText("RETAKE SLOT #${slotIndex + 1}", 600f, 720f, textPaint)
    return bitmap
}
