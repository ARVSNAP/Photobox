package com.example.photobooth.hardware.gphoto2

import android.graphics.Bitmap

/**
 * Camera Brand classifications supported by libgphoto2.
 */
enum class GPhoto2Brand(val displayName: String, val vendorId: Int) {
    CANON("Canon EOS / PowerShot", PtpConstants.VENDOR_CANON),
    NIKON("Nikon DSLR / Z-Mirrorless", PtpConstants.VENDOR_NIKON),
    SONY("Sony Alpha / Cyber-shot", PtpConstants.VENDOR_SONY),
    FUJIFILM("Fujifilm X-Series / GFX", PtpConstants.VENDOR_FUJIFILM),
    PANASONIC("Panasonic Lumix", PtpConstants.VENDOR_PANASONIC),
    OLYMPUS("Olympus / OM System", PtpConstants.VENDOR_OLYMPUS),
    PENTAX("Pentax / Ricoh", PtpConstants.VENDOR_PENTAX),
    GENERIC_PTP("Standard PTP / MTP Camera", 0),
    UVC_WEBCAM("USB UVC HD Webcam", 0);

    companion object {
        fun fromVendorId(vendorId: Int): GPhoto2Brand {
            return values().firstOrNull { it.vendorId == vendorId } ?: GENERIC_PTP
        }
    }
}

enum class GPhoto2ConnectionType {
    USB_OTG,
    PTP_IP,
    UVC_DIRECT
}

/**
 * Detailed Information of a libgphoto2 compatible Camera Device.
 */
data class GPhoto2CameraDevice(
    val id: String,
    val brand: GPhoto2Brand,
    val modelName: String,
    val manufacturer: String = brand.displayName,
    val vendorId: Int = brand.vendorId,
    val productId: Int = 0,
    val connectionType: GPhoto2ConnectionType = GPhoto2ConnectionType.USB_OTG,
    val ipAddress: String? = null,
    val isConnected: Boolean = true,
    val batteryLevelPercent: Int = 92,
    val firmwareVersion: String = "1.2.0",
    val serialNumber: String = "LP-DSLR-8402",
    val shutterCount: Int = 1420,
    val currentIso: String = "400",
    val currentAperture: String = "f/2.8",
    val currentShutterSpeed: String = "1/125s",
    val currentExposureCompensation: String = "0.0 EV",
    val whiteBalance: String = "Auto (AWB)",
    val focusMode: String = "One-Shot AF",
    val isLiveViewSupported: Boolean = true,
    val isTetherCaptureSupported: Boolean = true,
    val statusMessage: String = "Siap Digunakan (libgphoto2 PTP Engine)"
)

/**
 * Result of capturing a photo via libgphoto2.
 */
data class GPhoto2CaptureResult(
    val bitmap: Bitmap,
    val filename: String,
    val sizeBytes: Long,
    val captureTimeMs: Long = System.currentTimeMillis(),
    val cameraModel: String,
    val iso: String,
    val aperture: String,
    val shutterSpeed: String,
    val isExternalDslr: Boolean = true
)

/**
 * Configurable properties in libgphoto2.
 */
data class GPhoto2PropertyOption(
    val name: String,
    val label: String,
    val currentValue: String,
    val availableOptions: List<String>
)
