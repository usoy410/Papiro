package com.usoy.papiro.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.usoy.papiro.data.GeminiService
import com.usoy.papiro.data.NoteEntity
import com.usoy.papiro.data.NoteRepository
import com.usoy.papiro.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NoteViewModel(
    private val repository: NoteRepository,
    private val context: Context
) : ViewModel() {
    private val TAG = "NoteViewModel"

    val settingsStore = SettingsStore(context)
    private val documentRepo = com.usoy.papiro.data.DocumentExtractionRepository(settingsStore)

    val allNotes: StateFlow<List<NoteEntity>> = repository.allNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentNote = MutableStateFlow<NoteEntity?>(null)
    val currentNote: StateFlow<NoteEntity?> = _currentNote.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _uiEvent = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _isOcrRunning = MutableStateFlow(false)
    val isOcrRunning: StateFlow<Boolean> = _isOcrRunning.asStateFlow()

    fun selectNote(note: NoteEntity?) {
        _currentNote.value = note
    }

    fun createNewNote(onCreated: (NoteEntity) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val newNote = NoteEntity(title = "", content = "", backgroundType = settingsStore.paperDesign, timestamp = System.currentTimeMillis())
            val id = repository.insert(newNote)
            val insertedNote = newNote.copy(id = id)
            withContext(Dispatchers.Main) {
                _currentNote.value = insertedNote
                onCreated(insertedNote)
            }
        }
    }

    fun saveNote(title: String, content: String, backgroundType: String) {
        val current = _currentNote.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val updated = current.copy(title = title, content = content, backgroundType = backgroundType, timestamp = System.currentTimeMillis())
            repository.insert(updated)
            withContext(Dispatchers.Main) {
                _currentNote.value = updated
            }
        }
    }

    fun deleteNotes(ids: Set<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            ids.forEach { repository.deleteById(it) }
            withContext(Dispatchers.Main) {
                if (_currentNote.value?.id in ids) {
                    _currentNote.value = null
                }
            }
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteById(id)
            withContext(Dispatchers.Main) {
                if (_currentNote.value?.id == id) {
                    _currentNote.value = null
                }
            }
        }
    }

    fun generateAiNotes(topic: String, existingContent: String = "", onComplete: (String) -> Unit) {
        if (topic.trim().isEmpty()) return
        if (_isGenerating.value) {
            viewModelScope.launch {
                _uiEvent.emit("An AI generation is already in progress. Please wait for it to complete.")
            }
            return
        }
        _isGenerating.value = true
        val targetNote = _currentNote.value

        com.usoy.papiro.util.AppCoroutineScope.launch {
            try {
                val generatedMarkdown = GeminiService.generateStructuredNote(context, topic, existingContent, settingsStore)
                withContext(Dispatchers.Main) {
                    _isGenerating.value = false
                    val currentlyEditing = _currentNote.value
                    if (currentlyEditing != null && currentlyEditing.id == targetNote?.id) {
                        onComplete(generatedMarkdown)
                    } else {
                        val mergedContent = if (existingContent.isNotEmpty()) {
                            if (existingContent.endsWith("\n")) existingContent + generatedMarkdown else "$existingContent\n\n$generatedMarkdown"
                        } else {
                            generatedMarkdown
                        }
                        val noteToSave = targetNote?.copy(
                            title = topic.ifEmpty { "AI Note" },
                            content = mergedContent,
                            timestamp = System.currentTimeMillis()
                        ) ?: NoteEntity(
                            title = topic.ifEmpty { "AI Note" },
                            content = generatedMarkdown,
                            backgroundType = settingsStore.paperDesign,
                            timestamp = System.currentTimeMillis()
                        )
                        launch(Dispatchers.IO) {
                            val newId = repository.insert(noteToSave)
                            if (generatedMarkdown.trim().isNotEmpty()) {
                                repository.insertHistory(
                                    com.usoy.papiro.data.NoteHistoryEntity(
                                        noteId = if (noteToSave.id == 0L) newId else noteToSave.id,
                                        content = generatedMarkdown,
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed local generation in app-scope", e)
                withContext(Dispatchers.Main) {
                    _isGenerating.value = false
                    _uiEvent.emit("Error generating note: ${e.message}")
                }
            }
        }
    }

    fun generateCustomPrompt(prompt: String, onComplete: (String) -> Unit) {
        if (prompt.trim().isEmpty()) return
        if (_isGenerating.value) {
            viewModelScope.launch {
                _uiEvent.emit("An AI generation is already in progress. Please wait for it to complete.")
            }
            return
        }
        _isGenerating.value = true
        com.usoy.papiro.util.AppCoroutineScope.launch {
            try {
                val generated = GeminiService.generateCustomContent(context, prompt, settingsStore)
                withContext(Dispatchers.Main) {
                    _isGenerating.value = false
                    onComplete(generated)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Generate custom prompt failed", e)
                withContext(Dispatchers.Main) {
                    _isGenerating.value = false
                    _uiEvent.emit("Error generating custom content: ${e.message}")
                }
            }
        }
    }
    
    fun extractTextFromDocument(context: Context, uri: android.net.Uri, onComplete: (String) -> Unit) {
        if (_isOcrRunning.value) {
            viewModelScope.launch {
                _uiEvent.emit("OCR extraction is already in progress. Please wait.")
            }
            return
        }
        _isOcrRunning.value = true
        viewModelScope.launch {
            val result = documentRepo.extractTextFromDocument(context, uri)
            onComplete(result)
            _isOcrRunning.value = false
        }
    }

    fun extractTextFromImage(imageBytes: ByteArray, onComplete: (String) -> Unit) {
        if (_isOcrRunning.value) {
            viewModelScope.launch {
                _uiEvent.emit("OCR extraction is already in progress. Please wait.")
            }
            return
        }
        _isOcrRunning.value = true
        viewModelScope.launch {
            val result = documentRepo.extractTextFromImage(context, imageBytes)
            onComplete(result)
            _isOcrRunning.value = false
        }
    }

    fun generateTableOfContents(content: String, onComplete: (String) -> Unit) {
        if (content.trim().isEmpty()) return
        if (_isGenerating.value) {
            viewModelScope.launch {
                _uiEvent.emit("An AI generation is already in progress. Please wait for it to complete.")
            }
            return
        }
        _isGenerating.value = true
        val targetNote = _currentNote.value

        com.usoy.papiro.util.AppCoroutineScope.launch {
            try {
                val toc = GeminiService.generateTableOfContents(context, content, settingsStore)
                withContext(Dispatchers.Main) {
                    _isGenerating.value = false
                    val currentlyEditing = _currentNote.value
                    if (currentlyEditing != null && currentlyEditing.id == targetNote?.id) {
                        onComplete(toc)
                    } else if (targetNote != null) {
                        val updatedContent = if (toc.isNotEmpty()) "$toc\n\n${targetNote.content}" else targetNote.content
                        val noteToSave = targetNote.copy(
                            content = updatedContent,
                            timestamp = System.currentTimeMillis()
                        )
                        launch(Dispatchers.IO) {
                            repository.insert(noteToSave)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "TOC failed in app-scope", e)
                withContext(Dispatchers.Main) {
                    _isGenerating.value = false
                    _uiEvent.emit("Error generating table of contents: ${e.message}")
                }
            }
        }
    }

    fun enhanceNote(content: String, onComplete: (String) -> Unit) {
        if (content.trim().isEmpty()) return
        if (_isGenerating.value) {
            viewModelScope.launch {
                _uiEvent.emit("An AI generation is already in progress. Please wait for it to complete.")
            }
            return
        }
        _isGenerating.value = true
        val targetNote = _currentNote.value
        com.usoy.papiro.util.AppCoroutineScope.launch {
            try {
                val enhanced = GeminiService.enhanceNoteContent(context, content, settingsStore)
                withContext(Dispatchers.Main) {
                    _isGenerating.value = false
                    val currentlyEditing = _currentNote.value
                    if (currentlyEditing != null && currentlyEditing.id == targetNote?.id) {
                        onComplete(enhanced)
                    } else if (targetNote != null) {
                        val noteToSave = targetNote.copy(
                            content = enhanced,
                            timestamp = System.currentTimeMillis()
                        )
                        launch(Dispatchers.IO) {
                            repository.insert(noteToSave)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Enhance failed in app-scope", e)
                withContext(Dispatchers.Main) {
                    _isGenerating.value = false
                    _uiEvent.emit("Error enhancing content: ${e.message}")
                }
            }
        }
    }

    fun insertHistorySnapshot(noteId: Long, content: String) {
        if (noteId == 0L || content.trim().isEmpty()) return
        viewModelScope.launch {
            repository.insertHistory(
                com.usoy.papiro.data.NoteHistoryEntity(
                    noteId = noteId,
                    content = content,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun getHistoryForNote(noteId: Long): kotlinx.coroutines.flow.Flow<List<com.usoy.papiro.data.NoteHistoryEntity>> {
        return repository.getHistoryForNote(noteId)
    }

    fun generateTitleIfEmpty(currentTitle: String, content: String, onComplete: (String) -> Unit) {
        if (currentTitle.trim().isNotEmpty() && currentTitle.trim() != "Untitled Note") {
            onComplete(currentTitle)
            return
        }
        if (content.trim().isEmpty()) {
            onComplete("Untitled Note")
            return
        }
        
        com.usoy.papiro.util.AppCoroutineScope.launch {
            try {
                val generatedTitle = GeminiService.generateTitle(context, content, settingsStore)
                withContext(Dispatchers.Main) {
                    onComplete(generatedTitle.ifEmpty { "Untitled Note" })
                }
            } catch (e: Exception) {
                Log.e(TAG, "Generate title failed", e)
                withContext(Dispatchers.Main) {
                    _uiEvent.emit("Error generating title: ${e.message}")
                    onComplete("Untitled Note")
                }
            }
        }
    }

    fun updateExistingToc(content: String): String {
        val lines = content.split("\n")
        val tocLines = mutableListOf<String>()
        var hasHeaders = false
        
        for (line in lines) {
            if (line.startsWith("#")) {
                val level = line.takeWhile { it == '#' }.length
                val name = line.substring(level).trim()
                if (name.equals("TABLE OF CONTENTS", ignoreCase = true)) continue
                hasHeaders = true
                val link = name.lowercase().replace(" ", "-").replace(Regex("[^a-z0-9-]"), "")
                val indent = "  ".repeat(maxOf(0, level - 2))
                tocLines.add("$indent- [$name](#$link)")
            }
        }
        
        if (!hasHeaders) return content
        
        val newTocBlock = buildString {
            append("# TABLE OF CONTENTS\n\n")
            tocLines.forEach { append(it).append("\n") }
            append("\n")
        }
        
        val tocIndex = content.indexOf("# TABLE OF CONTENTS", ignoreCase = true)
        if (tocIndex != -1) {
            val nextHeadingIndex = content.indexOf("\n#", tocIndex + "# TABLE OF CONTENTS".length)
            if (nextHeadingIndex != -1) {
                return content.substring(0, tocIndex) + newTocBlock + content.substring(nextHeadingIndex + 1)
            } else {
                return content.substring(0, tocIndex) + newTocBlock
            }
        }
        return content
    }
}

class NoteViewModelFactory(
    private val repository: NoteRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
