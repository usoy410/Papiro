package com.usoy.papiro.viewmodel

import android.app.ActivityManager
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

    // Exposed flows
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

    private val _isOcrRunning = MutableStateFlow(false)
    val isOcrRunning: StateFlow<Boolean> = _isOcrRunning.asStateFlow()

    // RAM usage checking for local LLM safeguard
    data class RamStatus(
        val availableGb: Double,
        val totalGb: Double,
        val isLowMemory: Boolean,
        val percentageAvailable: Int
    )

    private val _ramStatus = MutableStateFlow<RamStatus?>(null)
    val ramStatus: StateFlow<RamStatus?> = _ramStatus.asStateFlow()

    init {
        checkSystemRam()
    }

    fun checkSystemRam() {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)

            val availGb = memoryInfo.availMem / (1024.0 * 1024.0 * 1024.0)
            val totalGb = memoryInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
            val pct = ((availGb / totalGb) * 100).toInt()

            _ramStatus.value = RamStatus(
                availableGb = String.format("%.2f", availGb).toDouble(),
                totalGb = String.format("%.2f", totalGb).toDouble(),
                isLowMemory = memoryInfo.lowMemory || availGb < 1.5, // Alert if under 1.5GB free
                percentageAvailable = pct
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inspect RAM availability", e)
        }
    }

    fun selectNote(note: NoteEntity?) {
        _currentNote.value = note
    }

    fun createNewNote() {
        _currentNote.value = NoteEntity(
            title = "",
            content = "",
            backgroundType = settingsStore.paperDesign
        )
    }

    fun saveNote(title: String, content: String, backgroundType: String, imagePath: String? = null) {
        val current = _currentNote.value ?: return
        viewModelScope.launch {
            val updated = current.copy(
                title = title.ifEmpty { "Untitled Note" },
                content = content,
                backgroundType = backgroundType,
                imagePath = imagePath,
                timestamp = System.currentTimeMillis()
            )
            val newId = repository.insert(updated)
            val finalId = if (updated.id == 0L) newId else updated.id
            val finalNote = updated.copy(id = finalId)
            _currentNote.value = finalNote

            if (content.trim().isNotEmpty()) {
                repository.insertHistory(
                    com.usoy.papiro.data.NoteHistoryEntity(
                        noteId = finalId,
                        content = content,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.deleteById(noteId)
            if (_currentNote.value?.id == noteId) {
                _currentNote.value = null
            }
        }
    }

    fun generateAiNotes(topic: String, existingContent: String = "", onComplete: (String) -> Unit) {
        if (topic.trim().isEmpty()) return
        _isGenerating.value = true
        checkSystemRam() // update memory status before running heavy generation

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
                        // The user has navigated away; save the generated note directly to the database so it's not lost
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
                }
            }
        }
    }

    fun extractTextFromImage(imageBytes: ByteArray, onComplete: (String) -> Unit) {
        _isOcrRunning.value = true
        viewModelScope.launch {
            try {
                val ocrResult = GeminiService.performOcr(imageBytes, settingsStore)
                onComplete(ocrResult)
            } catch (e: Exception) {
                Log.e(TAG, "OCR failed", e)
                onComplete("Failed to extract text from image: ${e.localizedMessage}")
            } finally {
                _isOcrRunning.value = false
            }
        }
    }

    fun generateTableOfContents(content: String, onComplete: (String) -> Unit) {
        if (content.trim().isEmpty()) return
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
                        // User navigated away; prepend the table of contents to the note content and save to database
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
                }
            }
        }
    }

    fun enhanceNote(content: String, onComplete: (String) -> Unit) {
        if (content.trim().isEmpty()) return
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
                        // User navigated away; update with enhanced content directly to the database
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
