package com.usoy.papiro.ui.components

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.*
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Properties
    enum class GridType { NONE, GRID, DOTS, RULED }
    var currentGridType: GridType = GridType.NONE
        set(value) {
            field = value
            invalidate()
        }

    var isFingerPaintingEnabled: Boolean = true
    var isSmartShapeEnabled: Boolean = false
    
    private var currentPaintColor = Color.WHITE
    private var currentStrokeWidth = 10f // internally stored in pixels
    var isEraserMode = false

    private var isCurrentTouchStylus = false
    private var drawingSessionId = 0

    fun getStrokeWidth(): Float {
        val density = resources.displayMetrics.density
        return if (density > 0) currentStrokeWidth / density else currentStrokeWidth
    }

    fun setStrokeWidth(widthDp: Float) {
        currentStrokeWidth = widthDp * resources.displayMetrics.density
        invalidate()
    }

    // Brush preview size indicator fields
    private var showTouchIndicator = false
    private var touchX = 0f
    private var touchY = 0f
    private val previewPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    private val previewOutlinePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    // Paint for drawing active lines
    private val drawPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    // List of drawn paths with their properties
    val paths = mutableListOf<DrawnPath>()
    private val redoPaths = mutableListOf<DrawnPath>()

    // Current drawing path
    private var currentPath: Path? = null
    private var currentPoints = mutableListOf<PointF>()
    private var lastX = 0f
    private var lastY = 0f
    private var lastRawY = 0f
    private val touchTolerance = 4f

    // Canvas Background Color (dark, e.g. #121212)
    var canvasBackgroundColor = Color.parseColor("#121212")

    // ML Kit Digital Ink Recognition
    private var recognizer: DigitalInkRecognizer? = null
    private var model: DigitalInkRecognitionModel? = null

    init {
        setBackgroundColor(canvasBackgroundColor)
        setupRecognizer()
        currentStrokeWidth *= resources.displayMetrics.density
    }

    private fun setupRecognizer() {
        try {
            val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("zxx-Zsym-x-autodraw")
            if (modelIdentifier != null) {
                val m = DigitalInkRecognitionModel.builder(modelIdentifier).build()
                model = m
                val recognizerOptions = DigitalInkRecognizerOptions.builder(m).build()
                recognizer = DigitalInkRecognition.getClient(recognizerOptions)
                
                // Download the model if not present
                val remoteModelManager = RemoteModelManager.getInstance()
                model?.let { m ->
                    remoteModelManager.download(m, DownloadConditions.Builder().build())
                        .addOnSuccessListener {
                            // Model downloaded successfully
                        }
                        .addOnFailureListener {
                            // Fallback will be used
                        }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setStrokeColor(color: Int) {
        currentPaintColor = color
        isEraserMode = false
    }

    fun getStrokeColor(): Int = currentPaintColor

    fun undo() {
        if (paths.isNotEmpty()) {
            val removed = paths.removeAt(paths.size - 1)
            redoPaths.add(removed)
            invalidate()
        }
    }

    fun redo() {
        if (redoPaths.isNotEmpty()) {
            val restored = redoPaths.removeAt(redoPaths.size - 1)
            paths.add(restored)
            invalidate()
        }
    }

    fun clear() {
        drawingSessionId++
        paths.clear()
        redoPaths.clear()
        currentPath = null
        currentPoints.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw guidelines if enabled
        if (currentGridType != GridType.NONE) {
            val density = resources.displayMetrics.density
            val isDark = (Color.red(canvasBackgroundColor) * 0.299f +
                          Color.green(canvasBackgroundColor) * 0.587f +
                          Color.blue(canvasBackgroundColor) * 0.114f) < 128f
            val gridColorStr = if (isDark) "#1538BDF8" else "#200284C7"
            val dotColorStr = if (isDark) "#2038BDF8" else "#250284C7"

            val paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                color = Color.parseColor(gridColorStr)
            }
            when (currentGridType) {
                GridType.GRID -> {
                    val stepPx = 28f * density
                    paint.strokeWidth = 1f * density
                    var y = 0f
                    while (y < height) {
                        canvas.drawLine(0f, y, width.toFloat(), y, paint)
                        y += stepPx
                    }
                    var x = 0f
                    while (x < width) {
                        canvas.drawLine(x, 0f, x, height.toFloat(), paint)
                        x += stepPx
                    }
                }
                GridType.RULED -> {
                    val stepPx = 32f * density
                    paint.strokeWidth = 1f * density
                    var y = 64f * density
                    while (y < height) {
                        canvas.drawLine(0f, y, width.toFloat(), y, paint)
                        y += stepPx
                    }
                }
                GridType.DOTS -> {
                    val stepPx = 24f * density
                    paint.style = Paint.Style.FILL
                    paint.color = Color.parseColor(dotColorStr)
                    val radius = 1.5f * density
                    var y = stepPx / 2
                    while (y < height) {
                        var x = stepPx / 2
                        while (x < width) {
                            canvas.drawCircle(x, y, radius, paint)
                            x += stepPx
                        }
                        y += stepPx
                    }
                }
                GridType.NONE -> {}
            }
        }

        // Draw existing paths
        for (drawnPath in paths) {
            canvas.drawPath(drawnPath.path, drawnPath.paint)
        }

        // Draw active path
        currentPath?.let {
            drawPaint.color = if (isEraserMode) canvasBackgroundColor else currentPaintColor
            drawPaint.strokeWidth = currentStrokeWidth
            canvas.drawPath(it, drawPaint)
        }

        // Draw brush size preview indicator if touch is active and finger painting (or stylus) is on
        if (showTouchIndicator && (isFingerPaintingEnabled || isCurrentTouchStylus)) {
            val radius = currentStrokeWidth / 2f
            
            // Semi-transparent solid fill circle indicating brush size
            val baseColor = if (isEraserMode) Color.GRAY else currentPaintColor
            val fillAlpha = 90 // ~35% opacity
            previewPaint.color = Color.argb(
                fillAlpha,
                Color.red(baseColor),
                Color.green(baseColor),
                Color.blue(baseColor)
            )
            canvas.drawCircle(touchX, touchY, radius, previewPaint)
            
            // Clean high-contrast outline
            val isDark = (Color.red(canvasBackgroundColor) * 0.299f +
                          Color.green(canvasBackgroundColor) * 0.587f +
                          Color.blue(canvasBackgroundColor) * 0.114f) < 128f
            previewOutlinePaint.color = if (isDark) Color.WHITE else Color.BLACK
            previewOutlinePaint.alpha = 180
            previewOutlinePaint.strokeWidth = 1f * resources.displayMetrics.density
            canvas.drawCircle(touchX, touchY, radius, previewOutlinePaint)
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!hasWindowFocus) {
            showTouchIndicator = false
            invalidate()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        showTouchIndicator = false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val x = event.x
        val y = event.y

        // Detect if the active pointer is a stylus or finger
        val toolType = if (event.pointerCount > 0) event.getToolType(0) else MotionEvent.TOOL_TYPE_UNKNOWN
        isCurrentTouchStylus = toolType == MotionEvent.TOOL_TYPE_STYLUS || toolType == MotionEvent.TOOL_TYPE_ERASER

        // Restrict drawing to inside left and right margins (16dp gutter) to prevent drawing on the outer edges
        val margin = 16f * resources.displayMetrics.density
        val minAllowedX = margin
        val maxAllowedX = width - margin

        // Stylus can always draw even if finger drawing is disabled!
        val isDrawingAllowed = isFingerPaintingEnabled || isCurrentTouchStylus

        if (!isDrawingAllowed) {
            showTouchIndicator = false
            parent?.requestDisallowInterceptTouchEvent(false)
            return false
        }

        // Clamped X coordinate to keep drawing strictly within the canvas/screen boundaries
        val clampedX = x.coerceIn(minAllowedX, maxAllowedX)

        // Update brush preview location and visibility safely using masked actions
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            showTouchIndicator = true
            touchX = clampedX
            touchY = y
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_POINTER_UP) {
            showTouchIndicator = false
        }

        // Disallow ScrollView intercepting touches
        parent?.requestDisallowInterceptTouchEvent(true)

        // Check if drawing near the bottom edge, expand view height if so
        if (action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_DOWN) {
            if (y > height - 150f) {
                val newHeight = height + 300
                layoutParams?.let {
                    it.height = newHeight
                    requestLayout()
                }
            }
        }

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                redoPaths.clear()
                val path = Path()
                path.moveTo(clampedX, y)
                currentPath = path
                currentPoints.clear()
                currentPoints.add(PointF(clampedX, y))
                lastX = clampedX
                lastY = y
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(clampedX - lastX)
                val dy = abs(y - lastY)
                if (dx >= touchTolerance || dy >= touchTolerance) {
                    currentPath?.quadTo(lastX, lastY, (clampedX + lastX) / 2, (y + lastY) / 2)
                    currentPoints.add(PointF(clampedX, y))
                    lastX = clampedX
                    lastY = y
                }
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // If they did a quick tap, force drawing a clean dot
                if (currentPoints.size == 1) {
                    // Force a tiny line segment (0.5dp) to render a perfect circular cap dot
                    val tinyShift = 0.5f * resources.displayMetrics.density
                    currentPath?.lineTo(clampedX + tinyShift, y)
                    currentPoints.add(PointF(clampedX + tinyShift, y))
                } else {
                    currentPath?.lineTo(clampedX, y)
                    currentPoints.add(PointF(clampedX, y))
                }
                
                val finalPath = currentPath
                if (finalPath != null && currentPoints.size > 1) {
                    val paint = Paint().apply {
                        isAntiAlias = true
                        isDither = true
                        style = Paint.Style.STROKE
                        strokeJoin = Paint.Join.ROUND
                        strokeCap = Paint.Cap.ROUND
                        color = if (isEraserMode) canvasBackgroundColor else currentPaintColor
                        strokeWidth = currentStrokeWidth
                    }

                    val drawnPath = DrawnPath(finalPath, paint, isEraserMode, currentPoints.toList())
                    
                    if (isSmartShapeEnabled && !isEraserMode) {
                        // Attempt to recognize and replace with a smart shape
                        recognizeSmartShape(drawnPath)
                    } else {
                        paths.add(drawnPath)
                        invalidate()
                    }
                }
                currentPath = null
                currentPoints.clear()
            }
        }
        return true
    }

    private fun recognizeSmartShape(drawnPath: DrawnPath) {
        val points = drawnPath.points
        if (points.size < 5) {
            paths.add(drawnPath)
            invalidate()
            return
        }

        val capturedSessionId = drawingSessionId
        if (recognizer != null && model != null) {
            val inkBuilder = Ink.builder()
            val strokeBuilder = Ink.Stroke.builder()
            var time = 0L
            for (p in points) {
                strokeBuilder.addPoint(Ink.Point.create(p.x, p.y, time))
                time += 10
            }
            inkBuilder.addStroke(strokeBuilder.build())
            val ink = inkBuilder.build()

            recognizer?.recognize(ink)
                ?.addOnSuccessListener { result ->
                    // Guard against canvas clear or model reset while processing
                    if (capturedSessionId != drawingSessionId) return@addOnSuccessListener
                    
                    val candidates = result.candidates
                    if (candidates.isNotEmpty()) {
                        val topCandidate = candidates[0].text.lowercase()
                        val replaced = tryToReplaceWithSmartShape(topCandidate, points, drawnPath.paint)
                        if (replaced) {
                            invalidate()
                            return@addOnSuccessListener
                        }
                    }
                    // If ML Kit didn't recognize or replace successfully, run heuristic fallback
                    runHeuristicRecognizer(drawnPath)
                }
                ?.addOnFailureListener {
                    if (capturedSessionId == drawingSessionId) {
                        // Fallback to heuristic
                        runHeuristicRecognizer(drawnPath)
                    }
                }
        } else {
            // No ML Kit recognizer, run heuristic
            runHeuristicRecognizer(drawnPath)
        }
    }

    private fun runHeuristicRecognizer(drawnPath: DrawnPath) {
        val points = drawnPath.points
        val heuristicShape = analyzeHeuristicShape(points)
        if (heuristicShape != null) {
            val replaced = tryToReplaceWithSmartShape(heuristicShape, points, drawnPath.paint)
            if (replaced) {
                invalidate()
                return
            }
        }
        paths.add(drawnPath)
        invalidate()
    }

    private fun tryToReplaceWithSmartShape(shapeName: String, points: List<PointF>, paint: Paint): Boolean {
        // Calculate bounding box of the drawn points
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE

        for (p in points) {
            if (p.x < minX) minX = p.x
            if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y
            if (p.y > maxY) maxY = p.y
        }

        val cx = (minX + maxX) / 2
        val cy = (minY + maxY) / 2
        val w = maxX - minX
        val h = maxY - minY

        val shapePath = Path()
        val shapePoints = mutableListOf<PointF>()

        when {
            shapeName.contains("circle") || shapeName.contains("oval") -> {
                val radius = (w + h) / 4
                shapePath.addCircle(cx, cy, radius, Path.Direction.CW)

                // Generate 72 smooth points (5-degree increments) around the circle/oval
                val rx = w / 2f
                val ry = h / 2f
                val numPoints = 72
                for (i in 0..numPoints) {
                    val angle = (2f * Math.PI * i / numPoints).toFloat()
                    val px = cx + rx * kotlin.math.cos(angle)
                    val py = cy + ry * kotlin.math.sin(angle)
                    shapePoints.add(PointF(px, py))
                }
            }
            shapeName.contains("rectangle") || shapeName.contains("square") || shapeName.contains("box") -> {
                shapePath.addRect(minX, minY, maxX, maxY, Path.Direction.CW)

                // Generate points along the edges to support eraser/interaction smoothly
                val numPointsPerEdge = 15
                // Top edge
                for (i in 0 until numPointsPerEdge) {
                    val f = i.toFloat() / numPointsPerEdge
                    shapePoints.add(PointF(minX + f * w, minY))
                }
                // Right edge
                for (i in 0 until numPointsPerEdge) {
                    val f = i.toFloat() / numPointsPerEdge
                    shapePoints.add(PointF(maxX, minY + f * h))
                }
                // Bottom edge
                for (i in 0 until numPointsPerEdge) {
                    val f = i.toFloat() / numPointsPerEdge
                    shapePoints.add(PointF(maxX - f * w, maxY))
                }
                // Left edge
                for (i in 0..numPointsPerEdge) {
                    val f = i.toFloat() / numPointsPerEdge
                    shapePoints.add(PointF(minX, maxY - f * h))
                }
            }
            shapeName.contains("triangle") -> {
                shapePath.moveTo(cx, minY)
                shapePath.lineTo(minX, maxY)
                shapePath.lineTo(maxX, maxY)
                shapePath.close()

                // Generate points along the triangle boundaries
                val numPointsPerEdge = 20
                // Edge 1: Top to Bottom-Left
                for (i in 0 until numPointsPerEdge) {
                    val f = i.toFloat() / numPointsPerEdge
                    shapePoints.add(PointF(cx + f * (minX - cx), minY + f * h))
                }
                // Edge 2: Bottom-Left to Bottom-Right
                for (i in 0 until numPointsPerEdge) {
                    val f = i.toFloat() / numPointsPerEdge
                    shapePoints.add(PointF(minX + f * w, maxY))
                }
                // Edge 3: Bottom-Right to Top
                for (i in 0..numPointsPerEdge) {
                    val f = i.toFloat() / numPointsPerEdge
                    shapePoints.add(PointF(maxX + f * (cx - maxX), maxY - f * h))
                }
            }
            shapeName.contains("line") || shapeName.contains("straight") || shapeName.contains("arrow") -> {
                val start = points.first()
                val end = points.last()
                shapePath.moveTo(start.x, start.y)
                shapePath.lineTo(end.x, end.y)

                // Generate points along the line segment
                val numPoints = 30
                for (i in 0..numPoints) {
                    val f = i.toFloat() / numPoints
                    shapePoints.add(PointF(start.x + f * (end.x - start.x), start.y + f * (end.y - start.y)))
                }
            }
            else -> return false
        }

        paths.add(DrawnPath(shapePath, paint, false, shapePoints))
        return true
    }

    private fun analyzeHeuristicShape(points: List<PointF>): String? {
        if (points.size < 5) return null

        val start = points.first()
        val end = points.last()

        // 1. Check if it is a Straight Line
        var totalLength = 0f
        for (i in 0 until points.size - 1) {
            totalLength += hypot(points[i+1].x - points[i].x, points[i+1].y - points[i].y)
        }
        val directDistance = hypot(end.x - start.x, end.y - start.y)
        if (totalLength > 0 && (directDistance / totalLength) > 0.93) {
            return "line"
        }

        // Calculate bounding box bounds
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE
        for (p in points) {
            minX = min(minX, p.x)
            maxX = max(maxX, p.x)
            minY = min(minY, p.y)
            maxY = max(maxY, p.y)
        }
        val cx = (minX + maxX) / 2
        val cy = (minY + maxY) / 2
        val w = maxX - minX
        val h = maxY - minY

        // Check if closed (or start and end are reasonably close compared to size)
        val isClosed = directDistance < max(w, h) * 0.3f

        if (isClosed) {
            // 2. Check if Circle
            val radius = (w + h) / 4
            var varianceSum = 0f
            for (p in points) {
                val dist = hypot(p.x - cx, p.y - cy)
                varianceSum += abs(dist - radius)
            }
            val averageVariance = varianceSum / points.size
            if (averageVariance / radius < 0.18f) {
                return "circle"
            }

            // 3. Check if Triangle or Rectangle
            if (w > 10 && h > 10) {
                var nearTopLeft = false
                var nearTopRight = false
                val thresholdX = w * 0.2f
                val thresholdY = h * 0.2f
                
                for (p in points) {
                    if (p.x < minX + thresholdX && p.y < minY + thresholdY) nearTopLeft = true
                    if (p.x > maxX - thresholdX && p.y < minY + thresholdY) nearTopRight = true
                }
                
                return if (nearTopLeft && nearTopRight) {
                    "rectangle"
                } else {
                    "triangle"
                }
            }
        }

        return null
    }

    fun getCroppedBitmap(): Bitmap? {
        if (paths.isEmpty()) return null

        val bounds = RectF()
        var hasBounds = false

        for (dp in paths) {
            if (dp.isEraser) continue // Skip erasers for crop
            val pathBounds = RectF()
            dp.path.computeBounds(pathBounds, true)
            if (!hasBounds) {
                bounds.set(pathBounds)
                hasBounds = true
            } else {
                bounds.union(pathBounds)
            }
        }

        if (!hasBounds) return null

        // Add 20px padding
        val padding = 20f
        val left = max(0f, bounds.left - padding)
        val top = max(0f, bounds.top - padding)
        val right = min(width.toFloat(), bounds.right + padding)
        val bottom = min(height.toFloat(), bounds.bottom + padding)

        val cropWidth = (right - left).toInt()
        val cropHeight = (bottom - top).toInt()

        if (cropWidth <= 0 || cropHeight <= 0) return null

        val croppedBitmap = Bitmap.createBitmap(cropWidth, cropHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(croppedBitmap)
        
        // Fill canvas with transparent background so it looks perfect in both themes
        canvas.drawColor(Color.TRANSPARENT)

        canvas.translate(-left, -top)

        for (dp in paths) {
            canvas.drawPath(dp.path, dp.paint)
        }

        return croppedBitmap
    }
}

data class DrawnPath(
    val path: Path,
    val paint: Paint,
    val isEraser: Boolean,
    val points: List<PointF>
)
