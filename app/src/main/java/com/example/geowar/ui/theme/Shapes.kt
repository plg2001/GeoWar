package com.example.geowar.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

class TerminalShape(private val cutCornerSize: Float = 20f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(
            path = Path().apply {
                moveTo(cutCornerSize, 0f)
                lineTo(size.width - cutCornerSize, 0f)
                lineTo(size.width, cutCornerSize)
                lineTo(size.width, size.height - cutCornerSize)
                lineTo(size.width - cutCornerSize, size.height)
                lineTo(cutCornerSize, size.height)
                lineTo(0f, size.height - cutCornerSize)
                lineTo(0f, cutCornerSize)
                close()
            }
        )
    }
}
