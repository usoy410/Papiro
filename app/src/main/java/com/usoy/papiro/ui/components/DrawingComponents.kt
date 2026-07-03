package com.usoy.papiro.ui.components

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import org.json.JSONArray
import org.json.JSONObject

data class Point(val x: Float, val y: Float)

data class DrawPath(
    val color: Int,
    val strokeWidth: Float,
    val points: List<Point>
)

data class DrawingData(
    val id: String,
    val width: Int,
    val height: Int,
    val paths: List<DrawPath>
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("id", id)
        json.put("width", width)
        json.put("height", height)
        val pathsArray = JSONArray()
        for (path in paths) {
            val pathObj = JSONObject()
            pathObj.put("color", path.color)
            pathObj.put("strokeWidth", path.strokeWidth.toDouble())
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
        fun fromJson(jsonStr: String): DrawingData? {
            return try {
                val json = JSONObject(jsonStr)
                val id = json.getString("id")
                val width = json.getInt("width")
                val height = json.getInt("height")
                val pathsArray = json.getJSONArray("paths")
                val paths = mutableListOf<DrawPath>()
                for (i in 0 until pathsArray.length()) {
                    val pathObj = pathsArray.getJSONObject(i)
                    val color = pathObj.getInt("color")
                    val strokeWidth = pathObj.getDouble("strokeWidth").toFloat()
                    val pointsArray = pathObj.getJSONArray("points")
                    val points = mutableListOf<Point>()
                    for (j in 0 until pointsArray.length()) {
                        val ptObj = pointsArray.getJSONObject(j)
                        val x = ptObj.getDouble("x").toFloat()
                        val y = ptObj.getDouble("y").toFloat()
                        points.add(Point(x, y))
                    }
                    paths.add(DrawPath(color, strokeWidth, points))
                }
                DrawingData(id, width, height, paths)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasCreatorDialog(
    onDismiss: () -> Unit,
    onConfirm: (width: Int, height: Int) -> Unit
) {
    var widthInput by remember { mutableStateOf("320") }
    var heightInput by remember { mutableStateOf("240") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Canvas Size") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = widthInput,
                    onValueChange = { widthInput = it },
                    label = { Text("Width") }
                )
                OutlinedTextField(
                    value = heightInput,
                    onValueChange = { heightInput = it },
                    label = { Text("Height") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val w = widthInput.toIntOrNull() ?: 320
                val h = heightInput.toIntOrNull() ?: 240
                onConfirm(w, h)
            }) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenDrawingEditor(
    width: Int,
    height: Int,
    initialPaths: List<DrawPath>,
    onDismiss: () -> Unit,
    onSave: (List<DrawPath>) -> Unit
) {
    var paths by remember { mutableStateOf(initialPaths) }
    var currentPath by remember { mutableStateOf<List<Point>?>(null) }
    
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column {
                TopAppBar(
                    title = { Text("Drawing Editor") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Clear, "Close") }
                    },
                    actions = {
                        IconButton(onClick = { paths = emptyList() }) {
                            Icon(Icons.Default.HighlightOff, "Clear")
                        }
                        IconButton(onClick = {
                            onSave(paths)
                        }) {
                            Icon(Icons.Default.Create, "Save")
                        }
                    }
                )
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(width.dp, height.dp)
                            .background(Color.White)
                            .border(1.dp, Color.Gray)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentPath = listOf(Point(offset.x, offset.y))
                                    },
                                    onDrag = { change, _ ->
                                        currentPath = currentPath?.plus(Point(change.position.x, change.position.y))
                                    },
                                    onDragEnd = {
                                        currentPath?.let {
                                            paths = paths + DrawPath(android.graphics.Color.BLACK, 4f, it)
                                        }
                                        currentPath = null
                                    },
                                    onDragCancel = {
                                        currentPath = null
                                    }
                                )
                            }
                    ) {
                        paths.forEach { drawP ->
                            if (drawP.points.size > 1) {
                                val p = Path().apply {
                                    moveTo(drawP.points.first().x, drawP.points.first().y)
                                    for (i in 1 until drawP.points.size) {
                                        lineTo(drawP.points[i].x, drawP.points[i].y)
                                    }
                                }
                                drawPath(p, Color(drawP.color), style = Stroke(drawP.strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                            }
                        }
                        currentPath?.let { cPath ->
                            if (cPath.size > 1) {
                                val p = Path().apply {
                                    moveTo(cPath.first().x, cPath.first().y)
                                    for (i in 1 until cPath.size) {
                                        lineTo(cPath[i].x, cPath[i].y)
                                    }
                                }
                                drawPath(p, Color.Black, style = Stroke(4f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                            }
                        }
                    }
                }
            }
        }
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
                    .background(Color.White)
                    .border(1.dp, Color.LightGray)
                    .clickable { onEditClick(drawingData) }
            ) {
                drawingData.paths.forEach { drawP ->
                    if (drawP.points.size > 1) {
                        val p = Path().apply {
                            moveTo(drawP.points.first().x, drawP.points.first().y)
                            for (i in 1 until drawP.points.size) {
                                lineTo(drawP.points[i].x, drawP.points[i].y)
                            }
                        }
                        drawPath(p, Color(drawP.color), style = Stroke(drawP.strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                    }
                }
            }
        }
    }
}

class MarkdownVisualTransformation(val isDark: Boolean, val primaryColor: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val spanStyles = mutableListOf<AnnotatedString.Range<SpanStyle>>()
        val out = buildAnnotatedString {
            append(text.text)
            // Just simple placeholder style
            if (text.text.startsWith("#")) {
                addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = primaryColor), 0, text.text.length)
            }
        }
        return TransformedText(out, androidx.compose.ui.text.input.OffsetMapping.Identity)
    }
}

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

