package com.vrw.app.telemetry

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.ByteBuffer

/**
 * Telemetry schema shared conceptually with the Windows relay. Received on port 45128 (the
 * Windows companion relays game telemetry in on 45127 and fans it back out to devices on 45128).
 */
data class TelemetryFrame(
    val speedKmh: Float,
    val rpm: Float,
    val maxRpm: Float,
    val gear: Int,
    val fuelLiters: Float,
    val lapTimeSeconds: Float,
    val tireTempsCelsius: FloatArray // [FL, FR, RL, RR]
)

class TelemetryReceiver(
    private val listenPort: Int = 45128,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private var socket: DatagramSocket? = null
    private var running = false

    fun start(onFrame: (TelemetryFrame) -> Unit) {
        if (running) return
        running = true
        scope.launch {
            try {
                val sock = DatagramSocket(listenPort)
                socket = sock
                val buffer = ByteArray(256)
                while (running) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    sock.receive(packet)
                    decode(packet.data, packet.length)?.let(onFrame)
                }
            } catch (_: Exception) {
                // Socket closed / receiver stopped - normal on stop().
            }
        }
    }

    fun stop() {
        running = false
        socket?.close()
        socket = null
    }

    companion object {
        fun decode(bytes: ByteArray, length: Int): TelemetryFrame? {
            if (length < 4 * 6 + 4 * 4) return null
            return try {
                val buffer = ByteBuffer.wrap(bytes, 0, length)
                TelemetryFrame(
                    speedKmh = buffer.float,
                    rpm = buffer.float,
                    maxRpm = buffer.float,
                    gear = buffer.int,
                    fuelLiters = buffer.float,
                    lapTimeSeconds = buffer.float,
                    tireTempsCelsius = floatArrayOf(buffer.float, buffer.float, buffer.float, buffer.float)
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
