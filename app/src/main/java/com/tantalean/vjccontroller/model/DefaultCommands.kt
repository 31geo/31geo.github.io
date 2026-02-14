package com.tantalean.vjccontroller.model


object DefaultCommands {

    /** Clips: genera 12 clips por layer (ahora 12 en total) */
    fun clipsForLayer(layer: Int): List<OscCommand> =
        (1..12).map { clip ->
            OscCommand(
                label = "CLIP $clip",
                address = "/composition/layers/$layer/clips/$clip/connect",
                args = listOf(1)
            )
        }

    /** Layer selection / solo */
    val layerCommands = listOf(
        OscCommand("Layer 1", "/composition/layers/1/solo", listOf(1)),
        OscCommand("Layer 2", "/composition/layers/2/solo", listOf(1)),
        OscCommand("Layer 3", "/composition/layers/3/solo", listOf(1))
    )

    /** Parámetros visuales: opacity (speed & transition removed) */
    val parameterCommands = listOf(
        OscCommand(
            label = "Opacity 100%",
            address = "/composition/layers/1/video/opacity",
            args = listOf(1.0f)
        ),
        OscCommand(
            label = "Opacity 50%",
            address = "/composition/layers/1/video/opacity",
            args = listOf(0.5f)
        )
    )
}