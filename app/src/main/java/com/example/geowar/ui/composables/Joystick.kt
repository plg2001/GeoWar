package com.example.geowar.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun Joystick(
    modifier: Modifier = Modifier,
    size: Int = 150,
    dotSize: Int = 50,
    onMove: (Float, Float) -> Unit
) {
    val maxRadius = (size - dotSize) / 2f
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color.Gray.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(dotSize.dp)
                .clip(CircleShape)
                .background(Color.DarkGray)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { /* No action needed */ },
                        onDragEnd = {
                            offsetX = 0f
                            offsetY = 0f
                            onMove(0f, 0f) // Notifica che il movimento è terminato
                        },
                        onDragCancel = {
                             offsetX = 0f
                             offsetY = 0f
                             onMove(0f, 0f)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()

                            val newOffsetX = offsetX + dragAmount.x
                            val newOffsetY = offsetY + dragAmount.y

                            val angle = atan2(newOffsetY, newOffsetX)
                            val distance = kotlin.math.sqrt(newOffsetX * newOffsetX + newOffsetY * newOffsetY)

                            val limitedDistance = min(distance, maxRadius)

                            offsetX = limitedDistance * cos(angle)
                            offsetY = limitedDistance * sin(angle)

                            // Normalizza l'output nell'intervallo -1..1
                            val normalizedX = offsetX / maxRadius
                            val normalizedY = offsetY / maxRadius
                            onMove(normalizedX, normalizedY)
                        }
                    )
                }
        )
    }
}
