package com.usoy.papiro.ui.components

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import android.content.Context
import android.graphics.PointF
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.ScrollView
import android.widget.Toast
import androidx.compose.ui.viewinterop.AndroidView
import com.usoy.papiro.R
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.filled.Edit
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalDensity
import org.json.JSONArray
import org.json.JSONObject

data class Point(val x: Float, val y: Float)

data class SaveResult(
    val width: Int,
    val height: Int,
    val paths: List<DrawPath>,
    val offsetX: Float,
    val offsetY: Float
)

data class DrawPath(
    val color: Int,
    val strokeWidth: Float,
    val points: List<Point>,
    val isEraser: Boolean = false
)

data class DrawingData(
    val id: String,
    val width: Int,
    val height: Int,
    val paths: List<DrawPath>,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("id", id)
        json.put("width", width)
        json.put("height", height)
        json.put("offsetX", offsetX.toDouble())
        json.put("offsetY", offsetY.toDouble())
        val pathsArray = JSONArray()
        for (path in paths) {
            val pathObj = JSONObject()
            pathObj.put("color", path.color)
            pathObj.put("strokeWidth", path.strokeWidth.toDouble())
            pathObj.put("isEraser", path.isEraser)
            val pointsArray = JSONArray()
            for (pt in path.points) {
                val ptObj = JSONObject()
                ptObj.put("x", pt.x.toDouble())
                ptObj.put("y", pt.y.toDouble())
                pointsArray.put(ptObj)
            }
            pathObj.put("points", pointsArray)
            pathsArray.put(pathObj)
        }
        json.put("paths", pathsArray)
        return json.toString()
    }

    companion object {
        private fun parseColor(colorObj: Any?): Int {
            if (colorObj == null) return android.graphics.Color.BLACK
            if (colorObj is Number) return colorObj.toInt()
            val str = colorObj.toString().trim()
            if (str.isEmpty()) return android.graphics.Color.BLACK
            try {
                if (str.startsWith("#")) {
                    if (str.length == 4) { // #RGB -> #RRGGBB
                        val r = str[1]
                        val g = str[2]
                        val b = str[3]
                        return android.graphics.Color.parseColor("#$r$r$g$g$b$b")
                    }
                    return android.graphics.Color.parseColor(str)
                }
                if (str.startsWith("0x") || str.startsWith("0X")) {
                    val hex = str.substring(2)
                    val value = hex.toLong(16)
                    if (hex.length == 6) {
                        return (0xFF000000L or value).toInt()
                    }
                    return value.toInt()
                }
                return when (str.lowercase()) {
                    "red" -> android.graphics.Color.RED
                    "green" -> android.graphics.Color.GREEN
                    "blue" -> android.graphics.Color.BLUE
                    "white" -> android.graphics.Color.WHITE
                    "black" -> android.graphics.Color.BLACK
                    "yellow" -> android.graphics.Color.YELLOW
                    "cyan" -> android.graphics.Color.CYAN
                    "magenta" -> android.graphics.Color.MAGENTA
                    "gray", "grey" -> android.graphics.Color.GRAY
                    "lightgray", "lightgrey" -> android.graphics.Color.LTGRAY
                    "darkgray", "darkgrey" -> android.graphics.Color.DKGRAY
                    else -> android.graphics.Color.parseColor(str)
                }
            } catch (e: Exception) {
                try {
                    return str.toInt()
                } catch (ex: Exception) {
                    return android.graphics.Color.BLACK
                }
            }
        }

        fun fromJson(jsonStr: String): DrawingData? {
            return try {
                val json = JSONObject(jsonStr)
                val id = json.optString("id", java.util.UUID.randomUUID().toString())
                val width = json.optInt("width", 320).let { if (it <= 0) 320 else it }
                val height = json.optInt("height", 240).let { if (it <= 0) 240 else it }
                val offsetX = json.optDouble("offsetX", 0.0).toFloat()
                val offsetY = json.optDouble("offsetY", 0.0).toFloat()
                val pathsArray = json.optJSONArray("paths") ?: JSONArray()
                val paths = mutableListOf<DrawPath>()
                for (i in 0 until pathsArray.length()) {
                    val pathObj = pathsArray.getJSONObject(i)
                    val colorVal = pathObj.opt("color")
                    val color = parseColor(colorVal)
                    val strokeWidth = pathObj.optDouble("strokeWidth", 4.0).toFloat()
                    val pointsArray = pathObj.optJSONArray("points") ?: JSONArray()
                    val points = mutableListOf<Point>()
                    for (j in 0 until pointsArray.length()) {
                        val ptObj = pointsArray.getJSONObject(j)
                        val x = ptObj.optDouble("x", 0.0).toFloat()
                        val y = ptObj.optDouble("y", 0.0).toFloat()
                        points.add(Point(x, y))
                    }
                    val isEraser = pathObj.optBoolean("isEraser", false)
                    paths.add(DrawPath(color, strokeWidth, points, isEraser))
                }
                DrawingData(id, width, height, paths, offsetX, offsetY)
            } catch (e: Exception) {
                null
            }
        }
    }
}

@Composable
fun BlueprintDoodle(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(32.dp)) {
        drawLine(Color.Gray, Offset(0f, size.height), Offset(size.width, size.height), 2f)
        drawLine(Color.Gray, Offset(size.width * 0.1f, size.height), Offset(size.width * 0.1f, size.height * 0.4f), 2f)
        drawLine(Color.Gray, Offset(size.width * 0.9f, size.height), Offset(size.width * 0.9f, size.height * 0.4f), 2f)
        drawLine(Color.Gray, Offset(size.width * 0.1f, size.height * 0.4f), Offset(size.width / 2f, size.height * 0.1f), 2f)
        drawLine(Color.Gray, Offset(size.width * 0.9f, size.height * 0.4f), Offset(size.width / 2f, size.height * 0.1f), 2f)
    }
}

@Composable
fun TableOfContentDoodle(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(32.dp)) {
        val path = Path().apply {
            // Header line
            moveTo(size.width * 0.1f, size.height * 0.15f)
            lineTo(size.width * 0.7f, size.height * 0.15f)
            
            // First item (bullet + text + page)
            moveTo(size.width * 0.1f, size.height * 0.4f)
            lineTo(size.width * 0.2f, size.height * 0.4f)
            moveTo(size.width * 0.3f, size.height * 0.4f)
            lineTo(size.width * 0.75f, size.height * 0.4f)
            moveTo(size.width * 0.85f, size.height * 0.4f)
            lineTo(size.width * 0.95f, size.height * 0.4f)
            
            // Second item
            moveTo(size.width * 0.1f, size.height * 0.65f)
            lineTo(size.width * 0.2f, size.height * 0.65f)
            moveTo(size.width * 0.3f, size.height * 0.65f)
            lineTo(size.width * 0.7f, size.height * 0.65f)
            moveTo(size.width * 0.85f, size.height * 0.65f)
            lineTo(size.width * 0.95f, size.height * 0.65f)
            
            // Third item
            moveTo(size.width * 0.1f, size.height * 0.9f)
            lineTo(size.width * 0.2f, size.height * 0.9f)
            moveTo(size.width * 0.3f, size.height * 0.9f)
            lineTo(size.width * 0.8f, size.height * 0.9f)
            moveTo(size.width * 0.9f, size.height * 0.9f)
            lineTo(size.width * 0.95f, size.height * 0.9f)
        }
        drawPath(path, Color.Gray, style = Stroke(2f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}

@Composable
fun TableDoodle(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(32.dp)) {
        val path = Path().apply {
            // Outer rectangle
            moveTo(size.width * 0.05f, size.height * 0.15f)
            lineTo(size.width * 0.95f, size.height * 0.15f)
            lineTo(size.width * 0.95f, size.height * 0.85f)
            lineTo(size.width * 0.05f, size.height * 0.85f)
            close()
            
            // Header separator
            moveTo(size.width * 0.05f, size.height * 0.35f)
            lineTo(size.width * 0.95f, size.height * 0.35f)
            
            // Row separator
            moveTo(size.width * 0.05f, size.height * 0.6f)
            lineTo(size.width * 0.95f, size.height * 0.6f)
            
            // Column separator
            moveTo(size.width * 0.35f, size.height * 0.15f)
            lineTo(size.width * 0.35f, size.height * 0.85f)
            
            moveTo(size.width * 0.65f, size.height * 0.15f)
            lineTo(size.width * 0.65f, size.height * 0.85f)
        }
        drawPath(path, Color.Gray, style = Stroke(2f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}



@Composable
fun FunctionDoodle(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(32.dp)) {
        val path = Path().apply {
            moveTo(0f, size.height / 2f)
            quadraticTo(size.width / 4f, 0f, size.width / 2f, size.height / 2f)
            quadraticTo(size.width * 3 / 4f, size.height, size.width, size.height / 2f)
        }
        drawPath(path, Color.Gray, style = Stroke(2f))
        drawLine(Color.Gray, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), 1f)
        drawLine(Color.Gray, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), 1f)
    }
}

@Composable
fun MoleculeDoodle(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(32.dp)) {
        drawCircle(Color.Gray, radius = 4f, center = Offset(size.width * 0.2f, size.height * 0.8f), style = Stroke(1.5f))
        drawCircle(Color.Gray, radius = 6f, center = Offset(size.width * 0.5f, size.height * 0.5f), style = Stroke(1.5f))
        drawCircle(Color.Gray, radius = 4f, center = Offset(size.width * 0.8f, size.height * 0.2f), style = Stroke(1.5f))
        drawCircle(Color.Gray, radius = 4f, center = Offset(size.width * 0.8f, size.height * 0.8f), style = Stroke(1.5f))
        
        drawLine(Color.Gray, Offset(size.width * 0.2f, size.height * 0.8f), Offset(size.width * 0.5f, size.height * 0.5f), 1.5f)
        drawLine(Color.Gray, Offset(size.width * 0.5f, size.height * 0.5f), Offset(size.width * 0.8f, size.height * 0.2f), 1.5f)
        drawLine(Color.Gray, Offset(size.width * 0.5f, size.height * 0.5f), Offset(size.width * 0.8f, size.height * 0.8f), 1.5f)
    }
}

@Composable
fun MathDoodle(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(32.dp)) {
        val path = Path().apply {
            // Integration symbol or a beautiful summation/square root
            // Let's do a square root sign with x inside
            moveTo(size.width * 0.1f, size.height * 0.5f)
            lineTo(size.width * 0.25f, size.height * 0.5f)
            lineTo(size.width * 0.35f, size.height * 0.8f)
            lineTo(size.width * 0.5f, size.height * 0.25f)
            lineTo(size.width * 0.85f, size.height * 0.25f)
            
            // Draw x inside the square root
            moveTo(size.width * 0.6f, size.height * 0.45f)
            lineTo(size.width * 0.75f, size.height * 0.65f)
            moveTo(size.width * 0.75f, size.height * 0.45f)
            lineTo(size.width * 0.6f, size.height * 0.65f)
        }
        drawPath(path, Color.Gray, style = Stroke(2f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}


enum class DrawTool { PENCIL, ERASER }

@Composable
fun EraserIcon(tint: Color) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .border(1.5.dp, tint, RoundedCornerShape(3.dp))
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path().apply {
                moveTo(0f, size.height)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height)
                close()
            }
            drawPath(path, tint.copy(alpha = 0.5f))
        }
    }
}

fun mapTouchToCanvas(touch: Offset, canvasW: Float, canvasH: Float, scale: Float, offset: Offset): Point {
    val x = (touch.x - canvasW / 2f - offset.x) / scale + canvasW / 2f
    val y = (touch.y - canvasH / 2f - offset.y) / scale + canvasH / 2f
    return Point(x.coerceIn(0f, canvasW), y.coerceIn(0f, canvasH))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenDrawingEditor(
    width: Int,
    height: Int,
    initialPaths: List<DrawPath>,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
    onDismiss: () -> Unit,
    onSave: (width: Int, height: Int, List<DrawPath>, offsetX: Float, offsetY: Float) -> Unit
) {
    androidx.activity.compose.BackHandler(onBack = onDismiss)

    Surface(modifier = Modifier.fillMaxSize()) {
        val composeBgColor = MaterialTheme.colorScheme.background
        val composeOnBgColor = MaterialTheme.colorScheme.onBackground
        val composeSurfaceColor = MaterialTheme.colorScheme.surface
        val composeOnSurfaceColor = MaterialTheme.colorScheme.onSurface
        val composePrimaryColor = MaterialTheme.colorScheme.primary

        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            factory = { context ->
                    val view = LayoutInflater.from(context).inflate(R.layout.drawing_editor_layout, null, false)
                    
                    val drawingView = view.findViewById<DrawingView>(R.id.drawing_view)
                    val scrollView = view.findViewById<ScrollView>(R.id.scroll_view)
                    
                    // Buttons
                    val btnBack = view.findViewById<ImageButton>(R.id.btn_back)
                    val btnFinish = view.findViewById<Button>(R.id.btn_finish)
                    val btnUndo = view.findViewById<ImageButton>(R.id.btn_undo)
                    val btnRedo = view.findViewById<ImageButton>(R.id.btn_redo)
                    val btnToolPen = view.findViewById<ImageButton>(R.id.btn_tool_pen)
                    val btnToolEraser = view.findViewById<ImageButton>(R.id.btn_tool_eraser)
                    val btnStrokeThickness = view.findViewById<ImageButton>(R.id.btn_stroke_thickness)
                    val btnColorPicker = view.findViewById<ImageButton>(R.id.btn_color_picker)
                    
                    val btnSmartShape = view.findViewById<ImageButton>(R.id.btn_smart_shape)
                    val btnFingerPainting = view.findViewById<ImageButton>(R.id.btn_finger_painting)
                    val btnGridType = view.findViewById<ImageButton>(R.id.btn_grid_type)

                    // Inline brush sizing and vertical color controls
                    val brushSizeSeekbar = view.findViewById<android.widget.SeekBar>(R.id.brush_size_seekbar)
                    val txtBrushSizeVal = view.findViewById<android.widget.TextView>(R.id.txt_brush_size_val)
                    val brushSizeSliderContainer = view.findViewById<android.view.View>(R.id.brush_size_slider_container)
                    val floatingColorContainer = view.findViewById<android.view.View>(R.id.floating_color_container)
                    val btnActiveColorIndicator = view.findViewById<android.view.View>(R.id.btn_active_color_indicator)
                    val activeColorCircle = view.findViewById<android.view.View>(R.id.active_color_circle)
                    val colorCustomPicker = view.findViewById<android.widget.ImageButton>(R.id.color_custom_picker)

                    // Dynamic Theme Integration
                    val themeBgColor = composeBgColor.toArgb()
                    val themeOnBgColor = composeOnBgColor.toArgb()
                    val themeSurfaceColor = composeSurfaceColor.toArgb()
                    val themeOnSurfaceColor = composeOnSurfaceColor.toArgb()
                    val themePrimaryColor = composePrimaryColor.toArgb()
                    val bgIsDark = (android.graphics.Color.red(themeBgColor) * 0.299f +
                                    android.graphics.Color.green(themeBgColor) * 0.587f +
                                    android.graphics.Color.blue(themeBgColor) * 0.114f) < 128f

                    val topBar = view.findViewById<android.view.View>(R.id.top_bar)
                    val bottomBar = view.findViewById<android.view.View>(R.id.bottom_bar)
                    val txtTitle = view.findViewById<TextView>(R.id.txt_title)

                    val density = context.resources.displayMetrics.density

                    // Apply dynamic colors to layout containers and headers
                    topBar.setBackgroundColor(themeSurfaceColor)
                    bottomBar.setBackgroundColor(themeSurfaceColor)
                    scrollView.setBackgroundColor(themeBgColor)
                    
                    drawingView.canvasBackgroundColor = themeBgColor
                    drawingView.setBackgroundColor(themeBgColor)
                    drawingView.setStrokeColor(themeOnBgColor)
                    
                    txtTitle?.setTextColor(themeOnSurfaceColor)
                    btnBack.setColorFilter(themeOnSurfaceColor)
                    btnFinish.setTextColor(themePrimaryColor)
                    btnUndo.setColorFilter(themeOnSurfaceColor)
                    btnRedo.setColorFilter(themeOnSurfaceColor)
                    
                    // Hide the old modal trigger buttons as they are now replaced by clean inline & vertical controls
                    btnColorPicker.visibility = android.view.View.GONE
                    btnStrokeThickness.visibility = android.view.View.GONE

                    // Set initial height of DrawingView if passed, scaling correctly with screen density
                    val defaultHeightPx = (1200 * density).toInt()
                    drawingView.layoutParams?.let { lp ->
                        if (height > 0) {
                            lp.height = max(defaultHeightPx, (height * density).toInt())
                        } else {
                            lp.height = defaultHeightPx
                        }
                        drawingView.requestLayout()
                    }

                    // Populate initial paths by scaling back to pixels with offset restoration
                    initialPaths.forEach { drawP ->
                        val path = android.graphics.Path()
                        if (drawP.points.isNotEmpty()) {
                            val first = drawP.points.first()
                            path.moveTo((first.x + offsetX) * density, (first.y + offsetY) * density)
                            for (i in 1 until drawP.points.size) {
                                val pt = drawP.points[i]
                                path.lineTo((pt.x + offsetX) * density, (pt.y + offsetY) * density)
                            }
                        }
                        
                        // Adaptive Path Contrast Adjustment
                        val isEraser = drawP.isEraser || (drawP.color == drawingView.canvasBackgroundColor)
                        var pathColor = drawP.color
                        if (isEraser) {
                            pathColor = drawingView.canvasBackgroundColor
                        } else {
                            if (bgIsDark) {
                                if (pathColor == android.graphics.Color.BLACK || pathColor == android.graphics.Color.parseColor("#121212")) {
                                    pathColor = themeOnBgColor
                                }
                            } else {
                                if (pathColor == android.graphics.Color.WHITE || pathColor == android.graphics.Color.parseColor("#FFFFFF")) {
                                    pathColor = themeOnBgColor
                                }
                            }
                        }

                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            isDither = true
                            style = android.graphics.Paint.Style.STROKE
                            strokeJoin = android.graphics.Paint.Join.ROUND
                            strokeCap = android.graphics.Paint.Cap.ROUND
                            color = pathColor
                            strokeWidth = drawP.strokeWidth * density
                        }
                        
                        drawingView.paths.add(DrawnPath(
                            path = path,
                            paint = paint,
                            isEraser = isEraser,
                            points = drawP.points.map { PointF((it.x + offsetX) * density, (it.y + offsetY) * density) }
                        ))
                    }
                    drawingView.invalidate()

                    // Auto-scroll the view port to make the drawing visible immediately
                    if (offsetY > 0f) {
                        scrollView.post {
                            val scrollY = ((offsetY - 20f) * density).toInt().coerceAtLeast(0)
                            scrollView.scrollTo(0, scrollY)
                        }
                    }

                    // Wiring up events
                    btnBack.setOnClickListener {
                        onDismiss()
                    }

                    btnFinish.setOnClickListener {
                        // Execution of Smart Crop
                        val croppedBitmap = drawingView.getCroppedBitmap()
                        if (croppedBitmap != null) {
                            Toast.makeText(context, "Smart crop completed successfully!", Toast.LENGTH_SHORT).show()
                        }
                        
                        // Extract points, crop, and convert to density-independent DP
                        val paths = drawingView.paths
                        val allPoints = paths.flatMap { it.points }
                        
                        val saveResult = if (allPoints.isNotEmpty()) {
                            val minX = allPoints.minOf { it.x }
                            val maxX = allPoints.maxOf { it.x }
                            val minY = allPoints.minOf { it.y }
                            val maxY = allPoints.maxOf { it.y }
                            
                            val paddingPx = 30f
                            val computedWidthPx = maxX - minX + paddingPx * 2
                            val computedHeightPx = maxY - minY + paddingPx * 2
                            
                            val widthDp = (computedWidthPx / density).toInt().coerceAtLeast(100)
                            val heightDp = (computedHeightPx / density).toInt().coerceAtLeast(100)
                            
                            val adjustedPaths = paths.map { drawnPath ->
                                DrawPath(
                                    color = drawnPath.paint.color,
                                    strokeWidth = drawnPath.paint.strokeWidth / density,
                                    points = drawnPath.points.map { pt ->
                                        Point(
                                            x = (pt.x - minX + paddingPx) / density,
                                            y = (pt.y - minY + paddingPx) / density
                                        )
                                    },
                                    isEraser = drawnPath.isEraser
                                )
                            }
                            val computedOffsetX = (minX - paddingPx) / density
                            val computedOffsetY = (minY - paddingPx) / density
                            
                            val finalOffsetX = if (computedOffsetX < 0f) 0f else computedOffsetX
                            val finalOffsetY = if (computedOffsetY < 0f) 0f else computedOffsetY
                            
                            SaveResult(widthDp, heightDp, adjustedPaths, finalOffsetX, finalOffsetY)
                        } else {
                            SaveResult(320, 240, emptyList<DrawPath>(), 0f, 0f)
                        }
                        
                        onSave(saveResult.width, saveResult.height, saveResult.paths, saveResult.offsetX, saveResult.offsetY)
                    }

                    btnUndo.setOnClickListener {
                        drawingView.undo()
                    }

                    btnRedo.setOnClickListener {
                        drawingView.redo()
                    }

                    // Create high-fidelity visual indicators using rounded shapes
                    val activeBgColor = composePrimaryColor.copy(alpha = 0.12f).toArgb()
                    val activeBg = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 8f * context.resources.displayMetrics.density
                        setColor(activeBgColor)
                    }

                    val inactiveColor = composeOnSurfaceColor.copy(alpha = 0.54f).toArgb()
                    val activeColor = themePrimaryColor
                    val whiteFilterColor = themeOnSurfaceColor

                    // Unified helper to update active tool visuals and internal states
                    fun updateToolSelection(activeToolId: Int) {
                        btnToolPen.background = null
                        btnSmartShape.background = null
                        btnToolEraser.background = null

                        btnToolPen.setColorFilter(inactiveColor)
                        btnSmartShape.setColorFilter(inactiveColor)
                        btnToolEraser.setColorFilter(inactiveColor)

                        when (activeToolId) {
                            R.id.btn_tool_pen -> {
                                btnToolPen.background = activeBg
                                btnToolPen.setColorFilter(activeColor)
                                drawingView.isEraserMode = false
                                drawingView.isSmartShapeEnabled = false
                            }
                            R.id.btn_smart_shape -> {
                                btnSmartShape.background = activeBg
                                btnSmartShape.setColorFilter(activeColor)
                                drawingView.isEraserMode = false
                                drawingView.isSmartShapeEnabled = true
                            }
                            R.id.btn_tool_eraser -> {
                                btnToolEraser.background = activeBg
                                btnToolEraser.setColorFilter(activeColor)
                                drawingView.isEraserMode = true
                                drawingView.isSmartShapeEnabled = false
                            }
                        }
                    }

                    fun updateFingerPaintingVisuals() {
                        if (drawingView.isFingerPaintingEnabled) {
                            btnFingerPainting.background = activeBg
                            btnFingerPainting.setColorFilter(activeColor)
                        } else {
                            btnFingerPainting.background = null
                            btnFingerPainting.setColorFilter(whiteFilterColor)
                        }
                    }

                    fun updateGridTypeVisuals() {
                        if (drawingView.currentGridType != DrawingView.GridType.NONE) {
                            btnGridType.background = activeBg
                            btnGridType.setColorFilter(activeColor)
                        } else {
                            btnGridType.background = null
                            btnGridType.setColorFilter(whiteFilterColor)
                        }
                    }

                    fun updateStrokeThicknessVisuals() {
                        val thickness = drawingView.getStrokeWidth().toInt()
                        brushSizeSeekbar.progress = (thickness - 4).coerceIn(0, 46)
                        txtBrushSizeVal.text = "$thickness dp"
                    }

                    // Default tool states
                    updateToolSelection(R.id.btn_tool_pen)
                    updateFingerPaintingVisuals()
                    updateGridTypeVisuals()
                    updateStrokeThicknessVisuals()

                    // Apply dynamic outline border only with transparent background to the slider container
                    val sliderBorderColor = if (bgIsDark) {
                        android.graphics.Color.argb(100, 255, 255, 255) // Highly visible transparent white border
                    } else {
                        android.graphics.Color.argb(100, 0, 0, 0) // Highly visible transparent black border
                    }
                    brushSizeSliderContainer.background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 24 * density
                        setColor(android.graphics.Color.TRANSPARENT)
                        setStroke((1.5f * density).toInt(), sliderBorderColor)
                    }

                    // Style the seekbar thumb & progress bar with the theme primary color to make it pop and be extremely visible
                    try {
                        brushSizeSeekbar.thumb.setTint(themePrimaryColor)
                        brushSizeSeekbar.progressDrawable.setTint(themePrimaryColor)
                    } catch (e: Exception) {
                        // Safe fallback if tint isn't supported on older drawable types
                    }

                    // Color palette container styling - matching the theme surface but with translucent visibility
                    val paletteBgAlphaColor = android.graphics.Color.argb(
                        225, // ~88% opacity for stunning translucent glassy appearance
                        android.graphics.Color.red(themeSurfaceColor),
                        android.graphics.Color.green(themeSurfaceColor),
                        android.graphics.Color.blue(themeSurfaceColor)
                    )
                    val paletteStrokeColor = if (bgIsDark) {
                        android.graphics.Color.argb(60, 255, 255, 255)
                    } else {
                        android.graphics.Color.argb(60, 0, 0, 0)
                    }
                    floatingColorContainer.background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 18 * density
                        setColor(paletteBgAlphaColor)
                        setStroke((1 * density).toInt(), paletteStrokeColor)
                    }

                    // Dynamic helper to create circle drawables
                    fun createCircleDrawable(colorInt: Int, isWithBorder: Boolean = false): android.graphics.drawable.GradientDrawable {
                        return android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.OVAL
                            setColor(colorInt)
                            if (isWithBorder) {
                                setStroke((1.5f * density).toInt(), if (bgIsDark) android.graphics.Color.parseColor("#44FFFFFF") else android.graphics.Color.parseColor("#44000000"))
                            }
                        }
                    }

                    // Active color indicator updater
                    fun updateActiveColorIndicator(colorInt: Int) {
                        activeColorCircle.background = createCircleDrawable(colorInt, isWithBorder = true)
                    }

                    // Initial Brush Size Seekbar progress & value mapping
                    val currentSize = drawingView.getStrokeWidth().toInt()
                    brushSizeSeekbar.progress = (currentSize - 4).coerceIn(0, 46)
                    txtBrushSizeVal.text = "$currentSize dp"

                    brushSizeSeekbar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(sb: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                            val dpVal = progress + 4
                            txtBrushSizeVal.text = "$dpVal dp"
                            drawingView.setStrokeWidth(dpVal.toFloat())
                        }
                        override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
                        override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
                    })

                    // Toggle vertical quick palette selection
                    btnActiveColorIndicator.setOnClickListener {
                        if (floatingColorContainer.visibility == android.view.View.VISIBLE) {
                            floatingColorContainer.animate().alpha(0f).translationY(20f * density).setDuration(150).withEndAction {
                                floatingColorContainer.visibility = android.view.View.GONE
                            }.start()
                        } else {
                            floatingColorContainer.visibility = android.view.View.VISIBLE
                            floatingColorContainer.alpha = 0f
                            floatingColorContainer.translationY = 20f * density
                            floatingColorContainer.animate().alpha(1f).translationY(0f).setDuration(200).start()
                        }
                    }

                    // Populate initial active color indicator
                    updateActiveColorIndicator(drawingView.getStrokeColor())

                    // Define and bind vertical quick color palette views
                    val colorViews = listOf(
                        R.id.color_white to themeOnBgColor,
                        R.id.color_red to android.graphics.Color.parseColor("#FF4444"),
                        R.id.color_blue to android.graphics.Color.parseColor("#33B5E5"),
                        R.id.color_green to android.graphics.Color.parseColor("#99CC00"),
                        R.id.color_yellow to android.graphics.Color.parseColor("#FFBB33"),
                        R.id.color_orange to android.graphics.Color.parseColor("#FF8800"),
                        R.id.color_purple to android.graphics.Color.parseColor("#AA66CC")
                    )

                    colorViews.forEach { (viewId, colorInt) ->
                        val colorBubble = view.findViewById<android.view.View>(viewId)
                        colorBubble.background = createCircleDrawable(colorInt, isWithBorder = (colorInt == themeOnBgColor))

                        colorBubble.setOnClickListener {
                            drawingView.setStrokeColor(colorInt)
                            updateActiveColorIndicator(colorInt)

                            if (drawingView.isEraserMode) {
                                updateToolSelection(R.id.btn_tool_pen)
                            }

                            // Pulse animation for selected item
                            colorBubble.animate().scaleX(1.25f).scaleY(1.25f).setDuration(150).withEndAction {
                                colorBubble.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                            }.start()

                            // Auto-close vertical palette with smooth fade & transition
                            floatingColorContainer.animate().alpha(0f).translationY(20f * density).setDuration(150).withEndAction {
                                floatingColorContainer.visibility = android.view.View.GONE
                            }.start()
                        }
                    }

                    // Setup the advanced custom color picker inside the vertical list with a gorgeous circular rainbow gradient background
                    val gradientColors = intArrayOf(
                        android.graphics.Color.parseColor("#FF3366"), // Pinkish Red
                        android.graphics.Color.parseColor("#FF9933"), // Orange-Yellow
                        android.graphics.Color.parseColor("#33CC66"), // Vibrant Green
                        android.graphics.Color.parseColor("#3399FF"), // Vibrant Blue
                        android.graphics.Color.parseColor("#9933FF"), // Purple
                        android.graphics.Color.parseColor("#FF3366")  // Loop back to Pinkish Red
                    )
                    val customPickerGradient = android.graphics.drawable.GradientDrawable(
                        android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                        gradientColors
                    ).apply {
                        shape = android.graphics.drawable.GradientDrawable.OVAL
                        setStroke((1.5f * density).toInt(), if (bgIsDark) android.graphics.Color.parseColor("#44FFFFFF") else android.graphics.Color.parseColor("#44000000"))
                    }
                    colorCustomPicker.setImageDrawable(null) // Completely remove standard icon to show pure gradient shape
                    colorCustomPicker.background = customPickerGradient
                    colorCustomPicker.setOnClickListener {
                        // Close vertical palette with fade
                        floatingColorContainer.animate().alpha(0f).translationY(20f * density).setDuration(150).withEndAction {
                            floatingColorContainer.visibility = android.view.View.GONE
                        }.start()

                        val colors = listOf(
                            android.graphics.Color.parseColor("#FF4444") to "Red",
                            android.graphics.Color.parseColor("#FF4081") to "Pink",
                            android.graphics.Color.parseColor("#AA66CC") to "Purple",
                            android.graphics.Color.parseColor("#673AB7") to "Deep Purple",
                            android.graphics.Color.parseColor("#3F51B5") to "Indigo",
                            android.graphics.Color.parseColor("#33B5E5") to "Blue",
                            android.graphics.Color.parseColor("#00BCD4") to "Cyan",
                            android.graphics.Color.parseColor("#009688") to "Teal",
                            android.graphics.Color.parseColor("#99CC00") to "Green",
                            android.graphics.Color.parseColor("#8BC34A") to "Light Green",
                            android.graphics.Color.parseColor("#CDDC39") to "Lime",
                            android.graphics.Color.parseColor("#FFBB33") to "Yellow",
                            android.graphics.Color.parseColor("#FF9800") to "Orange",
                            android.graphics.Color.parseColor("#FF5722") to "Deep Orange",
                            android.graphics.Color.parseColor("#795548") to "Brown",
                            android.graphics.Color.parseColor("#9E9E9E") to "Grey",
                            android.graphics.Color.WHITE to "White"
                        )

                        val dialogLayout = android.widget.LinearLayout(context).apply {
                            orientation = android.widget.LinearLayout.VERTICAL
                            setPadding((24 * density).toInt(), (24 * density).toInt(), (24 * density).toInt(), (24 * density).toInt())
                            setBackgroundColor(android.graphics.Color.parseColor("#1E1E1E"))
                        }

                        val titleTextView = android.widget.TextView(context).apply {
                            text = "Select Custom Color"
                            textSize = 20f
                            setTextColor(android.graphics.Color.WHITE)
                            setTypeface(null, android.graphics.Typeface.BOLD)
                            setPadding(0, 0, 0, (16 * density).toInt())
                        }
                        dialogLayout.addView(titleTextView)

                        var currentRow: android.widget.LinearLayout? = null
                        for (i in colors.indices) {
                            if (i % 4 == 0) {
                                currentRow = android.widget.LinearLayout(context).apply {
                                    orientation = android.widget.LinearLayout.HORIZONTAL
                                    gravity = android.view.Gravity.CENTER_HORIZONTAL
                                    layoutParams = android.widget.LinearLayout.LayoutParams(
                                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                                    ).apply {
                                        setMargins(0, (8 * density).toInt(), 0, (8 * density).toInt())
                                    }
                                }
                                dialogLayout.addView(currentRow)
                            }

                            val (cColor, cName) = colors[i]
                            val bubble = android.view.View(context).apply {
                                layoutParams = android.widget.LinearLayout.LayoutParams(
                                    (40 * density).toInt(),
                                    (40 * density).toInt()
                                ).apply {
                                    setMargins((12 * density).toInt(), 0, (12 * density).toInt(), 0)
                                }
                                val dDraw = android.graphics.drawable.GradientDrawable().apply {
                                    shape = android.graphics.drawable.GradientDrawable.OVAL
                                    setColor(cColor)
                                    setStroke((1 * density).toInt(), android.graphics.Color.parseColor("#33FFFFFF"))
                                }
                                background = dDraw
                                contentDescription = cName
                            }
                            currentRow?.addView(bubble)
                        }

                        val hexTitle = android.widget.TextView(context).apply {
                            text = "Or enter custom Hex color (#RRGGBB):"
                            textSize = 14f
                            setTextColor(android.graphics.Color.parseColor("#B0FFFFFF"))
                            setPadding(0, (24 * density).toInt(), 0, (8 * density).toInt())
                        }
                        dialogLayout.addView(hexTitle)

                        val hexInput = android.widget.EditText(context).apply {
                            hint = "#33B5E5"
                            setHintTextColor(android.graphics.Color.parseColor("#44FFFFFF"))
                            setTextColor(android.graphics.Color.WHITE)
                            maxLines = 1
                            setText(String.format("#%06X", (0xFFFFFF and drawingView.getStrokeColor())))
                            backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#44FFFFFF"))
                        }
                        dialogLayout.addView(hexInput)

                        val buttonLayout = android.widget.LinearLayout(context).apply {
                            orientation = android.widget.LinearLayout.HORIZONTAL
                            gravity = android.view.Gravity.END
                            setPadding(0, (16 * density).toInt(), 0, 0)
                        }

                        val cancelBtn = android.widget.Button(context, null, 0, android.R.style.Widget_Material_Button_Borderless).apply {
                            text = "Cancel"
                            setTextColor(android.graphics.Color.parseColor("#FF8888"))
                        }

                        val okBtn = android.widget.Button(context, null, 0, android.R.style.Widget_Material_Button_Borderless).apply {
                            text = "Apply"
                            setTextColor(android.graphics.Color.parseColor("#2196F3"))
                        }

                        buttonLayout.addView(cancelBtn)
                        buttonLayout.addView(okBtn)
                        dialogLayout.addView(buttonLayout)

                        val customDialog = android.app.AlertDialog.Builder(context)
                            .setView(dialogLayout)
                            .create()

                        var bubbleIndex = 0
                        for (idx in 0 until dialogLayout.childCount) {
                            val child = dialogLayout.getChildAt(idx)
                            if (child is android.widget.LinearLayout && child != buttonLayout) {
                                for (j in 0 until child.childCount) {
                                    val b = child.getChildAt(j)
                                    val localColor = colors[bubbleIndex].first
                                    val localName = colors[bubbleIndex].second
                                    b.setOnClickListener {
                                        drawingView.setStrokeColor(localColor)
                                        updateActiveColorIndicator(localColor)
                                        if (drawingView.isEraserMode) {
                                            updateToolSelection(R.id.btn_tool_pen)
                                        }
                                        Toast.makeText(context, "Selected: $localName", Toast.LENGTH_SHORT).show()
                                        customDialog.dismiss()
                                    }
                                    bubbleIndex++
                                }
                            }
                        }

                        cancelBtn.setOnClickListener {
                            customDialog.dismiss()
                        }

                        okBtn.setOnClickListener {
                            val hexStr = hexInput.text.toString().trim()
                            try {
                                val colorInt = android.graphics.Color.parseColor(hexStr)
                                drawingView.setStrokeColor(colorInt)
                                updateActiveColorIndicator(colorInt)
                                if (drawingView.isEraserMode) {
                                    updateToolSelection(R.id.btn_tool_pen)
                                }
                                customDialog.dismiss()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Invalid Hex Color code!", Toast.LENGTH_SHORT).show()
                            }
                        }

                        customDialog.show()
                        customDialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                    }

                    btnToolPen.setOnClickListener {
                        updateToolSelection(R.id.btn_tool_pen)
                    }

                    btnSmartShape.setOnClickListener {
                        updateToolSelection(R.id.btn_smart_shape)
                    }

                    btnToolEraser.setOnClickListener {
                        updateToolSelection(R.id.btn_tool_eraser)
                    }

                    btnFingerPainting.setOnClickListener {
                        drawingView.isFingerPaintingEnabled = !drawingView.isFingerPaintingEnabled
                        updateFingerPaintingVisuals()
                    }

                    btnGridType.setOnClickListener {
                        val nextGridType = when (drawingView.currentGridType) {
                            DrawingView.GridType.NONE -> DrawingView.GridType.GRID
                            DrawingView.GridType.GRID -> DrawingView.GridType.DOTS
                            DrawingView.GridType.DOTS -> DrawingView.GridType.RULED
                            DrawingView.GridType.RULED -> DrawingView.GridType.NONE
                        }
                        drawingView.currentGridType = nextGridType
                        updateGridTypeVisuals()
                    }

                    view
                },
                update = { view ->
                    // No manual padding adjustments needed. Compose statusBarsPadding() and navigationBarsPadding()
                    // handle safe system bar offsets automatically and gracefully.
                }
            )
        }
}

@Composable
fun DrawingBlockView(
    block: MarkdownBlock.DrawingBlock,
    onContentChanged: (String) -> Unit,
    currentContent: String,
    onEditClick: (DrawingData) -> Unit
) {
    val drawingData = remember(block.json) { DrawingData.fromJson(block.json) }
    val density = LocalDensity.current.density
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val themeOnBgColor = MaterialTheme.colorScheme.onBackground
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (drawingData != null) {
            Canvas(
                modifier = Modifier
                    .size(drawingData.width.dp, drawingData.height.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                    .clickable { onEditClick(drawingData) }
            ) {
                drawingData.paths.forEach { drawP ->
                    var pathColor = Color(drawP.color)
                    if (isDarkTheme) {
                        if (drawP.color == android.graphics.Color.BLACK || drawP.color == android.graphics.Color.parseColor("#121212")) {
                            pathColor = themeOnBgColor
                        }
                    } else {
                        if (drawP.color == android.graphics.Color.WHITE || drawP.color == android.graphics.Color.parseColor("#FFFFFF")) {
                            pathColor = themeOnBgColor
                        }
                    }

                    if (drawP.points.size > 1) {
                        val p = Path().apply {
                            moveTo(drawP.points.first().x * density, drawP.points.first().y * density)
                            for (i in 1 until drawP.points.size) {
                                lineTo(drawP.points[i].x * density, drawP.points[i].y * density)
                            }
                        }
                        drawPath(p, pathColor, style = Stroke(drawP.strokeWidth * density, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                    } else if (drawP.points.size == 1) {
                        val pt = drawP.points.first()
                        drawCircle(
                            color = pathColor,
                            radius = (drawP.strokeWidth * density) / 2f,
                            center = androidx.compose.ui.geometry.Offset(pt.x * density, pt.y * density)
                        )
                    }
                }
            }
        }
    }
}

class MarkdownVisualTransformation(val isDark: Boolean, val primaryColor: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        val transformed = java.lang.StringBuilder()
        
        // Maps each index in original to its corresponding index in transformed.
        val origToTrans = IntArray(original.length + 1)
        
        // Maps each index in transformed to its corresponding index in original.
        val transToOrig = ArrayList<Int>()
        
        // Span styles to apply to the transformed string
        val styles = ArrayList<AnnotatedStyleRange>()
        
        var i = 0
        val len = original.length
        
        // Keep track of active styles with their start index in the transformed string
        var boldStart: Int? = null
        var italicStart: Int? = null
        var codeStart: Int? = null
        var strikeStart: Int? = null
        
        // Line-based tracking for headers and list items
        var isStartOfLine = true
        var currentHeaderLevel = 0
        var headerStart: Int? = null
        
        while (i < len) {
            // 1. Start of line processing (Headers and Lists)
            if (isStartOfLine) {
                var spaceCount = 0
                while (i + spaceCount < len && original[i + spaceCount] == ' ') {
                    spaceCount++
                }
                
                val contentStart = i + spaceCount
                
                // Headers
                var hashCount = 0
                while (contentStart + hashCount < len && original[contentStart + hashCount] == '#') {
                    hashCount++
                }
                if (hashCount > 0 && hashCount <= 6 && contentStart + hashCount < len && original[contentStart + hashCount] == ' ') {
                    isStartOfLine = false
                    for (k in 0 until spaceCount) {
                        origToTrans[i + k] = transformed.length
                        transToOrig.add(i + k)
                        transformed.append(' ')
                    }
                    val skipCount = hashCount + 1
                    for (k in 0 until skipCount) {
                        origToTrans[contentStart + k] = transformed.length
                    }
                    i = contentStart + skipCount
                    currentHeaderLevel = hashCount
                    headerStart = transformed.length
                    continue
                }
                
                // Bullet list check
                if (contentStart + 2 < len && original[contentStart] == '-' && original[contentStart + 1] == '-' && original[contentStart + 2] == ' ') {
                    isStartOfLine = false
                    for (k in 0 until spaceCount) {
                        origToTrans[i + k] = transformed.length
                        transToOrig.add(i + k)
                        transformed.append(' ')
                    }
                    
                    origToTrans[contentStart] = transformed.length
                    transToOrig.add(contentStart)
                    transformed.append(' ')
                    
                    origToTrans[contentStart + 1] = transformed.length
                    transToOrig.add(contentStart + 1)
                    transformed.append('○')
                    
                    origToTrans[contentStart + 2] = transformed.length
                    transToOrig.add(contentStart + 2)
                    transformed.append(' ')
                    
                    i = contentStart + 3
                    continue
                }
                
                if (contentStart + 1 < len && original[contentStart] == '-' && original[contentStart + 1] == ' ') {
                    isStartOfLine = false
                    for (k in 0 until spaceCount) {
                        origToTrans[i + k] = transformed.length
                        transToOrig.add(i + k)
                        transformed.append(' ')
                    }
                    
                    origToTrans[contentStart] = transformed.length
                    transToOrig.add(contentStart)
                    transformed.append('•')
                    
                    origToTrans[contentStart + 1] = transformed.length
                    transToOrig.add(contentStart + 1)
                    transformed.append(' ')
                    
                    i = contentStart + 2
                    continue
                }
                
                if (contentStart < len) {
                    isStartOfLine = false
                }
            }
            
            // 2. Bold / Italic / Code / Strikethrough processing
            if (i < len - 1 && original[i] == '*' && original[i + 1] == '*') {
                origToTrans[i] = transformed.length
                origToTrans[i + 1] = transformed.length
                i += 2
                if (boldStart == null) {
                    boldStart = transformed.length
                } else {
                    styles.add(AnnotatedStyleRange(SpanStyle(fontWeight = FontWeight.Bold, color = primaryColor), boldStart, transformed.length))
                    boldStart = null
                }
                continue
            }
            
            if (i < len - 1 && original[i] == '_' && original[i + 1] == '_') {
                origToTrans[i] = transformed.length
                origToTrans[i + 1] = transformed.length
                i += 2
                if (boldStart == null) {
                    boldStart = transformed.length
                } else {
                    styles.add(AnnotatedStyleRange(SpanStyle(fontWeight = FontWeight.Bold, color = primaryColor), boldStart, transformed.length))
                    boldStart = null
                }
                continue
            }
            
            if (original[i] == '*') {
                origToTrans[i] = transformed.length
                i += 1
                if (italicStart == null) {
                    italicStart = transformed.length
                } else {
                    styles.add(AnnotatedStyleRange(SpanStyle(fontStyle = FontStyle.Italic), italicStart, transformed.length))
                    italicStart = null
                }
                continue
            }
            
            if (original[i] == '_') {
                origToTrans[i] = transformed.length
                i += 1
                if (italicStart == null) {
                    italicStart = transformed.length
                } else {
                    styles.add(AnnotatedStyleRange(SpanStyle(fontStyle = FontStyle.Italic), italicStart, transformed.length))
                    italicStart = null
                }
                continue
            }
            
            if (i < len - 1 && original[i] == '~' && original[i + 1] == '~') {
                origToTrans[i] = transformed.length
                origToTrans[i + 1] = transformed.length
                i += 2
                if (strikeStart == null) {
                    strikeStart = transformed.length
                } else {
                    styles.add(AnnotatedStyleRange(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough), strikeStart, transformed.length))
                    strikeStart = null
                }
                continue
            }
            
            if (original[i] == '`') {
                origToTrans[i] = transformed.length
                i += 1
                if (codeStart == null) {
                    codeStart = transformed.length
                } else {
                    styles.add(AnnotatedStyleRange(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = if (isDark) Color(0xFF2D2D2D) else Color(0xFFF0F0F0),
                            color = if (isDark) Color(0xFFE0E0E0) else Color(0xFF333333)
                        ),
                        codeStart,
                        transformed.length
                    ))
                    codeStart = null
                }
                continue
            }
            
            // 3. Regular character copy
            val c = original[i]
            origToTrans[i] = transformed.length
            transToOrig.add(i)
            transformed.append(c)
            
            if (c == '\n') {
                isStartOfLine = true
                if (headerStart != null) {
                    val headerSize = when (currentHeaderLevel) {
                        1 -> 24.sp
                        2 -> 20.sp
                        3 -> 18.sp
                        else -> 16.sp
                    }
                    styles.add(AnnotatedStyleRange(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = headerSize,
                            color = primaryColor
                        ),
                        headerStart,
                        transformed.length - 1
                    ))
                    headerStart = null
                    currentHeaderLevel = 0
                }
            }
            
            i++
        }
        
        if (headerStart != null) {
            val headerSize = when (currentHeaderLevel) {
                1 -> 24.sp
                2 -> 20.sp
                3 -> 18.sp
                else -> 16.sp
            }
            styles.add(AnnotatedStyleRange(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = headerSize,
                    color = primaryColor
                ),
                headerStart,
                transformed.length
            ))
        }
        if (boldStart != null) {
            styles.add(AnnotatedStyleRange(SpanStyle(fontWeight = FontWeight.Bold, color = primaryColor), boldStart, transformed.length))
        }
        if (italicStart != null) {
            styles.add(AnnotatedStyleRange(SpanStyle(fontStyle = FontStyle.Italic), italicStart, transformed.length))
        }
        if (strikeStart != null) {
            styles.add(AnnotatedStyleRange(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough), strikeStart, transformed.length))
        }
        if (codeStart != null) {
            styles.add(AnnotatedStyleRange(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = if (isDark) Color(0xFF2D2D2D) else Color(0xFFF0F0F0),
                    color = if (isDark) Color(0xFFE0E0E0) else Color(0xFF333333)
                ),
                codeStart,
                transformed.length
            ))
        }
        
        origToTrans[original.length] = transformed.length
        transToOrig.add(original.length)
        
        val annotatedString = buildAnnotatedString {
            append(transformed.toString())
            styles.forEach { range ->
                addStyle(range.style, range.start, range.end)
            }
        }
        
        val offsetMapping = object : androidx.compose.ui.text.input.OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, original.length)
                return origToTrans[clamped]
            }
            
            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, transformed.length)
                return transToOrig[clamped]
            }
        }
        
        return TransformedText(annotatedString, offsetMapping)
    }
}

private data class AnnotatedStyleRange(val style: SpanStyle, val start: Int, val end: Int)

fun updateBlockInContent(content: String, id: String, data: DrawingData): String {
    val regex = Regex("```drawing\\n(\\{.*?\\})\\n```", RegexOption.DOT_MATCHES_ALL)
    return regex.replace(content) { result ->
        val jsonStr = result.groupValues[1]
        try {
            val d = DrawingData.fromJson(jsonStr)
            if (d?.id == id) {
                "```drawing\n${data.toJson()}\n```"
            } else {
                result.value
            }
        } catch (e: Exception) {
            result.value
        }
    }
}

