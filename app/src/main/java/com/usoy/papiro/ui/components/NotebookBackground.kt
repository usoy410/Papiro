package com.usoy.papiro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

enum class NotebookBackgroundType {
    GRID,
    RULED,
    DOTS,
    BLANK
}

@Composable
fun NotebookBackground(
    type: NotebookBackgroundType,
    modifier: Modifier = Modifier,
    showMargin: Boolean = true,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    // Ultra-subtle blueprint blue engineering colors
    val gridColor = if (darkTheme) {
        Color(0xFF38BDF8).copy(alpha = 0.13f)
    } else {
        Color(0xFF0284C7).copy(alpha = 0.14f)
    }

    val ruledLineColor = if (darkTheme) {
        Color(0xFF38BDF8).copy(alpha = 0.13f)
    } else {
        Color(0xFF0284C7).copy(alpha = 0.14f)
    }

    val marginLineColor = if (darkTheme) {
        Color(0xFFFF5252).copy(alpha = 0.15f)
    } else {
        Color(0xFFFDA4AF).copy(alpha = 0.18f)
    }

    val dotColor = if (darkTheme) {
        Color(0xFF38BDF8).copy(alpha = 0.13f)
    } else {
        Color(0xFF0284C7).copy(alpha = 0.14f)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. Draw thematic background patterns
        when (type) {
            NotebookBackgroundType.GRID -> {
                val stepPx = 28.dp.toPx()
                // Horizontal lines
                var y = 0f
                while (y < height) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    y += stepPx
                }
                // Vertical lines
                var x = 0f
                while (x < width) {
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1.dp.toPx()
                    )
                    x += stepPx
                }
            }
            NotebookBackgroundType.RULED -> {
                val stepPx = 32.dp.toPx()
                var y = 64.dp.toPx() // Top padding
                while (y < height) {
                    drawLine(
                        color = ruledLineColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    y += stepPx
                }
            }
            NotebookBackgroundType.DOTS -> {
                val stepPx = 24.dp.toPx()
                var y = stepPx / 2
                while (y < height) {
                    var x = stepPx / 2
                    while (x < width) {
                        drawCircle(
                            color = dotColor,
                            radius = 1.5.dp.toPx(),
                            center = Offset(x, y)
                        )
                        x += stepPx
                    }
                    y += stepPx
                }
            }
            NotebookBackgroundType.BLANK -> {
                // Blank canvas
            }
        }

        // 2. Draw red vertical margin line (unless BLANK or showMargin is false)
        if (showMargin && type != NotebookBackgroundType.BLANK) {
            val marginXPx = 64.dp.toPx()
            drawLine(
                color = marginLineColor,
                start = Offset(marginXPx, 0f),
                end = Offset(marginXPx, height),
                strokeWidth = 1.5.dp.toPx()
            )

            // Double line for ruled notebook look
            if (type == NotebookBackgroundType.RULED) {
                drawLine(
                    color = marginLineColor.copy(alpha = marginLineColor.alpha * 0.5f),
                    start = Offset(marginXPx + 4.dp.toPx(), 0f),
                    end = Offset(marginXPx + 4.dp.toPx(), height),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
    }
}
