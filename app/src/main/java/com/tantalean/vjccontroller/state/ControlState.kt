package com.tantalean.vjccontroller.state

/**
 * Estado de la pantalla de control Resolume.
 */
data class ControlState(
    val lastMessage: String = "",
    val isSending: Boolean = false,
    val selectedLayer: Int = 1,
    val layerCount: Int = 3,
    val opacity: Float = 1f,
    val sentMessages: List<String> = emptyList(),
    val error: String? = null
)