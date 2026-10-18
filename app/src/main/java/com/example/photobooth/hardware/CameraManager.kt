package com.example.photobooth.hardware

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.nio.ByteBuffer

class CameraManager(private val context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private val backgroundHandler = Handler(Looper.getMainLooper())

    private val _iso = MutableStateFlow(400)
    val iso: StateFlow<Int> = _iso

    private val _exposure = MutableStateFlow(0)
    val exposure: StateFlow<Int> = _exposure

    private val _flashEnabled = MutableStateFlow(false)
    val flashEnabled: StateFlow<Boolean> = _flashEnabled

    private var currentCameraId: String = "0"

    init {
        try {
            val cameraIdList = cameraManager.cameraIdList
            if (cameraIdList.isNotEmpty()) {
                currentCameraId = cameraIdList[0]
            }
        } catch (e: Exception) {
            Log.e("CameraManager", "Error initializing camera ID", e)
        }
    }

    fun setIso(isoValue: Int) {
        _iso.value = isoValue
        updateCameraParameters()
    }

    fun setExposure(exposureValue: Int) {
        _exposure.value = exposureValue
        updateCameraParameters()
    }

    fun setFlash(enabled: Boolean) {
        _flashEnabled.value = enabled
        updateCameraParameters()
    }

    private fun updateCameraParameters() {
        try {
            captureSession?.let { session ->
                cameraDevice?.let { device ->
                    val requestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON)
                    if (_flashEnabled.value) {
                        requestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH)
                    } else {
                        requestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF)
                    }
                    session.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)
                }
            }
        } catch (e: Exception) {
            Log.e("CameraManager", "Failed to update camera parameters", e)
        }
    }

    fun capturePhoto(onPhotoCaptured: (Bitmap) -> Unit) {
        try {
            val device = cameraDevice ?: return
            val captureBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            imageReader?.surface?.let { captureBuilder.addTarget(it) }

            val readerListener = ImageReader.OnImageAvailableListener { reader ->
                val image = reader.acquireLatestImage()
                val buffer: ByteBuffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                image.close()
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                backgroundHandler.post {
                    onPhotoCaptured(bitmap)
                }
            }
            imageReader?.setOnImageAvailableListener(readerListener, backgroundHandler)

            captureSession?.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                    super.onCaptureCompleted(session, request, result)
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            Log.e("CameraManager", "Capture photo failed", e)
        }
    }

    fun release() {
        try {
            captureSession?.close()
            cameraDevice?.close()
            imageReader?.close()
        } catch (e: Exception) {
            Log.e("CameraManager", "Error releasing camera", e)
        }
    }
}
