package com.usoy.papiro.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usoy.papiro.data.NoteHistoryEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AiTopicGeneratorDialog(
    showAiDialog: Boolean,
    onDismiss: () -> Unit,
    onBuild: (String) -> Unit
) {
    if (!showAiDialog) return
    var aiTopic by remember { mutableStateOf("") }
    val providerName = "Google Gemini"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "GENERATE WITH AI",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Specify any topic or concept (e.g., Dijkstra's Algorithm, Big-O Notation, TCP Handshake) and our $providerName will compose an active note structure.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = aiTopic,
                    onValueChange = { aiTopic = it },
                    label = { Text("Topic or Concept") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onBuild(aiTopic)
                    onDismiss()
                },
                enabled = aiTopic.isNotBlank()
            ) {
                Text("BUILD NOTE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizGeneratorDialog(
    showQuizDialog: Boolean,
    onDismiss: () -> Unit,
    onGenerate: (type: String, difficulty: String, items: String) -> Unit
) {
    if (!showQuizDialog) return
    var quizType by remember { mutableStateOf("Multiple Choice") }
    var quizDifficulty by remember { mutableStateOf("Medium") }
    var quizItems by remember { mutableStateOf("5") }
    val providerName = "Google Gemini"

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
                        if (newValue.isEmpty()) {
                            quizItems = newValue
                        } else {
                            val intValue = newValue.toIntOrNull()
                            if (intValue != null && intValue in 1..20) {
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
                    HelpGuideRow(
                        "`some_variable_or_expression`",
                        "Inline Code: Use backticks to highlight variables or short formulas."
                    )
                    HelpGuideRow(
                        "```python\ndef hello():\n    print(\"World\")\n```",
                        "Code Block: Specify the language for syntax highlighting."
                    )
                    HelpGuideRow(
                        "```mermaid\ngraph TD;\n    A-->B;\n    A-->C;\n    B-->D;\n    C-->D;\n```",
                        "Mermaid Diagram: Renders flowcharts and architecture graphs dynamically."
                    )
                    HelpGuideRow(
                        """```json:illustration\n{\n  "canvasWidth": 300,\n  "canvasHeight": 200,\n  "elements": [\n    { "type": "rect", "x": 10, "y": 10, "width": 280, "height": 180, "color": "#FF5722", "strokeWidth": 4 },\n    { "type": "line", "startX": 10, "startY": 10, "endX": 290, "endY": 190, "color": "#2196F3", "strokeWidth": 2 },\n    { "type": "line", "startX": 10, "startY": 190, "endX": 290, "endY": 10, "color": "#2196F3", "strokeWidth": 2 }\n  ]\n}\n```""",
                        "Native Illustration: A JSON coordinates structure drawing a beautiful red outline box with a blue inner cross."
                    )
                    HelpGuideRow(
                        "| Header <!--style:col,green,default--> | Header 2 <!--style:col,blue,default--> |\n| --- | --- |\n| Cell 1 | Cell 2 |",
                        "Custom Tables: Styled headers with column background colors (green, blue, orange, red, purple, etc.)."
                    )
                    HelpGuideRow(
                        "\$\$E = mc^2\$\$",
                        "KaTeX Math Formula: For equations and mathematical notations."
                    )
                    HelpGuideRow(
                        "- Parent bullet point\n-- Child nested bullet point",
                        "Hierarchical Bullets: Use double dash (--) for secondary indented lists with open bullet point circles (○)."
                    )
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

@Composable
fun VersionHistoryDialog(
    showHistoryDialog: Boolean,
    snapshots: List<NoteHistoryEntity>,
    onDismiss: () -> Unit,
    onRestore: (String) -> Unit
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
                        val snapshot = snapshots[index]
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                        val dateStr = dateFormat.format(Date(snapshot.timestamp))

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
                                        text = dateStr,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${snapshot.content.length} chars",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = {
                                        onRestore(snapshot.content)
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
