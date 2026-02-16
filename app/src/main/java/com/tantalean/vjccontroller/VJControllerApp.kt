package com.tantalean.vjccontroller

import android.app.Application
import android.util.Log
import com.startapp.sdk.adsbase.StartAppSDK

class VJControllerApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Inicializar StartApp SDK (start.io) con tu App ID — opcional si el SDK ya auto-inicializa
        try {
            val startAppId = getString(com.tantalean.vjccontroller.R.string.startapp_app_id)
            StartAppSDK.init(this, startAppId, false)
        } catch (t: Throwable) {
            Log.w("VJControllerApp", "StartApp init failed", t)
        }
    }
}
