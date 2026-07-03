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
    onNoteExtracted: (NoteEntity) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }
    
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String>("") }
    
    var selectedModel by remember { mutableStateOf("gemini-3.5-flash") }
    val models = listOf(
        "gemini-3.5-flash" to "Flash (Recommended for free/fast)",
        "gemini-3.1-pro-preview" to "Pro (Best for complex logic)"
    )
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
            // Get simple file name
            selectedFileName = it.lastPathSegment ?: "Document"
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
                    modifier = Modifier.menuAnchor().fillMaxWidth()
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
            
            Spacer(modifier = Modifier.weight(1f))
            
            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
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
                                    val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                    val prompt = "Read this document and create a $summaryType note out of it."
                                    
                                    val request = GenerateContentRequest(
                                        contents = listOf(Content(
                                            parts = listOf(
                                                Part(text = prompt),
                                                Part(inlineData = InlineData(mimeType = mimeType, data = base64Data))
                                            )
                                        ))
                                    )
                                    
                                    val apiKeyToUse = settingsStore.geminiApiKey.ifEmpty { BuildConfig.GEMINI_API_KEY }
                                    
                                    val response = RetrofitClient.service.generateContent(
                                        model = selectedModel,
                                        apiKey = apiKeyToUse,
                                        request = request
                                    )
                                    
                                    val resultText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response"
                                    
                                    // Create note
                                    viewModel.createNewNote()
                                    val newNote = viewModel.currentNote.value
                                    if (newNote != null) {
                                        viewModel.saveNote(
                                            title = "$summaryType Note from $selectedFileName",
                                            content = resultText,
                                            backgroundType = newNote.backgroundType
                                        )
                                        // Wait a moment for save
                                        onNoteExtracted(viewModel.currentNote.value!!)
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
                enabled = selectedUri != null && !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Processing Document...")
                } else {
                    Text("Extract Note")
                }
            }
        }
    }
}
