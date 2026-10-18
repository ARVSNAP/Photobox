package com.example.photobooth.hardware.gphoto2

/**
 * PTP (Picture Transfer Protocol) and MTP standard constants,
 * aligned with libgphoto2 (camlibs/ptp2/ptp.h).
 */
object PtpConstants {
    // PTP Container Types
    const val PTP_USB_CONTAINER_UNDEFINED = 0x0000
    const val PTP_USB_CONTAINER_COMMAND = 0x0001
    const val PTP_USB_CONTAINER_DATA = 0x0002
    const val PTP_USB_CONTAINER_RESPONSE = 0x0003
    const val PTP_USB_CONTAINER_EVENT = 0x0004

    // Standard PTP Operation Codes (libgphoto2 standard)
    const val OC_GetDeviceInfo = 0x1001
    const val OC_OpenSession = 0x1002
    const val OC_CloseSession = 0x1003
    const val OC_GetStorageIDs = 0x1004
    const val OC_GetStorageInfo = 0x1005
    const val OC_GetNumObjects = 0x1006
    const val OC_GetObjectHandles = 0x1007
    const val OC_GetObjectInfo = 0x1008
    const val OC_GetObject = 0x1009
    const val OC_GetThumb = 0x100A
    const val OC_DeleteObject = 0x100B
    const val OC_SendObjectInfo = 0x100C
    const val OC_SendObject = 0x100D
    const val OC_InitiateCapture = 0x100E
    const val OC_FormatStore = 0x100F
    const val OC_ResetDevice = 0x1010
    const val OC_SelfTest = 0x1011
    const val OC_SetObjectProtection = 0x1012
    const val OC_PowerDown = 0x1013
    const val OC_GetDevicePropDesc = 0x1014
    const val OC_GetDevicePropValue = 0x1015
    const val OC_SetDevicePropValue = 0x1016
    const val OC_ResetDevicePropValue = 0x1017
    const val OC_TerminateOpenCapture = 0x1018
    const val OC_MoveObject = 0x1019
    const val OC_CopyObject = 0x101A
    const val OC_GetPartialObject = 0x101B
    const val OC_InitiateOpenCapture = 0x101C

    // Canon EOS Extension Opcodes (libgphoto2 camlibs/ptp2)
    const val OC_CANON_EOS_GetStorageIDs = 0x9101
    const val OC_CANON_EOS_GetStorageInfo = 0x9102
    const val OC_CANON_EOS_GetObjectInfo = 0x9103
    const val OC_CANON_EOS_GetObject = 0x9104
    const val OC_CANON_EOS_DeleteObject = 0x9105
    const val OC_CANON_EOS_FormatStore = 0x9106
    const val OC_CANON_EOS_GetPartialObject = 0x9107
    const val OC_CANON_EOS_GetDeviceInfoEx = 0x9108
    const val OC_CANON_EOS_GetObjectInfoEx = 0x9109
    const val OC_CANON_EOS_GetThumbEx = 0x910A
    const val OC_CANON_EOS_SendPartialObject = 0x910B
    const val OC_CANON_EOS_SetObjectAttributes = 0x910C
    const val OC_CANON_EOS_GetObjectTime = 0x910D
    const val OC_CANON_EOS_SetObjectTime = 0x910E
    const val OC_CANON_EOS_RemoteRelease = 0x910F  // Shutter trigger (Capture Photo)
    const val OC_CANON_EOS_SetEventMode = 0x9110
    const val OC_CANON_EOS_GetEvent = 0x9113
    const val OC_CANON_EOS_GetEventEx = 0x9114
    const val OC_CANON_EOS_TransferComplete = 0x9115
    const val OC_CANON_EOS_CancelTransfer = 0x9116
    const val OC_CANON_EOS_ResetTransfer = 0x9117
    const val OC_CANON_EOS_PCHDDCapacity = 0x911A
    const val OC_CANON_EOS_SetUILock = 0x911B
    const val OC_CANON_EOS_ResetUILock = 0x911C
    const val OC_CANON_EOS_KeepDeviceOn = 0x911D
    const val OC_CANON_EOS_SetNullPacketMode = 0x911E
    const val OC_CANON_EOS_UpdateFirmware = 0x911F
    const val OC_CANON_EOS_GetLiveViewPicture = 0x9128 // Live view frame

    // Nikon Extension Opcodes (libgphoto2 camlibs/ptp2)
    const val OC_NIKON_Capture = 0x9201           // Shutter capture to SD
    const val OC_NIKON_StartLiveView = 0x9202      // Start LiveView
    const val OC_NIKON_EndLiveView = 0x9203        // End LiveView
    const val OC_NIKON_GetLiveViewImage = 0x9204   // Fetch LiveView JPEG frame
    const val OC_NIKON_AfDrive = 0x9205            // Auto focus trigger
    const val OC_NIKON_DeviceReady = 0x90C8

    // Sony Alpha Extension Opcodes (libgphoto2 camlibs/ptp2)
    const val OC_SONY_SDIOConnect = 0x9280
    const val OC_SONY_GetSDIOGetExtDeviceInfo = 0x9281
    const val OC_SONY_SonyAlphaLiveView = 0x9283
    const val OC_SONY_ActControl = 0x9285          // Shutter trigger
    const val OC_SONY_GetAllDevicePropData = 0x9288

    // Fuji Extension Opcodes
    const val OC_FUJI_InitiateCapture = 0x9028
    const val OC_FUJI_GetLiveViewImage = 0x902B

    // Standard PTP Response Codes
    const val RC_Undefined = 0x2000
    const val RC_OK = 0x2001
    const val RC_GeneralError = 0x2002
    const val RC_SessionNotOpen = 0x2003
    const val RC_InvalidTransactionID = 0x2004
    const val RC_OperationNotSupported = 0x2005
    const val RC_ParameterNotSupported = 0x2006
    const val RC_IncompleteTransfer = 0x2007
    const val RC_InvalidStorageId = 0x2008
    const val RC_InvalidObjectHandle = 0x2009
    const val RC_DevicePropNotSupported = 0x200A
    const val RC_InvalidObjectFormatCode = 0x200B
    const val RC_StoreFull = 0x200C
    const val RC_ObjectWriteProtected = 0x200D
    const val RC_StoreReadOnly = 0x200E
    const val RC_AccessDenied = 0x200F
    const val RC_NoThumbnailPresent = 0x2010
    const val RC_SelfTestFailed = 0x2011
    const val RC_PartialDeletion = 0x2012
    const val RC_StoreNotAvailable = 0x2013
    const val RC_SpecificationByFormatUnsupported = 0x2014
    const val RC_NoValidObjectInfo = 0x2015
    const val RC_InvalidCodeFormat = 0x2016
    const val RC_UnknownVendorCode = 0x2017
    const val RC_CaptureAlreadyTerminated = 0x2018
    const val RC_DeviceBusy = 0x2019
    const val RC_InvalidDeviceInfo = 0x201A
    const val RC_InvalidObjectReference = 0x201B
    const val RC_InvalidDataset = 0x201C
    const val RC_DeviceReset = 0x201D
    const val RC_DeviceNotReady = 0x201E
    const val RC_SessionAlreadyOpen = 0x201E

    // Standard Device Property Codes (libgphoto2)
    const val DPC_BatteryLevel = 0x5001
    const val DPC_FunctionalMode = 0x5002
    const val DPC_ImageSize = 0x5003
    const val DPC_CompressionSetting = 0x5004
    const val DPC_WhiteBalance = 0x5005
    const val DPC_FNumber = 0x5007         // Aperture
    const val DPC_FocalLength = 0x5008
    const val DPC_FocusDistance = 0x5009
    const val DPC_FocusMode = 0x500A
    const val DPC_ExposureMeteringMode = 0x500B
    const val DPC_FlashMode = 0x500C
    const val DPC_ExposureTime = 0x500D    // Shutter speed
    const val DPC_ExposureProgramMode = 0x500E
    const val DPC_ExposureIndex = 0x500F   // ISO
    const val DPC_ExposureBiasCompensation = 0x5010
    const val DPC_DateTime = 0x5011
    const val DPC_CaptureDelay = 0x5012

    // Canon EOS Properties
    const val DPC_CANON_EOS_Aperture = 0xD101
    const val DPC_CANON_EOS_ShutterSpeed = 0xD102
    const val DPC_CANON_EOS_Iso = 0xD103
    const val DPC_CANON_EOS_ExposureCompensation = 0xD104
    const val DPC_CANON_EOS_ShootingMode = 0xD105
    const val DPC_CANON_EOS_DriveMode = 0xD106
    const val DPC_CANON_EOS_WhiteBalance = 0xD109
    const val DPC_CANON_EOS_ColorTemperature = 0xD10A
    const val DPC_CANON_EOS_BatteryLevel = 0xD118

    // USB Vendor IDs
    const val VENDOR_CANON = 0x04A9
    const val VENDOR_NIKON = 0x04B0
    const val VENDOR_SONY = 0x054C
    const val VENDOR_FUJIFILM = 0x04CB
    const val VENDOR_PANASONIC = 0x04DA
    const val VENDOR_OLYMPUS = 0x07B4
    const val VENDOR_PENTAX = 0x0A17
    const val VENDOR_GOPRO = 0x2672
    const val VENDOR_SIGMA = 0x0F0D
    const val VENDOR_LEICA = 0x1404

    // Default PTP/IP Port
    const val DEFAULT_PTPIP_PORT = 15740
}
