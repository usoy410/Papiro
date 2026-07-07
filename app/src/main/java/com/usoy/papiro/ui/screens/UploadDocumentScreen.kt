package com.usoy.papiro.ui.screens

import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.usoy.papiro.data.Content
import com.usoy.papiro.data.GenerateContentRequest
import com.usoy.papiro.data.InlineData
import com.usoy.papiro.data.Part
import com.usoy.papiro.data.RetrofitClient
import com.usoy.papiro.BuildConfig
import com.usoy.papiro.data.NoteEntity
import com.usoy.papiro.viewmodel.NoteViewModel
import com.usoy.papiro.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadDocumentScreen(
    viewModel: NoteViewModel,
    initialMode: String = "Note",
    onNoteExtracted: (NoteEntity) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }
    
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String>("") }
    
    var generationMode by remember { mutableStateOf(initialMode) }
    var quizType by remember { mutableStateOf("Multiple Choice") }
    var difficultyLevel by remember { mutableStateOf("Medium") }
    var numberOfItems by remember { mutableStateOf("5") }
    
    val models = listOf(
        "gemini-1.5-flash" to "Gemini 1.5 Flash (Ultra-fast, low cost)",
        "gemini-2.5-flash" to "Gemini 2.5 Flash (Improved reasoning, best balance)",
        "gemini-3.1-flash-lite" to "Gemini 3.1 Flash Lite (Cheapest, high speed)"
    )
    
    var selectedModel by remember { mutableStateOf("gemini-2.5-flash") }
    var expandedModelMenu by remember { mutableStateOf(false) }
    
    var summaryType by remember { mutableStateOf("Concise") }
    val summaryOptions = listOf("Concise", "Detailed", "Preserve Content")
    
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            selectedUri = it
            selectedFileName = getFileName(context, it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload Document", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            Card(
                onClick = { 
                    filePickerLauncher.launch(arrayOf(
                        "application/pdf",
                        "text/plain",
                        "image/png",
                        "image/jpeg",
                        "image/webp"
                    )) 
                },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = "Select File")
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = if (selectedUri != null) selectedFileName else "Tap to select PDF, TXT, or Document Image",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Text("Select AI Model", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ExposedDropdownMenuBox(
                expanded = expandedModelMenu,
                onExpandedChange = { expandedModelMenu = !expandedModelMenu }
            ) {
                OutlinedTextField(
                    value = models.find { it.first == selectedModel }?.second ?: selectedModel,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedModelMenu) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedModelMenu,
                    onDismissRequest = { expandedModelMenu = false }
                ) {
                    models.forEach { (modelId, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                selectedModel = modelId
                                expandedModelMenu = false
                            }
                        )
                    }
                }
            }
            
            Text("Generation Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = generationMode == "Note",
                    onClick = { generationMode = "Note" },
                    label = { Text("Study Note") }
                )
                FilterChip(
                    selected = generationMode == "Quiz",
                    onClick = { generationMode = "Quiz" },
                    label = { Text("Quiz Generator") }
                )
            }

            if (generationMode == "Note") {
                Text("Summary Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    summaryOptions.forEach { option ->
                        FilterChip(
                            selected = summaryType == option,
                            onClick = { summaryType = option },
                            label = { Text(option) }
                        )
                    }
                }
            } else {
                Text("Quiz Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Multiple Choice", "Flashcards").forEach { type ->
                        FilterChip(
                            selected = quizType == type,
                            onClick = { quizType = type },
                            label = { Text(type) }
                        )
                    }
                }

                Text("Difficulty Level", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Easy", "Medium", "Hard").forEach { level ->
                        FilterChip(
                            selected = difficultyLevel == level,
                            onClick = { difficultyLevel = level },
                            label = { Text(level) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = numberOfItems,
                    onValueChange = { newValue -> 
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            val intValue = newValue.toIntOrNull()
                            if (intValue == null || intValue <= 20) {
                                numberOfItems = newValue
                            }
                        }
                    },
                    label = { Text("Number of Items (Max 20)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            
            val isOcrSupported = true
            
            if (!isOcrSupported) {
                Text(
                    text = "Document Summary & OCR is only supported with Google Gemini or Local On-Device AI. Please switch your provider in Settings.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            Button(
                onClick = {
                    if (selectedUri != null) {
                        isProcessing = true
                        errorMessage = null
                        coroutineScope.launch {
                            try {
                                val mimeType = context.contentResolver.getType(selectedUri!!) ?: "application/pdf"
                                val bytes = withContext(Dispatchers.IO) {
                                    context.contentResolver.openInputStream(selectedUri!!)?.use { it.readBytes() }
                                }
                                if (bytes != null) {
                                    val promptText = if (generationMode == "Quiz") {
                                        com.usoy.papiro.util.PromptTemplates.getQuizPrompt(
                                            quizType = quizType,
                                            difficultyLevel = difficultyLevel,
                                            numberOfItems = numberOfItems,
                                            contextText = "the provided document"
                                        )
                                    } else {
                                        com.usoy.papiro.util.PromptTemplates.getSummaryPrompt(summaryType)
                                    }

                                    val useLocalExtraction = 
                                                             false || 
                                                             settingsStore.ocrStrategy == "ML_KIT"

                                    if (useLocalExtraction) {
                                        val textContent = if (mimeType == "text/plain") {
                                            String(bytes)
                                        } else {
                                            val repo = com.usoy.papiro.data.DocumentExtractionRepository(settingsStore)
                                            repo.extractTextFromDocument(context, selectedUri!!)
                                        }
                                        
                                        if (textContent.startsWith("Error:") || textContent.startsWith("Failed to extract text")) {
                                            errorMessage = textContent
                                            isProcessing = false
                                            return@launch
                                        }

                                        val combinedPrompt = "$promptText\n\nDocument Content:\n$textContent"
                                        val resultText = com.usoy.papiro.data.GeminiService.generateStructuredNote(
                                            context = context,
                                            topic = combinedPrompt,
                                            existingContent = "",
                                            settings = settingsStore
                                        )
                                        
                                        if (resultText.startsWith("Error:") || resultText.startsWith("OCR Failed:") || resultText.contains("No compatible model file was found")) {
                                            errorMessage = resultText
                                        } else {
                                            var suggestedTitle = ""
                                            try {
                                                val titlePrompt = if (generationMode == "Quiz") {
                                                    "Based on the following quiz, generate a short, highly suitable, descriptive, engaging title (maximum 4-6 words). Respond with ONLY the plain text title, no quotes, no markdown, no introduction/outro, no 'Quiz:' prefix:\n\n$resultText"
                                                } else {
                                                    "Based on the following study note, generate a short, highly suitable, descriptive, engaging title (maximum 4-6 words). Respond with ONLY the plain text title, no quotes, no markdown, no introduction/outro:\n\n$resultText"
                                                }
                                                val aiTitle = com.usoy.papiro.data.GeminiService.generateStructuredNote(
                                                    context = context,
                                                    topic = titlePrompt,
                                                    existingContent = "",
                                                    settings = settingsStore
                                                ).trim().removeSurrounding("\"").removeSurrounding("'").trim()
                                                
                                                if (aiTitle.isNotEmpty() && !aiTitle.startsWith("Error:") && aiTitle.length < 100) {
                                                    suggestedTitle = aiTitle
                                                }
                                            } catch (e: Exception) {
                                                // Fallback
                                            }

                                            viewModel.createNewNote()
                                            val newNote = viewModel.currentNote.value
                                            if (newNote != null) {
                                                val titleStr = if (suggestedTitle.isNotEmpty()) {
                                                    suggestedTitle
                                                } else {
                                                    if (generationMode == "Quiz") {
                                                        "$difficultyLevel $quizType Quiz: $selectedFileName"
                                                    } else {
                                                        "$summaryType Note from $selectedFileName"
                                                    }
                                                }
                                                val bgType = if (generationMode == "Quiz") "QUIZ" else newNote.backgroundType
                                                val finalContent = if (generationMode == "Quiz") {
                                                    "# TABLE OF CONTENTS\n\n- [📝 Practice Quiz](#practice-quiz)\n\n## 📝 Practice Quiz\n\n$resultText"
                                                } else {
                                                    resultText
                                                }
                                                viewModel.saveNote(
                                                    title = titleStr,
                                                    content = finalContent,
                                                    backgroundType = bgType
                                                )
                                                onNoteExtracted(viewModel.currentNote.value!!)
                                            }
                                        }
                                    } else {
                                        val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                        
                                        val request = GenerateContentRequest(
                                            contents = listOf(Content(
                                                parts = listOf(
                                                    Part(text = promptText),
                                                    Part(inlineData = InlineData(mimeType = mimeType, data = base64Data))
                                                )
                                            ))
                                        )
                                        
                                        val apiKeyToUse = settingsStore.geminiApiKey.ifEmpty { BuildConfig.GEMINI_API_KEY }
                                        
                                        if (apiKeyToUse.isEmpty()) {
                                            errorMessage = "Error: Gemini API Key is missing. Please go to Settings and enter your Gemini API Key to enable Document Processing."
                                            isProcessing = false
                                            return@launch
                                        }
                                        
                                        val response = RetrofitClient.service.generateContent(
                                            model = selectedModel,
                                            apiKey = apiKeyToUse,
                                            request = request
                                        )
                                        
                                        val resultText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response"
                                        
                                        var suggestedTitle = ""
                                        try {
                                            val titlePrompt = if (generationMode == "Quiz") {
                                                "Based on the following quiz, generate a short, highly suitable, descriptive, engaging title (maximum 4-6 words). Respond with ONLY the plain text title, no quotes, no markdown, no introduction/outro, no 'Quiz:' prefix:\n\n$resultText"
                                            } else {
                                                "Based on the following study note, generate a short, highly suitable, descriptive, engaging title (maximum 4-6 words). Respond with ONLY the plain text title, no quotes, no markdown, no introduction/outro:\n\n$resultText"
                                            }
                                            val aiTitle = com.usoy.papiro.data.GeminiService.generateStructuredNote(
                                                context = context,
                                                topic = titlePrompt,
                                                existingContent = "",
                                                settings = settingsStore
                                            ).trim().removeSurrounding("\"").removeSurrounding("'").trim()
                                            
                                            if (aiTitle.isNotEmpty() && !aiTitle.startsWith("Error:") && aiTitle.length < 100) {
                                                suggestedTitle = aiTitle
                                            }
                                        } catch (e: Exception) {
                                            // Fallback
                                        }

                                        // Create note
                                        viewModel.createNewNote()
                                        val newNote = viewModel.currentNote.value
                                        if (newNote != null) {
                                            val titleStr = if (suggestedTitle.isNotEmpty()) {
                                                suggestedTitle
                                            } else {
                                                if (generationMode == "Quiz") {
                                                    "$difficultyLevel $quizType Quiz: $selectedFileName"
                                                } else {
                                                    "$summaryType Note from $selectedFileName"
                                                }
                                            }
                                            val bgType = if (generationMode == "Quiz") "QUIZ" else newNote.backgroundType
                                            val finalContent = if (generationMode == "Quiz") {
                                                "# TABLE OF CONTENTS\n\n- [📝 Practice Quiz](#practice-quiz)\n\n## 📝 Practice Quiz\n\n$resultText"
                                            } else {
                                                resultText
                                            }
                                            viewModel.saveNote(
                                                title = titleStr,
                                                content = finalContent,
                                                backgroundType = bgType
                                            )
                                            // Wait a moment for save
                                            onNoteExtracted(viewModel.currentNote.value!!)
                                        }
                                    }
                                } else {
                                    errorMessage = "Could not read file."
                                }
                            } catch (e: Exception) {
                                errorMessage = "Error: ${e.message}"
                            } finally {
                                isProcessing = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedUri != null && !isProcessing && isOcrSupported
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Processing Document...")
                } else {
                    Text("Extract Note")
                }
            }
        }
    }
}

fun getFileName(context: android.content.Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        try {
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        result = it.getString(columnIndex)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    // Clean up if it's still showing raw document id format
    if (result != null && result!!.contains(":")) {
        result = result!!.substringAfterLast(":")
    }
    return result ?: "Document"
}
