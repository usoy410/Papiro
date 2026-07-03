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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsStore: SettingsStore,
    onThemeChanged: (String) -> Unit,
    onThemeModeChanged: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Expanded / Collapsed section states (Defaulted to false for minimalist clean aesthetic)
    var isThemeExpanded by remember { mutableStateOf(false) }
    var isAiExpanded by remember { mutableStateOf(false) }

    var selectedAppTheme by remember { mutableStateOf(settingsStore.appTheme) }
    var selectedTabMode by remember { mutableStateOf(if (settingsStore.appThemeMode == "DARK") 1 else 0) }
    var provider by remember { mutableStateOf(settingsStore.provider) }
    var localModelPath by remember { mutableStateOf(settingsStore.localModelPath) }
    var localMaxTokens by remember { mutableStateOf(settingsStore.localMaxTokens) }
    var localTemperature by remember { mutableStateOf(settingsStore.localTemperature) }
    var ollamaBaseUrl by remember { mutableStateOf(settingsStore.ollamaBaseUrl) }
    var ollamaModel by remember { mutableStateOf(settingsStore.ollamaModel) }
    var geminiApiKey by remember { mutableStateOf(settingsStore.geminiApiKey) }
    var paperDesign by remember { mutableStateOf(settingsStore.paperDesign) }
    var isGeminiKeyVisible by remember { mutableStateOf(false) }
    
    var localModelsList by remember { mutableStateOf(settingsStore.localModels) }
    var cloudModelsList by remember { mutableStateOf(settingsStore.cloudModels) }
    var selectedLocalModel by remember { mutableStateOf(settingsStore.selectedLocalModel) }
    var selectedCloudModel by remember { mutableStateOf(settingsStore.selectedCloudModel) }
    var newLocalModelInput by remember { mutableStateOf("") }
    var newCloudModelInput by remember { mutableStateOf("") }
    var showGuideDialog by remember { mutableStateOf(false) }
    var activeModelTab by remember { mutableStateOf(0) } // 0 = Local, 1 = Cloud
    var isFetchingModels by remember { mutableStateOf(false) }
    
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionTestMessage by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(connectionTestMessage) {
        connectionTestMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            connectionTestMessage = null
        }
    }



    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            val path = com.usoy.papiro.util.FileUtils.getPath(context, it) ?: it.toString()
            localModelPath = path
            settingsStore.localModelPath = path
        }
    }

    Scaffold(
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
                                .clickable { isThemeExpanded = !isThemeExpanded }
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
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (isThemeExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isThemeExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        AnimatedVisibility(visible = isThemeExpanded) {
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

                                Text(
                                    text = "AI Coprocessor Provider",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(
                                        SettingsStore.PROVIDER_GEMINI to "Google Gemini (AI Studio)",
                                        SettingsStore.PROVIDER_OLLAMA to "Ollama (Local / Termux)",
                                        SettingsStore.PROVIDER_LOCAL_ON_DEVICE to "Local On-Device task file"
                                    ).forEach { (p, label) ->
                                        val isSelected = provider == p
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    provider = p
                                                    settingsStore.provider = p
                                                }
                                                .padding(vertical = 4.dp)
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = {
                                                    provider = p
                                                    settingsStore.provider = p
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }

                                if (provider == SettingsStore.PROVIDER_OLLAMA) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Ollama Configuration", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                                IconButton(
                                                    onClick = {
                                                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                            isTestingConnection = true
                                                            try {
                                                                val request = okhttp3.Request.Builder().url(ollamaBaseUrl).get().build()
                                                                val client = okhttp3.OkHttpClient.Builder()
                                                                    .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                                                                    .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                                                                    .build()
                                                                val response = client.newCall(request).execute()
                                                                connectionTestMessage = "Ollama connection: ${response.code} ${response.message}"
                                                            } catch (e: Exception) {
                                                                connectionTestMessage = "Connection failed: ${e.message}"
                                                            } finally {
                                                                isTestingConnection = false
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    Icon(Icons.Default.PlayArrow, contentDescription = "Test Connection", tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                            
                                            OutlinedTextField(
                                                value = ollamaBaseUrl,
                                                onValueChange = {
                                                    ollamaBaseUrl = it
                                                    settingsStore.ollamaBaseUrl = it
                                                },
                                                label = { Text("Ollama Base URL") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                } else if (provider == SettingsStore.PROVIDER_GEMINI) {
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
                                            
                                            var isGeminiKeyVisibleInternal by remember { mutableStateOf(false) }
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
                                                trailingIcon = {
                                                    IconButton(onClick = { isGeminiKeyVisibleInternal = !isGeminiKeyVisibleInternal }) {
                                                        Icon(
                                                            imageVector = if (isGeminiKeyVisibleInternal) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                            contentDescription = "Toggle Visibility"
                                                        )
                                                    }
                                                },
                                                visualTransformation = if (isGeminiKeyVisibleInternal) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation()
                                            )
                                            
                                            var isTestingGeminiInternal by remember { mutableStateOf(false) }
                                            var geminiTestMessageInternal by remember { mutableStateOf("") }
                                            Button(
                                                onClick = {
                                                    val keyToTest = geminiApiKey.trim().ifEmpty { com.usoy.papiro.BuildConfig.GEMINI_API_KEY }
                                                    if (keyToTest.isEmpty()) {
                                                        geminiTestMessageInternal = "API Key cannot be empty."
                                                        return@Button
                                                    }
                                                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                        isTestingGeminiInternal = true
                                                        try {
                                                            val req = com.usoy.papiro.data.GenerateContentRequest(
                                                                contents = listOf(com.usoy.papiro.data.Content(parts = listOf(com.usoy.papiro.data.Part(text = "Hello"))))
                                                            )
                                                            val res = com.usoy.papiro.data.RetrofitClient.service.generateContent(
                                                                model = "gemini-3.5-flash",
                                                                apiKey = keyToTest,
                                                                request = req
                                                            )
                                                            val text = res.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                                                            geminiTestMessageInternal = if (text.isNotEmpty()) "Verified successfully!" else "Failed to verify."
                                                        } catch (e: Exception) {
                                                            geminiTestMessageInternal = "Error: ${e.message}"
                                                        } finally {
                                                            isTestingGeminiInternal = false
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                enabled = !isTestingGeminiInternal
                                            ) {
                                                if (isTestingGeminiInternal) {
                                                    androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Verifying...")
                                                } else {
                                                    Text("Verify API Key")
                                                }
                                            }
                                            if (geminiTestMessageInternal.isNotEmpty()) {
                                                Text(
                                                    text = geminiTestMessageInternal,
                                                    color = if (geminiTestMessageInternal.contains("successfully")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                } else if (provider == SettingsStore.PROVIDER_LOCAL_ON_DEVICE) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text("Local LLM Configuration", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                                            OutlinedTextField(
                                                value = localModelPath,
                                                onValueChange = {
                                                    localModelPath = it
                                                    settingsStore.localModelPath = it
                                                },
                                                label = { Text("Local Model File Path") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                trailingIcon = {
                                                    IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                                                        Icon(
                                                            imageVector = Icons.Default.Add, // You could use a folder icon if you want, but Add works
                                                            contentDescription = "Pick File"
                                                        )
                                                    }
                                                }
                                            )

                                            OutlinedTextField(
                                                value = localMaxTokens.toString(),
                                                onValueChange = {
                                                    it.toIntOrNull()?.let { num ->
                                                        localMaxTokens = num
                                                        settingsStore.localMaxTokens = num
                                                    }
                                                },
                                                label = { Text("Max Tokens") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Text(text = "Temperature: ${String.format("%.2f", localTemperature)}", style = MaterialTheme.typography.bodySmall)
                                            Slider(
                                                value = localTemperature,
                                                onValueChange = {
                                                    localTemperature = it
                                                    settingsStore.localTemperature = it
                                                },
                                                valueRange = 0f..1.5f,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Model Icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Model Management",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Select active models or scan your servers to discover successfully connected models.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                TabRow(
                                    selectedTabIndex = activeModelTab,
                                    containerColor = Color.Transparent,
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Tab(
                                        selected = activeModelTab == 0,
                                        onClick = { activeModelTab = 0 },
                                        text = { Text("Local Models") },
                                        icon = { Icon(Icons.Default.Computer, contentDescription = "Local") }
                                    )
                                    Tab(
                                        selected = activeModelTab == 1,
                                        onClick = { activeModelTab = 1 },
                                        text = { Text("Cloud Models") },
                                        icon = { Icon(Icons.Default.Cloud, contentDescription = "Cloud") }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (activeModelTab == 0) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "Active Local Model: " + if (selectedLocalModel.isEmpty()) "llama3 (Default)" else selectedLocalModel,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                            ),
                                            border = CardDefaults.outlinedCardBorder()
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                if (localModelsList.isEmpty()) {
                                                    Text(
                                                        text = "No local models available. Use the scanner below or add custom tags manually.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(8.dp)
                                                    )
                                                } else {
                                                    localModelsList.forEach { model ->
                                                        val isSelected = selectedLocalModel == model
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(
                                                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                                                    else Color.Transparent
                                                                )
                                                                .clickable {
                                                                    selectedLocalModel = model
                                                                    settingsStore.selectedLocalModel = model
                                                                    settingsStore.ollamaModel = model
                                                                    ollamaModel = model
                                                                }
                                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                RadioButton(
                                                                    selected = isSelected,
                                                                    onClick = {
                                                                        selectedLocalModel = model
                                                                        settingsStore.selectedLocalModel = model
                                                                        settingsStore.ollamaModel = model
                                                                        ollamaModel = model
                                                                    }
                                                                )
                                                                Text(
                                                                    text = model,
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                                )
                                                            }
                                                            IconButton(
                                                                onClick = {
                                                                    val newList = localModelsList.filter { it != model }
                                                                    localModelsList = newList
                                                                    settingsStore.localModels = newList
                                                                    if (selectedLocalModel == model) {
                                                                        val fallback = newList.firstOrNull() ?: ""
                                                                        selectedLocalModel = fallback
                                                                        settingsStore.selectedLocalModel = fallback
                                                                        settingsStore.ollamaModel = fallback
                                                                        ollamaModel = fallback
                                                                    }
                                                                }
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Delete,
                                                                    contentDescription = "Remove Model",
                                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = newLocalModelInput,
                                                onValueChange = { newLocalModelInput = it },
                                                label = { Text("Add Model Tag") },
                                                placeholder = { Text("e.g. llama3.2:3b") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Button(
                                                onClick = {
                                                    val trimmed = newLocalModelInput.trim()
                                                    if (trimmed.isNotEmpty() && !localModelsList.contains(trimmed)) {
                                                        val newList = localModelsList + trimmed
                                                        localModelsList = newList
                                                        settingsStore.localModels = newList
                                                        newLocalModelInput = ""
                                                    }
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Add")
                                            }
                                        }

                                         Row(
                                             modifier = Modifier.fillMaxWidth(),
                                             horizontalArrangement = Arrangement.spacedBy(8.dp)
                                         ) {
                                             Button(
                                                 onClick = {
                                                     val defaults = listOf("llama3", "llama3.2:1b", "qwen2.5:1.5b-instruct")
                                                     localModelsList = defaults
                                                     settingsStore.localModels = defaults
                                                     selectedLocalModel = "llama3"
                                                     settingsStore.selectedLocalModel = "llama3"
                                                     settingsStore.ollamaModel = "llama3"
                                                     ollamaModel = "llama3"
                                                     connectionTestMessage = "Reset to local Ollama defaults!"
                                                 },
                                                 modifier = Modifier.weight(1f),
                                                 colors = ButtonDefaults.buttonColors(
                                                     containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                     contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                 ),
                                                 shape = RoundedCornerShape(8.dp)
                                             ) {
                                                 Icon(Icons.Default.Delete, contentDescription = "Reset", modifier = Modifier.size(18.dp))
                                                 Spacer(modifier = Modifier.width(4.dp))
                                                 Text("Reset Defaults")
                                             }

                                             Button(
                                                 onClick = {
                                                     scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                         isFetchingModels = true
                                                         try {
                                                             val baseUrl = settingsStore.ollamaBaseUrl.trim().ifEmpty { "http://localhost:11434" }
                                                             val url = if (baseUrl.endsWith("/")) "${baseUrl}api/tags" else "$baseUrl/api/tags"
                                                             val request = okhttp3.Request.Builder().url(url).get().build()
                                                             val client = okhttp3.OkHttpClient.Builder()
                                                                 .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                                                                 .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                                                                 .build()
                                                             
                                                             val response = client.newCall(request).execute()
                                                             if (response.isSuccessful) {
                                                                 val body = response.body?.string() ?: ""
                                                                 val json = org.json.JSONObject(body)
                                                                 val modelsArray = json.optJSONArray("models")
                                                                 val fetched = mutableListOf<String>()
                                                                 if (modelsArray != null) {
                                                                     for (i in 0 until modelsArray.length()) {
                                                                         val mObj = modelsArray.getJSONObject(i)
                                                                         val name = mObj.optString("name")
                                                                         if (name.isNotEmpty()) {
                                                                             fetched.add(name)
                                                                         }
                                                                     }
                                                                 }
                                                                 if (fetched.isNotEmpty()) {
                                                                     val combined = (localModelsList + fetched).distinct()
                                                                     localModelsList = combined
                                                                     settingsStore.localModels = combined
                                                                     connectionTestMessage = "Discovered ${fetched.size} Ollama models!"
                                                                 } else {
                                                                     connectionTestMessage = "Connected to Ollama successfully, but found no downloaded models."
                                                                 }
                                                             } else {
                                                                 connectionTestMessage = "Ollama returned error code ${response.code}"
                                                             }
                                                         } catch (e: Exception) {
                                                             connectionTestMessage = "Could not reach Ollama server: ${e.localizedMessage}"
                                                         } finally {
                                                             isFetchingModels = false
                                                         }
                                                     }
                                                 },
                                                 modifier = Modifier.weight(1.1f),
                                                 colors = ButtonDefaults.buttonColors(
                                                     containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                     contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                 ),
                                                 shape = RoundedCornerShape(8.dp)
                                             ) {
                                                 if (isFetchingModels) {
                                                     CircularProgressIndicator(
                                                         modifier = Modifier.size(18.dp),
                                                         color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                         strokeWidth = 2.dp
                                                     )
                                                     Spacer(modifier = Modifier.width(4.dp))
                                                     Text("Scanning...")
                                                 } else {
                                                     Icon(Icons.Default.Refresh, contentDescription = "Scan", modifier = Modifier.size(18.dp))
                                                     Spacer(modifier = Modifier.width(4.dp))
                                                     Text("Scan Ollama")
                                                 }
                                             }
                                         }
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "Active Cloud Model: " + if (selectedCloudModel.isEmpty()) "gpt-4o-mini (Default)" else selectedCloudModel,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                            ),
                                            border = CardDefaults.outlinedCardBorder()
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                if (cloudModelsList.isEmpty()) {
                                                    Text(
                                                        text = "No cloud models available. Add custom IDs manually.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(8.dp)
                                                    )
                                                } else {
                                                     val handleModelSelect = { model: String ->
                                                         selectedCloudModel = model
                                                         settingsStore.selectedCloudModel = model
                                                     }

                                                    cloudModelsList.forEach { model ->
                                                        val isSelected = selectedCloudModel == model
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(
                                                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                                                    else Color.Transparent
                                                                )
                                                                .clickable { handleModelSelect(model) }
                                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                RadioButton(
                                                                    selected = isSelected,
                                                                    onClick = { handleModelSelect(model) }
                                                                )
                                                                Text(
                                                                    text = model,
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                                )
                                                            }
                                                            IconButton(
                                                                onClick = {
                                                                    val newList = cloudModelsList.filter { it != model }
                                                                    cloudModelsList = newList
                                                                    settingsStore.cloudModels = newList
                                                                     if (selectedCloudModel == model) {
                                                                         val fallback = newList.firstOrNull() ?: ""
                                                                         selectedCloudModel = fallback
                                                                         settingsStore.selectedCloudModel = fallback
                                                                     }
                                                                }
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Delete,
                                                                    contentDescription = "Remove Model",
                                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = newCloudModelInput,
                                                onValueChange = { newCloudModelInput = it },
                                                label = { Text("Add Model ID") },
                                                placeholder = { Text("e.g. gpt-4o-mini") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Button(
                                                onClick = {
                                                    val trimmed = newCloudModelInput.trim()
                                                    if (trimmed.isNotEmpty() && !cloudModelsList.contains(trimmed)) {
                                                        val newList = cloudModelsList + trimmed
                                                        cloudModelsList = newList
                                                        settingsStore.cloudModels = newList
                                                        newCloudModelInput = ""
                                                    }
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Add")
                                            }
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
                        Text("Get a free API key from Google AI Studio to use the latest Gemini models like gemini-3.5-flash and gemini-3.1-pro-preview directly.", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(onClick = { uriHandler.openUri("https://aistudio.google.com/app/apikey") }) {
                            Text("Get Gemini API Key")
                        }
                    }
                    item {
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("2. Local Ollama (via Termux on Android)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Run models entirely offline on your phone.", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { uriHandler.openUri("https://f-droid.org/packages/com.termux/") }) {
                            Text("Download Termux (F-Droid)")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Termux Setup Commands (Click to copy):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        
                        val cmds = listOf(
                            "pkg update && pkg upgrade -y",
                            "pkg install ollama -y",
                            "nohup ollama serve &",
                            "ollama run qwen2.5:0.5b"
                        )
                        
                        cmds.forEach { cmd ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { clipboardManager.setText(AnnotatedString(cmd)) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(cmd, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
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

