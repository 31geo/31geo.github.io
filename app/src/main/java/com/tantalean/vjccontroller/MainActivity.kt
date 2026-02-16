package com.tantalean.vjccontroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.AdRequest // Importación AdMob
import com.google.android.gms.ads.AdSize    // Importación AdMob
import com.google.android.gms.ads.AdView    // Importación AdMob
import com.startapp.sdk.ads.banner.Banner
import com.tantalean.vjccontroller.navigation.AppNav
import com.tantalean.vjccontroller.ui.theme.VJControllerOSCTheme
import com.tantalean.vjccontroller.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Comprobar actualizaciones remotas al iniciar
        com.tantalean.vjccontroller.util.UpdateChecker.checkForUpdate(this)

        enableEdgeToEdge()
        setContent {
            VJControllerOSCTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val viewModel: MainViewModel = viewModel()

                    Column {
                        // 1. BANNER DE ADMOB (Arriba)
                        AdmobBanner()

                        // Contenido principal
                        Column(modifier = Modifier.weight(1f)) {
                            AppNav(vm = viewModel, nav = navController)
                        }

                        // 2. BANNER DE START.IO (Abajo)
                        StartIoBanner()
                    }
                }
            }
        }
    }
}

@Composable
fun AdmobBanner() {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                // Tu nuevo ID de unidad de anuncios de AdMob
                adUnitId = "ca-app-pub-5263375378931462/4172847867"
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

@Composable
fun StartIoBanner() {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            Banner(context)
        }
    )
}
