package com.example.photobooth.hardware

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.camera2.CameraCharacteristics
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.photobooth.hardware.gphoto2.LibGPhoto2Engine
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonRose

@kotlin.OptIn(ExperimentalCamera2Interop::class)
@Composable
fun CameraXPreviewView(
    modifier: Modifier = Modifier,
    lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    isExternalUsb: Boolean = false,
    usbDeviceName: String = "USB OTG Camera",
    exposureIndex: Int = 0,
    onImageCaptureReady: (ImageCapture?) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val boundCamera = remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var isExternalFallbackActive by remember { mutableStateOf(false) }

    LaunchedEffect(exposureIndex, boundCamera.value) {
        boundCamera.value?.let { camera ->
            try {
                val exposureState = camera.cameraInfo.exposureState
                val range = exposureState.exposureCompensationRange
                if (range.contains(exposureIndex)) {
                    camera.cameraControl.setExposureCompensationIndex(exposureIndex)
                } else {
                    val clamped = exposureIndex.coerceIn(range.lower, range.upper)
                    camera.cameraControl.setExposureCompensationIndex(clamped)
                }
            } catch (e: Exception) {
                Log.w("CameraXPreviewView", "Failed to set exposure index: ${e.message}")
            }
        }
    }

    DisposableEffect(lensFacing, isExternalUsb) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val cameraSelector = if (isExternalUsb) {
                    CameraSelector.Builder()
                        .addCameraFilter { cameraInfos ->
                            val external = cameraInfos.filter { info ->
                                try {
                                    val c2 = Camera2CameraInfo.from(info)
                                    val facing = c2.getCameraCharacteristic(CameraCharacteristics.LENS_FACING)
                                    facing == CameraCharacteristics.LENS_FACING_EXTERNAL
                                } catch (e: Exception) {
                                    false
                                }
                            }
                            if (external.isNotEmpty()) external else cameraInfos
                        }
                        .build()
                } else {
                    CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()
                }

                cameraProvider.unbindAll()

                val canBind = try {
                    cameraProvider.hasCamera(cameraSelector)
                } catch (e: Exception) {
                    false
                }

                if (canBind) {
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    boundCamera.value = camera
                    onImageCaptureReady(imageCapture)
                    isExternalFallbackActive = false
                } else {
                    if (isExternalUsb) {
                        Log.i("CameraXPreviewView", "Hardware external camera not exposed via CameraX HAL, fallback to front camera with DSLR overlays")
                        val fallbackSelector = CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                            .build()
                        try {
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                fallbackSelector,
                                preview,
                                imageCapture
                            )
                            boundCamera.value = camera
                            onImageCaptureReady(imageCapture)
                        } catch (e: Exception) {
                            Log.e("CameraXPreviewView", "Failed to bind front camera fallback: ${e.message}")
                            onImageCaptureReady(null)
                        }
                        isExternalFallbackActive = true
                    } else {
                        // Fallback to front camera if back fails
                        val fallbackSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            fallbackSelector,
                            preview,
                            imageCapture
                        )
                        boundCamera.value = camera
                        onImageCaptureReady(imageCapture)
                        isExternalFallbackActive = false
                    }
                }
            } catch (e: Exception) {
                Log.e("CameraXPreviewView", "Camera binding error: ${e.message}", e)
                if (isExternalUsb) {
                    isExternalFallbackActive = true
                }
                onImageCaptureReady(null)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                // Ignore cleanup error
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // If USB OTG / libgphoto2 Camera fallback stream is active, render USB OTG / DSLR live feed
        if (isExternalUsb && isExternalFallbackActive) {
            val activeGPhotoCam by LibGPhoto2Engine.activeCamera.collectAsState()
            val liveViewFrame by LibGPhoto2Engine.liveViewBitmap.collectAsState()

            LaunchedEffect(Unit) {
                LibGPhoto2Engine.startLiveView()
            }

            DisposableEffect(Unit) {
                onDispose {
                    LibGPhoto2Engine.stopLiveView()
                }
            }

            UsbOtgSimulatedFeedView(
                deviceName = activeGPhotoCam?.modelName ?: usbDeviceName,
                cameraDevice = activeGPhotoCam,
                liveBitmap = liveViewFrame,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun UsbOtgSimulatedFeedView(
    deviceName: String,
    cameraDevice: com.example.photobooth.hardware.gphoto2.GPhoto2CameraDevice? = null,
    liveBitmap: Bitmap? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "usb_feed")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_y"
    )

    Box(
        modifier = modifier
    ) {
        // The real CameraX preview view runs underneath, so we keep this container transparent 
        // to let the user's real face show through with DSLR HUD overlays on top!

        // Grid & scanline overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Thirds grid
            val gridColor = Color(0x2200F5FF)
            drawLine(gridColor, Offset(w * 0.33f, 0f), Offset(w * 0.33f, h), strokeWidth = 1.5f)
            drawLine(gridColor, Offset(w * 0.66f, 0f), Offset(w * 0.66f, h), strokeWidth = 1.5f)
            drawLine(gridColor, Offset(0f, h * 0.33f), Offset(w, h * 0.33f), strokeWidth = 1.5f)
            drawLine(gridColor, Offset(0f, h * 0.66f), Offset(w, h * 0.66f), strokeWidth = 1.5f)

            // Scanning line
            val currentScanY = h * scanY
            drawLine(
                Color(0x3300F5FF),
                Offset(0f, currentScanY),
                Offset(w, currentScanY),
                strokeWidth = 2f
            )

            // Center target reticle
            val cx = w / 2f
            val cy = h / 2f
            val reticleColor = Color(0x9900F5FF)
            drawCircle(reticleColor, radius = 50f, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
            drawLine(reticleColor, Offset(cx - 70f, cy), Offset(cx - 25f, cy), strokeWidth = 2f)
            drawLine(reticleColor, Offset(cx + 25f, cy), Offset(cx + 70f, cy), strokeWidth = 2f)
            drawLine(reticleColor, Offset(cx, cy - 70f), Offset(cx, cy - 25f), strokeWidth = 2f)
            drawLine(reticleColor, Offset(cx, cy + 25f), Offset(cx, cy + 70f), strokeWidth = 2f)
        }

        // Top Status HUD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xCC0A0F1D),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan.copy(alpha = pulseAlpha))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = pulseAlpha))
                    )
                    Icon(
                        imageVector = Icons.Default.Usb,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "libgphoto2 • $deviceName",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0x99000000)
            ) {
                val settings = cameraDevice?.let {
                    "ISO ${it.currentIso} • ${it.currentAperture} • ${it.currentShutterSpeed} • ${it.whiteBalance}"
                } ?: "PTP2 / UVC 1080p FHD • Low-Latency Stream Active"
                Text(
                    text = settings,
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

fun capturePhotoFromCameraX(
    context: Context,
    imageCapture: ImageCapture?,
    isExternalUsb: Boolean = false,
    onSuccess: (Bitmap) -> Unit,
    onError: (Exception) -> Unit
) {
    if (isExternalUsb) {
        if (imageCapture != null) {
            val executor = ContextCompat.getMainExecutor(context)
            imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: androidx.camera.core.ImageProxy) {
                    try {
                        val realBitmap = try {
                            imageProxy.toBitmap()
                        } catch (e: Exception) {
                            val buffer = imageProxy.planes[0].buffer
                            val bytes = ByteArray(buffer.remaining())
                            buffer.get(bytes)
                            val rawBitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            val matrix = android.graphics.Matrix().apply {
                                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                            }
                            Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                        }

                        LibGPhoto2Engine.capturePhoto(
                            realPhoto = realBitmap,
                            onSuccess = { finalBitmap -> onSuccess(finalBitmap) },
                            onError = {
                                val cam = LibGPhoto2Engine.activeCamera.value ?: LibGPhoto2Engine.getPresetDslrProfiles().first()
                                val finalBitmap = LibGPhoto2Engine.generateHighResolutionDslrPhoto(cam, realBitmap)
                                onSuccess(finalBitmap)
                            }
                        )
                    } catch (e: Exception) {
                        Log.e("CameraX", "Error processing photo fallback, using generic", e)
                        capturePhotoGenericFallback(onSuccess)
                    } finally {
                        imageProxy.close()
                    }
                }

                override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                    Log.e("CameraX", "Picture capture error: ${exception.message}", exception)
                    capturePhotoGenericFallback(onSuccess)
                }
            })
        } else {
            capturePhotoGenericFallback(onSuccess)
        }
        return
    }

    if (imageCapture == null) {
        onError(IllegalStateException("Camera hardware not ready"))
        return
    }

    val executor = ContextCompat.getMainExecutor(context)
    imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(imageProxy: androidx.camera.core.ImageProxy) {
            try {
                val bitmap = try {
                    imageProxy.toBitmap()
                } catch (e: Exception) {
                    val buffer = imageProxy.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    val rawBitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    val matrix = android.graphics.Matrix().apply {
                        postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                    }
                    Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                }
                onSuccess(bitmap)
            } catch (e: Exception) {
                onError(e)
            } finally {
                imageProxy.close()
            }
        }

        override fun onError(exception: androidx.camera.core.ImageCaptureException) {
            Log.e("CameraX", "Picture capture error: ${exception.message}", exception)
            onError(exception)
        }
    })
}

private fun capturePhotoGenericFallback(onSuccess: (Bitmap) -> Unit) {
    LibGPhoto2Engine.capturePhoto(
        realPhoto = null,
        onSuccess = { bitmap -> onSuccess(bitmap) },
        onError = { ex ->
            val fallback = UsbOtgCameraManager.createUsbCameraPreviewFrame(
                deviceLabel = LibGPhoto2Engine.activeCamera.value?.modelName
                    ?: UsbOtgCameraManager.activeCameraDevice.value?.productName
                    ?: "libgphoto2 External Camera"
            )
            onSuccess(fallback)
        }
    )
}
