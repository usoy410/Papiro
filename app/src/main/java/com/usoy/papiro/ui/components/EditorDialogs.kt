package com.usoy.papiro.ui.components
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


@Composable
fun AiTopicGeneratorDialog(
    showAiDialog: Boolean,
    provider: String,
    onDismiss: () -> Unit,
    onBuild: (String) -> Unit
) {
    if (!showAiDialog) return
    var aiTopic by remember { mutableStateOf("") }
    val providerName = when (provider) {
        
        else -> "Google Gemini"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
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
                    onBuild(aiTopic)
                    onDismiss()
                }
            ) {
                Text("BUILD")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}

@Composable
fun QuizGeneratorDialog(
    showQuizDialog: Boolean,
    provider: String,
    onDismiss: () -> Unit,
    onGenerate: (quizType: String, quizDifficulty: String, quizItems: String) -> Unit
) {
    if (!showQuizDialog) return
    var quizType by remember { mutableStateOf("Multiple Choice") }
    var quizDifficulty by remember { mutableStateOf("Medium") }
    var quizItems by remember { mutableStateOf("5") }
    val providerName = when (provider) {
        
        else -> "Google Gemini"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "GENERATE QUIZ FROM NOTE",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Turn your current note into an interactive quiz using $providerName.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Quiz Type", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Multiple Choice", "Flashcards").forEach { type ->
                        FilterChip(
                            selected = quizType == type,
                            onClick = { quizType = type },
                            label = { Text(type) }
                        )
                    }
                }
                Text("Difficulty Level", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Easy", "Medium", "Hard").forEach { level ->
                        FilterChip(
                            selected = quizDifficulty == level,
                            onClick = { quizDifficulty = level },
                            label = { Text(level) }
                        )
                    }
                }
                OutlinedTextField(
                    value = quizItems,
                    onValueChange = { newValue -> 
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            val intValue = newValue.toIntOrNull()
                            if (intValue == null || intValue <= 20) {
                                quizItems = newValue
                            }
                        }
                    },
                    label = { Text("Number of Items (Max 20)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onGenerate(quizType, quizDifficulty, quizItems)
                    onDismiss()
                }
            ) {
                Text("GENERATE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}

@Composable
fun MarkdownHelpDialog(
    showHelpDialog: Boolean,
    onDismiss: () -> Unit
) {
    if (!showHelpDialog) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "BLUEPRINT ENGINE GUIDE",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Papiro uses a custom Markdown engine for rich scientific and academic notes. Tap any card below to copy its complete syntax directly to your clipboard!",
                    style = MaterialTheme.typography.bodyMedium
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    com.usoy.papiro.ui.components.HelpGuideRow(
                        "`some_variable_or_expression`",
                        "Inline Code: Use backticks to highlight variables or short formulas."
                    )
                    com.usoy.papiro.ui.components.HelpGuideRow(
                        "```kotlin\nval list = listOf(1, 2, 3)\nlist.map { it * 2 }\n```",
                        "Code Block: Specify the language for syntax highlighting."
                    )
                    com.usoy.papiro.ui.components.HelpGuideRow(
                        "```mermaid\ngraph TD\n  A[Start] --> B(Process)\n  B --> C{Success?}\n  C -- Yes --> D[Draw]\n  C -- No --> E[End]\n```",
                        "Mermaid Diagram: Renders flowcharts and architecture graphs dynamically."
                    )
                    com.usoy.papiro.ui.components.HelpGuideRow(
                        "```drawing\n{\n  \"id\": \"cross_box\",\n  \"width\": 320,\n  \"height\": 240,\n  \"paths\": [\n    {\n      \"color\": \"red\",\n      \"strokeWidth\": 4,\n      \"points\": [\n        {\"x\": 20, \"y\": 20},\n        {\"x\": 300, \"y\": 20},\n        {\"x\": 300, \"y\": 220},\n        {\"x\": 20, \"y\": 220},\n        {\"x\": 20, \"y\": 20}\n      ]\n    },\n    {\n      \"color\": \"blue\",\n      \"strokeWidth\": 6,\n      \"points\": [\n        {\"x\": 60, \"y\": 60},\n        {\"x\": 260, \"y\": 180}\n      ]\n    },\n    {\n      \"color\": \"blue\",\n      \"strokeWidth\": 6,\n      \"points\": [\n        {\"x\": 260, \"y\": 60},\n        {\"x\": 60, \"y\": 180}\n      ]\n    }\n  ]\n}\n```",
                        "Native Illustration: A JSON coordinates structure drawing a beautiful red outline box with a blue inner cross."
                    )
                    com.usoy.papiro.ui.components.HelpGuideRow(
                        "| Header <!--style:col,green,default--> | Header 2 <!--style:col,blue,default--> |\n| --- | --- |\n| Cell 1 | Cell 2 |",
                        "Custom Tables: Styled headers with column background colors (green, blue, orange, red, purple, etc.)."
                    )
                    com.usoy.papiro.ui.components.HelpGuideRow(
                        "\$\$E = mc^2\$\$",
                        "KaTeX Math Formula: For equations and mathematical notations."
                    )
                    com.usoy.papiro.ui.components.HelpGuideRow(
                        "- Parent bullet point\n-- Child nested bullet point",
                        "Hierarchical Bullets: Use double dash (--) for secondary indented lists with open bullet point circles (○)."
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("GOT IT") }
        }
    )
}
@Composable
fun VersionHistoryDialog(
    showHistoryDialog: Boolean,
    snapshots: List<com.usoy.papiro.ui.components.EditorMemento>,
    onDismiss: () -> Unit,
    onRestore: (androidx.compose.ui.text.input.TextFieldValue) -> Unit
) {
    if (!showHistoryDialog) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "NOTE VERSION HISTORY",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            if (snapshots.isEmpty()) {
                Text("No version history available.", style = MaterialTheme.typography.bodyMedium)
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                ) {
                    items(snapshots.size) { index ->
                        val snapshot = snapshots[snapshots.size - 1 - index]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Version ${snapshots.size - index}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${snapshot.state.text.length} characters",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = {
                                        onRestore(snapshot.state)
                                        onDismiss()
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
            TextButton(onClick = onDismiss) {
                Text("CLOSE")
            }
        }
    )
}
