package com.usoy.papiro.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.usoy.papiro.ui.components.NotebookBackground
import com.usoy.papiro.ui.components.NotebookBackgroundType
import com.usoy.papiro.ui.components.MarkdownRenderer
import com.usoy.papiro.ui.components.CanvasCreatorDialog
import com.usoy.papiro.ui.components.FullscreenDrawingEditor
import com.usoy.papiro.ui.components.DrawingData
import com.usoy.papiro.ui.components.DrawPath
import com.usoy.papiro.ui.components.MarkdownVisualTransformation
import com.usoy.papiro.ui.components.updateBlockInContent
import com.usoy.papiro.viewmodel.NoteViewModel
import java.io.ByteArrayOutputStream
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentNote by viewModel.currentNote.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val isOcrRunning by viewModel.isOcrRunning.collectAsState()

    // Editor state
    var title by remember { mutableStateOf("") }
    var contentValue by remember { mutableStateOf(TextFieldValue("")) }
    var backgroundType by remember { mutableStateOf(NotebookBackgroundType.GRID) }
    var isEditing by remember { mutableStateOf(false) }
    var showWrenchTools by remember { mutableStateOf(false) }
    var isTyping by remember { mutableStateOf(false) }

    // Drawing tool states
    var showCanvasCreator by remember { mutableStateOf(false) }
    var showDrawingEditor by remember { mutableStateOf(false) }
    var drawingWidth by remember { mutableIntStateOf(320) }
    var drawingHeight by remember { mutableIntStateOf(240) }
    var currentEditingDrawing by remember { mutableStateOf<DrawingData?>(null) }

    // Dialog state for AI generator
    var showAiDialog by remember { mutableStateOf(false) }
    var aiTopic by remember { mutableStateOf("") }

    // Dialog state for Formatting Help Guide
    var showHelpDialog by remember { mutableStateOf(false) }

    val anchorPositions = remember { mutableMapOf<String, androidx.compose.ui.layout.LayoutCoordinates>() }
    var readerContentCoords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Undo / Redo Manager
    val undoRedoManager = remember { UndoRedoManager(contentValue) }
    
    var showHistoryDialog by remember { mutableStateOf(false) }

    val historySnapshots by remember(currentNote) {
        val id = currentNote?.id ?: 0L
        if (id != 0L) {
            viewModel.getHistoryForNote(id)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    // Milestone undo snapshot recorder using UndoRedoManager
    LaunchedEffect(contentValue.text) {
        undoRedoManager.saveState(contentValue)
    }

    // Initialize editor fields when a note is loaded
    LaunchedEffect(currentNote) {
        currentNote?.let {
            title = it.title
            contentValue = TextFieldValue(it.content)
            backgroundType = try {
                NotebookBackgroundType.valueOf(it.backgroundType)
            } catch (e: Exception) {
                NotebookBackgroundType.GRID
            }
            isEditing = it.content.isEmpty() && it.title.isEmpty()
            undoRedoManager.reset(contentValue)
        }
    }

    // Media and OCR picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                    val bytes = baos.toByteArray()

                    viewModel.extractTextFromImage(bytes) { text ->
                        // Append OCR extracted text to the bottom of the content
                        val currentText = contentValue.text
                        val prefix = if (currentText.isNotEmpty()) {
                            if (currentText.endsWith("\n")) "\n" else "\n\n"
                        } else {
                            ""
                        }
                        val newText = currentText + prefix + text
                        contentValue = TextFieldValue(
                            text = newText,
                            selection = androidx.compose.ui.text.TextRange(newText.length)
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Dynamic photo/image import and cache launcher
    val picturePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val cacheFile = java.io.File(context.cacheDir, "imported_img_${System.currentTimeMillis()}.jpg")
                    val outputStream = java.io.FileOutputStream(cacheFile)
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()
                    
                    val path = cacheFile.absolutePath
                    val imageMarkdown = "\n![Image]($path)\n"
                    val currentText = contentValue.text
                    val cursorPosition = contentValue.selection.start
                    val newText = StringBuilder(currentText)
                        .insert(cursorPosition, imageMarkdown)
                        .toString()
                    contentValue = TextFieldValue(
                        text = newText,
                        selection = androidx.compose.ui.text.TextRange(cursorPosition + imageMarkdown.length)
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Draft Sheet" else "Blueprint View",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveNote(title, contentValue.text, backgroundType.name)
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Save and go back",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                actions = {
                    // Notebook theme changer
                    IconButton(
                        onClick = {
                            backgroundType = when (backgroundType) {
                                NotebookBackgroundType.GRID -> NotebookBackgroundType.RULED
                                NotebookBackgroundType.RULED -> NotebookBackgroundType.DOTS
                                NotebookBackgroundType.DOTS -> NotebookBackgroundType.BLANK
                                NotebookBackgroundType.BLANK -> NotebookBackgroundType.GRID
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "Switch paper type",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Edit / Save toggle action
                    if (isEditing) {
                        IconButton(onClick = { showHistoryDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        IconButton(onClick = { showHelpDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Markdown Guide",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (isEditing) {
                                viewModel.saveNote(title, contentValue.text, backgroundType.name)
                                isEditing = false
                            } else {
                                isEditing = true
                            }
                        },
                        modifier = Modifier.testTag(if (isEditing) "save_note_button" else "edit_note_button")
                    ) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Save else Icons.Default.Edit,
                            contentDescription = if (isEditing) "Save note" else "Edit note",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            // Toolbar accessories (bold, italic, tags, quick symbols)
            if (isEditing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    if (showWrenchTools) {
                        EngineeringToolsRow(
                            onAiClick = { showAiDialog = true },
                            onOcrClick = { imagePickerLauncher.launch("image/*") },
                            onDrawClick = { showCanvasCreator = true },
                            onImageImportClick = { picturePickerLauncher.launch("image/*") },
                            viewModel = viewModel
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }

                    FormattingToolbar(
                        onFormatAction = { action ->
                            undoRedoManager.recordExplicitSnapshot(contentValue)
                            val newContent = applyFormatting(contentValue.text, contentValue.selection, action)
                            contentValue = newContent
                            undoRedoManager.recordExplicitSnapshot(newContent)
                        },
                        onUndoClick = {
                            undoRedoManager.undo()?.let { previous ->
                                contentValue = previous
                            }
                        },
                        onRedoClick = {
                            undoRedoManager.redo()?.let { next ->
                                contentValue = next
                            }
                        },
                        canUndo = undoRedoManager.canUndo,
                        canRedo = undoRedoManager.canRedo,
                        onWrenchToggle = { showWrenchTools = !showWrenchTools },
                        showWrenchTools = showWrenchTools
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val minHeight = maxHeight
            val editorScrollState = rememberScrollState()
            var requestFocus by remember { mutableStateOf(false) }

            if (isEditing) {
                // High-fidelity Markdown/Diagram Editor View with Notebook styling and red margin line
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    NotebookBackground(type = backgroundType)
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(editorScrollState)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))

                            // Title Field with AI status in left margin
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (isGenerating || isTyping) {
                                    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "spin")
                                    val rotationState = infiniteTransition.animateFloat(
                                        initialValue = 0f,
                                        targetValue = 360f,
                                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                            animation = androidx.compose.animation.core.tween(2000, easing = androidx.compose.animation.core.LinearEasing),
                                            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
                                        ),
                                        label = "rotation"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(72.dp)
                                            .align(Alignment.CenterStart),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.material3.Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "Generating",
                                            modifier = Modifier
                                                .size(28.dp)
                                                .graphicsLayer { rotationZ = rotationState.value },
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                BasicTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    textStyle = MaterialTheme.typography.displayMedium.copy(
                                        color = MaterialTheme.colorScheme.secondary
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 72.dp, end = 16.dp)
                                        .testTag("note_title_input"),
                                    decorationBox = { innerTextField ->
                                        if (title.isEmpty()) {
                                            Text(
                                                text = "Untitled Note",
                                                style = MaterialTheme.typography.displayMedium.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                )
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "COMPUTED DESIGN BLUEPRINT",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(start = 72.dp, end = 16.dp)
                            )

                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 72.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )

                            // Content Field using unified high-fidelity live MarkdownRenderer
                            com.usoy.papiro.ui.components.MarkdownRenderer(
                                text = contentValue.text,
                                onContentChanged = { newText -> 
                                    contentValue = TextFieldValue(
                                        text = newText,
                                        selection = androidx.compose.ui.text.TextRange(newText.length)
                                    )
                                },
                                onEditDrawingClick = { drawing ->
                                    currentEditingDrawing = drawing
                                    drawingWidth = drawing.width
                                    drawingHeight = drawing.height
                                    showDrawingEditor = true
                                },
                                requestFocus = requestFocus,
                                onRequestFocusConsumed = { requestFocus = false },
                                onEnhanceBlock = { textToEnhance, onResult ->
                                    viewModel.enhanceNote(textToEnhance) { enhancedText ->
                                        onResult(enhancedText)
                                    }
                                },
                                contentPadding = PaddingValues(start = 72.dp, end = 16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 600.dp)
                                    .testTag("note_content_input")
                            )
                        }
                    }
                }
            } else {
                // High-fidelity Markdown/Diagram View with Notebook styling and red margin line
                Box(modifier = Modifier.fillMaxSize()) {
                    NotebookBackground(type = backgroundType)
                    
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(editorScrollState)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { readerContentCoords = it }
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))

                            // Document Header with AI status in left margin
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (isGenerating || isTyping) {
                                    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "spin")
                                    val rotationState = infiniteTransition.animateFloat(
                                        initialValue = 0f,
                                        targetValue = 360f,
                                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                            animation = androidx.compose.animation.core.tween(2000, easing = androidx.compose.animation.core.LinearEasing),
                                            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
                                        ),
                                        label = "rotation"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(72.dp)
                                            .align(Alignment.CenterStart),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.material3.Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "Generating",
                                            modifier = Modifier
                                                .size(28.dp)
                                                .graphicsLayer { rotationZ = rotationState.value },
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Text(
                                    text = title.ifEmpty { "Untitled Note" },
                                    style = MaterialTheme.typography.displayMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(start = 72.dp, end = 16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "COMPUTED DESIGN BLUEPRINT",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(start = 72.dp, end = 16.dp)
                            )

                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 72.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )

                            // Rich Renderer
                            MarkdownRenderer(
                                text = contentValue.text,
                                contentPadding = PaddingValues(start = 72.dp, end = 16.dp),
                                onContentChanged = null,
                                onEditDrawingClick = { drawing ->
                                    currentEditingDrawing = drawing
                                    drawingWidth = drawing.width
                                    drawingHeight = drawing.height
                                    showDrawingEditor = true
                                },
                                onLinkClick = { link ->
                                    android.util.Log.d("PapiroNoteEditor", "onLinkClick: $link")
                                    if (link.startsWith("#")) {
                                        val id = link.drop(1).lowercase()
                                        android.util.Log.d("PapiroNoteEditor", "Target ID: $id")
                                        
                                        // Attempt to find the target coordinates using exact or fuzzy match
                                        var targetCoords = anchorPositions[id]
                                        if (targetCoords == null) {
                                            android.util.Log.d("PapiroNoteEditor", "Target not found exactly, trying fuzzy. Keys: ${anchorPositions.keys}")
                                            // Fallback fuzzy match (e.g., if TOC stripped numbers or punctuation)
                                            val cleanId = id.replace(Regex("[^a-z0-9]"), "")
                                            val matchedKey = anchorPositions.keys.find { 
                                                val cleanKey = it.replace(Regex("[^a-z0-9]"), "")
                                                cleanKey == cleanId || cleanKey.contains(cleanId) || cleanId.contains(cleanKey)
                                            }
                                            if (matchedKey != null) {
                                                android.util.Log.d("PapiroNoteEditor", "Found fuzzy match: $matchedKey")
                                                targetCoords = anchorPositions[matchedKey]
                                            }
                                        }

                                        if (targetCoords != null && targetCoords.isAttached && readerContentCoords != null && readerContentCoords!!.isAttached) {
                                            // Get the exact unclipped relative Y position of the header inside the scrollable column content
                                            val relativeY = readerContentCoords!!.localBoundingBoxOf(targetCoords, clipBounds = false).top
                                            android.util.Log.d("PapiroNoteEditor", "Relative Y (unclipped): $relativeY, Scroll: ${editorScrollState.value}")
                                            
                                            // Since relativeY is the position within the scrolling content, 
                                            // we just need to animate scroll to this absolute offset (minus the visual margin of 120px)
                                            val targetScroll = relativeY.toInt() - 120
                                            
                                            // Ensure we don't scroll to a negative value
                                            val safeScroll = targetScroll.coerceAtLeast(0)
                                            android.util.Log.d("PapiroNoteEditor", "Target Scroll: $safeScroll")
                                            
                                            coroutineScope.launch {
                                                editorScrollState.animateScrollTo(safeScroll)
                                            }
                                        } else {
                                            android.util.Log.d("PapiroNoteEditor", "TargetCoords is null or not attached")
                                        }
                                    }
                                },
                                onHeaderPositioned = { id, coords ->
                                    android.util.Log.d("PapiroNoteEditor", "Header Positioned: $id")
                                    anchorPositions[id] = coords
                                }
                            )
                            Spacer(modifier = Modifier.height(600.dp))
                        }
                    }
                }
            }

            // High-fidelity Streaming/Loading Overlays for on-device tasks (Removed modal overlay)

            if (isOcrRunning) {
                var ocrLoadingStep by remember { mutableStateOf("Extracting text...") }
                LaunchedEffect(isOcrRunning) {
                    if (isOcrRunning) {
                        val steps = listOf(
                            "Extracting text...",
                            "Reading text...",
                            "Thinking..."
                        )
                        var index = 0
                        while (isOcrRunning) {
                            ocrLoadingStep = steps[index]
                            kotlinx.coroutines.delay(1800)
                            if (index < steps.lastIndex) {
                                index++
                            } else {
                                index = 0
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.secondary,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "GEMINI CLOUD SCANNER",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = ocrLoadingStep,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // AI Topic Generator Dialog
    if (showAiDialog) {
        val currentProvider = viewModel.settingsStore.provider
        val providerName = when (currentProvider) {
            com.usoy.papiro.data.SettingsStore.PROVIDER_OLLAMA -> "Local Ollama"
            com.usoy.papiro.data.SettingsStore.PROVIDER_LOCAL_ON_DEVICE -> "On-Device Model"
            else -> "Google Gemini"
        }
        AlertDialog(
            onDismissRequest = { showAiDialog = false },
            title = {
                Text(
                    text = "GENERATE BLUEPRINT NOTE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Specify any topic or concept (e.g., Dijkstra's Algorithm, Big-O Notation, TCP Handshake) and our $providerName will compose an active note structure.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = aiTopic,
                        onValueChange = { aiTopic = it },
                        placeholder = { Text("Topic keyword") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAiDialog = false
                        if (title.isEmpty()) {
                            title = aiTopic
                        }
                        isEditing = true
                        
                        val initialText = contentValue.text
                        val prefix = if (initialText.isNotEmpty()) {
                            if (initialText.endsWith("\n")) "\n" else "\n\n"
                        } else {
                            ""
                        }
                        
                        viewModel.generateAiNotes(aiTopic, existingContent = initialText) { generatedMarkdown ->
                            coroutineScope.launch {
                                isTyping = true
                                val baseText = initialText + prefix
                                val chunks = generatedMarkdown.split(Regex("(?<=\\s)|(?=[\\n])"))
                                var currentTyped = ""
                                for (chunk in chunks) {
                                    currentTyped += chunk
                                    val fullText = baseText + currentTyped
                                    contentValue = TextFieldValue(
                                        text = fullText,
                                        selection = androidx.compose.ui.text.TextRange(fullText.length)
                                    )
                                    kotlinx.coroutines.delay(20) // adjust typing speed here
                                }
                                isEditing = false
                                isTyping = false
                            }
                        }
                    }
                ) {
                    Text("BUILD")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    // Canvas Creator Dialog overlay
    if (showCanvasCreator) {
        CanvasCreatorDialog(
            onDismiss = { showCanvasCreator = false },
            onConfirm = { width, height ->
                drawingWidth = width
                drawingHeight = height
                currentEditingDrawing = null
                showCanvasCreator = false
                showDrawingEditor = true
            }
        )
    }

    // Fullscreen Sketchpad overlay
    if (showDrawingEditor) {
        FullscreenDrawingEditor(
            width = drawingWidth,
            height = drawingHeight,
            initialPaths = currentEditingDrawing?.paths ?: emptyList(),
            onDismiss = {
                showDrawingEditor = false
                currentEditingDrawing = null
            },
            onSave = { savedPaths ->
                val currentDrawing = currentEditingDrawing
                if (currentDrawing != null) {
                    val updatedDrawing = DrawingData(
                        id = currentDrawing.id,
                        width = drawingWidth,
                        height = drawingHeight,
                        paths = savedPaths
                    )
                    val updatedContent = updateBlockInContent(contentValue.text, currentDrawing.id, updatedDrawing)
                    contentValue = TextFieldValue(
                        text = updatedContent,
                        selection = androidx.compose.ui.text.TextRange(updatedContent.length)
                    )
                } else {
                    val newDrawing = DrawingData(
                        id = System.currentTimeMillis().toString(),
                        width = drawingWidth,
                        height = drawingHeight,
                        paths = savedPaths
                    )
                    val drawingCodeBlock = "\n```drawing\n${newDrawing.toJson()}\n```\n"
                    val currentText = contentValue.text
                    val cursorPosition = contentValue.selection.start
                    val newText = StringBuilder(currentText)
                        .insert(cursorPosition, drawingCodeBlock)
                        .toString()
                    contentValue = TextFieldValue(
                        text = newText,
                        selection = androidx.compose.ui.text.TextRange(cursorPosition + drawingCodeBlock.length)
                    )
                }
                showDrawingEditor = false
                currentEditingDrawing = null
                isEditing = false // Switch to blueprint preview to see drawing!
            }
        )
    }

    // Markdown formatting guide dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Text(
                    text = "BLUEPRINT ENGINE GUIDE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Standard Markdown Syntax and Advanced Interactive Engineering triggers:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Text(
                        text = "BASIC MARKDOWN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    HelpGuideRow(pattern = "# Heading", description = "Creates a large section header.")
                    HelpGuideRow(pattern = "## Subtitle", description = "Creates a subsection header.")
                    HelpGuideRow(pattern = "**text**", description = "Formats text in bold style.")
                    HelpGuideRow(pattern = "*text*", description = "Formats text in italic style.")
                    HelpGuideRow(pattern = "- item", description = "Creates a bullet list entry.")
                    HelpGuideRow(pattern = "`code`", description = "Inline code monospace wrapper.")
                    HelpGuideRow(pattern = "---", description = "Draws a horizontal divider line.")
                    HelpGuideRow(pattern = "[link](url)", description = "Creates an interactive clickable hyperlink.")
                    HelpGuideRow(pattern = "| Col A | Col B |", description = "Creates dynamic structured data tables.")

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Text(
                        text = "ENGINEERING BLUEPRINTS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    HelpGuideRow(
                        pattern = "```mermaid\ngraph TD;\n  A-->B;\n```",
                        description = "Generates a schematic node flowchart in your preview page using Mermaid syntax."
                    )
                    HelpGuideRow(
                        pattern = "Hand Sketch",
                        description = "Tap the sketch tool to sketch drawings directly onto your blueprints using your finger."
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showHelpDialog = false }
                ) {
                    Text("DISMISS")
                }
            }
        )
    }

    if (showHistoryDialog) {
        val sdf = remember { java.text.SimpleDateFormat("MMM dd, yyyy - hh:mm a", java.util.Locale.getDefault()) }
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = {
                Text(
                    text = "NOTE VERSION HISTORY",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            },
            text = {
                if (historySnapshots.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No snapshots recorded yet.\nSnapshots are automatically created when you run AI actions or format blocks.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        historySnapshots.forEach { snapshot ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = try { sdf.format(java.util.Date(snapshot.timestamp)) } catch (e: Exception) { "" },
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = snapshot.content.take(80) + if (snapshot.content.length > 80) "..." else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontStyle = FontStyle.Italic
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    Button(
                                        onClick = {
                                            undoRedoManager.recordExplicitSnapshot(contentValue)
                                            currentNote?.let { note ->
                                                viewModel.insertHistorySnapshot(note.id, contentValue.text)
                                            }
                                            val newContent = TextFieldValue(
                                                text = snapshot.content,
                                                selection = androidx.compose.ui.text.TextRange(snapshot.content.length)
                                            )
                                            contentValue = newContent
                                            undoRedoManager.recordExplicitSnapshot(newContent)
                                            showHistoryDialog = false
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("RESTORE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("CLOSE")
                }
            }
        )
    }
}

@Composable
private fun ToolButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(16.dp),
            tint = contentColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

@Composable
fun EngineeringToolsRow(
    onAiClick: () -> Unit,
    onOcrClick: () -> Unit,
    onDrawClick: () -> Unit,
    onImageImportClick: () -> Unit,
    viewModel: NoteViewModel
) {
    val settingsStore = viewModel.settingsStore
    val context = LocalContext.current

    // Observe SharedPreferences changes reactive-ly to keep everything perfectly in sync
    var provider by remember { mutableStateOf(settingsStore.provider) }
    var selectedCloudModel by remember { mutableStateOf(settingsStore.selectedCloudModel) }
    var selectedLocalModel by remember { mutableStateOf(settingsStore.selectedLocalModel) }
    var cloudModels by remember { mutableStateOf(settingsStore.cloudModels) }
    var localModels by remember { mutableStateOf(settingsStore.localModels) }

    DisposableEffect(settingsStore) {
        val prefs = context.getSharedPreferences("papiro_settings", android.content.Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "provider" -> provider = settingsStore.provider
                "selected_cloud_model" -> selectedCloudModel = settingsStore.selectedCloudModel
                "selected_local_model" -> selectedLocalModel = settingsStore.selectedLocalModel
                "cloud_models_list" -> cloudModels = settingsStore.cloudModels
                "local_models_list" -> localModels = settingsStore.localModels
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    var expanded by remember { mutableStateOf(false) }

    // Read active model and list of models based on active provider, keyed on SettingsStore values to ensure perfect synchronization
    var activeModel by remember(provider, selectedLocalModel, selectedCloudModel) {
        mutableStateOf(
            if (provider == com.usoy.papiro.data.SettingsStore.PROVIDER_OLLAMA) {
                selectedLocalModel.ifEmpty { "llama3" }
            } else if (provider == com.usoy.papiro.data.SettingsStore.PROVIDER_LOCAL_ON_DEVICE) {
                "On-Device Task"
            } else {
                selectedCloudModel.ifEmpty { "gemini-3.5-flash" }
            }
        )
    }

    val models = remember(provider, localModels, cloudModels) {
        if (provider == com.usoy.papiro.data.SettingsStore.PROVIDER_OLLAMA) {
            localModels
        } else if (provider == com.usoy.papiro.data.SettingsStore.PROVIDER_LOCAL_ON_DEVICE) {
            emptyList()
        } else {
            cloudModels
        }
    }

    val icon = when (provider) {
        com.usoy.papiro.data.SettingsStore.PROVIDER_OLLAMA -> Icons.Default.Computer
        com.usoy.papiro.data.SettingsStore.PROVIDER_LOCAL_ON_DEVICE -> Icons.Default.Memory
        else -> Icons.Default.Cloud
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // AI Note Assistant
        ToolButton(
            onClick = onAiClick,
            icon = Icons.Default.AutoAwesome,
            label = "AI Assist",
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )

        // OCR Scan Text
        ToolButton(
            onClick = onOcrClick,
            icon = Icons.Default.DocumentScanner,
            label = "Scan",
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )

        // Draw Canvas
        ToolButton(
            onClick = onDrawClick,
            icon = Icons.Default.Gesture,
            label = "Draw",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )

        // Import Picture
        ToolButton(
            onClick = onImageImportClick,
            icon = Icons.Default.AddPhotoAlternate,
            label = "Image",
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Active Model Selector with DropdownMenu
        Box {
            ToolButton(
                onClick = { expanded = true },
                icon = icon,
                label = activeModel,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (provider == com.usoy.papiro.data.SettingsStore.PROVIDER_LOCAL_ON_DEVICE) {
                    DropdownMenuItem(
                        text = { Text("Local Task File (Fixed)") },
                        onClick = { expanded = false }
                    )
                } else if (models.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No models found. Scan in Settings.") },
                        onClick = { expanded = false }
                    )
                } else {
                    models.forEach { model ->
                        val isSelected = (model == activeModel)
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = model,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ) 
                            },
                            onClick = {
                                expanded = false
                                activeModel = model
                                if (provider == com.usoy.papiro.data.SettingsStore.PROVIDER_OLLAMA) {
                                    settingsStore.selectedLocalModel = model
                                } else {
                                    settingsStore.selectedCloudModel = model
                                }
                            },
                            trailingIcon = {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FormattingToolbar(
    onFormatAction: (String) -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onWrenchToggle: () -> Unit,
    showWrenchTools: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Undo
        IconButton(
            onClick = onUndoClick,
            enabled = canUndo,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = "Undo",
                tint = if (canUndo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }

        // Redo
        IconButton(
            onClick = onRedoClick,
            enabled = canRedo,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Redo,
                contentDescription = "Redo",
                tint = if (canRedo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }

        VerticalDivider(
            modifier = Modifier.height(24.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Wrench toggler for advanced engineering tools
        IconButton(
            onClick = onWrenchToggle,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (showWrenchTools) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                contentColor = if (showWrenchTools) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = "Toggle Engineering Tools",
                modifier = Modifier.size(18.dp)
            )
        }

        VerticalDivider(
            modifier = Modifier.height(24.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Bold
        ToolbarIconButton(
            icon = Icons.Default.FormatBold,
            contentDescription = "Bold selection",
            onClick = { onFormatAction("BOLD") }
        )

        // Italic
        ToolbarIconButton(
            icon = Icons.Default.FormatItalic,
            contentDescription = "Italic selection",
            onClick = { onFormatAction("ITALIC") }
        )

        // Insert Link
        ToolbarIconButton(
            icon = Icons.Default.Link,
            contentDescription = "Insert link",
            onClick = { onFormatAction("LINK") }
        )

        // H1 header
        ToolbarTextButton(
            label = "H1",
            onClick = { onFormatAction("H1") }
        )

        // H2 header
        ToolbarTextButton(
            label = "H2",
            onClick = { onFormatAction("H2") }
        )

        // Bullet List
        ToolbarIconButton(
            icon = Icons.AutoMirrored.Filled.FormatListBulleted,
            contentDescription = "Bullet list",
            onClick = { onFormatAction("BULLET") }
        )

        // Inline Code bracket
        ToolbarTextButton(
            label = "{}",
            onClick = { onFormatAction("CODE") }
        )

        // Text Block
        ToolbarIconButton(
            icon = Icons.Default.Edit,
            contentDescription = "Text block",
            onClick = { onFormatAction("TEXT_BLOCK") }
        )

        // Code Block
        ToolbarIconButton(
            icon = Icons.Default.Code,
            contentDescription = "Code block",
            onClick = { onFormatAction("CODE_BLOCK") }
        )

        // Schema Diagram Block
        ToolbarIconButton(
            icon = Icons.Default.Schema,
            contentDescription = "Diagram",
            onClick = { onFormatAction("DIAGRAM") }
        )
    }
}

@Composable
fun ToolbarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun ToolbarTextButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun HelpGuideRow(pattern: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = pattern,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.65f)
        )
    }
}

private fun toggleInlineFormat(selectedText: String, delimiter: String): Pair<String, Boolean> {
    if (delimiter == "**") {
        if (selectedText.startsWith("***") && selectedText.endsWith("***") && selectedText.length >= 6) {
            return Pair(selectedText.substring(2, selectedText.length - 2), true)
        }
        if (selectedText.startsWith("**") && selectedText.endsWith("**") && selectedText.length >= 4) {
            return Pair(selectedText.substring(2, selectedText.length - 2), true)
        }
        return Pair("**$selectedText**", false)
    } else if (delimiter == "*") {
        if (selectedText.startsWith("***") && selectedText.endsWith("***") && selectedText.length >= 6) {
            return Pair(selectedText.substring(1, selectedText.length - 1), true)
        }
        if (selectedText.startsWith("**") && selectedText.endsWith("**") && selectedText.length >= 4) {
            return Pair("*$selectedText*", false)
        }
        if (selectedText.startsWith("*") && selectedText.endsWith("*") && selectedText.length >= 2) {
            return Pair(selectedText.substring(1, selectedText.length - 1), true)
        }
        return Pair("*$selectedText*", false)
    } else if (delimiter == "{") {
        if (selectedText.startsWith("{ ") && selectedText.endsWith(" }") && selectedText.length >= 4) {
            return Pair(selectedText.substring(2, selectedText.length - 2), true)
        }
        if (selectedText.startsWith("{") && selectedText.endsWith("}") && selectedText.length >= 2) {
            return Pair(selectedText.substring(1, selectedText.length - 1), true)
        }
        return Pair("{ $selectedText }", false)
    }
    return Pair(selectedText, false)
}

// Memento Pattern for Editor Undo/Redo
data class EditorMemento(val state: TextFieldValue)

class UndoRedoManager(initialState: TextFieldValue) {
    private val _undoStack = androidx.compose.runtime.mutableStateListOf<EditorMemento>()
    private val _redoStack = androidx.compose.runtime.mutableStateListOf<EditorMemento>()
    
    var currentState: TextFieldValue = initialState
        private set

    fun saveState(newState: TextFieldValue) {
        val currentText = currentState.text
        val newText = newState.text
        
        // Save milestone if significant change occurred (e.g. typing a word, deleting)
        if (kotlin.math.abs(newText.length - currentText.length) > 15 || 
            newText.endsWith(" ") || newText.endsWith("\n") || 
            newText.length < currentText.length) {
            
            if (_undoStack.isEmpty() || _undoStack.last().state.text != currentText) {
                _undoStack.add(EditorMemento(currentState))
                _redoStack.clear()
            }
            currentState = newState
        }
    }
    
    fun recordExplicitSnapshot(newState: TextFieldValue) {
        if (_undoStack.isEmpty() || _undoStack.last().state.text != currentState.text) {
             _undoStack.add(EditorMemento(currentState))
             _redoStack.clear()
        }
        currentState = newState
    }
    
    fun undo(): TextFieldValue? {
        if (_undoStack.isNotEmpty()) {
            _redoStack.add(EditorMemento(currentState))
            currentState = _undoStack.removeAt(_undoStack.lastIndex).state
            return currentState
        }
        return null
    }

    fun redo(): TextFieldValue? {
        if (_redoStack.isNotEmpty()) {
            _undoStack.add(EditorMemento(currentState))
            currentState = _redoStack.removeAt(_redoStack.lastIndex).state
            return currentState
        }
        return null
    }

    fun reset(initialState: TextFieldValue) {
        _undoStack.clear()
        _redoStack.clear()
        currentState = initialState
    }
    
    val canUndo: Boolean get() = _undoStack.isNotEmpty()
    val canRedo: Boolean get() = _redoStack.isNotEmpty()
}

private fun applyBlockFormat(
    currentText: String,
    selection: androidx.compose.ui.text.TextRange,
    formatType: String
): TextFieldValue {
    val start = selection.start
    val end = selection.end

    var lineStart = start
    while (lineStart > 0 && currentText[lineStart - 1] != '\n') {
        lineStart--
    }

    var lineEnd = end
    while (lineEnd < currentText.length && currentText[lineEnd] != '\n') {
        lineEnd++
    }

    val selectedLinesText = currentText.substring(lineStart, lineEnd)
    val lines = selectedLinesText.split("\n")

    val updatedLines = lines.map { line ->
        val trimmed = line.trimStart()
        val leadingWhitespace = line.substring(0, line.length - trimmed.length)

        val prefixInfo = when {
            trimmed.startsWith("### ") -> Pair("### ", trimmed.removePrefix("### "))
            trimmed.startsWith("## ") -> Pair("## ", trimmed.removePrefix("## "))
            trimmed.startsWith("# ") -> Pair("# ", trimmed.removePrefix("# "))
            trimmed.startsWith("- ") -> Pair("- ", trimmed.removePrefix("- "))
            else -> Pair("", trimmed)
        }

        val existingPrefix = prefixInfo.first
        val contentWithoutPrefix = prefixInfo.second

        val targetPrefix = when (formatType) {
            "H1" -> "# "
            "H2" -> "## "
            "BULLET" -> "- "
            else -> ""
        }

        if (existingPrefix == targetPrefix) {
            leadingWhitespace + contentWithoutPrefix
        } else {
            leadingWhitespace + targetPrefix + contentWithoutPrefix
        }
    }

    val newSelectedLinesText = updatedLines.joinToString("\n")
    val newText = currentText.replaceRange(lineStart, lineEnd, newSelectedLinesText)

    return TextFieldValue(
        text = newText,
        selection = androidx.compose.ui.text.TextRange(lineStart, lineStart + newSelectedLinesText.length)
    )
}

private fun applyFormatting(
    currentText: String,
    selection: androidx.compose.ui.text.TextRange,
    formatType: String
): TextFieldValue {
    val start = selection.start
    val end = selection.end

    return when (formatType) {
        "BOLD" -> {
            if (start != end) {
                val selectedText = currentText.substring(start, end)
                
                var prefix = ""
                var contentToFormat = selectedText
                val headerPrefixes = listOf("### ", "## ", "# ", "- ")
                for (hp in headerPrefixes) {
                    if (selectedText.startsWith(hp)) {
                        prefix = hp
                        contentToFormat = selectedText.substring(hp.length)
                        break
                    }
                }

                val (newSelected, _) = toggleInlineFormat(contentToFormat, "**")
                val replacement = prefix + newSelected
                val newText = currentText.replaceRange(start, end, replacement)
                TextFieldValue(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(start, start + replacement.length)
                )
            } else {
                val newText = currentText.substring(0, start) + "****" + currentText.substring(start)
                TextFieldValue(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(start + 2)
                )
            }
        }
        "ITALIC" -> {
            if (start != end) {
                val selectedText = currentText.substring(start, end)
                
                var prefix = ""
                var contentToFormat = selectedText
                val headerPrefixes = listOf("### ", "## ", "# ", "- ")
                for (hp in headerPrefixes) {
                    if (selectedText.startsWith(hp)) {
                        prefix = hp
                        contentToFormat = selectedText.substring(hp.length)
                        break
                    }
                }

                val (newSelected, _) = toggleInlineFormat(contentToFormat, "*")
                val replacement = prefix + newSelected
                val newText = currentText.replaceRange(start, end, replacement)
                TextFieldValue(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(start, start + replacement.length)
                )
            } else {
                val newText = currentText.substring(0, start) + "**" + currentText.substring(start)
                TextFieldValue(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(start + 1)
                )
            }
        }
        "CODE" -> {
            if (start != end) {
                val selectedText = currentText.substring(start, end)
                
                var prefix = ""
                var contentToFormat = selectedText
                val headerPrefixes = listOf("### ", "## ", "# ", "- ")
                for (hp in headerPrefixes) {
                    if (selectedText.startsWith(hp)) {
                        prefix = hp
                        contentToFormat = selectedText.substring(hp.length)
                        break
                    }
                }

                val (newSelected, _) = toggleInlineFormat(contentToFormat, "{")
                val replacement = prefix + newSelected
                val newText = currentText.replaceRange(start, end, replacement)
                TextFieldValue(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(start, start + replacement.length)
                )
            } else {
                val newText = currentText.substring(0, start) + "{  }" + currentText.substring(start)
                TextFieldValue(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(start + 2)
                )
            }
        }
        "LINK" -> {
            if (start != end) {
                val selectedText = currentText.substring(start, end)
                val wrapped = "[$selectedText](https://github.com)"
                val newText = currentText.replaceRange(start, end, wrapped)
                TextFieldValue(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(start, start + wrapped.length)
                )
            } else {
                val insert = "[text](https://github.com)"
                val newText = currentText.substring(0, start) + insert + currentText.substring(start)
                TextFieldValue(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(start + 1, start + 5)
                )
            }
        }
        "H1", "H2", "BULLET" -> {
            applyBlockFormat(currentText, selection, formatType)
        }
        "TEXT_BLOCK" -> {
            val insert = "\n\n"
            val newText = currentText.substring(0, start) + insert + currentText.substring(start)
            TextFieldValue(
                text = newText,
                selection = androidx.compose.ui.text.TextRange(start + 2)
            )
        }
        "CODE_BLOCK" -> {
            if (start != end) {
                val selectedText = currentText.substring(start, end)
                val wrapped = "\n```kotlin\n$selectedText\n```\n"
                val newText = currentText.replaceRange(start, end, wrapped)
                TextFieldValue(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(start + wrapped.length)
                )
            } else {
                val insert = "\n```kotlin\n\n```\n"
                val newText = currentText.substring(0, start) + insert + currentText.substring(start)
                TextFieldValue(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(start + 11)
                )
            }
        }
        "DIAGRAM" -> {
            if (start != end) {
                val selectedText = currentText.substring(start, end)
                val wrapped = "\n```mermaid\n$selectedText\n```\n"
                val newText = currentText.replaceRange(start, end, wrapped)
                TextFieldValue(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(start + wrapped.length)
                )
            } else {
                val insert = "\n```mermaid\ngraph TD;\n  A-->B;\n```\n"
                val newText = currentText.substring(0, start) + insert + currentText.substring(start)
                TextFieldValue(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(start + 12)
                )
            }
        }
        else -> TextFieldValue(currentText, selection)
    }
}
