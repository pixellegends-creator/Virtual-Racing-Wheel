package com.vrw.app.pairing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.UUID

/**
 * Handles LAN broadcast auto-discovery of the Windows companion, followed by a PIN confirmation
 * step so pairing requires a human confirming a PIN shown on the PC console/UI - not just being
 * on the same network. Device identity persists across restarts via a stable per-install ID.
 */
class PairingClient(
    private val deviceIdProvider: DeviceIdProvider,
    private val broadcastPort: Int = 45099,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    interface DeviceIdProvider {
        fun getOrCreateDeviceId(): String
        fun getSavedPin(): String?
        fun savePin(pin: String)
    }

    data class DiscoveredHost(val hostname: String, val ipAddress: String, val port: Int)

    sealed class PairingResult {
        data class AlreadyTrusted(val host: DiscoveredHost) : PairingResult()
        data class NeedsPinConfirmation(val host: DiscoveredHost, val pin: String) : PairingResult()
        object NoHostFound : PairingResult()
        data class Error(val message: String) : PairingResult()
    }

    fun discover(timeoutMs: Int = 3000, onResult: (PairingResult) -> Unit) {
        scope.launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(broadcastPort)
                socket.broadcast = true
                socket.soTimeout = timeoutMs

                val buffer = ByteArray(512)
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)

                val message = String(packet.data, 0, packet.length)
                val parts = message.split("|")
                if (parts.size < 3 || parts[0] != "VRW_COMPANION") {
                    onResult(PairingResult.Error("Malformed discovery response"))
                    return@launch
                }

                val host = DiscoveredHost(
                    hostname = parts[1],
                    ipAddress = packet.address.hostAddress ?: "",
                    port = parts[2].toIntOrNull() ?: 45100
                )

                val deviceId = deviceIdProvider.getOrCreateDeviceId()
                val savedPin = deviceIdProvider.getSavedPin()

                if (savedPin != null) {
                    // Previously trusted - companion re-validates deviceId server-side.
                    onResult(PairingResult.AlreadyTrusted(host))
                } else {
                    val pin = requestPinFromCompanion(host, deviceId)
                    if (pin != null) {
                        onResult(PairingResult.NeedsPinConfirmation(host, pin))
                    } else {
                        onResult(PairingResult.Error("Companion did not issue a PIN"))
                    }
                }
            } catch (e: Exception) {
                onResult(PairingResult.Error(e.message ?: "Discovery failed"))
            } finally {
                socket?.close()
            }
        }
    }

    private fun requestPinFromCompanion(host: DiscoveredHost, deviceId: String): String? {
        // Simplified request/response over the same UDP channel; the companion generates
        // and displays a PIN on its console/UI, and echoes it back so the phone can show
        // "confirm this matches" rather than blindly trusting the network.
        return try {
            DatagramSocket().use { socket ->
                val requestMsg = "PAIR_REQUEST|$deviceId"
                val requestBytes = requestMsg.toByteArray()
                val address = InetAddress.getByName(host.ipAddress)
                socket.send(DatagramPacket(requestBytes, requestBytes.size, address, host.port))

                val responseBuffer = ByteArray(64)
                val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
                socket.soTimeout = 2000
                socket.receive(responsePacket)

                val response = String(responsePacket.data, 0, responsePacket.length)
                response.removePrefix("PAIR_PIN|")
            }
        } catch (_: Exception) {
            null
        }
    }

    fun confirmPin(pin: String) {
        deviceIdProvider.savePin(pin)
    }

    class DefaultDeviceIdProvider(
        private val prefs: android.content.SharedPreferences
    ) : DeviceIdProvider {
        override fun getOrCreateDeviceId(): String {
            val existing = prefs.getString(KEY_DEVICE_ID, null)
            if (existing != null) return existing
            val newId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
            return newId
        }

        override fun getSavedPin(): String? = prefs.getString(KEY_PIN, null)

        override fun savePin(pin: String) {
            prefs.edit().putString(KEY_PIN, pin).apply()
        }

        companion object {
            private const val KEY_DEVICE_ID = "vrw_device_id"
            private const val KEY_PIN = "vrw_pin"
        }
    }
}
