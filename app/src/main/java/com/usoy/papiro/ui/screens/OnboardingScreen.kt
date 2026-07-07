package com.usoy.papiro.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usoy.papiro.data.GeminiService
import com.usoy.papiro.data.SettingsStore
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    settingsStore: SettingsStore,
    onFinish: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Crossfade(
                targetState = step,
                modifier = Modifier.weight(1f),
                label = "onboarding_crossfade"
            ) { currentStep ->
                when (currentStep) {
                    0 -> FeaturesIntroScreen()
                    1 -> ApiKeySetupScreen(settingsStore)
                }
            }

            // Bottom Navigation Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step == 0) {
                        TextButton(onClick = onFinish) {
                            Text("SKIP")
                        }
                    } else {
                        TextButton(onClick = { step = 0 }) {
                            Text("BACK")
                        }
                    }

                    Button(
                        onClick = {
                            if (step == 0) {
                                step = 1
                            } else {
                                onFinish()
                            }
                        }
                    ) {
                        Text(if (step == 0) "NEXT" else "FINISH")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (step == 0) Icons.Default.ArrowForward else Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeaturesIntroScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MenuBook,
            contentDescription = "App Logo",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Welcome to Papiro v2.1",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your smart academic companion and advanced markdown notebook powered by AI.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))

        FeatureRow(
            icon = Icons.Default.AutoAwesome,
            title = "AI Coprocessor",
            description = "Generate notes, quizzes, and titles instantly with Google Gemini."
        )
        Spacer(modifier = Modifier.height(24.dp))
        FeatureRow(
            icon = Icons.Default.DocumentScanner,
            title = "Document OCR",
            description = "Extract text from images automatically and precisely."
        )
        Spacer(modifier = Modifier.height(24.dp))
        FeatureRow(
            icon = Icons.Default.Code,
            title = "Advanced Markdown",
            description = "Support for Mermaid diagrams, Math formulas, and custom drawings."
        )
    }
}

@Composable
fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ApiKeySetupScreen(settingsStore: SettingsStore) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var apiKeyInput by remember { mutableStateOf(settingsStore.geminiApiKey) }
    var isVerifying by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.VpnKey,
            contentDescription = "API Key",
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Set Up Google AI",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "To use Papiro's AI features, please provide a Google Gemini API Key. This key is stored securely on your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("GET API KEY FROM AI STUDIO")
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = apiKeyInput,
            onValueChange = { apiKeyInput = it },
            label = { Text("Paste API Key Here") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (apiKeyInput.trim().isEmpty()) {
                    Toast.makeText(context, "Please enter an API Key", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isVerifying = true
                coroutineScope.launch {
                    val (success, message) = GeminiService.testApiKey(apiKeyInput.trim())
                    isVerifying = false
                    if (success) {
                        settingsStore.geminiApiKey = apiKeyInput.trim()
                        Toast.makeText(context, "API Key Verified & Saved!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Verification Failed: \$message", Toast.LENGTH_LONG).show()
                    }
                }
            },
            enabled = !isVerifying,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isVerifying) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("VERIFY & SAVE")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "You can also skip and set this up later in Settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
