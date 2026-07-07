package com.usoy.papiro.ui.components
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.window.Dialog

sealed class MarkdownBlock {
    data class TextBlock(val text: String) : MarkdownBlock()
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data class ListItem(val text: String, val level: Int = 1) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class DiagramBlock(val syntax: String, val title: String = "diagram") : MarkdownBlock()
    data class DrawingBlock(val json: String, val rawBlock: String) : MarkdownBlock()
    data class Image(val path: String, val altText: String) : MarkdownBlock()
    data class TableBlock(val headers: List<String>, val rows: List<List<String>>, val alignments: List<String>) : MarkdownBlock()
    data class MathBlock(val equation: String) : MarkdownBlock()
    data class HorizontalRule(val dummy: Boolean = true) : MarkdownBlock()
}

val LocalMarkdownFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.Serif }

@Composable
fun InlineBlockEditor(
    initialText: String,
    style: TextStyle,
    isDark: Boolean,
    primaryColor: Color,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textValue by remember {
        mutableStateOf(
            androidx.compose.ui.text.input.TextFieldValue(
                text = initialText,
                selection = androidx.compose.ui.text.TextRange(initialText.length)
            )
        )
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Row(
        modifier = modifier
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BasicTextField(
            value = textValue,
            onValueChange = {
                textValue = it
                onValueChange(it.text)
            },
            textStyle = style,
            cursorBrush = SolidColor(primaryColor),
            visualTransformation = remember(isDark, primaryColor) {
                MarkdownVisualTransformation(isDark, primaryColor)
            },
            modifier = Modifier
                .weight(1f)
                .wrapContentHeight()
                .focusRequester(focusRequester)
        )

        IconButton(
            onClick = onDone,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Done editing",
                tint = primaryColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun EditableBlockContainer(
    index: Int,
    blockType: String,
    onRemove: () -> Unit,
    onMoveBlock: ((from: Int, to: Int) -> Unit)? = null,
    onEnhance: (() -> Unit)? = null,
    showControls: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        if (showControls) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (onMoveBlock != null) {
                        var draggingIndex by remember { mutableStateOf<Int?>(null) }
                        var totalDragAmount by remember { mutableStateOf(0f) }
                        val currentIndex by rememberUpdatedState(index)
                        val moveHandler by rememberUpdatedState(onMoveBlock)

                        Icon(
                            imageVector = Icons.Default.DragIndicator,
                            contentDescription = "Drag to reorder",
                            modifier = Modifier
                                .size(18.dp)
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { _ -> draggingIndex = currentIndex },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            totalDragAmount += dragAmount.y
                                            val threshold = 60f
                                            if (totalDragAmount > threshold) {
                                                moveHandler(currentIndex, currentIndex + 1)
                                                totalDragAmount -= threshold
                                            } else if (totalDragAmount < -threshold && currentIndex > 0) {
                                                moveHandler(currentIndex, currentIndex - 1)
                                                totalDragAmount += threshold
                                            }
                                        },
                                        onDragEnd = { draggingIndex = null; totalDragAmount = 0f },
                                        onDragCancel = { draggingIndex = null; totalDragAmount = 0f }
                                    )
                                },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val icon = when (blockType) {
                        "CODE" -> Icons.Default.Code
                        "DIAGRAM" -> Icons.Default.Build
                        "DRAWING" -> Icons.Default.Create
                        "TEXT" -> Icons.Default.Edit
                        else -> Icons.Default.Image
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = blockType,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onEnhance != null) {
                        TextButton(
                            onClick = onEnhance,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Enhance",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Enhance",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove Block",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = if (showControls) 1.dp else 0.dp,
                    color = if (showControls) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            content()
        }
    }
}

class MarkdownFormatController {
    var onFormatAction: ((String) -> Unit)? = null
}

fun applyFormattingToLocal(
    value: androidx.compose.ui.text.input.TextFieldValue,
    formatType: String
): androidx.compose.ui.text.input.TextFieldValue {
    val currentText = value.text
    val selection = value.selection
    val start = kotlin.math.min(selection.start, selection.end)
    val end = kotlin.math.max(selection.start, selection.end)

    return when (formatType) {
        "BOLD" -> {
            if (start != end) {
                val selectedText = currentText.substring(start, end)
                val (newSelected, _) = toggleInlineFormatLocal(selectedText, "**")
                val replacement = newSelected
                val newText = currentText.replaceRange(start, end, replacement)
                androidx.compose.ui.text.input.TextFieldValue(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(start, start + replacement.length)
                )
            } else {
                val newText = currentText.substring(0, start) + "****" + currentText.substring(start)
                androidx.compose.ui.text.input.TextFieldValue(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(start + 2)
                )
            }
        }
        "ITALIC" -> {
            if (start != end) {
                val selectedText = currentText.substring(start, end)
                val (newSelected, _) = toggleInlineFormatLocal(selectedText, "*")
                val replacement = newSelected
                val newText = currentText.replaceRange(start, end, replacement)
                androidx.compose.ui.text.input.TextFieldValue(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(start, start + replacement.length)
                )
            } else {
                val newText = currentText.substring(0, start) + "**" + currentText.substring(start)
                androidx.compose.ui.text.input.TextFieldValue(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(start + 1)
                )
            }
        }
        "LINK" -> {
            if (start != end) {
                val selectedText = currentText.substring(start, end)
                val wrapped = "[$selectedText](https://github.com)"
                val newText = currentText.replaceRange(start, end, wrapped)
                androidx.compose.ui.text.input.TextFieldValue(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(start, start + wrapped.length)
                )
            } else {
                val insert = "[text](https://github.com)"
                val newText = currentText.substring(0, start) + insert + currentText.substring(start)
                androidx.compose.ui.text.input.TextFieldValue(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(start + 1, start + 5)
                )
            }
        }
        "H1", "H2", "H3", "H4", "H5", "H6", "BULLET" -> {
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

                androidx.compose.ui.text.input.TextFieldValue(
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

                androidx.compose.ui.text.input.TextFieldValue(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(lineStart, lineStart + newSelectedLinesText.length)
                )
            }
        }
        "MATH" -> {
            val insert = "\n$$\nE = mc^2\n$$\n"
            val newText = currentText + insert
            androidx.compose.ui.text.input.TextFieldValue(
                text = newText,
                selection = androidx.compose.ui.text.TextRange(newText.length)
            )
        }
        "TABLE" -> {
            if (start != end) {
                val selectedText = currentText.substring(start, end)
                val wrapped = "\n\n| Header 1 | Header 2 |\n|---|---|\n| $selectedText | |\n\n"
                androidx.compose.ui.text.input.TextFieldValue(
                    text = currentText.replaceRange(start, end, wrapped),
                    selection = androidx.compose.ui.text.TextRange(start, start + wrapped.length)
                )
            } else {
                val insert = "\n\n| Header 1 | Header 2 |\n|---|---|\n| Cell 1 | Cell 2 |\n\n"
                val newText = currentText.substring(0, start) + insert + currentText.substring(start)
                androidx.compose.ui.text.input.TextFieldValue(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(start, start + insert.length)
                )
            }
        }
        else -> value
    }
}

private fun toggleInlineFormatLocal(selectedText: String, delimiter: String): Pair<String, Boolean> {
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
    }
    return Pair(selectedText, false)
}

fun handleBulletListTyping(
    oldValue: androidx.compose.ui.text.input.TextFieldValue,
    newValue: androidx.compose.ui.text.input.TextFieldValue
): androidx.compose.ui.text.input.TextFieldValue {
    val oldText = oldValue.text
    val newText = newValue.text
    
    if (newText.length == oldText.length + 1) {
        val cursor = newValue.selection.start
        if (cursor > 0 && newText[cursor - 1] == '\n') {
            val lastNewlineBefore = newText.lastIndexOf('\n', cursor - 2)
            val lineStart = if (lastNewlineBefore == -1) 0 else lastNewlineBefore + 1
            val lineEnd = cursor - 1
            if (lineStart <= lineEnd) {
                val completedLine = newText.substring(lineStart, lineEnd)
                
                val prefix = when {
                    completedLine.startsWith("-- ") -> "-- "
                    completedLine.startsWith("- ") -> "- "
                    else -> null
                }
                
                if (prefix != null) {
                    if (completedLine == prefix) {
                        if (prefix == "-- ") {
                            val beforeLine = newText.substring(0, lineStart)
                            val afterLine = newText.substring(lineEnd)
                            val resultText = beforeLine + "- " + afterLine
                            val newCursor = cursor - 1
                            return newValue.copy(
                                text = resultText,
                                selection = androidx.compose.ui.text.TextRange(newCursor.coerceIn(0, resultText.length))
                            )
                        } else {
                            val beforeLine = newText.substring(0, lineStart)
                            val afterLine = newText.substring(lineEnd)
                            val resultText = beforeLine + afterLine
                            val newCursor = cursor - prefix.length
                            return newValue.copy(
                                text = resultText,
                                selection = androidx.compose.ui.text.TextRange(newCursor.coerceIn(0, resultText.length))
                            )
                        }
                    } else {
                        val beforeCursor = newText.substring(0, cursor)
                        val afterCursor = newText.substring(cursor)
                        val resultText = beforeCursor + prefix + afterCursor
                        val newCursor = cursor + prefix.length
                        return newValue.copy(
                            text = resultText,
                            selection = androidx.compose.ui.text.TextRange(newCursor.coerceIn(0, resultText.length))
                        )
                    }
                }
            }
        }
    }
    return newValue
}

@Composable
fun MarkdownRenderer(
    text: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onContentChanged: ((String) -> Unit)? = null,
    formatController: MarkdownFormatController? = null,
    onEditDrawingClick: ((DrawingData) -> Unit)? = null,
    fontFamily: FontFamily = FontFamily.Serif,
    onLinkClick: ((String) -> Unit)? = null,
    onHeaderPositioned: ((String, androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    requestFocus: Boolean = false,
    onRequestFocusConsumed: () -> Unit = {},
    onEnhanceBlock: ((String, (String) -> Unit) -> Unit)? = null,
    showMarkdownSymbols: Boolean = false,
    isGeneratingQuiz: Boolean = false
) {
    val blocks = remember(text) { parseMarkdown(text) }
    var editingBlockIndex by remember { mutableStateOf<Int?>(null) }
    var selectedBlockIndex by remember { mutableStateOf<Int?>(null) }
    var enhancingBlockIndex by remember { mutableStateOf<Int?>(null) }
    val isDark = isSystemInDarkTheme()
    val primaryColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(requestFocus) {
        if (requestFocus && onContentChanged != null) {
            val updated = blocks.toMutableList()
            if (updated.isEmpty() || updated.last() !is MarkdownBlock.TextBlock || (updated.last() as MarkdownBlock.TextBlock).text.isNotEmpty()) {
                updated.add(MarkdownBlock.TextBlock(""))
                onContentChanged(blocksToMarkdown(updated))
                editingBlockIndex = updated.size - 1
            } else {
                editingBlockIndex = updated.size - 1
            }
            onRequestFocusConsumed()
        }
    }

    CompositionLocalProvider(LocalMarkdownFontFamily provides fontFamily) {
        val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
        val startPadding = contentPadding.calculateStartPadding(layoutDirection)
        val endPadding = contentPadding.calculateEndPadding(layoutDirection)
        val topPadding = contentPadding.calculateTopPadding()
        val bottomPadding = contentPadding.calculateBottomPadding()

        Column(
            modifier = modifier
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    enabled = onContentChanged != null
                ) {
                    val updated = blocks.toMutableList()
                    if (updated.isEmpty() || updated.last() !is MarkdownBlock.TextBlock || (updated.last() as MarkdownBlock.TextBlock).text.isNotEmpty()) {
                        updated.add(MarkdownBlock.TextBlock(""))
                        onContentChanged?.invoke(blocksToMarkdown(updated))
                        editingBlockIndex = updated.size - 1
                    } else {
                        editingBlockIndex = updated.size - 1
                    }
                },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (blocks.isEmpty()) {
                if (onContentChanged != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .padding(contentPadding)
                    ) {
                        Text(
                            text = "Tap here to write or draw your engineering schematics...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontStyle = FontStyle.Italic
                        )
                    }
                } else {
                    Text(
                        text = "Empty Note",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontStyle = FontStyle.Italic
                    )
                }
            } else {
                if (onContentChanged != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            ) {
                                val updated = blocks.toMutableList()
                                updated.add(0, MarkdownBlock.TextBlock(""))
                                onContentChanged(blocksToMarkdown(updated))
                                editingBlockIndex = 0
                            }
                    )
                }
                
                blocks.forEachIndexed { index, block ->
                    key(index) {
                        val removeBlock = {
                            if (onContentChanged != null) {
                                val updated = blocks.toMutableList()
                                updated.removeAt(index)
                                onContentChanged(blocksToMarkdown(updated))
                                selectedBlockIndex = null
                                editingBlockIndex = null
                            }
                        }
                        val onSelect = {
                            selectedBlockIndex = if (selectedBlockIndex == index) null else index
                        }
                        
                        val enhanceBlock: (() -> Unit)? = if (onEnhanceBlock != null && onContentChanged != null) {
                            {
                                val blockText = blocksToMarkdown(listOf(block))
                                enhancingBlockIndex = index
                                onEnhanceBlock(blockText) { enhanced ->
                                    val updated = blocks.toMutableList()
                                    val newBlocks = parseMarkdown(enhanced)
                                    updated.removeAt(index)
                                    updated.addAll(index, newBlocks)
                                    onContentChanged(blocksToMarkdown(updated))
                                    enhancingBlockIndex = null
                                }
                            }
                        } else null
                        
                        val moveBlock: ((Int, Int) -> Unit)? = if (onContentChanged != null) {
                            { from: Int, to: Int ->
                                if (from in blocks.indices && to in blocks.indices && from != to) {
                                    val updated = blocks.toMutableList()
                                    val blockToMove = updated.removeAt(from)
                                    updated.add(to, blockToMove)
                                    onContentChanged(blocksToMarkdown(updated))
                                    if (editingBlockIndex == from) {
                                        editingBlockIndex = to
                                    } else if (editingBlockIndex == to) {
                                        editingBlockIndex = from
                                    }
                                }
                            }
                        } else null

                        when (block) {
                            is MarkdownBlock.TextBlock -> {
                                if (onContentChanged != null) {
                                    var textValue by remember {
                                        mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(block.text))
                                    }
                                    var isFocused by remember { mutableStateOf(false) }
                                    
                                    LaunchedEffect(isFocused, textValue, blocks, index) {
                                        if (isFocused && formatController != null) {
                                            formatController.onFormatAction = { action ->
                                                val command = com.usoy.papiro.ui.components.LocalFormatCommand(action)
                                                val updated = command.execute(textValue)
                                                textValue = updated
                                                val updatedBlocks = blocks.toMutableList()
                                                updatedBlocks[index] = MarkdownBlock.TextBlock(updated.text)
                                                onContentChanged(blocksToMarkdown(updatedBlocks))
                                            }
                                        }
                                    }
                                    
                                    LaunchedEffect(block.text) {
                                        if (textValue.text != block.text) {
                                            textValue = textValue.copy(text = block.text)
                                        }
                                    }

                                    val focusRequester = remember { FocusRequester() }
                                    LaunchedEffect(editingBlockIndex) {
                                        if (editingBlockIndex == index) {
                                            try { focusRequester.requestFocus() } catch (e: Exception) {}
                                            editingBlockIndex = null // Reset so we don't keep requesting
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        val isEnhancing = enhancingBlockIndex == index
                                        Box(
                                            modifier = Modifier
                                                .width(startPadding)
                                                .padding(top = 16.dp),
                                            contentAlignment = Alignment.TopCenter
                                        ) {
                                            if (isEnhancing) {
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
                                                androidx.compose.material3.Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = "Enhancing",
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .graphicsLayer { rotationZ = rotationState.value },
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            } else if (textValue.text.contains("Table of Contents", ignoreCase = true)) {
                                                TableOfContentDoodle()
                                            }
                                        }
                                        Box(modifier = Modifier.weight(1f).padding(end = endPadding)) {
                                            BasicTextField(
                                            value = textValue,
                                            onValueChange = { newVal ->
                                                val processedValue = handleBulletListTyping(textValue, newVal)
                                                textValue = processedValue
                                                val updatedBlocks = blocks.toMutableList()
                                                updatedBlocks[index] = MarkdownBlock.TextBlock(processedValue.text)
                                                onContentChanged(blocksToMarkdown(updatedBlocks))
                                            },
                                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                                fontFamily = LocalMarkdownFontFamily.current,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 22.sp
                                            ),
                                            cursorBrush = SolidColor(primaryColor),
                                            visualTransformation = remember(isDark, primaryColor, showMarkdownSymbols) {
                                                if (showMarkdownSymbols) {
                                                    androidx.compose.ui.text.input.VisualTransformation.None
                                                } else {
                                                    MarkdownVisualTransformation(isDark, primaryColor)
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .focusRequester(focusRequester)
                                                .onFocusChanged { focusState ->
                                                    isFocused = focusState.isFocused
                                                }
                                        )
                                        if (enhanceBlock != null && textValue.text.isNotBlank()) {
                                            IconButton(
                                                onClick = enhanceBlock,
                                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = "Enhance Text",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                    ) {
                                        if (block.text.contains("Table of Contents", ignoreCase = true)) {
                                            Box(
                                                modifier = Modifier
                                                    .width(startPadding)
                                                    .padding(top = 16.dp),
                                                contentAlignment = Alignment.TopCenter
                                            ) {
                                                TableOfContentDoodle()
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.width(startPadding))
                                        }
                                        Box(modifier = Modifier.weight(1f).padding(end = endPadding)) {
                                        if (block.text.isEmpty()) {
                                            // Empty in preview, show nothing
                                        } else {
                                            Column {
                                                val subBlocks = remember(block.text) { parseMarkdownElements(block.text) }
                                                subBlocks.forEach { subBlock ->
                                                    when (subBlock) {
                                                        is MarkdownBlock.Header -> HeaderView(subBlock, onLinkClick, onHeaderPositioned, null)
                                                        is MarkdownBlock.ListItem -> ListItemView(subBlock, onLinkClick, null)
                                                        is MarkdownBlock.Paragraph -> ParagraphView(subBlock, onLinkClick, null)
                                                        is MarkdownBlock.HorizontalRule -> HorizontalRuleView()
                                                        else -> {}
                                                    }
                                                }
                                        }
                                            }
                                        }
                                    }
                                }
                            }
                            is MarkdownBlock.Header, is MarkdownBlock.ListItem, is MarkdownBlock.Paragraph, is MarkdownBlock.HorizontalRule -> {}
                             is MarkdownBlock.CodeBlock -> {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    val isEnhancing = enhancingBlockIndex == index
                                    val isQuizGen = block.language.lowercase().trim() == "quiz" && isGeneratingQuiz
                                    Box(
                                        modifier = Modifier
                                            .width(startPadding)
                                            .padding(top = 16.dp),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            FunctionDoodle()
                                            if (isEnhancing || isQuizGen) {
                                                Spacer(modifier = Modifier.height(8.dp))
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
                                                androidx.compose.material3.Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = if (isQuizGen) "Generating Quiz" else "Enhancing",
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .graphicsLayer { rotationZ = rotationState.value },
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                    if (block.language.lowercase().trim() == "quiz") {
                                        Box(
                                            modifier = Modifier
                                                .padding(start = 0.dp, end = endPadding, top = topPadding, bottom = bottomPadding)
                                                .weight(1f)
                                        ) {
                                            QuizBlockView(block = block, isGenerating = isQuizGen)
                                        }
                                    } else {
                                        EditableBlockContainer(
                                            index = index,
                                            blockType = "CODE",
                                            onRemove = removeBlock,
                                            onMoveBlock = moveBlock,
                                            onEnhance = enhanceBlock,
                                            showControls = onContentChanged != null,
                                            modifier = Modifier
                                                .padding(start = 0.dp, end = endPadding, top = topPadding, bottom = bottomPadding)
                                                .weight(1f)
                                        ) {
                                            CodeBlockView(
                                                block = block,
                                                onBlockUpdated = if (onContentChanged != null) { { updatedBlock ->
                                                    val updatedBlocks = blocks.map { if (it === block) updatedBlock else it }
                                                    onContentChanged(blocksToMarkdown(updatedBlocks))
                                                } } else null
                                            )
                                        }
                                    }
                                }
                            }
                            is MarkdownBlock.MathBlock -> {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .width(startPadding)
                                            .padding(top = 16.dp),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        MathDoodle()
                                    }
                                    EditableBlockContainer(
                                        index = index,
                                        blockType = "MATH",
                                        onRemove = removeBlock,
                                        onMoveBlock = moveBlock,
                                        onEnhance = null,
                                        showControls = onContentChanged != null,
                                        modifier = Modifier
                                            .padding(start = 0.dp, end = endPadding, top = topPadding, bottom = bottomPadding)
                                            .weight(1f)
                                    ) {
                                        MathBlockView(
                                            block = block,
                                            onBlockUpdated = if (onContentChanged != null) { { updatedBlock ->
                                                val updatedBlocks = blocks.map { if (it === block) updatedBlock else it }
                                                onContentChanged(blocksToMarkdown(updatedBlocks))
                                            } } else null
                                        )
                                    }
                                }
                            }
                            is MarkdownBlock.TableBlock -> {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    val isEnhancing = enhancingBlockIndex == index
                                    Box(
                                        modifier = Modifier
                                            .width(startPadding)
                                            .padding(top = 16.dp),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            TableDoodle()
                                            if (isEnhancing) {
                                                Spacer(modifier = Modifier.height(8.dp))
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
                                                androidx.compose.material3.Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = "Enhancing",
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .graphicsLayer { rotationZ = rotationState.value },
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                    EditableBlockContainer(
                                        index = index,
                                        blockType = "TABLE",
                                        onRemove = removeBlock,
                                        onMoveBlock = moveBlock,
                                        onEnhance = enhanceBlock,
                                        showControls = onContentChanged != null,
                                        modifier = Modifier
                                            .padding(start = 0.dp, end = endPadding, top = topPadding, bottom = bottomPadding)
                                            .weight(1f)
                                    ) {
                                        TableBlockView(
                                            block = block,
                                            onBlockUpdated = if (onContentChanged != null) { { updatedBlock ->
                                                val updatedBlocks = blocks.map { if (it === block) updatedBlock else it }
                                                onContentChanged(blocksToMarkdown(updatedBlocks))
                                            } } else null
                                        )
                                    }
                                }
                            }
                            is MarkdownBlock.DiagramBlock -> {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    val isEnhancing = enhancingBlockIndex == index
                                    Box(
                                        modifier = Modifier
                                            .width(startPadding)
                                            .padding(top = 16.dp),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            MoleculeDoodle()
                                            if (isEnhancing) {
                                                Spacer(modifier = Modifier.height(8.dp))
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
                                                androidx.compose.material3.Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = "Enhancing",
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .graphicsLayer { rotationZ = rotationState.value },
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                    EditableBlockContainer(
                                        index = index,
                                        blockType = "DIAGRAM",
                                        onRemove = removeBlock,
                                        onMoveBlock = moveBlock,
                                        onEnhance = enhanceBlock,
                                        showControls = onContentChanged != null,
                                        modifier = Modifier
                                            .padding(start = 0.dp, end = endPadding, top = topPadding, bottom = bottomPadding)
                                            .weight(1f)
                                    ) {
                                        DiagramBlockView(
                                            block = block,
                                            onBlockUpdated = if (onContentChanged != null) { { updatedBlock ->
                                                val updatedBlocks = blocks.map { if (it === block) updatedBlock else it }
                                                onContentChanged(blocksToMarkdown(updatedBlocks))
                                            } } else null
                                        )
                                    }
                                }
                            }
                            is MarkdownBlock.DrawingBlock -> {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .width(startPadding)
                                            .padding(top = 16.dp),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        BlueprintDoodle()
                                    }
                                    EditableBlockContainer(
                                        index = index,
                                        blockType = "DRAWING",
                                        onRemove = removeBlock,
                                        onMoveBlock = moveBlock,
                                        onEnhance = null,
                                        showControls = onContentChanged != null,
                                        modifier = Modifier
                                            .padding(start = 0.dp, end = endPadding, top = topPadding, bottom = bottomPadding)
                                            .weight(1f)
                                    ) {
                                        if (onContentChanged != null && onEditDrawingClick != null) {
                                            DrawingBlockView(
                                                block = block,
                                                onContentChanged = onContentChanged,
                                                currentContent = text,
                                                onEditClick = onEditDrawingClick
                                            )
                                        } else {
                                            val drawingData = remember(block.json) { DrawingData.fromJson(block.json) }
                                            if (drawingData != null) {
                                                val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
                                                val themeOnBgColor = MaterialTheme.colorScheme.onBackground
                                                val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(drawingData.height.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Canvas(
                                                        modifier = Modifier
                                                            .width(drawingData.width.dp)
                                                            .height(drawingData.height.dp)
                                                            .graphicsLayer { alpha = 0.99f }
                                                    ) {
                                                        val d = density
                                                        for (path in drawingData.paths) {
                                                            var pathColor = Color(path.color)
                                                            var blendMode = androidx.compose.ui.graphics.BlendMode.SrcOver
                                                            
                                                            if (path.isEraser) {
                                                                pathColor = Color.Transparent
                                                                blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                                                            } else if (isDarkTheme) {
                                                                if (path.color == android.graphics.Color.BLACK || path.color == android.graphics.Color.parseColor("#121212")) {
                                                                    pathColor = themeOnBgColor
                                                                }
                                                            } else {
                                                                if (path.color == android.graphics.Color.WHITE || path.color == android.graphics.Color.parseColor("#FFFFFF")) {
                                                                    pathColor = themeOnBgColor
                                                                }
                                                            }

                                                            if (path.points.size > 1) {
                                                                val strokePath = Path().apply {
                                                                    val first = path.points.first()
                                                                    moveTo(first.x * d, first.y * d)
                                                                    for (i in 1 until path.points.size) {
                                                                        val pt = path.points[i]
                                                                        lineTo(pt.x * d, pt.y * d)
                                                                    }
                                                                }
                                                                drawPath(
                                                                    path = strokePath,
                                                                    color = pathColor,
                                                                    style = Stroke(
                                                                        width = path.strokeWidth * d,
                                                                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                                                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                                                                    ),
                                                                    blendMode = blendMode
                                                                )
                                                            } else if (path.points.size == 1) {
                                                                val pt = path.points.first()
                                                                drawCircle(
                                                                    color = pathColor,
                                                                    radius = (path.strokeWidth * d) / 2f,
                                                                    center = Offset(pt.x * d, pt.y * d),
                                                                    blendMode = blendMode
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            is MarkdownBlock.Image -> {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .width(startPadding)
                                            .padding(top = 16.dp),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        BlueprintDoodle()
                                    }
                                    EditableBlockContainer(
                                        index = index,
                                        blockType = "IMAGE",
                                        onRemove = removeBlock,
                                        onMoveBlock = moveBlock,
                                        onEnhance = null,
                                        showControls = onContentChanged != null,
                                        modifier = Modifier
                                            .padding(start = 0.dp, end = endPadding, top = topPadding, bottom = bottomPadding)
                                            .weight(1f)
                                    ) {
                                        ImageView(block)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Insert placeholder QuizBlockView if we are generating a quiz but no quiz block is parsed/loaded yet
            val hasQuizBlock = blocks.any { it is MarkdownBlock.CodeBlock && it.language.lowercase().trim() == "quiz" }
            if (isGeneratingQuiz && !hasQuizBlock) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .width(startPadding)
                            .padding(top = 16.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FunctionDoodle()
                            Spacer(modifier = Modifier.height(8.dp))
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
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Generating Quiz",
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer { rotationZ = rotationState.value },
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .padding(start = 0.dp, end = endPadding, top = topPadding, bottom = bottomPadding)
                            .weight(1f)
                    ) {
                        QuizBlockView(
                            block = MarkdownBlock.CodeBlock(language = "quiz", code = ""),
                            isGenerating = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImageView(block: MarkdownBlock.Image) {
    var hasError by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    
    if (showPreview) {
        Dialog(
            onDismissRequest = { showPreview = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ZoomableBox(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        coil.compose.AsyncImage(
                            model = block.path,
                            contentDescription = block.altText,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    }

                    IconButton(
                        onClick = { showPreview = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Preview",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { showPreview = true },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = block.altText.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (hasError) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Failed to load image from: ${block.path}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                coil.compose.AsyncImage(
                    model = block.path,
                    contentDescription = block.altText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    onError = {
                        hasError = true
                    }
                )
            }
        }
    }
}

@Composable
fun ClickableMarkdownText(
    annotatedString: AnnotatedString,
    style: TextStyle,
    color: Color = Color.Unspecified,
    lineHeight: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    onLinkClick: ((String) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val context = LocalContext.current
    
    var layoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }

    Text(
        text = annotatedString,
        style = style.copy(color = color, lineHeight = lineHeight),
        onTextLayout = { layoutResult = it },
        modifier = Modifier.pointerInput(annotatedString) {
            detectTapGestures { pos ->
                var isClickHandled = false
                layoutResult?.let { layout ->
                    val offset = layout.getOffsetForPosition(pos)
                    val annotation = annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset).firstOrNull()
                    if (annotation != null) {
                        isClickHandled = true
                        val url = annotation.item
                        if (url.startsWith("#")) {
                            onLinkClick?.invoke(url)
                        } else {
                            try {
                                uriHandler.openUri(url)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open link: $url", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                if (!isClickHandled) {
                    onClick?.invoke()
                }
            }
        }
    )
}

@Composable
fun HeaderView(
    block: MarkdownBlock.Header,
    onLinkClick: ((String) -> Unit)? = null,
    onHeaderPositioned: ((String, androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val customFontFamily = LocalMarkdownFontFamily.current
    val style = when (block.level) {
        1 -> MaterialTheme.typography.titleLarge
        2 -> MaterialTheme.typography.titleMedium
        else -> MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
    }

    Column(modifier = Modifier
        .padding(vertical = 6.dp)
        .onGloballyPositioned { coordinates ->
            val anchor = block.text.lowercase().replace(" ", "-").replace(Regex("[^a-z0-9-]"), "")
            onHeaderPositioned?.invoke(anchor, coordinates)
        }
    ) {
        ClickableMarkdownText(
            annotatedString = parseInlineMarkdown(block.text),
            style = style.copy(
                fontFamily = customFontFamily,
                fontStyle = FontStyle.Italic
            ),
            color = MaterialTheme.colorScheme.secondary,
            onLinkClick = onLinkClick,
            onClick = onClick
        )
        if (block.level == 1) {
            HorizontalDivider(
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
                thickness = 1.5.dp
            )
        }
    }
}

@Composable
fun ListItemView(
    block: MarkdownBlock.ListItem,
    onLinkClick: ((String) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val customFontFamily = LocalMarkdownFontFamily.current
    val isChild = block.level == 2
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isChild) 24.dp else 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = if (isChild) "○" else "•",
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = customFontFamily),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp)
        )
        ClickableMarkdownText(
            annotatedString = parseInlineMarkdown(block.text),
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = customFontFamily),
            color = MaterialTheme.colorScheme.onSurface,
            onLinkClick = onLinkClick,
            onClick = onClick
        )
    }
}

@Composable
fun ParagraphView(
    block: MarkdownBlock.Paragraph,
    onLinkClick: ((String) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val customFontFamily = LocalMarkdownFontFamily.current
    ClickableMarkdownText(
        annotatedString = parseInlineMarkdown(block.text),
        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = customFontFamily),
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = 22.sp,
        onLinkClick = onLinkClick,
        onClick = onClick
    )
}

@Composable
fun HorizontalRuleView() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        thickness = 1.dp
    )
}

fun blocksToMarkdown(blocks: List<MarkdownBlock>): String {
    return blocks.joinToString("\n") { block ->
        when (block) {
            is MarkdownBlock.HorizontalRule -> "---"
            is MarkdownBlock.TextBlock -> block.text
            is MarkdownBlock.Header -> "#".repeat(block.level) + " " + block.text
            is MarkdownBlock.Paragraph -> block.text
            is MarkdownBlock.ListItem -> (if (block.level == 2) "-- " else "- ") + block.text
            is MarkdownBlock.CodeBlock -> "```${block.language}\n${block.code}\n```"
            is MarkdownBlock.DiagramBlock -> if (block.title.lowercase() == "mermaid" || block.title.lowercase() == "diagram" || block.title.isEmpty()) {
                "```mermaid\n${block.syntax}\n```"
            } else {
                "```mermaid ${block.title}\n${block.syntax}\n```"
            }
            is MarkdownBlock.DrawingBlock -> "```drawing\n${block.json}\n```"
            is MarkdownBlock.Image -> "![${block.altText}](${block.path})"
            is MarkdownBlock.MathBlock -> "$$\n${block.equation}\n$$"
            is MarkdownBlock.TableBlock -> {
                val sb = StringBuilder()
                sb.append("| ").append(block.headers.joinToString(" | ")).append(" |\n")
                sb.append("| ").append(block.alignments.map { align ->
                    when (align) {
                        "center" -> ":---:"
                        "right" -> "---:"
                        else -> ":---"
                    }
                }.joinToString(" | ")).append(" |")
                if (block.rows.isNotEmpty()) {
                    sb.append("\n")
                    sb.append(block.rows.joinToString("\n") { row ->
                        "| " + row.joinToString(" | ") + " |"
                    })
                }
                sb.toString()
            }
        }
    }
}

@Composable
fun CodeBlockView(
    block: MarkdownBlock.CodeBlock,
    onBlockUpdated: ((MarkdownBlock.CodeBlock) -> Unit)? = null
) {
    val context = LocalContext.current
    var isEditing by remember { mutableStateOf(false) }
    var codeText by remember(block.code) { mutableStateOf(block.code) }
    var languageText by remember(block.language) { mutableStateOf(block.language) }
    val highlightedCode = remember(codeText) { highlightCode(codeText) }

    val isExecutable = remember(languageText) {
        val lang = languageText.lowercase().trim()
        lang == "html" || lang == "htm" || lang == "svg" || lang == "xml"
    }
    var isRunning by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditing && onBlockUpdated != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "LANG:",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        BasicTextField(
                            value = languageText,
                            onValueChange = {
                                languageText = it
                                onBlockUpdated(block.copy(language = it, code = codeText))
                            },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = true,
                            modifier = Modifier
                                .width(100.dp)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            decorationBox = { innerTextField ->
                                if (languageText.isEmpty()) {
                                    Text(
                                        text = "code",
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                                innerTextField()
                             }
                        )
                    }
                } else {
                    Text(
                        text = block.language.uppercase().ifEmpty { "CODE" },
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isExecutable) {
                        IconButton(
                            onClick = { isRunning = !isRunning },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = if (isRunning) "Stop HTML Preview" else "Run HTML Preview",
                                tint = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (onBlockUpdated != null) {
                        IconButton(
                            onClick = { isEditing = !isEditing },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Code,
                                contentDescription = if (isEditing) "Finish Editing" else "Edit Code",
                                tint = if (isEditing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Copied Code", codeText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy code",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (isEditing && onBlockUpdated != null) {
                BasicTextField(
                    value = codeText,
                    onValueChange = {
                        codeText = it
                        onBlockUpdated(block.copy(code = it, language = languageText))
                    },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                        .padding(8.dp)
                )
            } else {
                Text(
                    text = highlightedCode,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (isExecutable && isRunning) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Preview header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Running",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Live Preview",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { refreshTrigger++ },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload Preview",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        IconButton(
                            onClick = { isRunning = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Preview",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                
                // WebView component inside a Box with fixed height and scroll/interaction support
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                        .background(androidx.compose.ui.graphics.Color.White)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                        )
                ) {
                    val key = remember(codeText, refreshTrigger) { "$codeText-$refreshTrigger" }
                    key.let { _ ->
                        androidx.compose.ui.viewinterop.AndroidView(
                            factory = { ctx ->
                                android.webkit.WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.useWideViewPort = true
                                    settings.loadWithOverviewMode = true
                                    webViewClient = android.webkit.WebViewClient()
                                    setBackgroundColor(android.graphics.Color.WHITE)
                                }
                            },
                            update = { webView ->
                                val htmlContent = if (languageText.lowercase().trim() == "svg") {
                                    """
                                    <!DOCTYPE html>
                                    <html>
                                    <head>
                                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                        <style>
                                            body {
                                                margin: 0;
                                                padding: 16px;
                                                display: flex;
                                                justify-content: center;
                                                align-items: center;
                                                min-height: 100vh;
                                                background-color: transparent;
                                            }
                                            svg {
                                                max-width: 100%;
                                                max-height: 100%;
                                            }
                                        </style>
                                    </head>
                                    <body>
                                        $codeText
                                    </body>
                                    </html>
                                    """.trimIndent()
                                } else {
                                    codeText
                                }
                                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

        }
    }
}

@Composable
fun QuizBlockView(
    block: MarkdownBlock.CodeBlock,
    isGenerating: Boolean = false
) {
    val context = LocalContext.current
    var showQuizWebView by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "Quiz Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Interactive Quiz",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isGenerating) "Compiling questions from your note..." else "Includes auto-checker & review mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Text(
                text = "Challenge yourself with interactive study questions compiled from your uploaded file. Complete the test to view corrections and detailed explanations.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Button(
                onClick = { if (!isGenerating) showQuizWebView = true },
                enabled = !isGenerating,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGenerating) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                    contentColor = if (isGenerating) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                if (isGenerating) {
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
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Generating",
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { rotationZ = rotationState.value },
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generating Quiz Questions...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Quiz",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Quiz", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }

    if (showQuizWebView) {
        Dialog(
            onDismissRequest = { showQuizWebView = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(onClick = { showQuizWebView = false }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Quiz",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    "Interactive Study Quiz",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            IconButton(onClick = { refreshTrigger++ }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reload Quiz",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    
                    // WebView itself
                    val cleanHtml = remember(block.code) {
                        // Extract HTML if it has markdown wrapper or use raw code
                        var html = block.code.trim()
                        if (html.startsWith("```html")) {
                            html = html.substring(7)
                        }
                        if (html.endsWith("```")) {
                            html = html.substring(0, html.length - 3)
                        }
                        html.trim()
                    }
                    
                    val key = remember(cleanHtml, refreshTrigger) { "$cleanHtml-$refreshTrigger" }
                    key.let { _ ->
                        androidx.compose.ui.viewinterop.AndroidView(
                            factory = { ctx ->
                                android.webkit.WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.useWideViewPort = true
                                    settings.loadWithOverviewMode = true
                                    webViewClient = android.webkit.WebViewClient()
                                }
                            },
                            update = { webView ->
                                webView.loadDataWithBaseURL(null, cleanHtml, "text/html", "UTF-8", null)
                            },
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

enum class TableHeaderStyle {
    ROW, COL, NONE
}

enum class TableHeaderColor {
    DEFAULT, PRIMARY, SECONDARY, GREEN, BLUE, RED, PURPLE
}

fun parseTableStyle(headerCell: String): Triple<TableHeaderStyle, TableHeaderColor, String> {
    val regex = """<!--style:([a-zA-Z0-9_]+),([a-zA-Z0-9_]+),([a-zA-Z0-9_#]+)-->""".toRegex()
    val match = regex.find(headerCell)
    if (match != null) {
        val rowColStr = match.groupValues[1]
        val colorStr = match.groupValues[2]
        
        val style = when (rowColStr) {
            "row" -> TableHeaderStyle.ROW
            "col" -> TableHeaderStyle.COL
            "none" -> TableHeaderStyle.NONE
            else -> TableHeaderStyle.ROW
        }
        
        val color = try {
            TableHeaderColor.valueOf(colorStr.uppercase())
        } catch (e: Exception) {
            TableHeaderColor.DEFAULT
        }
        
        val cleanHeader = headerCell.replace(match.value, "").trim()
        return Triple(style, color, cleanHeader)
    }
    return Triple(TableHeaderStyle.ROW, TableHeaderColor.DEFAULT, headerCell)
}

fun updateTableBlockWithStyle(
    headers: List<String>,
    rows: List<List<String>>,
    alignments: List<String>,
    style: TableHeaderStyle,
    color: TableHeaderColor,
    onBlockUpdated: (MarkdownBlock.TableBlock) -> Unit
) {
    val cleanHeaders = headers.toMutableList()
    val firstHeaderRaw = cleanHeaders.firstOrNull() ?: ""
    val regex = """<!--style:[a-zA-Z0-9_]+,[a-zA-Z0-9_]+,[a-zA-Z0-9_#]+-->""".toRegex()
    val baseFirstHeader = firstHeaderRaw.replace(regex, "").trim()
    
    val rowStyleStr = when (style) {
        TableHeaderStyle.ROW -> "row"
        TableHeaderStyle.COL -> "col"
        TableHeaderStyle.NONE -> "none"
    }
    val colorStr = color.name.lowercase()
    
    if (cleanHeaders.isNotEmpty()) {
        cleanHeaders[0] = "$baseFirstHeader <!--style:${rowStyleStr},${colorStr},default-->"
    }
    
    onBlockUpdated(
        MarkdownBlock.TableBlock(
            headers = cleanHeaders,
            rows = rows,
            alignments = alignments
        )
    )
}

fun resizeTable(
    block: MarkdownBlock.TableBlock,
    newRows: Int,
    newCols: Int
): Pair<List<String>, List<List<String>>> {
    val currentHeaders = block.headers
    val currentRows = block.rows
    
    val newHeaders = List(newCols) { col ->
        currentHeaders.getOrNull(col) ?: "Header ${col + 1}"
    }
    
    val newRowsList = List(newRows) { rowIdx ->
        val currentRow = currentRows.getOrNull(rowIdx)
        List(newCols) { colIdx ->
            currentRow?.getOrNull(colIdx) ?: "Cell ${rowIdx + 1}-${colIdx + 1}"
        }
    }
    
    return Pair(newHeaders, newRowsList)
}

fun generateMarkdownTable(
    rows: Int,
    cols: Int,
    headerStyle: TableHeaderStyle,
    headerColor: TableHeaderColor
): String {
    val sb = StringBuilder()
    sb.append("\n\n")
    
    val rowStyleStr = when (headerStyle) {
        TableHeaderStyle.ROW -> "row"
        TableHeaderStyle.COL -> "col"
        TableHeaderStyle.NONE -> "none"
    }
    val colorStr = headerColor.name.lowercase()
    
    // Header Row
    sb.append("| ")
    for (c in 0 until cols) {
        val cellName = if (c == 0) {
            val styleTag = "<!--style:${rowStyleStr},${colorStr},default-->"
            if (headerStyle == TableHeaderStyle.COL) {
                "Title $styleTag"
            } else if (headerStyle == TableHeaderStyle.NONE) {
                "Cell 1-1 $styleTag"
            } else {
                "Header 1 $styleTag"
            }
        } else {
            if (headerStyle == TableHeaderStyle.COL) {
                "Col $c"
            } else if (headerStyle == TableHeaderStyle.NONE) {
                "Cell 1-${c + 1}"
            } else {
                "Header ${c + 1}"
            }
        }
        sb.append(cellName)
        if (c < cols - 1) sb.append(" | ")
    }
    sb.append(" |\n")
    
    // Delimiter Row
    sb.append("| ")
    for (c in 0 until cols) {
        sb.append("---")
        if (c < cols - 1) sb.append(" | ")
    }
    sb.append(" |\n")
    
    // Rows
    for (r in 0 until rows) {
        sb.append("| ")
        for (c in 0 until cols) {
            val cellName = if (headerStyle == TableHeaderStyle.COL && c == 0) {
                "Header ${r + 1}"
            } else if (headerStyle == TableHeaderStyle.COL) {
                "Cell ${r + 1}-${c}"
            } else if (headerStyle == TableHeaderStyle.NONE) {
                "Cell ${r + 2}-${c + 1}"
            } else {
                "Cell ${r + 1}-${c + 1}"
            }
            sb.append(cellName)
            if (c < cols - 1) sb.append(" | ")
        }
        sb.append(" |")
        if (r < rows - 1) sb.append("\n")
    }
    sb.append("\n\n")
    return sb.toString()
}

@Composable
fun TableCell(
    text: String,
    isHeader: Boolean,
    isTitleStyle: Boolean,
    headerColor: TableHeaderColor,
    isFocused: Boolean,
    onClick: () -> Unit,
    onValueChange: (String) -> Unit,
    onFocusLost: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    
    val (bgColor, textColor) = when {
        isTitleStyle -> {
            when (headerColor) {
                TableHeaderColor.DEFAULT -> Pair(
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    MaterialTheme.colorScheme.onSecondaryContainer
                )
                TableHeaderColor.PRIMARY -> Pair(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer
                )
                TableHeaderColor.SECONDARY -> Pair(
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.onSecondaryContainer
                )
                TableHeaderColor.GREEN -> {
                    if (isDark) Pair(Color(0xFF1B5E20), Color(0xFFC8E6C9))
                    else Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
                }
                TableHeaderColor.BLUE -> {
                    if (isDark) Pair(Color(0xFF0D47A1), Color(0xFFBBDEFB))
                    else Pair(Color(0xFFE3F2FD), Color(0xFF1565C0))
                }
                TableHeaderColor.RED -> {
                    if (isDark) Pair(Color(0xFFB71C1C), Color(0xFFFFCDD2))
                    else Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
                }
                TableHeaderColor.PURPLE -> {
                    if (isDark) Pair(Color(0xFF4A148C), Color(0xFFE1BEE7))
                    else Pair(Color(0xFFF3E5F5), Color(0xFF6A1B9A))
                }
            }
        }
        else -> Pair(Color.Transparent, MaterialTheme.colorScheme.onSurface)
    }

    Box(
        modifier = modifier
            .background(bgColor)
            .clickable(enabled = !isFocused) { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (isFocused) {
            var tempText by remember(text) { mutableStateOf(text) }
            val focusRequester = remember { FocusRequester() }
            var hasGainedFocus by remember { mutableStateOf(false) }
            
            BasicTextField(
                value = tempText,
                onValueChange = {
                    tempText = it
                    onValueChange(it)
                },
                textStyle = TextStyle(
                    color = if (isTitleStyle) textColor else MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal
                ),
                cursorBrush = SolidColor(if (isTitleStyle) textColor else MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            hasGainedFocus = true
                        } else if (hasGainedFocus) {
                            onFocusLost()
                        }
                    },
                singleLine = true
            )
            
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        } else {
            val annotatedText = parseInlineMarkdown(text)
            Text(
                text = annotatedText,
                style = TextStyle(
                    color = if (isTitleStyle) textColor else MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = if (isHeader || isTitleStyle) FontWeight.Bold else FontWeight.Normal
                )
            )
        }
    }
}

@Composable
fun TableConfigDialog(
    initialRows: Int,
    initialCols: Int,
    initialStyle: TableHeaderStyle,
    initialColor: TableHeaderColor,
    onDismiss: () -> Unit,
    onConfirm: (rows: Int, cols: Int, style: TableHeaderStyle, color: TableHeaderColor) -> Unit
) {
    var rowsVal by remember { mutableStateOf(initialRows.toString()) }
    var colsVal by remember { mutableStateOf(initialCols.toString()) }
    var styleVal by remember { mutableStateOf(initialStyle) }
    var colorVal by remember { mutableStateOf(initialColor) }
    
    var styleDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Table Settings") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = rowsVal,
                        onValueChange = { rowsVal = it.filter { c -> c.isDigit() } },
                        label = { Text("Rows") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = colsVal,
                        onValueChange = { colsVal = it.filter { c -> c.isDigit() } },
                        label = { Text("Columns") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Column {
                    Text(
                        text = "Header Layout",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { styleDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (styleVal) {
                                        TableHeaderStyle.ROW -> "Header Row"
                                        TableHeaderStyle.COL -> "Header Column"
                                        TableHeaderStyle.NONE -> "No Headers"
                                    },
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Dropdown"
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = styleDropdownExpanded,
                            onDismissRequest = { styleDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DropdownMenuItem(
                                text = { Text("Header Row") },
                                onClick = {
                                    styleVal = TableHeaderStyle.ROW
                                    styleDropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Header Column") },
                                onClick = {
                                    styleVal = TableHeaderStyle.COL
                                    styleDropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("No Headers") },
                                onClick = {
                                    styleVal = TableHeaderStyle.NONE
                                    styleDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = "Header Color Theme",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TableHeaderColor.values().forEach { colOpt ->
                            val isSelected = colorVal == colOpt
                            val circleColor = when (colOpt) {
                                TableHeaderColor.DEFAULT -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                TableHeaderColor.PRIMARY -> MaterialTheme.colorScheme.primary
                                TableHeaderColor.SECONDARY -> MaterialTheme.colorScheme.secondary
                                TableHeaderColor.GREEN -> Color(0xFF2E7D32)
                                TableHeaderColor.BLUE -> Color(0xFF1565C0)
                                TableHeaderColor.RED -> Color(0xFFC62828)
                                TableHeaderColor.PURPLE -> Color(0xFF6A1B9A)
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(circleColor)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { colorVal = colOpt },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = if (colOpt == TableHeaderColor.DEFAULT) Color.Black else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val r = rowsVal.toIntOrNull()?.coerceIn(1, 100) ?: 3
                    val c = colsVal.toIntOrNull()?.coerceIn(1, 20) ?: 3
                    onConfirm(r, c, styleVal, colorVal)
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TableBlockView(
    block: MarkdownBlock.TableBlock,
    onBlockUpdated: ((MarkdownBlock.TableBlock) -> Unit)? = null
) {
    var isEditingMarkdown by remember { mutableStateOf(false) }
    var tableText by remember(block) { mutableStateOf(blocksToMarkdown(listOf(block))) }
    
    val firstHeader = block.headers.firstOrNull() ?: ""
    val (headerStyle, headerColor, cleanFirstHeader) = parseTableStyle(firstHeader)
    
    val maxChars = remember(block.headers, block.rows, cleanFirstHeader) {
        List(block.headers.size) { colIndex ->
            val headerText = if (colIndex == 0) cleanFirstHeader else block.headers.getOrNull(colIndex) ?: ""
            val headerLen = headerText.length
            val rowLens = block.rows.map { row ->
                row.getOrNull(colIndex)?.length ?: 0
            }
            (rowLens + headerLen).maxOrNull() ?: 5
        }
    }
    
    var showConfigDialog by remember { mutableStateOf(false) }
    
    var activeEditingCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var editingCellValue by remember { mutableStateOf("") }

    if (showConfigDialog && onBlockUpdated != null) {
        TableConfigDialog(
            initialRows = block.rows.size,
            initialCols = block.headers.size,
            initialStyle = headerStyle,
            initialColor = headerColor,
            onDismiss = { showConfigDialog = false },
            onConfirm = { newRows, newCols, style, color ->
                showConfigDialog = false
                val (resizedHeaders, resizedRows) = resizeTable(block, newRows, newCols)
                updateTableBlockWithStyle(
                    headers = resizedHeaders,
                    rows = resizedRows,
                    alignments = List(newCols) { "left" },
                    style = style,
                    color = color,
                    onBlockUpdated = onBlockUpdated
                )
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "TABLE (${block.rows.size}x${block.headers.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBlockUpdated != null) {
                        IconButton(
                            onClick = { showConfigDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Table settings",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (isEditingMarkdown) {
                                    val parsed = parseMarkdown(tableText)
                                    if (parsed.isNotEmpty() && parsed.first() is MarkdownBlock.TableBlock) {
                                        onBlockUpdated(parsed.first() as MarkdownBlock.TableBlock)
                                    }
                                }
                                isEditingMarkdown = !isEditingMarkdown
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isEditingMarkdown) Icons.Default.Check else Icons.Default.Code,
                                contentDescription = if (isEditingMarkdown) "Finish Editing" else "Edit Raw Markdown",
                                tint = if (isEditingMarkdown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (isEditingMarkdown && onBlockUpdated != null) {
                BasicTextField(
                    value = tableText,
                    onValueChange = { tableText = it },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp), RoundedCornerShape(4.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(8.dp)
                )
            } else {
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(min = 400.dp)
                    ) {
                        val showHeaderRow = headerStyle == TableHeaderStyle.ROW
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            block.headers.forEachIndexed { colIndex, header ->
                                if (colIndex > 0) {
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .fillMaxHeight()
                                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                    )
                                }
                                
                                val isCellFocused = activeEditingCell == Pair(-1, colIndex)
                                val cleanCellText = if (colIndex == 0) cleanFirstHeader else header
                                
                                TableCell(
                                    text = cleanCellText,
                                    isHeader = true,
                                    isTitleStyle = showHeaderRow,
                                    headerColor = headerColor,
                                    isFocused = isCellFocused,
                                    onClick = {
                                        if (onBlockUpdated != null) {
                                            activeEditingCell = Pair(-1, colIndex)
                                            editingCellValue = cleanCellText
                                        }
                                    },
                                    onValueChange = { newValue ->
                                        editingCellValue = newValue
                                    },
                                    onFocusLost = {
                                        if (activeEditingCell == Pair(-1, colIndex)) {
                                            activeEditingCell = null
                                            val updatedHeaders = block.headers.toMutableList()
                                            updatedHeaders[colIndex] = editingCellValue
                                            updateTableBlockWithStyle(
                                                headers = updatedHeaders,
                                                rows = block.rows,
                                                alignments = block.alignments,
                                                style = headerStyle,
                                                color = headerColor,
                                                onBlockUpdated = onBlockUpdated!!
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .width(
                                            remember(maxChars, colIndex) {
                                                val chars = maxChars.getOrNull(colIndex) ?: 15
                                                (chars * 8 + 48).dp.coerceIn(120.dp, 400.dp)
                                            }
                                        )
                                )
                            }
                        }

                        block.rows.forEachIndexed { rowIndex, row ->
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            
                            val isAltRow = rowIndex % 2 == 0
                            val defaultRowBg = if (isAltRow) {
                                MaterialTheme.colorScheme.surfaceColorAtElevation(1.5.dp)
                            } else {
                                MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min)
                                    .background(defaultRowBg),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                row.forEachIndexed { colIndex, cell ->
                                    if (colIndex > 0) {
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .fillMaxHeight()
                                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                        )
                                    }

                                    val isColHeader = headerStyle == TableHeaderStyle.COL && colIndex == 0
                                    val isCellFocused = activeEditingCell == Pair(rowIndex, colIndex)

                                    TableCell(
                                        text = cell,
                                        isHeader = false,
                                        isTitleStyle = isColHeader,
                                        headerColor = headerColor,
                                        isFocused = isCellFocused,
                                        onClick = {
                                            if (onBlockUpdated != null) {
                                                activeEditingCell = Pair(rowIndex, colIndex)
                                                editingCellValue = cell
                                            }
                                        },
                                        onValueChange = { newValue ->
                                            editingCellValue = newValue
                                        },
                                        onFocusLost = {
                                            if (activeEditingCell == Pair(rowIndex, colIndex)) {
                                                activeEditingCell = null
                                                val updatedRows = block.rows.map { it.toMutableList() }
                                                updatedRows[rowIndex][colIndex] = editingCellValue
                                                updateTableBlockWithStyle(
                                                    headers = block.headers,
                                                    rows = updatedRows,
                                                    alignments = block.alignments,
                                                    style = headerStyle,
                                                    color = headerColor,
                                                    onBlockUpdated = onBlockUpdated!!
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .width(
                                                remember(maxChars, colIndex) {
                                                    val chars = maxChars.getOrNull(colIndex) ?: 15
                                                    (chars * 8 + 48).dp.coerceIn(120.dp, 400.dp)
                                                }
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun shouldUseMermaid(title: String, syntax: String): Boolean {
    val lowerTitle = title.lowercase().trim()
    if (lowerTitle == "mermaid") return true
    
    val cleanSyntax = syntax.trim()
    val hasMermaidKeywords = cleanSyntax.startsWith("graph ") || 
                             cleanSyntax.startsWith("graph\t") ||
                             cleanSyntax.startsWith("flowchart ") || 
                             cleanSyntax.startsWith("flowchart\t") ||
                             cleanSyntax.startsWith("sequenceDiagram") || 
                             cleanSyntax.startsWith("classDiagram") || 
                             cleanSyntax.startsWith("stateDiagram") || 
                             cleanSyntax.startsWith("erDiagram") || 
                             cleanSyntax.startsWith("gantt") || 
                             cleanSyntax.startsWith("pie") || 
                             cleanSyntax.startsWith("gitGraph") ||
                             cleanSyntax.startsWith("mindmap") ||
                             cleanSyntax.startsWith("journey")
    
    if (hasMermaidKeywords) return true
    
    val isKnownMermaidType = lowerTitle == "flowchart" || 
                             lowerTitle == "sequence" || 
                             lowerTitle == "gantt" || 
                             lowerTitle == "class" || 
                             lowerTitle == "state" || 
                             lowerTitle == "er" || 
                             lowerTitle == "pie" || 
                             lowerTitle == "gitgraph" || 
                             lowerTitle == "journey" || 
                             lowerTitle == "mindmap"
    if (isKnownMermaidType) return true
    
    if (lowerTitle == "diagram" || lowerTitle.isEmpty()) {
        val simpleNodeRegex = Regex("^\\[.*?\\](?:\\s*->\\s*\\[.*?\\])*$")
        if (!cleanSyntax.replace("\n", "").trim().matches(simpleNodeRegex)) {
            return true
        }
    }
    
    return false
}

@Composable
private fun HorizontalFlowDiagram(
    nodes: List<String>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        nodes.forEachIndexed { index, nodeText ->
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = nodeText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            if (index < nodes.size - 1) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Arrow",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun DiagramBlockView(
    block: MarkdownBlock.DiagramBlock,
    onBlockUpdated: ((MarkdownBlock.DiagramBlock) -> Unit)? = null
) {
    var isEditing by remember { mutableStateOf(false) }
    var syntaxText by remember(block.syntax) { mutableStateOf(block.syntax) }
    var diagramTitle by remember(block.title) { mutableStateOf(block.title) }
    var showPreview by remember { mutableStateOf(false) }
    
    val isDark = isSystemInDarkTheme()

    if (showPreview) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showPreview = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (shouldUseMermaid(diagramTitle, syntaxText)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 56.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            MermaidRenderer(
                                mermaidCode = syntaxText,
                                isDark = isDark,
                                zoomable = true,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        val nodes = remember(syntaxText) { parseDiagramNodes(syntaxText) }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 56.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ZoomableBox(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                HorizontalFlowDiagram(
                                    nodes = nodes,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { showPreview = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(
                                color = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Preview",
                            tint = if (isDark) Color.White else Color.Black
                        )
                    }
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .then(
                if (!isEditing) {
                    Modifier.clickable { showPreview = true }
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditing && onBlockUpdated != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "DIAGRAM:",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        BasicTextField(
                            value = diagramTitle,
                            onValueChange = {
                                diagramTitle = it
                                onBlockUpdated(block.copy(syntax = syntaxText, title = it))
                            },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = true,
                            modifier = Modifier
                                .width(150.dp)
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            decorationBox = { innerTextField ->
                                if (diagramTitle.isEmpty()) {
                                    Text(
                                        text = "diagram",
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                } else {
                    Text(
                        text = if (diagramTitle.isEmpty()) "DIAGRAM" else diagramTitle.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                if (onBlockUpdated != null) {
                    IconButton(
                        onClick = { isEditing = !isEditing },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = if (isEditing) "Finish Editing" else "Edit Diagram Syntax",
                            tint = if (isEditing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (isEditing && onBlockUpdated != null) {
                BasicTextField(
                    value = syntaxText,
                    onValueChange = {
                        syntaxText = it
                        onBlockUpdated(block.copy(syntax = it, title = diagramTitle))
                    },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp), RoundedCornerShape(4.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(8.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                if (shouldUseMermaid(diagramTitle, syntaxText)) {
                    MermaidRenderer(mermaidCode = syntaxText, isDark = isDark)
                } else {
                    val nodes = remember(syntaxText) { parseDiagramNodes(syntaxText) }
                    if (nodes.isEmpty()) {
                        Text(
                            text = "Invalid diagram syntax.\nUse [Node A] -> [Node B]",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        HorizontalFlowDiagram(
                            nodes = nodes,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (!isEditing) {
                    IconButton(
                        onClick = { showPreview = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(36.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp).copy(alpha = 0.85f),
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Maximize Diagram",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// Simple logic to parse diagram syntax like "[Node A] -> [Node B] -> [Node C]"
private fun parseDiagramNodes(syntax: String): List<String> {
    val clean = syntax.replace("\n", "").trim()
    val parts = clean.split("->")
    return parts.mapNotNull { part ->
        val node = part.trim()
        if (node.startsWith("[") && node.endsWith("]")) {
            node.substring(1, node.length - 1)
        } else if (node.isNotEmpty()) {
            node
        } else null
    }
}

// Multi-line Markdown Parser
private fun splitTableRow(row: String): List<String> {
    val trimmed = row.trim()
    val raw = trimmed.split("|").map { it.trim() }
    if (raw.size <= 1) return emptyList()
    
    val startIdx = if (trimmed.startsWith("|")) 1 else 0
    val endIdx = if (trimmed.endsWith("|") && raw.size > startIdx) raw.size - 1 else raw.size
    
    return if (startIdx < endIdx) raw.subList(startIdx, endIdx) else emptyList()
}

fun parseMarkdown(text: String): List<MarkdownBlock> {
    val nonStyleCommentRegex = """<!--(?!style:).*?-->""".toRegex(RegexOption.DOT_MATCHES_ALL)
    val cleanedText = text.replace(nonStyleCommentRegex, "")
    val lines = cleanedText.split("\n")
    val blocks = mutableListOf<MarkdownBlock>()
    
    var i = 0
    val textContent = StringBuilder()

    fun flushText() {
        if (textContent.isNotEmpty()) {
            val textStr = textContent.toString()
            blocks.add(MarkdownBlock.TextBlock(if (textStr.endsWith("\n")) textStr.dropLast(1) else textStr))
            textContent.clear()
        }
    }

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        // 1.5 Block Math check: $$ ... $$
        if (trimmed.startsWith("$$")) {
            flushText()
            val mathContent = StringBuilder()
            if (trimmed.endsWith("$$") && trimmed.length > 2) {
                // Single line block math, e.g., $$E_m = ...$$
                mathContent.append(trimmed.substring(2, trimmed.length - 2))
                i++
            } else {
                // Multiline block math
                mathContent.append(trimmed.substring(2)).append("\n")
                i++
                while (i < lines.size && !lines[i].trim().endsWith("$$")) {
                    mathContent.append(lines[i]).append("\n")
                    i++
                }
                if (i < lines.size) {
                    val lastLine = lines[i].trim()
                    mathContent.append(lastLine.substring(0, lastLine.length - 2))
                    i++
                }
            }
            blocks.add(MarkdownBlock.MathBlock(mathContent.toString().trim()))
            continue
        }

        // 1. Code block check
        if (trimmed.startsWith("```")) {
            flushText()
            val codeLanguage = trimmed.substring(3).trim()
            val codeContent = StringBuilder()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeContent.append(lines[i]).append("\n")
                i++
            }
            val codeStr = codeContent.toString().trimEnd()
            val langParts = codeLanguage.split(" ", limit = 2)
            val lang = langParts.firstOrNull()?.lowercase() ?: ""
            val isDiagramLang = lang == "diagram" || lang == "mermaid" || 
                                lang == "flowchart" || lang == "sequence" || 
                                lang == "gantt" || lang == "mindmap" || 
                                lang == "pie" || lang == "classdiagram" || 
                                lang == "statediagram" || lang == "erdiagram" || 
                                lang == "gitgraph" || lang == "journey"
            if (isDiagramLang) {
                val title = if (langParts.size > 1) langParts[1].trim() else lang
                blocks.add(MarkdownBlock.DiagramBlock(codeStr, title))
            } else if (lang == "drawing") {
                blocks.add(MarkdownBlock.DrawingBlock(codeStr, "```drawing\n$codeStr\n```"))
            } else {
                blocks.add(MarkdownBlock.CodeBlock(codeLanguage, codeStr))
            }
            if (i < lines.size) {
                i++ // Skip the closing ```
            }
            continue
        }

        // 2. Image pattern check: ![altText](path)
        val imageRegex = Regex("^\\!\\[(.*?)\\]\\((.*?)\\)$")
        val imageMatch = imageRegex.matchEntire(trimmed)
        if (imageMatch != null) {
            flushText()
            val altText = imageMatch.groups[1]?.value ?: "Image"
            val path = imageMatch.groups[2]?.value ?: ""
            blocks.add(MarkdownBlock.Image(path, altText))
            i++
            continue
        }

        // 3. Auto-detect Mermaid diagrams without ``` wrap!
        val isMermaidStart = trimmed.startsWith("graph ") || 
                             trimmed.startsWith("graph\t") ||
                             trimmed.startsWith("flowchart ") || 
                             trimmed.startsWith("flowchart\t") ||
                             trimmed.startsWith("sequenceDiagram") || 
                             trimmed.startsWith("classDiagram") || 
                             trimmed.startsWith("stateDiagram") || 
                             trimmed.startsWith("erDiagram") || 
                             trimmed.startsWith("gantt") || 
                             trimmed.startsWith("pie") || 
                             trimmed.startsWith("gitGraph")
                             
        if (isMermaidStart) {
            flushText()
            val diagramContent = StringBuilder()
            while (i < lines.size) {
                val dLine = lines[i]
                val dTrimmed = dLine.trim()
                if (dTrimmed.isEmpty() || 
                    dTrimmed.startsWith("```") || 
                    dTrimmed.startsWith("#") || 
                    (dTrimmed.startsWith("-") && dTrimmed.length > 1 && dTrimmed[1].isWhitespace()) ||
                    dTrimmed.startsWith("|")) {
                    break
                }
                diagramContent.append(dLine).append("\n")
                i++
            }
            blocks.add(MarkdownBlock.DiagramBlock(diagramContent.toString().trimEnd(), "mermaid"))
            continue
        }

        // 4. Markdown Table check!
        if (trimmed.contains("|") && i + 1 < lines.size) {
            val nextLine = lines[i + 1].trim()
            val isDelimiter = nextLine.contains("|") && nextLine.all { c -> c == '|' || c == '-' || c == ':' || c.isWhitespace() } && nextLine.any { c -> c == '-' }
            if (isDelimiter) {
                flushText()
                val headers = splitTableRow(line)
                val delims = splitTableRow(nextLine)
                
                val alignments = delims.map { delim ->
                    when {
                        delim.startsWith(":") && delim.endsWith(":") -> "center"
                        delim.endsWith(":") -> "right"
                        else -> "left"
                    }
                }

                val rows = mutableListOf<List<String>>()
                i += 2 // Skip header and delimiter

                // Consume body rows
                while (i < lines.size) {
                    val rLine = lines[i]
                    val rTrimmed = rLine.trim()
                    if (!rTrimmed.contains("|")) {
                        break
                    }
                    val cells = splitTableRow(rLine)
                    rows.add(cells)
                    i++
                }
                blocks.add(MarkdownBlock.TableBlock(headers, rows, alignments))
                continue
            }
        }

        textContent.append(line).append("\n")
        i++
    }

    flushText()

    val normalized = mutableListOf<MarkdownBlock>()
    if (blocks.isEmpty() || blocks.first() !is MarkdownBlock.TextBlock) {
        normalized.add(MarkdownBlock.TextBlock(""))
    }
    for (idx in blocks.indices) {
        normalized.add(blocks[idx])
        if (blocks[idx] !is MarkdownBlock.TextBlock) {
            if (idx == blocks.lastIndex || blocks[idx + 1] !is MarkdownBlock.TextBlock) {
                normalized.add(MarkdownBlock.TextBlock(""))
            }
        }
    }
    return normalized
}

fun parseMarkdownElements(text: String): List<MarkdownBlock> {
    val lines = text.split("\n")
    val blocks = mutableListOf<MarkdownBlock>()
    for (line in lines) {
        val trimmed = line.trim()
        val isHr = trimmed.length >= 3 && (
            trimmed.all { it == '-' } ||
            trimmed.all { it == '*' } ||
            trimmed.all { it == '_' }
        )
        if (isHr) {
            blocks.add(MarkdownBlock.HorizontalRule())
        } else if (trimmed.startsWith("#")) {
            var level = 0
            while (level < trimmed.length && trimmed[level] == '#') {
                level++
            }
            val headerText = trimmed.substring(level).trim()
            blocks.add(MarkdownBlock.Header(level, headerText))
        } else if (trimmed.startsWith("--") && trimmed.length > 2 && trimmed[2].isWhitespace()) {
            blocks.add(MarkdownBlock.ListItem(trimmed.substring(2).trim(), level = 2))
        } else if (trimmed.startsWith("-") && trimmed.length > 1 && trimmed[1].isWhitespace()) {
            blocks.add(MarkdownBlock.ListItem(trimmed.substring(1).trim(), level = 1))
        } else if (trimmed.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(line))
        }
    }
    return blocks
}

// Basic inline markdown styling parsing (Bold **, Italic *, and Links [text](url))
@Composable
private fun parseInlineMarkdown(text: String): AnnotatedString {
    val isDark = isSystemInDarkTheme()
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val codeColor = if (isDark) Color(0xFFF43F5E) else Color(0xFFBE123C)
    val codeBg = if (isDark) Color(0x33808080) else Color(0x1F808080)

    return remember(text, isDark, primaryColor, secondaryColor) {
        buildAnnotatedString {
            var i = 0
            while (i < text.length) {
                when {
                    text.startsWith("**", i) -> {
                        val end = text.indexOf("**", i + 2)
                        if (end != -1) {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(text.substring(i + 2, end))
                            }
                            i = end + 2
                        } else {
                            append("**")
                            i += 2
                        }
                    }
                    text.startsWith("__", i) -> {
                        val end = text.indexOf("__", i + 2)
                        if (end != -1) {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(text.substring(i + 2, end))
                            }
                            i = end + 2
                        } else {
                            append("__")
                            i += 2
                        }
                    }
                    text.startsWith("*", i) -> {
                        val end = text.indexOf("*", i + 1)
                        if (end != -1) {
                            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                                append(text.substring(i + 1, end))
                            }
                            i = end + 1
                        } else {
                            append("*")
                            i += 1
                        }
                    }
                    text.startsWith("_", i) -> {
                        val end = text.indexOf("_", i + 1)
                        if (end != -1) {
                            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                                append(text.substring(i + 1, end))
                            }
                            i = end + 1
                        } else {
                            append("_")
                            i += 1
                        }
                    }
                    text.startsWith("`", i) -> {
                        val end = text.indexOf("`", i + 1)
                        if (end != -1) {
                            withStyle(
                                style = SpanStyle(
                                    fontFamily = FontFamily.Monospace,
                                    color = codeColor,
                                    background = codeBg
                                )
                            ) {
                                append(text.substring(i + 1, end))
                            }
                            i = end + 1
                        } else {
                            append("`")
                            i += 1
                        }
                    }
                    text.startsWith("$", i) -> {
                        if (text.startsWith("$$", i)) {
                            val end = text.indexOf("$$", i + 2)
                            if (end != -1) {
                                withStyle(
                                    style = SpanStyle(
                                        fontFamily = FontFamily.Serif,
                                        fontStyle = FontStyle.Italic,
                                        color = primaryColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    appendInlineMath(text.substring(i + 2, end))
                                }
                                i = end + 2
                            } else {
                                append("$$")
                                i += 2
                            }
                        } else {
                            val end = text.indexOf("$", i + 1)
                            if (end != -1 && end > i + 1) {
                                withStyle(
                                    style = SpanStyle(
                                        fontFamily = FontFamily.Serif,
                                        fontStyle = FontStyle.Italic,
                                        color = secondaryColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                ) {
                                    appendInlineMath(text.substring(i + 1, end))
                                }
                                i = end + 1
                            } else {
                                append("$")
                                i += 1
                            }
                        }
                    }
                    text.startsWith("[", i) -> {
                        val closeBracket = text.indexOf("]", i + 1)
                        if (closeBracket != -1) {
                            var parenStartIndex = closeBracket + 1
                            while (parenStartIndex < text.length && text[parenStartIndex].isWhitespace()) {
                                parenStartIndex++
                            }
                            if (parenStartIndex < text.length && text[parenStartIndex] == '(') {
                                val closeParen = text.indexOf(")", parenStartIndex + 1)
                                if (closeParen != -1) {
                                    val linkText = text.substring(i + 1, closeBracket)
                                    val linkUrl = text.substring(parenStartIndex + 1, closeParen).trim()
                                    
                                    pushStringAnnotation(tag = "URL", annotation = linkUrl)
                                    withStyle(
                                        style = SpanStyle(
                                            color = Color(0xFF1E88E5), // Beautiful GitHub-style blue link
                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                            fontWeight = FontWeight.Medium
                                        )
                                    ) {
                                        append(linkText)
                                    }
                                    pop()
                                    i = closeParen + 1
                                    continue
                                }
                            }
                        }
                        append("[")
                        i++
                    }
                    else -> {
                        append(text[i])
                        i++
                    }
                }
            }
        }
    }
}

// Simple highlighter for basic keywords
private fun highlightCode(code: String): AnnotatedString {
    val keywords = setOf(
        "fun", "val", "var", "class", "import", "package", "return", "if", "else", "for", "while",
        "let", "const", "function", "int", "float", "double", "string", "boolean", "void", "public", "private"
    )

    val words = code.split(Regex("(?<=\\b)|(?=\\b)|(?<=\\W)|(?=\\W)"))
    return buildAnnotatedString {
        for (word in words) {
            when {
                keywords.contains(word.trim()) -> {
                    withStyle(style = SpanStyle(color = Color(0xFF00E676), fontWeight = FontWeight.Bold)) {
                        append(word)
                    }
                }
                word.startsWith("//") || word.startsWith("/*") -> {
                    withStyle(style = SpanStyle(color = Color(0xFF90A4AE), fontStyle = FontStyle.Italic)) {
                        append(word)
                    }
                }
                word.startsWith("\"") && word.endsWith("\"") -> {
                    withStyle(style = SpanStyle(color = Color(0xFFFFB74D))) {
                        append(word)
                    }
                }
                else -> {
                    append(word)
                }
            }
        }
    }
}

fun AnnotatedString.Builder.appendInlineMath(mathText: String) {
    var j = 0
    while (j < mathText.length) {
        when {
            mathText.startsWith("_", j) -> {
                j++
                if (j < mathText.length) {
                    if (mathText[j] == '{') {
                        val endCurly = mathText.indexOf('}', j)
                        if (endCurly != -1) {
                            val sub = mathText.substring(j + 1, endCurly)
                            withStyle(style = SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 10.sp)) {
                                appendInlineMath(sub)
                            }
                            j = endCurly + 1
                        } else {
                            append("{")
                            j++
                        }
                    } else {
                        // Single char subscript
                        val sub = mathText[j].toString()
                        withStyle(style = SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 10.sp)) {
                            append(sub)
                        }
                        j++
                    }
                }
            }
            mathText.startsWith("^", j) -> {
                j++
                if (j < mathText.length) {
                    if (mathText[j] == '{') {
                        val endCurly = mathText.indexOf('}', j)
                        if (endCurly != -1) {
                            val sup = mathText.substring(j + 1, endCurly)
                            withStyle(style = SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 10.sp)) {
                                appendInlineMath(sup)
                            }
                            j = endCurly + 1
                        } else {
                            append("{")
                            j++
                        }
                    } else {
                        // Single char superscript
                        val sup = mathText[j].toString()
                        withStyle(style = SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 10.sp)) {
                            append(sup)
                        }
                        j++
                    }
                }
            }
            mathText.startsWith("\\frac", j) -> {
                j += 5
                if (j < mathText.length && mathText[j] == '{') {
                    val numEnd = findMatchingCurly(mathText, j)
                    if (numEnd != -1) {
                        val num = mathText.substring(j + 1, numEnd)
                        val denStart = numEnd + 1
                        if (denStart < mathText.length && mathText[denStart] == '{') {
                            val denEnd = findMatchingCurly(mathText, denStart)
                            if (denEnd != -1) {
                                val den = mathText.substring(denStart + 1, denEnd)
                                append("(")
                                appendInlineMath(num)
                                append("/")
                                appendInlineMath(den)
                                append(")")
                                j = denEnd + 1
                                continue
                            }
                        }
                    }
                }
                append("\\frac")
            }
            mathText.startsWith("\\text", j) -> {
                j += 5
                if (j < mathText.length && mathText[j] == '{') {
                    val endCurly = mathText.indexOf('}', j)
                    if (endCurly != -1) {
                        val textContent = mathText.substring(j + 1, endCurly)
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Normal)) {
                            append(textContent)
                        }
                        j = endCurly + 1
                    } else {
                        append("{")
                        j++
                    }
                } else {
                    append("\\text")
                }
            }
            mathText.startsWith("\\Delta", j) -> {
                append("Δ")
                j += 6
            }
            mathText.startsWith("\\Omega", j) -> {
                append("Ω")
                j += 6
            }
            mathText.startsWith("\\log", j) -> {
                withStyle(style = SpanStyle(fontStyle = FontStyle.Normal)) {
                    append("log")
                }
                j += 4
            }
            mathText.startsWith("\\ln", j) -> {
                withStyle(style = SpanStyle(fontStyle = FontStyle.Normal)) {
                    append("ln")
                }
                j += 3
            }
            mathText.startsWith("\\left(", j) -> {
                append("(")
                j += 6
            }
            mathText.startsWith("\\right)", j) -> {
                append(")")
                j += 7
            }
            mathText.startsWith("\\", j) -> {
                j++
            }
            else -> {
                append(mathText[j])
                j++
            }
        }
    }
}

private fun findMatchingCurly(text: String, startIdx: Int): Int {
    var count = 0
    for (i in startIdx until text.length) {
        if (text[i] == '{') count++
        else if (text[i] == '}') {
            count--
            if (count == 0) return i
        }
    }
    return -1
}

private fun replaceFracs(input: String): String {
    var s = input
    while (true) {
        val fracIdx = s.indexOf("\\frac")
        if (fracIdx == -1) break
        
        // Find first curly bracket of numerator
        val numStart = s.indexOf('{', fracIdx + 5)
        if (numStart == -1) break
        
        val numEnd = findMatchingCurly(s, numStart)
        if (numEnd == -1) break
        
        val num = s.substring(numStart + 1, numEnd)
        
        // Find second curly bracket of denominator
        val denStart = s.indexOf('{', numEnd + 1)
        if (denStart == -1 || denStart > numEnd + 3) { 
            break
        }
        val denEnd = findMatchingCurly(s, denStart)
        if (denEnd == -1) break
        
        val den = s.substring(denStart + 1, denEnd)
        
        // Replace \frac{num}{den} with ((num)/(den))
        val before = s.substring(0, fracIdx)
        val after = s.substring(denEnd + 1)
        s = "$before(($num)/($den))$after"
    }
    return s
}

private fun cleanLaTeXForEvaluation(latex: String): Pair<String, List<String>> {
    var rawExpr = latex.replace(Regex("\\\\begin\\{.*?\\}"), "")
                       .replace(Regex("\\\\end\\{.*?\\}"), "")
    val lines = rawExpr.split("\\\\\\\\", "\n")
    rawExpr = lines.firstOrNull { it.contains("=") } ?: lines.firstOrNull() ?: latex

    val parts = rawExpr.split("=")
    var expressionToParse = if (parts.size > 1) parts.drop(1).joinToString("=") else rawExpr

    val commaIdx = expressionToParse.indexOf(",")
    if (commaIdx != -1) {
        expressionToParse = expressionToParse.substring(0, commaIdx)
    }

    var expr = expressionToParse
    
    expr = replaceFracs(expr)
    
    expr = expr.replace("\\ln", " ln ")
    expr = expr.replace("\\log_{10}", " log10 ")
    expr = expr.replace("\\log", " log ")
    expr = expr.replace("\\left(", " ( ")
    expr = expr.replace("\\right)", " ) ")
    expr = expr.replace("\\left[", " [ ")
    expr = expr.replace("\\right]", " ] ")
    expr = expr.replace("\\cdot", " * ")
    expr = expr.replace("\\times", " * ")
    
    val textRegex = Regex("\\\\text\\{([^}]+)\\}")
    expr = textRegex.replace(expr) { "" }
    
    expr = expr.replace(Regex("\\[([A-Za-z]+)\\^\\{\\+?\\-?\\}\\]_\\{?([A-Za-z]+)\\}?")) { matchResult ->
        "${matchResult.groupValues[1]}_${matchResult.groupValues[2]}"
    }
    expr = expr.replace(Regex("\\[([A-Za-z]+)\\^\\+?\\]_\\{?([A-Za-z]+)\\}?")) { matchResult ->
        "${matchResult.groupValues[1]}_${matchResult.groupValues[2]}"
    }
    expr = expr.replace(Regex("\\[([A-Za-z]+)\\^\\-?\\]_\\{?([A-Za-z]+)\\}?")) { matchResult ->
        "${matchResult.groupValues[1]}_${matchResult.groupValues[2]}"
    }
    expr = expr.replace(Regex("\\[([A-Za-z0-9_]+)\\]")) { it.groupValues[1] }
    
    expr = expr.replace(Regex("([A-Za-z]+)_\\{([^}]+)\\}")) { matchResult ->
        "${matchResult.groupValues[1]}_${matchResult.groupValues[2]}"
    }
    expr = expr.replace(Regex("([A-Za-z]+)_([A-Za-z0-9])")) { matchResult ->
        "${matchResult.groupValues[1]}_${matchResult.groupValues[2]}"
    }
    
    expr = expr.replace("{", " ( ")
    expr = expr.replace("}", " ) ")
    expr = expr.replace("\\", "")
    
    val words = Regex("[A-Za-z_][A-Za-z0-9_]*").findAll(expr).map { it.value }.toSet()
    val nonVariables = setOf("ln", "log", "log10", "sin", "cos", "tan", "sqrt", "left", "right")
    val constants = setOf("pi", "e")
    val variables = words.filter { it.lowercase() !in nonVariables && it !in constants && !it.all { char -> char.isDigit() } }.sorted()
    
    return Pair(expr, variables)
}

class MathEvaluator(private val expression: String, private val variables: Map<String, Double>) {
    private var pos = -1
    private var ch = 0

    private fun nextChar() {
        ch = if (++pos < expression.length) expression[pos].code else -1
    }

    private fun eat(charToEat: Int): Boolean {
        while (ch == ' '.code) nextChar()
        if (ch == charToEat) {
            nextChar()
            return true
        }
        return false
    }

    fun parse(): Double {
        nextChar()
        val x = parseExpression()
        return x
    }

    private fun parseExpression(): Double {
        var x = parseTerm()
        while (true) {
            if (eat('+'.code)) x += parseTerm() 
            else if (eat('-'.code)) x -= parseTerm() 
            else return x
        }
    }

    private fun parseTerm(): Double {
        var x = parseFactor()
        while (true) {
            if (eat('*'.code)) x *= parseFactor() 
            else if (eat('/'.code)) x /= parseFactor() 
            else if (ch >= '0'.code && ch <= '9'.code || ch == '('.code || ch == '['.code || ch == '{'.code || (ch >= 'A'.code && ch <= 'Z'.code) || (ch >= 'a'.code && ch <= 'z'.code) || ch == '_'.code) {
                x *= parseFactor()
            } else return x
        }
    }

    private fun parseFactor(): Double {
        if (eat('+'.code)) return parseFactor() 
        if (eat('-'.code)) return -parseFactor() 

        var x: Double
        val startPos = pos
        if (eat('('.code) || eat('['.code)) { 
            x = parseExpression()
            eat(')'.code)
            eat(']'.code)
        } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) { 
            while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
            x = expression.substring(startPos, pos).toDouble()
        } else if ((ch >= 'A'.code && ch <= 'Z'.code) || (ch >= 'a'.code && ch <= 'z'.code) || ch == '_'.code) { 
            while ((ch >= 'A'.code && ch <= 'Z'.code) || (ch >= 'a'.code && ch <= 'z'.code) || (ch >= '0'.code && ch <= '9'.code) || ch == '_'.code) nextChar()
            val name = expression.substring(startPos, pos)
            if (name.lowercase() == "pi") {
                x = Math.PI
            } else if (name == "e") {
                x = Math.E
            } else if (variables.containsKey(name)) {
                x = variables[name] ?: 0.0
            } else {
                x = parseFactor()
                x = when (name.lowercase()) {
                    "sqrt" -> Math.sqrt(x)
                    "sin" -> Math.sin(Math.toRadians(x))
                    "cos" -> Math.cos(Math.toRadians(x))
                    "tan" -> Math.tan(Math.toRadians(x))
                    "log" -> Math.log10(x)
                    "log10" -> Math.log10(x)
                    "ln" -> Math.log(x)
                    else -> throw RuntimeException("Unknown function/variable: $name")
                }
            }
        } else {
            return 0.0
        }

        if (eat('^'.code)) x = Math.pow(x, parseFactor()) 

        return x
    }
}

@Composable
fun MathBlockView(
    block: MarkdownBlock.MathBlock,
    onBlockUpdated: ((MarkdownBlock.MathBlock) -> Unit)? = null
) {
    var isEditing by remember { mutableStateOf(false) }
    var mathText by remember(block.equation) { mutableStateOf(block.equation) }
    var showHighFidelity by remember { mutableStateOf(false) }
    var showCalculator by remember { mutableStateOf(false) }
    var calculatorResult by remember { mutableStateOf<String?>(null) }
    var calculatorError by remember { mutableStateOf<String?>(null) }

    val parsedEquation = remember(block.equation) { cleanLaTeXForEvaluation(block.equation) }
    val variables = parsedEquation.second
    val cleanedExpr = parsedEquation.first

    val defaultVariableValues = remember(variables) {
        val map = mutableStateMapOf<String, String>()
        variables.forEach { v ->
            val defaultVal = when (v) {
                "R" -> "8.314"
                "T" -> "310.15"
                "F" -> "96485"
                "P_K" -> "1.0"
                "P_Na" -> "0.04"
                "P_Cl" -> "0.45"
                "K_out" -> "4.0"
                "K_in" -> "140.0"
                "Na_out" -> "145.0"
                "Na_in" -> "12.0"
                "Cl_out" -> "115.0"
                "Cl_in" -> "4.0"
                "A_d" -> "100000.0"
                "A_cm" -> "10.0"
                "m" -> "1.0"
                "c" -> "299792458.0"
                else -> "1.0"
            }
            map[v] = defaultVal
        }
        map
    }

    val outputVariableName = remember(block.equation) {
        val parts = block.equation.split("=")
        if (parts.size > 1) {
            parts[0].trim().replace("\\", "").replace("{", "").replace("}", "").trim()
        } else {
            "Result"
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Math Block",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MATH FORMULA",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { showHighFidelity = !showHighFidelity },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (showHighFidelity) Icons.Default.Check else Icons.Default.PlayArrow,
                            contentDescription = if (showHighFidelity) "Show Native" else "Show KaTeX",
                            tint = if (showHighFidelity) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (onBlockUpdated != null) {
                        IconButton(
                            onClick = { isEditing = !isEditing },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = if (isEditing) "Done" else "Edit Formula",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isEditing && onBlockUpdated != null) {
                OutlinedTextField(
                    value = mathText,
                    onValueChange = {
                        mathText = it
                        onBlockUpdated(MarkdownBlock.MathBlock(it))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                    placeholder = { Text("Enter LaTeX formula...") }
                )
            } else {
                if (showHighFidelity) {
                    MathWebView(
                        formula = block.equation,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )
                } else {
                    val secondaryColor = MaterialTheme.colorScheme.secondary
                    val formattedMath = remember(block.equation, secondaryColor) {
                        buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    fontFamily = FontFamily.Serif,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 18.sp,
                                    color = secondaryColor,
                                    fontWeight = FontWeight.Medium
                                )
                            ) {
                                appendInlineMath(block.equation)
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = formattedMath,
                            textAlign = TextAlign.Center,
                            lineHeight = 28.sp
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { showCalculator = !showCalculator },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = "Evaluate",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showCalculator) "Hide Calculator" else "Calculate",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            if (showCalculator) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                )

                Text(
                    text = "Interactive Evaluation",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                )

                if (variables.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                try {
                                    val evaluator = MathEvaluator(cleanedExpr, emptyMap())
                                    val res = evaluator.parse()
                                    if (res.isNaN() || res.isInfinite()) {
                                        calculatorResult = "Undefined"
                                    } else {
                                        calculatorResult = String.format("%.4f", res)
                                    }
                                    calculatorError = null
                                } catch (e: Exception) {
                                    calculatorError = e.message ?: "Evaluation error"
                                    calculatorResult = null
                                }
                            }
                        ) {
                            Text("Compute Value")
                        }
                    }
                } else {
                    Text(
                        text = "Enter values for the variables:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        variables.forEach { v ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = v,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontStyle = FontStyle.Italic,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = defaultVariableValues[v] ?: "",
                                    onValueChange = { newValue ->
                                        defaultVariableValues[v] = newValue
                                    },
                                    modifier = Modifier.width(180.dp),
                                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            try {
                                val valueMap = defaultVariableValues.mapValues { (_, v) -> v.toDoubleOrNull() ?: 0.0 }
                                val evaluator = MathEvaluator(cleanedExpr, valueMap)
                                val res = evaluator.parse()
                                if (res.isNaN() || res.isInfinite()) {
                                    calculatorResult = "Undefined"
                                } else {
                                    calculatorResult = String.format("%.4f", res)
                                }
                                calculatorError = null
                            } catch (e: Exception) {
                                calculatorError = e.message ?: "Evaluation error"
                                calculatorResult = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Run",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run Calculation")
                    }
                }

                if (calculatorResult != null || calculatorError != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (calculatorError != null) {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                            }
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (calculatorError != null) {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (calculatorError != null) {
                                Text(
                                    text = "Error: ${calculatorError}",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "$outputVariableName = ",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = FontFamily.Serif,
                                            fontStyle = FontStyle.Italic,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = calculatorResult ?: "",
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MathWebView(formula: String, modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) "#E3E2E6" else "#1A1C1E"
    
    val html = remember(formula, isDark) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.css">
            <script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.js"></script>
            <script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/contrib/auto-render.min.js" onload="renderMathInElement(document.body);"></script>
            <style>
                body {
                    color: $textColor;
                    background-color: transparent;
                    font-family: 'Times New Roman', Times, serif;
                    font-size: 18px;
                    margin: 0;
                    padding: 0;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    min-height: 100px;
                    overflow: hidden;
                }
                .katex-display {
                    margin: 0 !important;
                }
            </style>
        </head>
        <body>
            <div style="text-align: center; width: 100%;">
                $$ $formula $$
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { context ->
            android.webkit.WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                webViewClient = android.webkit.WebViewClient()
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        },
        modifier = modifier
    )
}
