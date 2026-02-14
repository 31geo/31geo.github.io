package com.tantalean.vjccontroller.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Cliente OSC mínimo implementado sobre UDP para Android.
 * - Soporta argumentos Int y Float y String.
 * - Evita dependencias que requieren AWT (soluciona "failed resolution of: Ljava/awt/Color;").
 */
class OscClient {

    private var socket: DatagramSocket? = null
    private var address: InetAddress? = null
    private var port: Int? = null

    suspend fun connect(ip: String, port: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (socket != null && this@OscClient.address?.hostAddress == ip && this@OscClient.port == port) {
                return@withContext Result.success(Unit)
            }
            disconnect()
            address = InetAddress.getByName(ip)
            socket = DatagramSocket()
            this@OscClient.port = port
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("OscClient", "connect error", e)
            Result.failure(e)
        }
    }

    fun disconnect() {
        try {
            socket?.close()
        } catch (_: Exception) { }
        socket = null
        address = null
        port = null
    }

    suspend fun send(addressPath: String, args: List<Any> = emptyList()): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val addr = address ?: return@withContext Result.failure(IllegalStateException("OSC Client no conectado"))
                val p = port ?: return@withContext Result.failure(IllegalStateException("OSC Client no conectado"))

                val payload = buildOscMessage(addressPath, args)
                val packet = DatagramPacket(payload, payload.size, addr, p)
                socket?.send(packet)
                Result.success(Unit)
            } catch (t: Throwable) {
                Log.e("OscClient", "Error enviando OSC to $addressPath args=$args", t)
                Result.failure(t)
            }
        }

    val isConnected: Boolean
        get() = socket != null && address != null && port != null

    // ---- Helpers: serialización OSC (simple) ----
    private fun padNull(src: ByteArray): ByteArray {
        val pad = (4 - (src.size % 4)) % 4
        return if (pad == 0) src else src + ByteArray(pad)
    }

    private fun buildOscMessage(address: String, args: List<Any>): ByteArray {
        val addrBytes = padNull((address + "\u0000").toByteArray(Charsets.UTF_8))

        val typeTags = StringBuilder()
        typeTags.append(',')
        val argBytesList = ArrayList<ByteArray>()

        for (arg in args) {
            when (arg) {
                is Int -> {
                    typeTags.append('i')
                    argBytesList.add(intToBytes(arg))
                }
                is Float -> {
                    typeTags.append('f')
                    argBytesList.add(floatToBytes(arg))
                }
                is Double -> {
                    typeTags.append('f')
                    argBytesList.add(floatToBytes(arg.toFloat()))
                }
                is String -> {
                    typeTags.append('s')
                    argBytesList.add(padNull((arg + "\u0000").toByteArray(Charsets.UTF_8)))
                }
                else -> {
                    if (arg is Number) {
                        val f = arg.toFloat()
                        typeTags.append('f')
                        argBytesList.add(floatToBytes(f))
                    } else {
                        Log.w("OscClient", "Tipo de argumento OSC no soportado: ${arg::class}")
                    }
                }
            }
        }

        val typeTagBytes = padNull((typeTags.toString() + "\u0000").toByteArray(Charsets.UTF_8))

        val totalLength = addrBytes.size + typeTagBytes.size + argBytesList.sumOf { it.size }
        val buffer = ByteArray(totalLength)
        var pos = 0
        System.arraycopy(addrBytes, 0, buffer, pos, addrBytes.size); pos += addrBytes.size
        System.arraycopy(typeTagBytes, 0, buffer, pos, typeTagBytes.size); pos += typeTagBytes.size
        for (b in argBytesList) {
            System.arraycopy(b, 0, buffer, pos, b.size); pos += b.size
        }
        return buffer
    }

    private fun intToBytes(v: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(v).array()
    }

    private fun floatToBytes(v: Float): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putFloat(v).array()
    }
}