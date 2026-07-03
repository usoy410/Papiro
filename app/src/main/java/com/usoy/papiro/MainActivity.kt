package com.usoy.papiro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.usoy.papiro.data.NoteDatabase
import com.usoy.papiro.data.NoteRepository
import com.usoy.papiro.data.SettingsStore
import com.usoy.papiro.ui.screens.NoteEditorScreen
import com.usoy.papiro.ui.screens.NoteListScreen
import com.usoy.papiro.ui.screens.SettingsScreen
import com.usoy.papiro.ui.screens.UploadDocumentScreen
import com.usoy.papiro.ui.theme.PapiroTheme
import com.usoy.papiro.viewmodel.NoteViewModel
import com.usoy.papiro.viewmodel.NoteViewModelFactory

enum class Screen {
    LIST, EDITOR, SETTINGS, UPLOAD
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = NoteDatabase.getDatabase(this)
        val repository = NoteRepository(database.noteDao())
        val settingsStore = SettingsStore(this)
        val factory = NoteViewModelFactory(repository, this)
        val viewModel: NoteViewModel by viewModels { factory }

        setContent {
            var currentTheme by remember { mutableStateOf(settingsStore.appTheme) }
            var currentThemeMode by remember { mutableStateOf(settingsStore.appThemeMode) }

            val systemInDarkTheme = isSystemInDarkTheme()
            val isDarkTheme = when (currentThemeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> systemInDarkTheme
            }

            PapiroTheme(themeName = currentTheme, darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf(Screen.LIST) }

                    // Sync currentScreen with ViewModel's active note
                    val currentNote by viewModel.currentNote.collectAsState()

                    BackHandler(enabled = currentScreen != Screen.LIST) {
                        if (currentScreen == Screen.EDITOR) {
                            currentNote?.let {
                                viewModel.saveNote(it.title, it.content, it.backgroundType)
                            }
                        }
                        currentScreen = Screen.LIST
                    }

                    when (currentScreen) {
                        Screen.LIST -> {
                            NoteListScreen(
                                viewModel = viewModel,
                                onNoteSelected = { note ->
                                    if (note.id != 0L) {
                                        viewModel.selectNote(note)
                                    }
                                    currentScreen = Screen.EDITOR
                                },
                                onSettingsClick = {
                                    currentScreen = Screen.SETTINGS
                                },
                                onUploadDocumentClick = {
                                    currentScreen = Screen.UPLOAD
                                }
                            )
                        }
                        Screen.EDITOR -> {
                            NoteEditorScreen(
                                viewModel = viewModel,
                                onBack = {
                                    currentScreen = Screen.LIST
                                }
                            )
                        }
                        Screen.SETTINGS -> {
                            SettingsScreen(
                                settingsStore = settingsStore,
                                onThemeChanged = { newTheme ->
                                    currentTheme = newTheme
                                },
                                onThemeModeChanged = { newMode ->
                                    currentThemeMode = newMode
                                },
                                onBack = {
                                    currentScreen = Screen.LIST
                                }
                            )
                        }
                        Screen.UPLOAD -> {
                            UploadDocumentScreen(
                                viewModel = viewModel,
                                onNoteExtracted = { note ->
                                    currentScreen = Screen.EDITOR
                                },
                                onBack = {
                                    currentScreen = Screen.LIST
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
