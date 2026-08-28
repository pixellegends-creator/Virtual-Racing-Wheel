package com.vrw.app.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

/**
 * Sends steering/pedal control frames to the Windows companion over UDP.
 * Fire-and-forget by design (control input is latency-sensitive; retries would add lag).
 */
class ControlUdpClient(
    private val targetHost: String,
    private val targetPort: Int = 45100,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private var socket: DatagramSocket? = null
    private val address: InetAddress by lazy { InetAddress.getByName(targetHost) }

    data class ControlFrame(
        val steering: Float,
        val throttle: Float,
        val brake: Float,
        val clutch: Float,
        val cameraX: Float,
        val cameraY: Float,
        val sequenceNumber: Long
    )

    fun start() {
        if (socket == null) {
            socket = DatagramSocket()
        }
    }

    fun stop() {
        socket?.close()
        socket = null
    }

    fun send(frame: ControlFrame) {
        val sock = socket ?: return
        scope.launch {
            try {
                val buffer = encode(frame)
                val packet = DatagramPacket(buffer, buffer.size, address, targetPort)
                sock.send(packet)
            } catch (_: Exception) {
                // Best-effort UDP send; a dropped frame is fine, the next frame supersedes it.
            }
        }
    }

    companion object {
        const val MAGIC: Int = 0x56524331 // "VRC1"

        fun encode(frame: ControlFrame): ByteArray {
            val buffer = ByteBuffer.allocate(4 + 4 * 6 + 8)
            buffer.putInt(MAGIC)
            buffer.putFloat(frame.steering)
            buffer.putFloat(frame.throttle)
            buffer.putFloat(frame.brake)
            buffer.putFloat(frame.clutch)
            buffer.putFloat(frame.cameraX)
            buffer.putFloat(frame.cameraY)
            buffer.putLong(frame.sequenceNumber)
            return buffer.array()
        }

        fun decode(bytes: ByteArray): ControlFrame? {
            if (bytes.size < 4 + 4 * 6 + 8) return null
            val buffer = ByteBuffer.wrap(bytes)
            val magic = buffer.int
            if (magic != MAGIC) return null
            return ControlFrame(
                steering = buffer.float,
                throttle = buffer.float,
                brake = buffer.float,
                clutch = buffer.float,
                cameraX = buffer.float,
                cameraY = buffer.float,
                sequenceNumber = buffer.long
            )
        }
    }
}
