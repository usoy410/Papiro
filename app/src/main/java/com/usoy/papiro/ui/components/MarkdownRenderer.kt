package com.usoy.papiro.ui.components
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.AutoAwesome

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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.window.Dialog

sealed class MarkdownBlock {
    data class TextBlock(val text: String) : MarkdownBlock()
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data class ListItem(val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class DiagramBlock(val syntax: String, val title: String = "diagram") : MarkdownBlock()
    data class DrawingBlock(val json: String, val rawBlock: String) : MarkdownBlock()
    data class Image(val path: String, val altText: String) : MarkdownBlock()
    data class TableBlock(val headers: List<String>, val rows: List<List<String>>, val alignments: List<String>) : MarkdownBlock()
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

@Composable
fun MarkdownRenderer(
    text: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onContentChanged: ((String) -> Unit)? = null,
    onEditDrawingClick: ((DrawingData) -> Unit)? = null,
    fontFamily: FontFamily = FontFamily.Serif,
    onLinkClick: ((String) -> Unit)? = null,
    onHeaderPositioned: ((String, androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    requestFocus: Boolean = false,
    onRequestFocusConsumed: () -> Unit = {},
    onEnhanceBlock: ((String, (String) -> Unit) -> Unit)? = null
) {
    val blocks = remember(text) { parseMarkdown(text) }
    var editingBlockIndex by remember { mutableStateOf<Int?>(null) }
    var selectedBlockIndex by remember { mutableStateOf<Int?>(null) }
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
                                onEnhanceBlock(blockText) { enhanced ->
                                    val updated = blocks.toMutableList()
                                    val newBlocks = parseMarkdown(enhanced)
                                    updated.removeAt(index)
                                    updated.addAll(index, newBlocks)
                                    onContentChanged(blocksToMarkdown(updated))
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
                                        if (textValue.text.contains("Table of Contents", ignoreCase = true)) {
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
                                            BasicTextField(
                                            value = textValue,
                                            onValueChange = { newVal ->
                                                textValue = newVal
                                                val updatedBlocks = blocks.toMutableList()
                                                updatedBlocks[index] = MarkdownBlock.TextBlock(newVal.text)
                                                onContentChanged(blocksToMarkdown(updatedBlocks))
                                            },
                                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                                fontFamily = LocalMarkdownFontFamily.current,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 22.sp
                                            ),
                                            cursorBrush = SolidColor(primaryColor),
                                            visualTransformation = remember(isDark, primaryColor) {
                                                MarkdownVisualTransformation(isDark, primaryColor)
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .focusRequester(focusRequester)
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
                                    Box(
                                        modifier = Modifier
                                            .width(startPadding)
                                            .padding(top = 16.dp),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        FunctionDoodle()
                                    }
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
                            is MarkdownBlock.TableBlock -> {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .width(startPadding)
                                            .padding(top = 16.dp),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        TableDoodle()
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
                                    Box(
                                        modifier = Modifier
                                            .width(startPadding)
                                            .padding(top = 16.dp),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        MoleculeDoodle()
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
                                                val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(drawingData.height.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                                                        .background(MaterialTheme.colorScheme.surface),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Canvas(
                                                        modifier = Modifier
                                                            .width(drawingData.width.dp)
                                                            .height(drawingData.height.dp)
                                                    ) {
                                                        for (path in drawingData.paths) {
                                                            if (path.points.size > 1) {
                                                                val strokePath = Path().apply {
                                                                    val first = path.points.first()
                                                                    moveTo(first.x, first.y)
                                                                    for (i in 1 until path.points.size) {
                                                                        val pt = path.points[i]
                                                                        lineTo(pt.x, pt.y)
                                                                    }
                                                                }
                                                                drawPath(
                                                                    path = strokePath,
                                                                    color = Color(path.color),
                                                                    style = Stroke(
                                                                        width = path.strokeWidth,
                                                                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                                                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                                                                    )
                                                                )
                                                            } else if (path.points.size == 1) {
                                                                val pt = path.points.first()
                                                                drawCircle(
                                                                    color = Color(path.color),
                                                                    radius = path.strokeWidth / 2f,
                                                                    center = Offset(pt.x, pt.y)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
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
            is MarkdownBlock.ListItem -> "- " + block.text
            is MarkdownBlock.CodeBlock -> "```${block.language}\n${block.code}\n```"
            is MarkdownBlock.DiagramBlock -> if (block.title.lowercase() == "mermaid" || block.title.lowercase() == "diagram" || block.title.isEmpty()) {
                "```mermaid\n${block.syntax}\n```"
            } else {
                "```mermaid ${block.title}\n${block.syntax}\n```"
            }
            is MarkdownBlock.DrawingBlock -> "```drawing\n${block.json}\n```"
            is MarkdownBlock.Image -> "![${block.altText}](${block.path})"
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

        }
    }
}

@Composable
fun TableBlockView(
    block: MarkdownBlock.TableBlock,
    onBlockUpdated: ((MarkdownBlock.TableBlock) -> Unit)? = null
) {
    var isEditing by remember { mutableStateOf(false) }
    var tableText by remember(block) { mutableStateOf(blocksToMarkdown(listOf(block))) }

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
                Text(
                    text = "TABLE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                if (onBlockUpdated != null) {
                    IconButton(
                        onClick = {
                            if (isEditing) {
                                val parsed = parseMarkdown(tableText)
                                if (parsed.isNotEmpty() && parsed.first() is MarkdownBlock.TableBlock) {
                                    onBlockUpdated(parsed.first() as MarkdownBlock.TableBlock)
                                }
                            }
                            isEditing = !isEditing
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = if (isEditing) "Finish Editing" else "Edit Table Markdown",
                            tint = if (isEditing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (isEditing && onBlockUpdated != null) {
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
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(min = 400.dp)
                            .padding(vertical = 4.dp)
                    ) {
                        // Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            block.headers.forEachIndexed { colIndex, header ->
                                val align = block.alignments.getOrNull(colIndex) ?: "left"
                                val textAlign = when (align) {
                                    "center" -> TextAlign.Center
                                    "right" -> TextAlign.Right
                                    else -> TextAlign.Left
                                }
                                Text(
                                    text = parseInlineMarkdown(header),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    textAlign = textAlign,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                )
                            }
                        }

                        // Data Rows
                        block.rows.forEachIndexed { rowIndex, row ->
                            val isLast = rowIndex == block.rows.lastIndex
                            val rowBg = if (rowIndex % 2 == 0) {
                                MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                            } else {
                                MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                            }
                            val borderShape = if (isLast) {
                                RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                              } else {
                                RoundedCornerShape(0.dp)
                              }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(rowBg, borderShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), borderShape)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                row.forEachIndexed { colIndex, cell ->
                                    val align = block.alignments.getOrNull(colIndex) ?: "left"
                                    val textAlign = when (align) {
                                        "center" -> TextAlign.Center
                                        "right" -> TextAlign.Right
                                        else -> TextAlign.Left
                                    }
                                    Text(
                                        text = parseInlineMarkdown(cell),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = textAlign,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 4.dp)
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
fun DiagramBlockView(
    block: MarkdownBlock.DiagramBlock,
    onBlockUpdated: ((MarkdownBlock.DiagramBlock) -> Unit)? = null
) {
    var isEditing by remember { mutableStateOf(false) }
    var syntaxText by remember(block.syntax) { mutableStateOf(block.syntax) }
    var diagramTitle by remember(block.title) { mutableStateOf(block.title) }
    var showPreview by remember { mutableStateOf(false) }
    
    val isDark = MaterialTheme.colorScheme.background == androidx.compose.ui.graphics.Color.Black // Or check system theme

    if (showPreview) {
        Dialog(
            onDismissRequest = { showPreview = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = if (isDark) Color.Black else Color.White
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (diagramTitle.lowercase() == "mermaid") {
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
                                val scrollState = rememberScrollState()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
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

            if (diagramTitle.lowercase() == "mermaid") {
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
                    val scrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
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
    val lines = text.split("\n")
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
            if (lang == "diagram" || lang == "mermaid") {
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
        } else if (trimmed.startsWith("-") && trimmed.length > 1 && trimmed[1].isWhitespace()) {
            blocks.add(MarkdownBlock.ListItem(trimmed.substring(1).trim()))
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
    val codeColor = if (isDark) Color(0xFFF43F5E) else Color(0xFFBE123C)
    val codeBg = if (isDark) Color(0x33808080) else Color(0x1F808080)

    return remember(text, isDark, primaryColor) {
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
