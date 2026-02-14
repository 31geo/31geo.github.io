package com.tantalean.vjccontroller


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
// MobileAds initialization moved to Application class
import com.tantalean.vjccontroller.navigation.AppNav
import com.tantalean.vjccontroller.ui.theme.VJControllerOSCTheme
import com.tantalean.vjccontroller.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // MobileAds se inicializa en Application.onCreate (VJControllerApp)

        enableEdgeToEdge()
        try {
            setContent {
                VJControllerOSCTheme(darkTheme = true) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        val viewModel: MainViewModel = viewModel()

                        // 2. Colocamos el Banner y el resto de la App en una columna
                        Column {
                            // El contenido de tu App ocupa el espacio de arriba
                            Column(modifier = Modifier.weight(1f)) {
                                AppNav(
                                    vm = viewModel,
                                    nav = navController
                                )
                            }

                            // El Banner se coloca en la parte inferior
                                AdmobBanner(adUnitId = "ca-app-pub-5263375378931462/9265392444")
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            android.util.Log.e("MainActivity", "Error inicializando UI", t)
            // Fallback UI para evitar que la app se cierre inmediatamente y mostrar el error
            setContent {
                VJControllerOSCTheme(darkTheme = true) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        androidx.compose.material3.Text(
                            text = "Error inicializando UI: ${'$'}{t::class.simpleName} - ${'$'}{t.message}",
                            modifier = Modifier.padding(16.dp),
                            color = androidx.compose.ui.graphics.Color.Red
                        )
                    }
                }
            }
        }
    }
}

// 3. Componente para el Banner
@Composable
fun AdmobBanner(adUnitId: String) {
    val context = LocalContext.current
    val isDebug = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    val usedAdUnitId = if (isDebug) "ca-app-pub-3940256099942544/6300978111" else adUnitId

    val adRef = remember { mutableStateOf<AdView?>(null) }

    AndroidView(
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = usedAdUnitId
                loadAd(AdRequest.Builder().build())
                adRef.value = this
            }
        },
        modifier = Modifier.fillMaxWidth()
    )

    DisposableEffect(Unit) {
        onDispose {
            adRef.value?.destroy()
            adRef.value = null
        }
    }
}