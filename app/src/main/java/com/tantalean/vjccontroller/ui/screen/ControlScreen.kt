package com.tantalean.vjccontroller.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tantalean.vjccontroller.component.OscPadButton
import com.tantalean.vjccontroller.viewmodel.MainViewModel
import kotlinx.coroutines.launch

// AdMob / AndroidView imports
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import android.util.Log
import androidx.compose.runtime.DisposableEffect
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError

// ── Colores de los pads ──────────────────────────────────────────
private val LayerBlue = Color(0xFF1565C0)
private val LayerBlueSelected = Color(0xFF42A5F5)
private val ClipGreen = Color(0xFF2E7D32)
private val ClipGreenLight = Color(0xFF43A047)
private val SliderBlue = Color(0xFF2196F3)
private val SliderOrange = Color(0xFFFF9800)
private val SliderTeal = Color(0xFF009688)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ControlScreen(
    vm: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val controlState by vm.controlState.collectAsState()
    val configState by vm.configState.collectAsState()
    val selectedLayer = controlState.selectedLayer

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = "Menú",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Control Resolume – OSC",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {


            // ═══════════════════════════════════════════════════
            // ── LAYER BUTTONS (grandes) y control dinámico de Layers ──
            // ═══════════════════════════════════════════════════
            // botones + / - arriba
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = { vm.addLayer() }, enabled = configState.isConnected, modifier = Modifier.height(40.dp)) {
                    Text(text = "+ Layer", fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { vm.removeLayer() }, enabled = configState.isConnected, modifier = Modifier.height(40.dp)) {
                    Text(text = "- Layer", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // fila de Layers (debajo de + / -)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (layer in 1..controlState.layerCount) {
                    val isSelected = controlState.selectedLayer == layer
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(if (isSelected) LayerBlueSelected else LayerBlue)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { vm.selectLayer(layer) },
                                onLongClick = { kotlinx.coroutines.GlobalScope.launch { snackbarHostState.showSnackbar("Layer $layer") } }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val labelText = if (layer >= 5) "L" else "LAYER $layer"
                        Text(text = labelText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ═══════════════════════════════════════════════════
            // ── CLIP PADS 3x3 (para el layer seleccionado) ──────
            // ═══════════════════════════════════════════════════
            val clips = vm.getClipsForSelectedLayer()

            for (row in 0..2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (col in 0..2) {
                        val index = row * 3 + col
                        val cmd = clips[index]
                        val padColor = if ((row + col) % 2 == 0) ClipGreen else ClipGreenLight

                        OscPadButton(
                            text = cmd.label,
                            color = padColor,
                            onClick = { vm.sendCommand(cmd) },
                            onLongClick = { vm.sendCommandForAssignment(cmd) },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.3f),
                            fontSize = 15.sp,
                            cornerRadius = 12.dp,
                            elevation = 10.dp,
                            enabled = configState.isConnected
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ═══════════════════════════════════════════════════
            // ── SLIDERS (Opacity, Speed, Transition) ──────────
            // ═══════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column {
                    // ── OPACITY (aplica a layer seleccionado) ──
                    OscSliderRow(
                        label = "OPACITY",
                        value = controlState.opacity,
                        color = SliderBlue,
                        onValueChange = { vm.updateOpacity(it) },
                        onValueChangeFinished = { vm.sendOpacity() },
                        enabled = configState.isConnected
                    )

                    Spacer(modifier = Modifier.height(12.dp))


                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Status ──
            if (controlState.lastMessage.isNotEmpty()) {
                Text(
                    text = controlState.lastMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Banner Ad (FULL_BANNER 468x60) — App ID ya configurado en AndroidManifest
            // LCID / Ad Unit (reemplazado por tu unidad que genera ingresos)
            BannerAd(adUnitId = "ca-app-pub-5263375378931462/2908400482")

            Spacer(modifier = Modifier.height(8.dp))

            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Fila de slider con label a la izquierda y slider estilizado.
 */
@Composable
private fun OscSliderRow(
    label: String,
    value: Float,
    color: Color,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    enabled: Boolean = true
) {
    Column {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.85f),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = 0f..1f,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = if (enabled) color else color.copy(alpha = 0.35f),
                activeTrackColor = if (enabled) color else color.copy(alpha = 0.2f),
                inactiveTrackColor = color.copy(alpha = 0.12f)
            )
        )
    }
}


/**
 * Banner Ad composable (AdMob).
 * - usa ID de prueba automáticamente en builds DEBUG
 * - usa tamaño anclado adaptativo recomendado por AdMob
 * - registra cargas y errores en Logcat para depuración
 */
@Composable
fun BannerAd(adUnitId: String) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val adWidthDp = (configuration.screenWidthDp - 32).coerceAtLeast(320) // resta padding horizontal
    val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp)

    // altura del ad en dp para el modifier
    val adHeightDp = with(LocalDensity.current) { adSize.getHeightInPixels(context).toDp() }

    // MobileAds se inicializa en Application.onCreate (VJControllerApp)

    // detectar si la app está en modo debug en tiempo de ejecución (evita depender de BuildConfig)
    val isDebug = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    val usedAdUnitId = if (isDebug) {
        "ca-app-pub-3940256099942544/6300978111" // ADMOB_TEST_BANNER
    } else {
        adUnitId
    }

    val adRef = remember { mutableStateOf<AdView?>(null) }

    AndroidView(
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(adSize)
                this.adUnitId = usedAdUnitId

                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d("BannerAd", "onAdLoaded — unit=$usedAdUnitId size=$adSize")
                    }
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.d("BannerAd", "onAdFailedToLoad: ${loadAdError.message} (code=${loadAdError.code})")
                    }
                }

                loadAd(AdRequest.Builder().build())
                adRef.value = this
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(adHeightDp)
            .padding(top = 16.dp),
        update = { /* no-op */ }
    )

    DisposableEffect(Unit) {
        onDispose {
            adRef.value?.destroy()
            adRef.value = null
        }
    }
}