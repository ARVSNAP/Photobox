package com.example.photobooth.hardware.gphoto2

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * PTP/IP Wireless Tethering Network Transport,
 * implementing the PTP/IP protocol on port 15740 as in libgphoto2 (camlibs/ptp2/ptpip.c).
 */
class PtpIpTransport(
    val host: String,
    val port: Int = PtpConstants.DEFAULT_PTPIP_PORT
) {
    companion object {
        private const val TAG = "PtpIpTransport"
        private const val CONNECT_TIMEOUT_MS = 3000

        // PTP/IP Packet Types
        const val PTPIP_INIT_COMMAND_REQUEST = 1
        const val PTPIP_INIT_COMMAND_ACK = 2
        const val PTPIP_INIT_EVENT_REQUEST = 3
        const val PTPIP_INIT_EVENT_ACK = 4
        const val PTPIP_INIT_FAIL = 5
        const val PTPIP_CMD_REQUEST = 6
        const val PTPIP_CMD_RESPONSE = 7
        const val PTPIP_EVENT = 8
        const val PTPIP_START_DATA_PACKET = 9
        const val PTPIP_DATA_PACKET = 10
        const val PTPIP_CANCEL_TRANSACTION = 11
        const val PTPIP_END_DATA_PACKET = 12
        const val PTPIP_PING = 13
        const val PTPIP_PONG = 14
    }

    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var connectionNumber: Int = 0
    private var transactionId: Int = 1

    val isConnected: Boolean
        get() = socket?.isConnected == true && socket?.isClosed == false

    suspend fun connect(clientFriendlyName: String = "PhotoBooth Kiosk"): Boolean = withContext(Dispatchers.IO) {
        try {
            val sock = Socket()
            sock.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
            sock.soTimeout = 4000

            val out = sock.getOutputStream()
            val inp = sock.getInputStream()

            socket = sock
            outputStream = out
            inputStream = inp

            // Send Init Command Request
            // Packet: Length (4), Type (4), GUID (16), FriendlyName (null-terminated UTF-16LE), Version (4)
            val nameBytes = (clientFriendlyName + "\u0000").toByteArray(Charsets.UTF_16LE)
            val length = 4 + 4 + 16 + nameBytes.size + 4
            val buffer = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN)

            buffer.putInt(length)
            buffer.putInt(PTPIP_INIT_COMMAND_REQUEST)
            // 16-byte dummy client GUID
            val guid = byteArrayOf(
                0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
                0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10
            )
            buffer.put(guid)
            buffer.put(nameBytes)
            buffer.putInt(0x00010000) // Protocol version 1.0

            out.write(buffer.array())
            out.flush()

            // Read Init Command Ack
            val header = ByteArray(8)
            var bytesRead = inp.read(header)
            if (bytesRead >= 8) {
                val headerBuf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
                val respLen = headerBuf.int
                val respType = headerBuf.int

                if (respType == PTPIP_INIT_COMMAND_ACK && respLen >= 12) {
                    val body = ByteArray(respLen - 8)
                    inp.read(body)
                    val bodyBuf = ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN)
                    connectionNumber = bodyBuf.int
                    Log.i(TAG, "PTP/IP Connected to $host:$port, ConnectionNumber=$connectionNumber")
                    return@withContext true
                }
            }

            Log.i(TAG, "PTP/IP connection established to $host:$port")
            true
        } catch (e: Exception) {
            Log.w(TAG, "PTP/IP connection to $host:$port failed: ${e.message}")
            disconnect()
            false
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            outputStream?.close()
            inputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing PTP/IP socket: ${e.message}")
        } finally {
            socket = null
            outputStream = null
            inputStream = null
        }
    }

    suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext false
        try {
            val buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
            buffer.putInt(8)
            buffer.putInt(PTPIP_PING)
            outputStream?.write(buffer.array())
            outputStream?.flush()
            true
        } catch (e: Exception) {
            false
        }
    }
}
