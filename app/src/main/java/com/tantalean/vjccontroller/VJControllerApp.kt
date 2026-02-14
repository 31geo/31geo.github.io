package com.tantalean.vjccontroller

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.startapp.sdk.adsbase.StartAppSDK

class VJControllerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializa MobileAds solo una vez para toda la aplicación
        MobileAds.initialize(this)

        // Inicializar StartApp SDK (start.io) con tu App ID — opcional si el SDK ya auto-inicializa
        try {
            StartAppSDK.init(this, "201633923", false)
        } catch (t: Throwable) {
            Log.w("VJControllerApp", "StartApp init failed", t)
        }
    }
}
