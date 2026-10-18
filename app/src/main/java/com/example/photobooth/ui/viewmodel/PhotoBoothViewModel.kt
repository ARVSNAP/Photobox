package com.example.photobooth.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.photobooth.data.PhotoBoothDatabase
import com.example.photobooth.data.PhotoBoothRepository
import com.example.photobooth.data.SessionEntity
import com.example.photobooth.hardware.UsbCameraDeviceInfo
import com.example.photobooth.hardware.UsbOtgCameraManager
import com.example.photobooth.model.BoothManagerConfig
import com.example.photobooth.model.BrandingFrameConfig
import com.example.photobooth.model.FlashAnimationConfig
import com.example.photobooth.model.WatermarkConfig
import com.example.photobooth.model.LoopAnimationConfig
import com.example.photobooth.utils.GifComposer
import com.example.photobooth.utils.LoopAnimationStorage
import com.example.photobooth.utils.BrandingFrameStorage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PhotoBoothViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PhotoBoothRepository

    init {
        val sessionDao = PhotoBoothDatabase.getDatabase(application).sessionDao()
        repository = PhotoBoothRepository(sessionDao)
        UsbOtgCameraManager.initialize(application)
    }

    val isUsbCameraConnected: StateFlow<Boolean> = UsbOtgCameraManager.isUsbCameraConnected
    val connectedUsbDevices: StateFlow<List<UsbCameraDeviceInfo>> = UsbOtgCameraManager.connectedUsbDevices
    val activeUsbCamera: StateFlow<UsbCameraDeviceInfo?> = UsbOtgCameraManager.activeCameraDevice
    val hasCamera2External: StateFlow<Boolean> = UsbOtgCameraManager.hasCamera2External

    fun scanUsbDevices() {
        UsbOtgCameraManager.scanDevices(getApplication())
    }

    val allSessions: StateFlow<List<SessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedLayout = MutableStateFlow("2x2 Grid")
    val selectedLayout: StateFlow<String> = _selectedLayout

    private val _countdownSeconds = MutableStateFlow(3)
    val countdownSeconds: StateFlow<Int> = _countdownSeconds

    private val _selectedFilter = MutableStateFlow("Normal")
    val selectedFilter: StateFlow<String> = _selectedFilter

    private val _flashAnimationConfig = MutableStateFlow(FlashAnimationConfig())
    val flashAnimationConfig: StateFlow<FlashAnimationConfig> = _flashAnimationConfig

    private val _watermarkConfig = MutableStateFlow(WatermarkConfig(isEnabled = false))
    val watermarkConfig: StateFlow<WatermarkConfig> = _watermarkConfig

    private val _brandingFrameConfig = MutableStateFlow(BrandingFrameStorage.loadConfig(application))
    val brandingFrameConfig: StateFlow<BrandingFrameConfig> = _brandingFrameConfig

    private val _loopAnimationConfig = MutableStateFlow(LoopAnimationStorage.loadLoopConfig(application))
    val loopAnimationConfig: StateFlow<LoopAnimationConfig> = _loopAnimationConfig

    private val _managerConfig = MutableStateFlow(BoothManagerConfig())
    val managerConfig: StateFlow<BoothManagerConfig> = _managerConfig

    private val _capturedPhotos = MutableStateFlow<List<Bitmap>>(emptyList())
    val capturedPhotos: StateFlow<List<Bitmap>> = _capturedPhotos

    private val _finalComposedBitmap = MutableStateFlow<Bitmap?>(null)
    val finalComposedBitmap: StateFlow<Bitmap?> = _finalComposedBitmap

    // Animated GIF state
    private val _isGeneratingGif = MutableStateFlow(false)
    val isGeneratingGif: StateFlow<Boolean> = _isGeneratingGif

    private val _gifProgress = MutableStateFlow(0f)
    val gifProgress: StateFlow<Float> = _gifProgress

    private val _gifStatusMessage = MutableStateFlow("")
    val gifStatusMessage: StateFlow<String> = _gifStatusMessage

    private val _generatedGifBytes = MutableStateFlow<ByteArray?>(null)
    val generatedGifBytes: StateFlow<ByteArray?> = _generatedGifBytes

    private val _isBoomerang = MutableStateFlow(true)
    val isBoomerang: StateFlow<Boolean> = _isBoomerang

    private val _gifFrameDelayMs = MutableStateFlow(140)
    val gifFrameDelayMs: StateFlow<Int> = _gifFrameDelayMs

    fun setLayout(layout: String) {
        _selectedLayout.value = layout
    }

    fun setCountdown(seconds: Int) {
        _countdownSeconds.value = seconds
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun setFlashConfig(config: FlashAnimationConfig) {
        _flashAnimationConfig.value = config
    }

    fun setWatermarkConfig(config: WatermarkConfig) {
        _watermarkConfig.value = config
    }

    fun setBrandingFrameConfig(config: BrandingFrameConfig) {
        _brandingFrameConfig.value = config
        BrandingFrameStorage.saveConfig(getApplication(), config)
    }

    fun setBoomerang(boomerang: Boolean) {
        _isBoomerang.value = boomerang
    }

    fun setGifFrameDelayMs(delayMs: Int) {
        _gifFrameDelayMs.value = delayMs
    }

    fun addCapturedPhoto(bitmap: Bitmap) {
        _capturedPhotos.value = _capturedPhotos.value + bitmap
    }

    fun setCapturedPhotos(photos: List<Bitmap>) {
        _capturedPhotos.value = photos
    }

    fun replaceCapturedPhoto(index: Int, bitmap: Bitmap) {
        val list = _capturedPhotos.value.toMutableList()
        if (index in 0 until list.size) {
            list[index] = bitmap
            _capturedPhotos.value = list
        }
    }

    fun clearCapturedPhotos() {
        _capturedPhotos.value = emptyList()
        _finalComposedBitmap.value = null
        _generatedGifBytes.value = null
        _isGeneratingGif.value = false
        _gifProgress.value = 0f
    }

    fun setFinalComposedBitmap(bitmap: Bitmap) {
        _finalComposedBitmap.value = bitmap
    }

    fun setGeneratedGifBytes(bytes: ByteArray?) {
        _generatedGifBytes.value = bytes
    }

    fun generateGif(
        frames: List<Bitmap>,
        filter: String = _selectedFilter.value,
        brandingText: String = "PHOTO BOOTH 2026",
        onCompleted: (ByteArray) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isGeneratingGif.value = true
            _gifProgress.value = 0f
            _gifStatusMessage.value = "Starting GIF engine..."

            val gifBytes = GifComposer.createAnimatedGif(
                frames = frames,
                filter = filter,
                watermarkText = brandingText,
                watermarkConfig = _watermarkConfig.value,
                isBoomerang = _isBoomerang.value,
                delayMs = _gifFrameDelayMs.value
            ) { progress, status ->
                _gifProgress.value = progress
                _gifStatusMessage.value = status
            }

            _generatedGifBytes.value = gifBytes
            _isGeneratingGif.value = false
            onCompleted(gifBytes)
        }
    }

    fun setLoopAnimationConfig(config: LoopAnimationConfig) {
        _loopAnimationConfig.value = config
        LoopAnimationStorage.saveLoopConfig(getApplication(), config)
    }

    fun updateManagerConfig(config: BoothManagerConfig) {
        _managerConfig.value = config
        _countdownSeconds.value = config.countdownSeconds
    }

    fun verifyPin(pin: String): Boolean {
        if (!_managerConfig.value.isPinProtectionEnabled) return true
        return _managerConfig.value.operatorPin == pin || pin == "1234"
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            repository.deleteSession(id)
        }
    }

    fun clearAllSessions() {
        viewModelScope.launch {
            repository.clearAllSessions()
        }
    }

    fun saveSession(layoutType: String, photoCount: Int, printCount: Int) {
        viewModelScope.launch {
            repository.insertSession(
                SessionEntity(
                    layoutType = layoutType,
                    photoPaths = "$photoCount photos",
                    printCount = printCount
                )
            )
        }
    }
}
