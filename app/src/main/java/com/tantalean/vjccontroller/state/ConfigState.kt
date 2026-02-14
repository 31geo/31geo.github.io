package com.tantalean.vjccontroller.state

/**
 * Estado de la configuración OSC.
 */
data class ConfigState(
    val ip: String = "192.168.1.100",
    val port: String = "7000",
    val isConnected: Boolean = false,
    val statusMessage: String = "Sin conexión"
)