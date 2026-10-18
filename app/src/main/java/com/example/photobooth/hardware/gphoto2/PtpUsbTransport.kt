package com.example.photobooth.hardware.gphoto2

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Low-level USB Host PTP (Picture Transfer Protocol) transport implementation,
 * translating libgphoto2 ptp2 camlib operations into Android USB Host bulk transactions.
 */
class PtpUsbTransport(
    private val usbManager: UsbManager,
    val usbDevice: UsbDevice
) {
    companion object {
        private const val TAG = "PtpUsbTransport"
        private const val TIMEOUT_MS = 5000
    }

    private var connection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var endpointIn: UsbEndpoint? = null
    private var endpointOut: UsbEndpoint? = null
    private var endpointEvent: UsbEndpoint? = null

    private var transactionId = 1

    val isConnected: Boolean
        get() = connection != null && endpointIn != null && endpointOut != null

    fun open(): Boolean {
        try {
            // Find appropriate PTP or Camera interface
            // Class 6: USB_CLASS_STILL_IMAGE (PTP standard)
            // Or vendor-specific interface with Bulk IN and Bulk OUT endpoints
            var targetInterface: UsbInterface? = null
            var inEp: UsbEndpoint? = null
            var outEp: UsbEndpoint? = null
            var eventEp: UsbEndpoint? = null

            for (i in 0 until usbDevice.interfaceCount) {
                val iface = usbDevice.getInterface(i)
                var tempIn: UsbEndpoint? = null
                var tempOut: UsbEndpoint? = null
                var tempEvent: UsbEndpoint? = null

                for (j in 0 until iface.endpointCount) {
                    val ep = iface.getEndpoint(j)
                    if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        if (ep.direction == UsbConstants.USB_DIR_IN) {
                            tempIn = ep
                        } else if (ep.direction == UsbConstants.USB_DIR_OUT) {
                            tempOut = ep
                        }
                    } else if (ep.type == UsbConstants.USB_ENDPOINT_XFER_INT && ep.direction == UsbConstants.USB_DIR_IN) {
                        tempEvent = ep
                    }
                }

                if (tempIn != null && tempOut != null) {
                    targetInterface = iface
                    inEp = tempIn
                    outEp = tempOut
                    eventEp = tempEvent
                    if (iface.interfaceClass == UsbConstants.USB_CLASS_STILL_IMAGE) {
                        break // Preferred PTP interface
                    }
                }
            }

            if (targetInterface == null || inEp == null || outEp == null) {
                Log.w(TAG, "No suitable PTP bulk endpoints found on ${usbDevice.deviceName}")
                return false
            }

            val conn = usbManager.openDevice(usbDevice)
            if (conn == null) {
                Log.w(TAG, "Cannot open UsbDeviceConnection (permission required or busy)")
                return false
            }

            if (!conn.claimInterface(targetInterface, true)) {
                Log.w(TAG, "Failed to claim USB interface")
                conn.close()
                return false
            }

            this.connection = conn
            this.usbInterface = targetInterface
            this.endpointIn = inEp
            this.endpointOut = outEp
            this.endpointEvent = eventEp

            Log.i(TAG, "Successfully connected to PTP USB camera: ${usbDevice.productName}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error opening PTP USB connection: ${e.message}", e)
            close()
            return false
        }
    }

    fun close() {
        try {
            usbInterface?.let { connection?.releaseInterface(it) }
            connection?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing connection: ${e.message}")
        } finally {
            connection = null
            usbInterface = null
            endpointIn = null
            endpointOut = null
            endpointEvent = null
        }
    }

    /**
     * Sends a PTP Command block
     */
    fun sendCommand(opcode: Int, vararg params: Int): Int {
        val currentTxId = transactionId++
        val length = 12 + (params.size * 4)
        val buffer = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN)

        buffer.putInt(length)
        buffer.putShort(PtpConstants.PTP_USB_CONTAINER_COMMAND.toShort())
        buffer.putShort(opcode.toShort())
        buffer.putInt(currentTxId)
        for (p in params) {
            buffer.putInt(p)
        }

        val bytes = buffer.array()
        val transferred = connection?.bulkTransfer(endpointOut, bytes, bytes.size, TIMEOUT_MS) ?: -1
        if (transferred < bytes.size) {
            Log.w(TAG, "Failed to send full command 0x${Integer.toHexString(opcode)}")
        }
        return currentTxId
    }

    /**
     * Reads a PTP Response block
     */
    fun readResponse(): PtpResponse? {
        val buffer = ByteArray(1024)
        val read = connection?.bulkTransfer(endpointIn, buffer, buffer.size, TIMEOUT_MS) ?: -1
        if (read < 12) {
            return null
        }

        val byteBuffer = ByteBuffer.wrap(buffer, 0, read).order(ByteOrder.LITTLE_ENDIAN)
        val length = byteBuffer.int
        val type = byteBuffer.short.toInt()
        val code = byteBuffer.short.toInt() and 0xFFFF
        val txId = byteBuffer.int

        val paramCount = (length - 12) / 4
        val params = mutableListOf<Int>()
        for (i in 0 until paramCount.coerceAtLeast(0)) {
            if (byteBuffer.remaining() >= 4) {
                params.add(byteBuffer.int)
            }
        }

        return PtpResponse(type, code, txId, params)
    }

    /**
     * Reads raw bulk data from the PTP data phase
     */
    fun readData(): ByteArray? {
        val conn = connection ?: return null
        val epIn = endpointIn ?: return null

        val headerBuffer = ByteArray(12)
        val headerRead = conn.bulkTransfer(epIn, headerBuffer, headerBuffer.size, TIMEOUT_MS)
        if (headerRead < 12) {
            Log.w(TAG, "Failed to read PTP data container header")
            return null
        }

        val byteBuffer = ByteBuffer.wrap(headerBuffer).order(ByteOrder.LITTLE_ENDIAN)
        val length = byteBuffer.int
        val type = byteBuffer.short.toInt()
        val code = byteBuffer.short.toInt() and 0xFFFF
        val txId = byteBuffer.int

        if (length <= 12) {
            return ByteArray(0)
        }

        val dataSize = length - 12
        val dataBuffer = ByteArray(dataSize)
        var totalRead = 0
        val tempBuf = ByteArray(32768) // Use large buffer for high throughput

        while (totalRead < dataSize) {
            val toRead = (dataSize - totalRead).coerceAtMost(tempBuf.size)
            val read = conn.bulkTransfer(epIn, tempBuf, toRead, TIMEOUT_MS)
            if (read <= 0) {
                break
            }
            System.arraycopy(tempBuf, 0, dataBuffer, totalRead, read)
            totalRead += read
        }

        return dataBuffer
    }

    data class PtpResponse(
        val type: Int,
        val responseCode: Int,
        val transactionId: Int,
        val parameters: List<Int>
    ) {
        val isOk: Boolean get() = responseCode == PtpConstants.RC_OK
    }
}
