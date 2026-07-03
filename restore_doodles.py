import os

code = """
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
    val regex = Regex("```drawing\\\\n(\\\\{.*?\\\\})\\\\n```", RegexOption.DOT_MATCHES_ALL)
    return regex.replace(content) { result ->
        val jsonStr = result.groupValues[1]
        try {
            val d = DrawingData.fromJson(jsonStr)
            if (d?.id == id) {
                "```drawing\\n${data.toJson()}\\n```"
            } else {
                result.value
            }
        } catch (e: Exception) {
            result.value
        }
    }
}

"""

with open('app/src/main/java/com/usoy/papiro/ui/components/DrawingComponents.kt', 'r') as f:
    orig = f.read()

# Make sure we don't duplicate
if "fun CanvasCreatorDialog" not in orig:
    # First, let's remove that dangling `@OptIn(ExperimentalMaterial3Api::class)` at the end
    if orig.strip().endswith("@OptIn(ExperimentalMaterial3Api::class)"):
        orig = orig.strip()[:-len("@OptIn(ExperimentalMaterial3Api::class)")]
    
    with open('app/src/main/java/com/usoy/papiro/ui/components/DrawingComponents.kt', 'w') as f:
        f.write(orig + "\n" + code)

