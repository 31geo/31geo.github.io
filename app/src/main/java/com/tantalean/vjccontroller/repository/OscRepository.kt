package com.tantalean.vjccontroller.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tantalean.vjccontroller.model.OscCommand
import com.tantalean.vjccontroller.network.OscClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "osc_settings")

/**
 * Repositorio que gestiona la comunicación OSC y la persistencia de configuración.
 */
class OscRepository(private val context: Context) {

    private val client = OscClient()

    companion object {
        private val KEY_IP = stringPreferencesKey("osc_ip")
        private val KEY_PORT = stringPreferencesKey("osc_port")
    }

    // ── DataStore ────────────────────────────────────────────

    val ipFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_IP] ?: "192.168.1.100"
    }

    val portFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_PORT] ?: "7000"
    }

    suspend fun saveConfig(ip: String, port: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IP] = ip
            prefs[KEY_PORT] = port
        }
    }

    // ── OSC ─────────────────────────────────────────────────

    suspend fun connect(ip: String, port: Int): Result<Unit> =
        client.connect(ip, port)

    fun disconnect() = client.disconnect()

    val isConnected: Boolean
        get() = client.isConnected

    suspend fun sendCommand(command: OscCommand): Result<Unit> =
        client.send(command.address, command.args)

    suspend fun sendRaw(address: String, args: List<Any> = emptyList()): Result<Unit> =
        client.send(address, args)
}