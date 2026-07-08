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
import com.usoy.papiro.data.ImageSearchResolver
import com.usoy.papiro.ui.screens.NoteEditorScreen
import com.usoy.papiro.ui.screens.NoteListScreen
import com.usoy.papiro.ui.screens.SettingsScreen
import com.usoy.papiro.ui.screens.UploadDocumentScreen
import com.usoy.papiro.ui.theme.PapiroTheme
import com.usoy.papiro.viewmodel.NoteViewModel
import com.usoy.papiro.viewmodel.NoteViewModelFactory

import com.usoy.papiro.ui.screens.OnboardingScreen

enum class Screen {
    LIST, EDITOR, SETTINGS, UPLOAD, ONBOARDING
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize global Coil ImageLoader with SVG support and realistic User-Agent
        val imageLoader = coil.ImageLoader.Builder(this)
            .components {
                add(coil.decode.SvgDecoder.Factory())
            }
            .okHttpClient {
                okhttp3.OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36")
                            .build()
                        chain.proceed(request)
                    }
                    .build()
            }
            .build()
        coil.Coil.setImageLoader(imageLoader)

        // Warm up the Render-hosted image search backend on app launch
        ImageSearchResolver.warmup()

        val database = NoteDatabase.getDatabase(this)
        val repository = NoteRepository(database.noteDao())
        val settingsStore = SettingsStore(this)
        val factory = NoteViewModelFactory(repository, this)
        val viewModel: NoteViewModel by viewModels { factory }

        setContent {
            var currentTheme by remember { mutableStateOf(settingsStore.appTheme) }
            var currentThemeMode by remember { mutableStateOf(settingsStore.appThemeMode) }

            var currentScreen by remember { mutableStateOf(if (settingsStore.isOnboardingCompleted) Screen.LIST else Screen.ONBOARDING) }
            var uploadPreselectedMode by remember { mutableStateOf("Note") }
            val currentNote by viewModel.currentNote.collectAsState()

            val systemInDarkTheme = isSystemInDarkTheme()
            val isDarkTheme = when (currentThemeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> systemInDarkTheme
            }

            PapiroTheme(themeName = currentTheme, darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {

                    BackHandler(enabled = currentScreen != Screen.LIST) {
                        if (currentScreen == Screen.EDITOR) {
                            currentNote?.let {
                                viewModel.saveNote(it.title, it.content, it.backgroundType)
                            }
                        }
                        currentScreen = Screen.LIST
                    }

                    when (currentScreen) {
                        Screen.ONBOARDING -> {
                            OnboardingScreen(
                                settingsStore = settingsStore,
                                onFinish = { 
                                    settingsStore.isOnboardingCompleted = true
                                    currentScreen = Screen.LIST 
                                }
                            )
                        }
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
                                onUploadDocumentClick = { mode ->
                                    uploadPreselectedMode = mode
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
                                viewModel = viewModel,
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
                                initialMode = uploadPreselectedMode,
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
