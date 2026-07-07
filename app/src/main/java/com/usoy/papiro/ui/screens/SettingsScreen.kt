package com.usoy.papiro.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usoy.papiro.data.SettingsStore
import kotlinx.coroutines.launch
import com.usoy.papiro.viewmodel.NoteViewModel

import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsStore: SettingsStore,
    viewModel: NoteViewModel,
    onThemeChanged: (String) -> Unit,
    onThemeModeChanged: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Expanded / Collapsed section states (Defaulted to false for minimalist clean aesthetic)
    var isAiExpanded by remember { mutableStateOf(false) }

    var selectedAppTheme by remember { mutableStateOf(settingsStore.appTheme) }
    var selectedTabMode by remember { mutableStateOf(if (settingsStore.appThemeMode == "DARK") 1 else 0) }
    var provider by remember { mutableStateOf(settingsStore.provider) }
    var localModelPath by remember { mutableStateOf("") }
    var localMaxTokens by remember { mutableStateOf(512) }
    var localTemperature by remember { mutableStateOf(0.7f) }
    var geminiApiKey by remember { mutableStateOf(settingsStore.geminiApiKey) }
    var paperDesign by remember { mutableStateOf(settingsStore.paperDesign) }
    var ocrStrategy by remember { mutableStateOf(settingsStore.ocrStrategy) }
    
    var cloudModelsList by remember { mutableStateOf(settingsStore.cloudModels) }
    var selectedCloudModel by remember { mutableStateOf(settingsStore.selectedCloudModel) }
    var newCloudModelInput by remember { mutableStateOf("") }
    var showGuideDialog by remember { mutableStateOf(false) }
    var activeModelTab by remember { mutableStateOf(0) } // 0 = Local, 1 = Cloud
    var isFetchingModels by remember { mutableStateOf(false) }
    
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionTestMessage by remember { mutableStateOf<String?>(null) }
    
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    LaunchedEffect(connectionTestMessage) {
        connectionTestMessage?.let {
            snackbarHostState.showSnackbar(it)
            connectionTestMessage = null
        }
    }



    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            
            
            
            
            
            
            
        }
    }

    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showGuideDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Guide"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // SECTION 1: THEME & APPEARANCE (COLLAPSIBLE)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Theme Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Theme & Appearance",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Colors & Default Notebook Design",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                             
                        }

                        
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                Text(
                                    text = "Appearance Mode",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val isDark = selectedTabMode == 1
                                    
                                    // Light Mode Tab
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(
                                                if (!isDark) MaterialTheme.colorScheme.primary
                                                else Color.Transparent
                                            )
                                            .clickable {
                                                selectedTabMode = 0
                                                settingsStore.appThemeMode = "LIGHT"
                                                onThemeModeChanged("LIGHT")
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Light Mode",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (!isDark) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Dark Mode Tab
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(
                                                if (isDark) MaterialTheme.colorScheme.primary
                                                else Color.Transparent
                                            )
                                            .clickable {
                                                selectedTabMode = 1
                                                settingsStore.appThemeMode = "DARK"
                                                onThemeModeChanged("DARK")
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Dark Mode",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Select Palette Theme",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                val themesList = if (selectedTabMode == 1) {
                                    listOf(
                                        "INDIGO" to Triple("Indigo Classic", Color(0xFFD0BCFF), Color(0xFF4F378B)),
                                        "EMERALD" to Triple("Emerald Mint", Color(0xFF2DD4BF), Color(0xFF115E59)),
                                        "AMBER" to Triple("Warm Golden Amber", Color(0xFFFBBF24), Color(0xFF78350F)),
                                        "ROSE" to Triple("Romantic Rose Crimson", Color(0xFFFB7185), Color(0xFF881337)),
                                        "COSMIC" to Triple("Cosmic Slate Blue", Color(0xFF60A5FA), Color(0xFF1E293B))
                                    )
                                } else {
                                    listOf(
                                        "INDIGO" to Triple("Indigo Classic", Color(0xFF4F378B), Color(0xFFEADDFF)),
                                        "EMERALD" to Triple("Emerald Mint", Color(0xFF0F766E), Color(0xFFCCFBF1)),
                                        "AMBER" to Triple("Warm Golden Amber", Color(0xFFB45309), Color(0xFFFEF3C7)),
                                        "ROSE" to Triple("Romantic Rose Crimson", Color(0xFFBE123C), Color(0xFFFFE4E6)),
                                        "COSMIC" to Triple("Cosmic Slate Blue", Color(0xFF1E3A8A), Color(0xFFDBEAFE))
                                    )
                                }

                                themesList.forEach { (themeKey, details) ->
                                    val (themeNameLabel, primaryColor, containerColor) = details
                                    val isSelected = selectedAppTheme == themeKey
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                selectedAppTheme = themeKey
                                                settingsStore.appTheme = themeKey
                                                onThemeChanged(themeKey)
                                            }
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                                else Color.Transparent
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Preview Circle 1
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(primaryColor)
                                                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), CircleShape)
                                            )
                                            // Preview Circle 2
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(containerColor)
                                                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = themeNameLabel,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                Text(
                                    text = "Default Notebook Design",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf("GRID", "RULED", "DOTS", "BLANK").forEach { design ->
                                        val isSelected = paperDesign == design
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    paperDesign = design
                                                    settingsStore.paperDesign = design
                                                },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                            ),
                                            border = if (isSelected) null else CardDefaults.outlinedCardBorder().copy(
                                                brush = androidx.compose.ui.graphics.SolidColor(
                                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                                )
                                            )
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(vertical = 10.dp)
                                                    .fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = design,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        
                    }
                }
            }



            // SECTION 3: AI COPROCESSOR SETTINGS (COLLAPSIBLE)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isAiExpanded = !isAiExpanded }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "AI Coprocessor Settings",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Provider selection, model configurations & parameters",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (isAiExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isAiExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        AnimatedVisibility(visible = isAiExpanded) {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                Divider(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                Text(
                                    text = "Document Extraction (OCR) Strategy",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(
                                        "ML_KIT" to "ML Kit + AI Refine (Fast, Local parsing)",
                                        "GEMINI_VISION" to "Gemini Vision (Cloud, Handles complex layouts)"
                                    ).forEach { (p, label) ->
                                        val isSelected = ocrStrategy == p
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    ocrStrategy = p
                                                    settingsStore.ocrStrategy = p
                                                }
                                                .padding(vertical = 4.dp)
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = {
                                                    ocrStrategy = p
                                                    settingsStore.ocrStrategy = p
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("Google Gemini Configuration", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "Configure your Google AI Studio Gemini API key to enable high-quality cloud processing for note generation and document OCR.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        
                                        OutlinedTextField(
                                            value = geminiApiKey,
                                            onValueChange = { 
                                                geminiApiKey = it
                                                settingsStore.geminiApiKey = it
                                            },
                                            label = { Text("Gemini API Key") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            placeholder = { Text("Enter your Gemini API key") },
                                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                                        )
                                        
                                        var isTestingGemini by remember { mutableStateOf(false) }
                                        var geminiTestMessage by remember { mutableStateOf("") }
                                        Button(
                                            onClick = {
                                                val keyToTest = geminiApiKey.trim().ifEmpty { com.usoy.papiro.BuildConfig.GEMINI_API_KEY }
                                                if (keyToTest.isEmpty()) {
                                                    geminiTestMessage = "API Key cannot be empty."
                                                    return@Button
                                                }
                                                scope.launch {
                                                    isTestingGemini = true
                                                    val (success, msg) = com.usoy.papiro.data.GeminiService.testApiKey(keyToTest)
                                                    geminiTestMessage = msg
                                                    isTestingGemini = false
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            enabled = !isTestingGemini
                                        ) {
                                            if (isTestingGemini) {
                                                androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Verifying...")
                                            } else {
                                                Text("Verify API Key")
                                            }
                                        }
                                        if (geminiTestMessage.isNotEmpty()) {
                                            Text(
                                                text = geminiTestMessage,
                                                color = if (geminiTestMessage.contains("successfully")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
    if (showGuideDialog) {
        val uriHandler = LocalUriHandler.current
        val clipboardManager = LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = { showGuideDialog = false },
            title = { Text("AI Provider Guide & Setup", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item {
                        Text("1. Google AI Studio (Gemini API)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Get a free API key from Google AI Studio to use the latest Gemini models like gemini-1.5-flash, gemini-2.5-flash and gemini-3.1-flash-lite directly.", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(onClick = { uriHandler.openUri("https://aistudio.google.com/app/apikey") }) {
                            Text("Get Gemini API Key")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGuideDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

