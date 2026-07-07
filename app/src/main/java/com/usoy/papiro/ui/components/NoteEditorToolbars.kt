package com.usoy.papiro.ui.components

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
import com.usoy.papiro.ui.components.FullscreenDrawingEditor
import com.usoy.papiro.ui.components.DrawingData
import com.usoy.papiro.ui.components.DrawPath
import com.usoy.papiro.ui.components.MarkdownVisualTransformation
import com.usoy.papiro.ui.components.updateBlockInContent
import com.usoy.papiro.viewmodel.NoteViewModel
import java.io.ByteArrayOutputStream
import java.io.InputStream

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
    onQuizClick: () -> Unit,
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
    var selectedLocalModel by remember { mutableStateOf("") }
    var cloudModels by remember { mutableStateOf(settingsStore.cloudModels) }
    var localModels by remember { mutableStateOf(emptyList<String>()) }

    DisposableEffect(settingsStore) {
        val prefs = context.getSharedPreferences("papiro_settings", android.content.Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "provider" -> provider = settingsStore.provider
                "selected_cloud_model" -> selectedCloudModel = settingsStore.selectedCloudModel
                "selected_local_model" -> selectedLocalModel = ""
                "cloud_models_list" -> cloudModels = settingsStore.cloudModels
                "local_models_list" -> localModels = emptyList<String>()
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    var expanded by remember { mutableStateOf(false) }

    // Read active model and list of models based on active provider, keyed on SettingsStore values to ensure perfect synchronization
    
    var activeModel by remember(provider, selectedCloudModel) {
        mutableStateOf(selectedCloudModel.ifEmpty { "gemini-2.5-flash" })
    }
    val models = remember(provider, cloudModels) {
        cloudModels.ifEmpty { listOf("gemini-1.5-flash", "gemini-2.5-flash", "gemini-3.1-flash-lite") }
    }
    
    val icon = Icons.Default.Cloud
    
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

        // Quiz Generator
        ToolButton(
            onClick = onQuizClick,
            icon = Icons.Default.Quiz,
            label = "Quiz",
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
                if (models.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No models found. Scan/Download in Settings.") },
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
                                if (model != "Download a model in Settings") {
                                    activeModel = model
                                    if (false) {
                                        var foundCustom = false
                                        emptySet<String>().forEach { customPath ->
                                            val customFile = java.io.File(customPath)
                                            if (customFile.exists() && customFile.name == model) {
                                                
                                                foundCustom = true
                                            }
                                        }
                                        if (!foundCustom) {
                                            val selectedFile = java.io.File(java.io.File(context.filesDir, "models"), model)
                                            
                                        }
                                    } else {
                                        settingsStore.selectedCloudModel = model
                                    }
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

        // Wrench toggler for advanced engineering tools with chevron at the bottom-right
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable { onWrenchToggle() }
                .background(if (showWrenchTools) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = "Toggle Engineering Tools",
                tint = if (showWrenchTools) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Toggle Engineering Tools Dropdown",
                tint = if (showWrenchTools) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(10.dp)
                    .graphicsLayer(rotationZ = -45f)
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

        // Combined Header button with dropdown support
        HeaderDropdownButton(
            onHeaderSelect = { headerType -> onFormatAction(headerType) }
        )

        // Bullet List
        ToolbarIconButton(
            icon = Icons.AutoMirrored.Filled.FormatListBulleted,
            contentDescription = "Bullet list",
            onClick = { onFormatAction("BULLET") }
        )

        // Table Block
        ToolbarIconButton(
            icon = Icons.Default.GridOn,
            contentDescription = "Table block",
            onClick = { onFormatAction("TABLE") }
        )

        // Math Block
        ToolbarIconButton(
            icon = Icons.Default.Functions,
            contentDescription = "Math block",
            onClick = { onFormatAction("MATH") }
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
fun HeaderDropdownButton(
    onHeaderSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable { expanded = true }
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "H",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select Header Level",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(10.dp)
                    .graphicsLayer(rotationZ = -45f)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("H1 - Heading 1", fontWeight = FontWeight.Bold) },
                onClick = {
                    onHeaderSelect("H1")
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("H2 - Heading 2", fontWeight = FontWeight.Bold) },
                onClick = {
                    onHeaderSelect("H2")
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("H3 - Heading 3", fontWeight = FontWeight.Bold) },
                onClick = {
                    onHeaderSelect("H3")
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("H4 - Heading 4", fontWeight = FontWeight.Bold) },
                onClick = {
                    onHeaderSelect("H4")
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("H5 - Heading 5", fontWeight = FontWeight.Bold) },
                onClick = {
                    onHeaderSelect("H5")
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("H6 - Heading 6", fontWeight = FontWeight.Bold) },
                onClick = {
                    onHeaderSelect("H6")
                    expanded = false
                }
            )
        }
    }
}

@Composable
fun HelpGuideRow(pattern: String, description: String) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(pattern))
                android.widget.Toast.makeText(context, "Copied sample to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(0.45f)) {
                Text(
                    text = pattern,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap to copy",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.55f)
            )
        }
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

// Command Pattern for Editor basic writing and formatting functions
interface EditorCommand {
    val description: String
    fun execute(currentState: TextFieldValue): TextFieldValue
}

// Concrete Command: Typing / Text editing
class WriteTextCommand(
    val newTextValue: TextFieldValue,
    override val description: String = "Type text"
) : EditorCommand {
    override fun execute(currentState: TextFieldValue): TextFieldValue {
        return newTextValue
    }
}

// Concrete Command: Formatting toolbar actions
class FormatCommand(
    val formatType: String,
    override val description: String = "Apply formatting: $formatType"
) : EditorCommand {
    override fun execute(currentState: TextFieldValue): TextFieldValue {
        return applyFormatting(currentState.text, currentState.selection, formatType)
    }
}

// Concrete Command: Block-level markdown formatting
class LocalFormatCommand(
    val formatType: String,
    override val description: String = "Apply block format: $formatType"
) : EditorCommand {
    override fun execute(currentState: TextFieldValue): TextFieldValue {
        return applyFormattingToLocal(currentState, formatType)
    }
}

// Concrete Command: Inserting tables
class InsertTableCommand(
    val tableMarkdown: String,
    override val description: String = "Insert Markdown table"
) : EditorCommand {
    override fun execute(currentState: TextFieldValue): TextFieldValue {
        val currentText = currentState.text
        val start = kotlin.math.min(currentState.selection.start, currentState.selection.end)
        val end = kotlin.math.max(currentState.selection.start, currentState.selection.end)
        val newText = if (start != end) {
            currentText.replaceRange(start, end, tableMarkdown)
        } else {
            currentText.substring(0, start) + tableMarkdown + currentText.substring(start)
        }
        return TextFieldValue(
            text = newText,
            selection = androidx.compose.ui.text.TextRange(start + tableMarkdown.length)
        )
    }
}

class UndoRedoManager(initialState: TextFieldValue) {
    private val _undoStack = androidx.compose.runtime.mutableStateListOf<EditorMemento>()
    private val _redoStack = androidx.compose.runtime.mutableStateListOf<EditorMemento>()
    
    var currentState: TextFieldValue = initialState
        private set

    // Invoker function to execute a command and capture state using Memento Pattern
    fun executeCommand(command: EditorCommand) {
        val previousState = currentState
        val nextState = command.execute(currentState)
        
        if (nextState.text != previousState.text || nextState.selection != previousState.selection) {
            if (command is WriteTextCommand) {
                val prevText = previousState.text
                val nextText = nextState.text
                // Save milestone if significant change occurred (e.g. typing a word, deleting)
                if (kotlin.math.abs(nextText.length - prevText.length) > 15 || 
                    nextText.endsWith(" ") || nextText.endsWith("\n") || 
                    nextText.length < prevText.length) {
                    
                    if (_undoStack.isEmpty() || _undoStack.last().state.text != prevText) {
                        _undoStack.add(EditorMemento(previousState))
                        _redoStack.clear()
                    }
                }
            } else {
                if (_undoStack.isEmpty() || _undoStack.last().state.text != previousState.text) {
                    _undoStack.add(EditorMemento(previousState))
                    _redoStack.clear()
                }
            }
            currentState = nextState
        } else {
            currentState = nextState
        }
    }

    fun saveState(newState: TextFieldValue) {
        executeCommand(WriteTextCommand(newState))
    }
    
    fun recordExplicitSnapshot(newState: TextFieldValue) {
        val previousState = currentState
        if (_undoStack.isEmpty() || _undoStack.last().state.text != previousState.text) {
             _undoStack.add(EditorMemento(previousState))
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
    val snapshots: List<EditorMemento> get() = _undoStack.toList()
}

private fun applyBlockFormat(
    currentText: String,
    selection: androidx.compose.ui.text.TextRange,
    formatType: String
): TextFieldValue {
    val start = kotlin.math.min(selection.start, selection.end)
    val end = kotlin.math.max(selection.start, selection.end)

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

    if (lines.size == 1) {
        val line = lines[0]
        val trimmed = line.trimStart()
        val leadingWhitespace = line.substring(0, line.length - trimmed.length)

        val prefixInfo = when {
            trimmed.startsWith("###### ") -> Pair("###### ", trimmed.removePrefix("###### "))
            trimmed.startsWith("##### ") -> Pair("##### ", trimmed.removePrefix("##### "))
            trimmed.startsWith("#### ") -> Pair("#### ", trimmed.removePrefix("#### "))
            trimmed.startsWith("### ") -> Pair("### ", trimmed.removePrefix("### "))
            trimmed.startsWith("## ") -> Pair("## ", trimmed.removePrefix("## "))
            trimmed.startsWith("# ") -> Pair("# ", trimmed.removePrefix("# "))
            trimmed.startsWith("-- ") -> Pair("-- ", trimmed.removePrefix("-- "))
            trimmed.startsWith("- ") -> Pair("- ", trimmed.removePrefix("- "))
            else -> Pair("", trimmed)
        }

        val existingPrefix = prefixInfo.first
        val contentWithoutPrefix = prefixInfo.second

        val targetPrefix = when (formatType) {
            "H1" -> "# "
            "H2" -> "## "
            "H3" -> "### "
            "H4" -> "#### "
            "H5" -> "##### "
            "H6" -> "###### "
            "BULLET" -> {
                when (existingPrefix) {
                    "- " -> {
                        val prevLineText = if (lineStart > 0) {
                            var prevLineStart = lineStart - 1
                            while (prevLineStart > 0 && currentText[prevLineStart - 1] != '\n') {
                                prevLineStart--
                            }
                            currentText.substring(prevLineStart, lineStart - 1)
                        } else {
                            ""
                        }
                        val prevLineTrimmed = prevLineText.trimStart()
                        val isPrevLineList = prevLineTrimmed.startsWith("- ") || prevLineTrimmed.startsWith("-- ")
                        if (isPrevLineList) "-- " else ""
                    }
                    "-- " -> ""
                    else -> "- "
                }
            }
            else -> ""
        }

        val newLine = if (existingPrefix == targetPrefix) {
            leadingWhitespace + contentWithoutPrefix
        } else {
            leadingWhitespace + targetPrefix + contentWithoutPrefix
        }

        val newText = currentText.replaceRange(lineStart, lineEnd, newLine)

        val oldPrefixLen = leadingWhitespace.length + existingPrefix.length
        val newPrefixLen = leadingWhitespace.length + targetPrefix.length
        val diff = newPrefixLen - oldPrefixLen

        val newStart = if (start >= lineStart + oldPrefixLen) {
            (start + diff).coerceIn(lineStart + newPrefixLen, lineStart + newLine.length)
        } else {
            lineStart + newPrefixLen
        }

        val newEnd = if (end >= lineStart + oldPrefixLen) {
            (end + diff).coerceIn(lineStart + newPrefixLen, lineStart + newLine.length)
        } else {
            lineStart + newPrefixLen
        }

        return TextFieldValue(
            text = newText,
            selection = androidx.compose.ui.text.TextRange(newStart, newEnd)
        )
    } else {
        val updatedLines = lines.map { line ->
            val trimmed = line.trimStart()
            val leadingWhitespace = line.substring(0, line.length - trimmed.length)

            val prefixInfo = when {
                trimmed.startsWith("###### ") -> Pair("###### ", trimmed.removePrefix("###### "))
                trimmed.startsWith("##### ") -> Pair("##### ", trimmed.removePrefix("##### "))
                trimmed.startsWith("#### ") -> Pair("#### ", trimmed.removePrefix("#### "))
                trimmed.startsWith("### ") -> Pair("### ", trimmed.removePrefix("### "))
                trimmed.startsWith("## ") -> Pair("## ", trimmed.removePrefix("## "))
                trimmed.startsWith("# ") -> Pair("# ", trimmed.removePrefix("# "))
                trimmed.startsWith("-- ") -> Pair("-- ", trimmed.removePrefix("-- "))
                trimmed.startsWith("- ") -> Pair("- ", trimmed.removePrefix("- "))
                else -> Pair("", trimmed)
            }

            val existingPrefix = prefixInfo.first
            val contentWithoutPrefix = prefixInfo.second

            val targetPrefix = when (formatType) {
                "H1" -> "# "
                "H2" -> "## "
                "H3" -> "### "
                "H4" -> "#### "
                "H5" -> "##### "
                "H6" -> "###### "
                "BULLET" -> {
                    when (existingPrefix) {
                        "- " -> {
                            val prevLineText = if (lineStart > 0) {
                                var prevLineStart = lineStart - 1
                                while (prevLineStart > 0 && currentText[prevLineStart - 1] != '\n') {
                                    prevLineStart--
                                }
                                currentText.substring(prevLineStart, lineStart - 1)
                            } else {
                                ""
                            }
                            val prevLineTrimmed = prevLineText.trimStart()
                            val isPrevLineList = prevLineTrimmed.startsWith("- ") || prevLineTrimmed.startsWith("-- ")
                            if (isPrevLineList) "-- " else ""
                        }
                        "-- " -> ""
                        else -> "- "
                    }
                }
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
}

fun applyFormatting(
    currentText: String,
    selection: androidx.compose.ui.text.TextRange,
    formatType: String
): TextFieldValue {
    val start = kotlin.math.min(selection.start, selection.end)
    val end = kotlin.math.max(selection.start, selection.end)

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
        "TABLE" -> {
            if (start != end) {
                val selectedText = currentText.substring(start, end)
                val wrapped = "\n\n| Header 1 | Header 2 |\n|---|---|\n| $selectedText | |\n\n"
                TextFieldValue(
                    text = currentText.replaceRange(start, end, wrapped),
                    selection = androidx.compose.ui.text.TextRange(start, start + wrapped.length)
                )
            } else {
                val insert = "\n\n| Header 1 | Header 2 |\n|---|---|\n| Cell 1 | Cell 2 |\n\n"
                val newText = currentText.substring(0, start) + insert + currentText.substring(start)
                TextFieldValue(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(start, start + insert.length)
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
        "H1", "H2", "H3", "H4", "H5", "H6", "BULLET" -> {
            applyBlockFormat(currentText, selection, formatType)
        }
        "MATH" -> {
            if (start != end) {
                val selectedText = currentText.substring(start, end)
                val wrapped = "\n$$\n$selectedText\n$$\n"
                val newText = currentText.replaceRange(start, end, wrapped)
                TextFieldValue(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(start + wrapped.length)
                )
            } else {
                val insert = "\n$$\nE_m = \\frac{RT}{F} \\ln \\left( \\frac{P_{K}[K^+]_{out} + P_{Na}[Na^+]_{out} + P_{Cl}[Cl^-]_{in}}{P_{K}[K^+]_{in} + P_{Na}[Na^+]_{in} + P_{Cl}[Cl^-]_{out}} \\right)\n$$\n"
                val newText = currentText.substring(0, start) + insert + currentText.substring(start)
                TextFieldValue(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(start + 4, start + insert.length - 4)
                )
            }
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

@Composable
fun AskAiTutorInputBox(
    viewModel: NoteViewModel,
    currentNoteContent: String,
    onAnswerReceived: (String) -> Unit
) {
    var questionText by remember { mutableStateOf("") }
    var isAILoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding(),
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = questionText,
                    onValueChange = { questionText = it },
                    placeholder = { Text("Ask AI Tutor a question...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3,
                    enabled = !isAILoading
                )

                IconButton(
                    onClick = {
                        if (questionText.trim().isNotEmpty()) {
                            val userQuestion = questionText.trim()
                            questionText = ""
                            isAILoading = true
                            
                            coroutineScope.launch {
                                try {
                                    val prompt = com.usoy.papiro.util.PromptTemplates.getTutorPrompt(
                                        currentNoteContent = currentNoteContent,
                                        userQuestion = userQuestion
                                    )
                                    
                                    val generatedMarkdown = com.usoy.papiro.data.GeminiService.generateStructuredNote(
                                        context = context,
                                        topic = prompt,
                                        existingContent = "",
                                        settings = viewModel.settingsStore
                                    )
                                    
                                    val newContent = buildString {
                                        append(currentNoteContent)
                                        if (!currentNoteContent.endsWith("\n")) {
                                            append("\n")
                                        }
                                        if (!currentNoteContent.contains("## 📚 Tutor Q&A")) {
                                            append("\n---\n## 📚 Tutor Q&A\n")
                                        }
                                        append("\n")
                                        append(generatedMarkdown)
                                        append("\n---\n")
                                    }
                                    
                                    val finalContent = newContent
                                    onAnswerReceived(finalContent)
                                } catch (e: Exception) {
                                    val errorContent = buildString {
                                        append(currentNoteContent)
                                        if (!currentNoteContent.endsWith("\n")) {
                                            append("\n")
                                        }
                                        if (!currentNoteContent.contains("## 📚 Tutor Q&A")) {
                                            append("\n---\n## 📚 Tutor Q&A\n")
                                        }
                                        append("\n### 🙋 Question\n")
                                        append("> *")
                                        append(userQuestion)
                                        append("*\n\n")
                                        append("### ❌ Error\n")
                                        append("Failed to generate response: ${e.message}\n---\n")
                                    }
                                    val finalErrorContent = errorContent
                                    onAnswerReceived(finalErrorContent)
                                } finally {
                                    isAILoading = false
                                }
                            }
                        }
                    },
                    enabled = !isAILoading && questionText.trim().isNotEmpty(),
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (questionText.trim().isNotEmpty() && !isAILoading) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    if (isAILoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Question",
                            tint = if (questionText.trim().isNotEmpty()) MaterialTheme.colorScheme.onPrimary 
                                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
