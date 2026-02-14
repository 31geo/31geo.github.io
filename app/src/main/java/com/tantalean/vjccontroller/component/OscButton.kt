package com.tantalean.vjccontroller.component

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Botón pad estilo controlador físico (ORCA PAD / Resolume).
 * Bordes redondeados, sombra, animación spring, vibración háptica.
 */
@Composable
fun OscPadButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    fontSize: TextUnit = 14.sp,
    cornerRadius: Dp = 14.dp,
    elevation: Dp = 10.dp,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null
) {
    // Vibración eliminada completamente

    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = 0.45f,
            stiffness = 900f
        ),
        label = "padScale"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        if (!enabled) return@detectTapGestures
                        try {
                            onLongClick?.invoke()
                        } catch (_: Exception) { /* ignore */ }
                    },
                    onPress = {
                        if (!enabled) return@detectTapGestures
                        isPressed = true
                        // Vibración eliminada
                        try {
                            onClick()
                        } catch (_: Exception) {
                            // Evitar que excepciones en el handler provoquen el cierre de la app
                        }
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = if (enabled) color else color.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) 2.dp else elevation,
            pressedElevation = 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = fontSize,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}