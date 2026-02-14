package com.tantalean.vjccontroller

import android.app.Application
import android.util.Log
class VJControllerApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Inicializar StartApp SDK (start.io) con tu App ID — opcional si el SDK ya auto-inicializa
        try {
            StartAppSDK.init(this, "201633923", false)
        } catch (t: Throwable) {
            Log.w("VJControllerApp", "StartApp init failed", t)
        }
    }
}
