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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.foundation.interaction.collectIsPressedAsState
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

// StartApp banner will be rendered via AndroidView (reflection)
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import android.util.Log
import android.view.View
import android.content.Context
import androidx.compose.runtime.DisposableEffect

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
                val addInteraction = remember { MutableInteractionSource() }
                val addPressed by addInteraction.collectIsPressedAsState()

                Button(
                    onClick = { vm.addLayer() },
                    interactionSource = addInteraction,
                    enabled = configState.isConnected,
                    modifier = Modifier.height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (addPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary)
                ) {
                    Text(text = "+ Layer", fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                val removeInteraction = remember { MutableInteractionSource() }
                val removePressed by removeInteraction.collectIsPressedAsState()

                Button(
                    onClick = { vm.removeLayer() },
                    interactionSource = removeInteraction,
                    enabled = configState.isConnected,
                    modifier = Modifier.height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (removePressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary)
                ) {
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
                        // Mostrar números cuando el total de layers sea >= 5; si no, mostrar "LAYER X"
                        val labelText = if (controlState.layerCount >= 5) layer.toString() else "LAYER $layer"
                        Text(text = labelText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ═══════════════════════════════════════════════════
            // ── CLIP PADS 3x3 (para el layer seleccionado) ──────
            // ═══════════════════════════════════════════════════
            val clips = vm.getClipsForSelectedLayer()

            // Renderizar filas de 3 por número dinámico de clips (soporta 12)
            val clipsPerRow = 3
            val rows = (clips.size + clipsPerRow - 1) / clipsPerRow
            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (col in 0 until clipsPerRow) {
                        val index = row * clipsPerRow + col
                        if (index >= clips.size) {
                            Spacer(modifier = Modifier.weight(1f))
                            continue
                        }
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
            // LCID / Ad Unit (usar StartApp ID para el panel principal)
            BannerAd(adUnitId = "201633923")

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
 * Banner composable: soporta AdMob (si `adUnitId` es AdMob) y cae de vuelta a StartApp si no.
 * - Si `adUnitId` empieza con `ca-app-pub-` renderiza un `AdView` (AdMob).
 * - Si no, intenta crear el banner de StartApp por reflexión (comportamiento previo).
 */
@Composable
fun BannerAd(adUnitId: String) {
    val context = LocalContext.current
    // altura de banner (aprox. StartApp ~50dp, AdMob FULL_BANNER es 60dp)
    val bannerHeight = 60.dp

    AndroidView(factory = { ctx ->
        // Si el adUnitId parece ser de AdMob, crear un AdView y cargar AdRequest
        if (adUnitId.startsWith("ca-app-pub-")) {
            AdView(ctx).apply {
                setAdSize(AdSize.FULL_BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        } else {
            // Intentar crear el Banner StartApp por reflexión (comportamientos previos)
            val classNames = listOf(
                "com.startapp.sdk.ads.banner.Banner",
                "com.startapp.sdk.ads.banner.StartAppBanner",
                "com.startapp.ads.banner.Banner",
                "com.startapp.sdk.adsbase.banner.Banner"
            )

            var bannerView: View? = null
            for (cn in classNames) {
                try {
                    val clazz = Class.forName(cn)
                    val ctor = clazz.getConstructor(Context::class.java)
                    val instance = ctor.newInstance(ctx) as? View
                    if (instance != null) {
                        bannerView = instance
                        break
                    }
                } catch (t: Throwable) {
                    // seguir intentando con otros nombres
                }
            }

            // fallback: View vacío si no se encontró la clase StartAppBanner
            bannerView ?: View(ctx)
        }
    }, modifier = Modifier
        .fillMaxWidth()
        .height(bannerHeight)
        .padding(top = 16.dp))

    DisposableEffect(Unit) { onDispose { } }
}