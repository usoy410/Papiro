package com.usoy.papiro.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.usoy.papiro.data.NoteEntity
import com.usoy.papiro.data.ImageSearchResolver
import com.usoy.papiro.viewmodel.NoteViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NoteListScreen(
    viewModel: NoteViewModel,
    onNoteSelected: (NoteEntity) -> Unit,
    onSettingsClick: () -> Unit,
    onUploadDocumentClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val notes by viewModel.allNotes.collectAsState()
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()) }

    var selectedNoteIds by remember { mutableStateOf(setOf<Long>()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isFabExpanded by remember { mutableStateOf(false) }
    val fabRotation by animateFloatAsState(
        targetValue = if (isFabExpanded) 45f else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedNoteIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { isSelectionMode = false; selectedNoteIds = emptySet() }) {
                            Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, "Delete")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Papiro", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge) },
                    actions = {
                        IconButton(onClick = onSettingsClick) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedVisibility(
                        visible = isFabExpanded,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(end = 12.dp)
                                ) {
                                    Text(
                                        "Upload Document", 
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), 
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FloatingActionButton(
                                    onClick = {
                                        isFabExpanded = false
                                        ImageSearchResolver.warmup()
                                        onUploadDocumentClick("Note")
                                    },
                                    modifier = Modifier.size(48.dp),
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ) {
                                    Icon(Icons.Default.UploadFile, contentDescription = "Upload Document")
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(end = 12.dp)
                                ) {
                                    Text(
                                        "New Note", 
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), 
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FloatingActionButton(
                                    onClick = {
                                        isFabExpanded = false
                                        ImageSearchResolver.warmup()
                                        viewModel.createNewNote { insertedNote ->
                                            onNoteSelected(insertedNote)
                                        }
                                    },
                                    modifier = Modifier.size(48.dp),
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "New Note")
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    FloatingActionButton(
                        onClick = {
                            isFabExpanded = !isFabExpanded
                            if (isFabExpanded) {
                                ImageSearchResolver.warmup()
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.graphicsLayer { rotationZ = fabRotation }
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "New Note Options")
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Notes") },
                text = { Text("Are you sure you want to delete the selected note(s)?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteNotes(selectedNoteIds)
                            showDeleteDialog = false
                            selectedNoteIds = emptySet()
                            isSelectionMode = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Empty notes icon",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No notes yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Click the + button to create a note.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    val notePreview = remember(note.content) { getNotePreview(note.content) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .combinedClickable(
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedNoteIds = if (selectedNoteIds.contains(note.id)) selectedNoteIds - note.id else selectedNoteIds + note.id
                                        if (selectedNoteIds.isEmpty()) isSelectionMode = false
                                    } else {
                                        onNoteSelected(note)
                                    }
                                },
                                onLongClick = {
                                    isSelectionMode = true
                                    selectedNoteIds = selectedNoteIds + note.id
                                }
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedNoteIds.contains(note.id)) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = note.title.ifEmpty { "Untitled" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = dateFormat.format(Date(note.timestamp)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = notePreview,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (note.backgroundType != "GRID") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(note.backgroundType, style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.height(24.dp)
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

private fun getNotePreview(content: String): String {
    if (content.isEmpty()) return ""
    
    val builder = StringBuilder()
    var lastIndex = 0
    while (true) {
        val startIndex = content.indexOf("```", lastIndex)
        if (startIndex == -1) {
            if (lastIndex < content.length) {
                builder.append(content.substring(lastIndex))
            }
            break
        }
        
        // Append text before code block
        builder.append(content.substring(lastIndex, startIndex))
        
        // Find end of line to see the block type
        var nextNewline = content.indexOf('\n', startIndex + 3)
        if (nextNewline == -1) nextNewline = content.length
        
        val type = content.substring(startIndex + 3, nextNewline).trim()
        val displayType = if (type.isNotEmpty()) {
            type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
        } else {
            "Code"
        }
        
        val endIndex = content.indexOf("```", nextNewline)
        if (endIndex == -1) {
            // Unclosed block (due to truncation)
            builder.append("[$displayType]")
            break
        }
        builder.append("[$displayType]")
        lastIndex = endIndex + 3
    }
    
    var cleanText = builder.toString()
    
    // Normalize spaces and newlines
    cleanText = cleanText.replace(Regex("\\s+"), " ").trim()
    
    return if (cleanText.length > 150) {
        cleanText.take(150) + "..."
    } else {
        cleanText
    }
}
