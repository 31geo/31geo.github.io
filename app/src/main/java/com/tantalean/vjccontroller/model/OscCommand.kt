package com.tantalean.vjccontroller.model

/**
 * Representa un comando OSC para Resolume Arena.
 */
data class OscCommand(
    val label: String,
    val address: String,
    val args: List<Any> = emptyList()
)