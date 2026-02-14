package com.tantalean.vjccontroller.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tantalean.vjccontroller.model.DefaultCommands
import com.tantalean.vjccontroller.model.OscCommand
import com.tantalean.vjccontroller.repository.OscRepository
import com.tantalean.vjccontroller.state.ConfigState
import com.tantalean.vjccontroller.state.ControlState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = OscRepository(app.applicationContext)

    private val _configState = MutableStateFlow(ConfigState())
    val configState: StateFlow<ConfigState> = _configState.asStateFlow()

    private val _controlState = MutableStateFlow(ControlState())
    val controlState: StateFlow<ControlState> = _controlState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repo.ipFlow, repo.portFlow) { ip, port -> Pair(ip, port) }
                .collect { (ip, port) ->
                    _configState.update { it.copy(ip = ip, port = port) }
                }
        }
    }

    // ── Configuración ──────────────────────────────────────

    fun saveConfig(ip: String, port: String) {
        viewModelScope.launch {
            repo.saveConfig(ip, port)
            _configState.update { it.copy(ip = ip, port = port) }

            val portInt = port.toIntOrNull()
            if (portInt != null && ip.isNotBlank()) {
                try {
                    val result = repo.connect(ip, portInt)
                    _configState.update {
                        it.copy(
                            isConnected = result.isSuccess,
                            statusMessage = if (result.isSuccess) "Conectado a $ip:$port"
                            else "Error: ${result.exceptionOrNull()?.message}"
                        )
                    }
                } catch (e: Exception) {
                    _configState.update { it.copy(isConnected = false, statusMessage = "Error: ${e.message}") }
                }
            }
        }
    }

    /**
     * Envío diagnóstico: mensaje simple para comprobar recepción en Resolume.
     */
    fun sendDiagnostic() {
        val ip = _configState.value.ip
        val port = _configState.value.port.toIntOrNull()

        if (ip.isBlank() || port == null) {
            _configState.update { it.copy(statusMessage = "Error: IP/puerto inválidos") }
            return
        }

        viewModelScope.launch {
            _configState.update { it.copy(statusMessage = "Enviando prueba OSC...") }
            try {
                if (!repo.isConnected) repo.connect(ip, port)
                val result = repo.sendRaw("/composition/master", emptyList())
                _configState.update {
                    it.copy(statusMessage = if (result.isSuccess) "Prueba enviada ✓" else "Error enviando prueba: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _configState.update { it.copy(statusMessage = "Exception: ${e.message}") }
            }
        }
    }

    // ── Layers ─────────────────────────────────────────────

    fun selectLayer(layer: Int) {
        _controlState.update { it.copy(selectedLayer = layer) }
    }

    fun addLayer() {
        _controlState.update { it.copy(layerCount = it.layerCount + 1) }
    }

    fun removeLayer() {
        _controlState.update { state ->
            val newCount = (state.layerCount - 1).coerceAtLeast(1)
            val newSelected = state.selectedLayer.coerceAtMost(newCount)
            state.copy(layerCount = newCount, selectedLayer = newSelected)
        }
    }

    /** Devuelve los clips del layer seleccionado */
    fun getClipsForSelectedLayer(): List<OscCommand> {
        return DefaultCommands.clipsForLayer(_controlState.value.selectedLayer)
    }

    /** Devuelve los clips de cualquier layer (uso en UI) */
    fun getClipsForLayer(layer: Int): List<OscCommand> = DefaultCommands.clipsForLayer(layer)

    // ── Enviar comando ─────────────────────────────────────

    fun sendCommand(command: OscCommand) {
        val ip = _configState.value.ip
        val port = _configState.value.port.toIntOrNull() ?: return

        // Forzar que la dirección use la capa seleccionada (evita que siempre mande layer 1)
        val selectedLayer = _controlState.value.selectedLayer
        val correctedAddress = command.address.replace("/composition/layers/\\d+/".toRegex(), "/composition/layers/$selectedLayer/")
        val corrected = command.copy(address = correctedAddress)

        // Mostrar estado de envío (no mostrar notificación para envíos exitosos de clips)
        _controlState.update { it.copy(isSending = true, error = null) }

        viewModelScope.launch {
            try {
                if (!repo.isConnected) {
                    repo.connect(ip, port)
                }

                val result = repo.sendRaw(corrected.address, corrected.args)

                // Actualizar log de salidas
                _controlState.update { st ->
                    val newList = (listOf(corrected.address) + st.sentMessages).take(20)
                    st.copy(
                        isSending = false,
                        sentMessages = newList,
                        // No mostrar notificación en UI para envíos exitosos de clips; sólo registrar errores
                        lastMessage = if (result.isSuccess) st.lastMessage
                        else "✗ Error al enviar: ${result.exceptionOrNull()?.message ?: "desconocido"}",
                        error = result.exceptionOrNull()?.message
                    )
                }
            } catch (e: Exception) {
                _controlState.update { it.copy(isSending = false, lastMessage = "✗ Exception", error = e.message) }
            }
        }
    }

    /**
     * Envío de dirección explícito para asignaciones (long-press)
     * Cuando se trata de clips, enviamos la dirección a todas las capas (1..3)
     * para que Resolume pueda detectar y asignar correctamente el atajo.
     */
    fun sendCommandForAssignment(command: OscCommand) {
        val ip = _configState.value.ip
        val port = _configState.value.port.toIntOrNull() ?: return

        _controlState.update { it.copy(lastMessage = "Asignando: ${command.address}") }

        viewModelScope.launch {
            try {
                if (!repo.isConnected) repo.connect(ip, port)

                // Detectar si el address pertenece a un clip de un layer
                val regex = "/composition/layers/(\\d+)/clips/(\\d+)(/.*)?".toRegex()
                if (regex.matches(command.address)) {
                    // Siempre enviar la dirección reemplazada hacia la capa actualmente seleccionada.
                    val selectedLayer = _controlState.value.selectedLayer
                    val replaced = command.address.replace("/composition/layers/\\d+/".toRegex(), "/composition/layers/$selectedLayer/")
                    val r = repo.sendRaw(replaced, command.args)
                    _controlState.update { it.copy(lastMessage = if (r.isSuccess) "Asignación enviada: $replaced" else "Error asignación: ${r.exceptionOrNull()?.message}") }
                    return@launch
                }

                // Otros tipos de direcciones: enviar normal
                val result = repo.sendRaw(command.address, command.args)
                _controlState.update {
                    it.copy(lastMessage = if (result.isSuccess) "Asignación enviada: ${command.address}" else "Error asignación: ${result.exceptionOrNull()?.message}")
                }

            } catch (e: Exception) {
                _controlState.update { it.copy(lastMessage = "Exception asignación: ${e.message}") }
            }
        }
    }


    // ── Sliders ────────────────────────────────────────────

    fun updateOpacity(value: Float) {
        _controlState.update { it.copy(opacity = value) }
    }

    fun sendOpacity() {
        val layer = _controlState.value.selectedLayer
        val value = _controlState.value.opacity
        // Enviar a la dirección principal y a la alternativa 'mixer/opacity' por compatibilidad
        // NO actualizar `lastMessage` para evitar mensajes spam al mover el slider
        sendOsc("/composition/layers/$layer/video/opacity", listOf(value), showStatus = false)
        sendOsc("/composition/layers/$layer/video/mixer/opacity", listOf(value), showStatus = false)
    }

    /** Trigger all clips of a layer when that Layer button is clicked */
    suspend fun sendClipWithDelay(cmd: OscCommand, delayMs: Long = 40L) {
        sendCommand(cmd)
        kotlinx.coroutines.delay(delayMs)
    }

    fun triggerAllClipsInLayer(layer: Int) {
        val clips = DefaultCommands.clipsForLayer(layer)
        viewModelScope.launch {
            try {
                // asegurar conexión antes de enviar
                val ip = _configState.value.ip
                val port = _configState.value.port.toIntOrNull() ?: return@launch
                if (!repo.isConnected) repo.connect(ip, port)

                val sent = mutableListOf<String>()
                for (cmd in clips) {
                    val correctedAddr = cmd.address.replace("/composition/layers/\\d+/".toRegex(), "/composition/layers/$layer/")
                    val res = repo.sendRaw(correctedAddr, cmd.args)
                    if (res.isSuccess) sent.add(correctedAddr)
                    // pequeño retardo para no saturar el receptor
                    kotlinx.coroutines.delay(60L)
                }
                _controlState.update { it.copy(lastMessage = "Sent ${sent.size} OSC to layer $layer", sentMessages = (sent + it.sentMessages).take(20)) }
            } catch (e: Exception) {
                _controlState.update { it.copy(lastMessage = "Error triggering layer $layer: ${e.message}") }
            }
        }
    }

    private fun sendOsc(address: String, args: List<Any>, showStatus: Boolean = true) {
        val ip = _configState.value.ip
        val portString = _configState.value.port
        val port = portString.toIntOrNull()

        if (ip.isBlank()) {
            if (showStatus) _controlState.update { it.copy(lastMessage = "✗ No IP configurada", error = "IP vacía") }
            return
        }
        if (port == null) {
            if (showStatus) _controlState.update { it.copy(lastMessage = "✗ Puerto inválido", error = "Puerto: '$portString' no es un número") }
            return
        }

        viewModelScope.launch {
            try {
                if (!repo.isConnected) {
                    repo.connect(ip, port)
                }
                val result = repo.sendRaw(address, args)
                if (showStatus) {
                    _controlState.update {
                        it.copy(
                            lastMessage = if (result.isSuccess) "✓ $address"
                            else "✗ Error",
                            error = result.exceptionOrNull()?.message
                        )
                    }
                } else {
                    // solo registrar error si falla
                    if (!result.isSuccess) {
                        _controlState.update { it.copy(error = result.exceptionOrNull()?.message) }
                    }
                }
            } catch (e: Exception) {
                if (showStatus) _controlState.update { it.copy(lastMessage = "✗ Exception", error = e.message) }
                else _controlState.update { it.copy(error = e.message) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repo.disconnect()
    }
}