package com.usoy.papiro.ui.screens

import com.usoy.papiro.ui.components.*
import com.usoy.papiro.ui.components.*

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
    var showAiTutorBox by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var showMarkdownSymbols by remember { mutableStateOf(false) }
    var showWrenchTools by remember { mutableStateOf(false) }
    var isTyping by remember { mutableStateOf(false) }
    var isQuizGenerating by remember { mutableStateOf(false) }

    // Drawing tool states
    var showCanvasCreator by remember { mutableStateOf(false) }
    var showDrawingEditor by remember { mutableStateOf(false) }
    var drawingWidth by remember { mutableIntStateOf(320) }
    var drawingHeight by remember { mutableIntStateOf(240) }
    var currentEditingDrawing by remember { mutableStateOf<DrawingData?>(null) }

    // Dialog state for AI generator
    var showAiDialog by remember { mutableStateOf(false) }
    var aiTopic by remember { mutableStateOf("") }

    // Dialog state for Quiz generator
    var showQuizDialog by remember { mutableStateOf(false) }
    var quizType by remember { mutableStateOf("Multiple Choice") }
    var quizDifficulty by remember { mutableStateOf("Medium") }
    var quizItems by remember { mutableStateOf("5") }

    // Dialog state for Formatting Help Guide
    var showHelpDialog by remember { mutableStateOf(false) }

    // Dynamic temporary text generation states
    var textBeforeGeneration by remember { mutableStateOf("") }
    var isGeneratingTempText by remember { mutableStateOf(false) }
    var tempTextJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val anchorPositions = remember { mutableMapOf<String, androidx.compose.ui.layout.LayoutCoordinates>() }
    var readerContentCoords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Undo / Redo Manager
    val undoRedoManager = remember { UndoRedoManager(contentValue) }
    
    val formatController = remember { com.usoy.papiro.ui.components.MarkdownFormatController() }
    
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showTableConfigDialog by remember { mutableStateOf(false) }

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

    LaunchedEffect(isGenerating, isTyping) {
        if (!isGenerating && !isTyping) {
            isQuizGenerating = false
        }
    }

    LaunchedEffect(isGenerating) {
        if (!isGenerating && isGeneratingTempText) {
            tempTextJob?.cancel()
            tempTextJob = null
            // Revert back to original text if the temporary markers are still present
            val curText = contentValue.text
            if (curText.contains("🤖 Generating text") || 
                curText.contains("🧠 Thinking...") || 
                curText.contains("Drafting structured") || 
                curText.contains("Formatting beautiful")) {
                contentValue = TextFieldValue(text = textBeforeGeneration, selection = androidx.compose.ui.text.TextRange(textBeforeGeneration.length))
            }
            isGeneratingTempText = false
        }
    }

    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            snackbarHostState.showSnackbar(event)
        }
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
            isEditing = if (it.backgroundType == "QUIZ") false else (it.content.isEmpty() && it.title.isEmpty())
            showAiTutorBox = (it.backgroundType == "QUIZ")
            undoRedoManager.reset(contentValue)
        }
    }

    // Media and OCR picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                viewModel.extractTextFromDocument(context, uri) { text ->
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
                    viewModel.generateTitleIfEmpty(title, contentValue.text) { newTitle ->
                        title = newTitle
                        val saveBgType = if (currentNote?.backgroundType == "QUIZ") "QUIZ" else backgroundType.name
                        viewModel.saveNote(title, contentValue.text, saveBgType)
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
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
                        val saveBgType = if (currentNote?.backgroundType == "QUIZ") "QUIZ" else backgroundType.name
                        viewModel.saveNote(title, contentValue.text, saveBgType)
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
                    // Notebook theme changer only visible in view mode
                    if (!isEditing) {
                        IconButton(
                            onClick = {
                                showAiTutorBox = !showAiTutorBox
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = "Toggle AI Tutor",
                                tint = if (showAiTutorBox) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        }
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
                    } else {
                        // "fat Eye" toggler only visible in edit mode
                        IconButton(
                            onClick = {
                                showMarkdownSymbols = !showMarkdownSymbols
                            }
                        ) {
                            Icon(
                                imageVector = if (showMarkdownSymbols) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Markdown Symbols",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
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

                    if (currentNote?.backgroundType != "QUIZ") {
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
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (showAiTutorBox && !isEditing) {
                AskAiTutorInputBox(
                    viewModel = viewModel,
                    currentNoteContent = contentValue.text,
                    onAnswerReceived = { updatedContent ->
                        currentNote?.let { note ->
                            val updatedWithToc = viewModel.updateExistingToc(updatedContent)
                            viewModel.generateTitleIfEmpty(title, updatedWithToc) { newTitle ->
                                title = newTitle
                                viewModel.saveNote(title, updatedWithToc, note.backgroundType)
                                contentValue = TextFieldValue(updatedWithToc)
                            }
                        }
                    }
                )
            } else if (isEditing) {
                // Toolbar accessories (bold, italic, tags, quick symbols)
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
                            onQuizClick = { showQuizDialog = true },
                            onOcrClick = { imagePickerLauncher.launch(arrayOf("image/*", "application/pdf")) },
                            onDrawClick = {
                                drawingWidth = 320
                                drawingHeight = 240
                                currentEditingDrawing = null
                                showDrawingEditor = true
                            },
                            onImageImportClick = { picturePickerLauncher.launch("image/*") },
                            
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }

                    FormattingToolbar(
                        onFormatAction = { action ->
                            when (action) {
                                "TABLE" -> {
                                    showTableConfigDialog = true
                                }
                                "MATH" -> {
                                    val newMath = com.usoy.papiro.ui.components.MarkdownBlock.MathBlock(
                                        "E_m = \\frac{RT}{F} \\ln \\left( \\frac{P_{K}[K^+]_{out} + P_{Na}[Na^+]_{out} + P_{Cl}[Cl^-]_{in}}{P_{K}[K^+]_{in} + P_{Na}[Na^+]_{in} + P_{Cl}[Cl^-]_{out}} \\right)"
                                    )
                                    val updatedText = com.usoy.papiro.ui.components.insertMarkdownBlock(contentValue.text, newMath, formatController)
                                    val newValue = TextFieldValue(text = updatedText, selection = androidx.compose.ui.text.TextRange(updatedText.length))
                                    contentValue = newValue
                                    undoRedoManager.recordExplicitSnapshot(newValue)
                                }
                                "CODE_BLOCK" -> {
                                    val newCode = com.usoy.papiro.ui.components.MarkdownBlock.CodeBlock(
                                        language = "kotlin",
                                        code = ""
                                    )
                                    val updatedText = com.usoy.papiro.ui.components.insertMarkdownBlock(contentValue.text, newCode, formatController)
                                    val newValue = TextFieldValue(text = updatedText, selection = androidx.compose.ui.text.TextRange(updatedText.length))
                                    contentValue = newValue
                                    undoRedoManager.recordExplicitSnapshot(newValue)
                                }
                                "DIAGRAM" -> {
                                    val newDiagram = com.usoy.papiro.ui.components.MarkdownBlock.DiagramBlock(
                                        title = "mermaid",
                                        syntax = "graph TD;\n  A-->B;"
                                    )
                                    val updatedText = com.usoy.papiro.ui.components.insertMarkdownBlock(contentValue.text, newDiagram, formatController)
                                    val newValue = TextFieldValue(text = updatedText, selection = androidx.compose.ui.text.TextRange(updatedText.length))
                                    contentValue = newValue
                                    undoRedoManager.recordExplicitSnapshot(newValue)
                                }
                                else -> {
                                    val localAction = formatController.onFormatAction
                                    if (localAction != null) {
                                        localAction(action)
                                    } else {
                                        val command = com.usoy.papiro.ui.components.FormatCommand(action)
                                        undoRedoManager.executeCommand(command)
                                        contentValue = undoRedoManager.currentState
                                    }
                                }
                            }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val editorScrollState = rememberScrollState()
            LaunchedEffect(contentValue.text) {
                if (currentNote?.backgroundType == "QUIZ") {
                    editorScrollState.animateScrollTo(editorScrollState.maxValue)
                }
            }
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
                                formatController = formatController,
                                onEditDrawingClick = { drawing ->
                                    currentEditingDrawing = drawing
                                    drawingWidth = drawing.width
                                    drawingHeight = drawing.height
                                    showDrawingEditor = true
                                },
                                requestFocus = requestFocus,
                                onRequestFocusConsumed = { requestFocus = false },
                                onEnhanceBlock = if (true) {
                                    { textToEnhance, onResult ->
                                        viewModel.enhanceNote(textToEnhance) { enhancedText ->
                                            onResult(enhancedText)
                                        }
                                    }
                                } else null,
                                contentPadding = PaddingValues(start = 72.dp, end = 16.dp),
                                showMarkdownSymbols = showMarkdownSymbols,
                                isGeneratingQuiz = isQuizGenerating && (isGenerating || isTyping),
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
                                },
                                isGeneratingQuiz = isQuizGenerating && (isGenerating || isTyping)
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
                                text = "SMART SCANNER",
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
    com.usoy.papiro.ui.components.AiTopicGeneratorDialog(
        showAiDialog = showAiDialog,
        
        onDismiss = { showAiDialog = false },
        onBuild = { topic ->
            if (title.isEmpty()) title = topic
            isEditing = true
            val initialText = contentValue.text
            val prefix = if (initialText.isNotEmpty()) { if (initialText.endsWith("\n")) "\n" else "\n\n" } else { "" }
            
            textBeforeGeneration = initialText
            isGeneratingTempText = true
            tempTextJob?.cancel()
            tempTextJob = startTemporaryTextCycle(coroutineScope, initialText, prefix) { updatedValue ->
                contentValue = updatedValue
            }

            viewModel.generateAiNotes(topic, existingContent = initialText) { generatedMarkdown ->
                tempTextJob?.cancel()
                tempTextJob = null
                isGeneratingTempText = false
                coroutineScope.launch {
                    val baseText = initialText + prefix
                    val fullText = baseText + generatedMarkdown
                    contentValue = TextFieldValue(text = fullText, selection = androidx.compose.ui.text.TextRange(fullText.length))
                    isEditing = false
                    viewModel.generateTitleIfEmpty(title, contentValue.text) { newTitle ->
                        title = newTitle
                        currentNote?.let { note -> viewModel.saveNote(title, contentValue.text, note.backgroundType) }
                    }
                }
            }
        }
    )

    // Quiz Generator Dialog
    com.usoy.papiro.ui.components.QuizGeneratorDialog(
        showQuizDialog = showQuizDialog,
        
        onDismiss = { showQuizDialog = false },
        onGenerate = { generatedQuizType, generatedQuizDifficulty, generatedQuizItems ->
            isEditing = true
            isQuizGenerating = true
            val initialText = contentValue.text
            val prefix = if (initialText.isNotEmpty()) { if (initialText.endsWith("\n")) "\n" else "\n\n" } else { "" }
            val quizPrompt = com.usoy.papiro.util.PromptTemplates.getQuizPrompt(
                quizType = generatedQuizType,
                difficultyLevel = generatedQuizDifficulty,
                numberOfItems = generatedQuizItems,
                contextText = "the following note:\n${contentValue.text}"
            )
            
            textBeforeGeneration = initialText
            isGeneratingTempText = true
            tempTextJob?.cancel()
            tempTextJob = startTemporaryTextCycle(coroutineScope, initialText, prefix) { updatedValue ->
                contentValue = updatedValue
            }

            viewModel.generateCustomPrompt(quizPrompt) { generatedMarkdown ->
                tempTextJob?.cancel()
                tempTextJob = null
                isGeneratingTempText = false
                coroutineScope.launch {
                    val baseText = initialText + prefix
                    val quizHeader = if (!initialText.contains("## 📝 Practice Quiz") && !initialText.contains("## 📝 Interactive Study Quiz")) {
                        "## 📝 Practice Quiz\n\n"
                    } else {
                        ""
                    }
                    val fullText = baseText + quizHeader + generatedMarkdown
                    val fullTextWithToc = viewModel.updateExistingToc(fullText)
                    contentValue = TextFieldValue(text = fullTextWithToc, selection = androidx.compose.ui.text.TextRange(fullTextWithToc.length))
                    isEditing = false
                    isQuizGenerating = false
                    viewModel.generateTitleIfEmpty(title, contentValue.text) { newTitle ->
                        title = newTitle
                        currentNote?.let { note -> viewModel.saveNote(title, contentValue.text, note.backgroundType) }
                    }
                }
            }
        }
    )

    // Fullscreen Sketchpad overlay
    if (showDrawingEditor) {
        FullscreenDrawingEditor(
            width = drawingWidth,
            height = drawingHeight,
            initialPaths = currentEditingDrawing?.paths ?: emptyList(),
            offsetX = currentEditingDrawing?.offsetX ?: 0f,
            offsetY = currentEditingDrawing?.offsetY ?: 0f,
            onDismiss = {
                showDrawingEditor = false
                currentEditingDrawing = null
            },
            onSave = { computedWidth, computedHeight, savedPaths, computedOffsetX, computedOffsetY ->
                val currentDrawing = currentEditingDrawing
                if (currentDrawing != null) {
                    val updatedDrawing = DrawingData(
                        id = currentDrawing.id,
                        width = computedWidth,
                        height = computedHeight,
                        paths = savedPaths,
                        offsetX = computedOffsetX,
                        offsetY = computedOffsetY
                    )
                    val updatedContent = updateBlockInContent(contentValue.text, currentDrawing.id, updatedDrawing)
                    contentValue = TextFieldValue(
                        text = updatedContent,
                        selection = androidx.compose.ui.text.TextRange(updatedContent.length)
                    )
                } else {
                    val newDrawing = DrawingData(
                        id = System.currentTimeMillis().toString(),
                        width = computedWidth,
                        height = computedHeight,
                        paths = savedPaths,
                        offsetX = computedOffsetX,
                        offsetY = computedOffsetY
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

    if (showTableConfigDialog) {
        com.usoy.papiro.ui.components.TableConfigDialog(
            initialRows = 3,
            initialCols = 3,
            initialStyle = com.usoy.papiro.ui.components.TableHeaderStyle.ROW,
            initialColor = com.usoy.papiro.ui.components.TableHeaderColor.DEFAULT,
            onDismiss = { showTableConfigDialog = false },
            onConfirm = { rows, cols, style, color ->
                showTableConfigDialog = false
                val tableMarkdown = com.usoy.papiro.ui.components.generateMarkdownTable(rows, cols, style, color)
                val parsedTable = com.usoy.papiro.ui.components.parseMarkdown(tableMarkdown).firstOrNull { it is com.usoy.papiro.ui.components.MarkdownBlock.TableBlock }
                if (parsedTable != null) {
                    val updatedText = com.usoy.papiro.ui.components.insertMarkdownBlock(contentValue.text, parsedTable, formatController)
                    val newValue = TextFieldValue(text = updatedText, selection = androidx.compose.ui.text.TextRange(updatedText.length))
                    contentValue = newValue
                    undoRedoManager.recordExplicitSnapshot(newValue)
                }
            }
        )
    }

    // Markdown formatting guide dialog
    com.usoy.papiro.ui.components.MarkdownHelpDialog(
        showHelpDialog = showHelpDialog,
        onDismiss = { showHelpDialog = false }
    )

    com.usoy.papiro.ui.components.VersionHistoryDialog(
        showHistoryDialog = showHistoryDialog,
        snapshots = historySnapshots,
        onDismiss = { showHistoryDialog = false },
        onRestore = { restoredString ->
            val restoredContent = TextFieldValue(text = restoredString, selection = androidx.compose.ui.text.TextRange(restoredString.length))
            contentValue = restoredContent
            undoRedoManager.recordExplicitSnapshot(restoredContent)
        }
    )
    }
}

private fun startTemporaryTextCycle(
    scope: kotlinx.coroutines.CoroutineScope,
    initialText: String,
    prefix: String,
    onUpdate: (TextFieldValue) -> Unit
): kotlinx.coroutines.Job {
    val statuses = listOf(
        "🤖 Generating text, please wait...",
        "🧠 Thinking...",
        "Drafting structured sections...",
        "Formatting beautiful layouts..."
    )
    val jokes = listOf(
        "Why do programmers wear glasses? Because they can't C#!",
        "There are 10 types of people in the world: those who understand binary, and those who don't.",
        "How many programmers does it take to change a light bulb? None, that's a hardware problem!",
        "Why did the database administrator leave the restaurant? There were no joined tables.",
        "What do you call a group of 8 hobbits? A hobbyte!"
    )
    val tips = listOf(
        "💡 Pro-Tip: You can maximize any diagram to full screen with the zoom icon in the corner!",
        "💡 Pro-Tip: Use `[Node A] -> [Node B]` to quickly build automatic flowcharts.",
        "💡 Pro-Tip: Papiro saves everything offline automatically as you type.",
        "💡 Pro-Tip: Want a quiz? Use the Quiz Generator button to challenge yourself!",
        "💡 Pro-Tip: You can drag and drop sections easily in Papiro."
    )

    return scope.launch {
        var cycle = 0
        while (true) {
            val status = statuses[cycle % statuses.size]
            val joke = jokes[cycle % jokes.size]
            val tip = tips[cycle % tips.size]

            val tempMarkdown = """

---
### $status
*Please hold on while the AI crafts your notes...*

${if (cycle % 2 == 0) "*😄 Here's a tech joke to pass the time:*\n> $joke" else "*$tip*"}
---
""".trimIndent()

            val fullText = initialText + prefix + tempMarkdown
            onUpdate(TextFieldValue(text = fullText, selection = androidx.compose.ui.text.TextRange(fullText.length)))

            cycle++
            kotlinx.coroutines.delay(2500) // Cycle every 2.5 seconds
        }
    }
}

