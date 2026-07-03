import sys

with open("app/src/main/java/com/usoy/papiro/ui/screens/SettingsScreen.kt", "r") as f:
    lines = f.readlines()

new_lines = []
for i, line in enumerate(lines):
    new_lines.append(line)
    if 'text = "Model Management"' in line:
        # We need to go back and insert before the Row
        # Let's just find the Row start.
        pass

# simpler approach: just find 'Text("Model Management"' and insert before its parent Row.
content = "".join(lines)
target = """                                Row(
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
                                            text = "Model Management","""

replacement = """                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Key Icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Gemini API Settings",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Used for note generation and document processing.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                var isGeminiKeyVisible by remember { mutableStateOf(false) }
                                OutlinedTextField(
                                    value = geminiApiKey,
                                    onValueChange = { 
                                        geminiApiKey = it
                                        settingsStore.geminiApiKey = it
                                        if (it.isNotEmpty()) {
                                            settingsStore.provider = SettingsStore.PROVIDER_GEMINI
                                            provider = SettingsStore.PROVIDER_GEMINI
                                        }
                                    },
                                    label = { Text("Gemini API Key") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    placeholder = { Text("Enter your Gemini API key") },
                                    trailingIcon = {
                                        IconButton(onClick = { isGeminiKeyVisible = !isGeminiKeyVisible }) {
                                            Icon(
                                                imageVector = if (isGeminiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Toggle Visibility"
                                            )
                                        }
                                    },
                                    visualTransformation = if (isGeminiKeyVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation()
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
                                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                            isTestingGemini = true
                                            try {
                                                val req = com.usoy.papiro.data.GenerateContentRequest(
                                                    contents = listOf(com.usoy.papiro.data.Content(parts = listOf(com.usoy.papiro.data.Part(text = "Hello"))))
                                                )
                                                val res = com.usoy.papiro.data.RetrofitClient.service.generateContent(
                                                    model = "gemini-1.5-flash",
                                                    apiKey = keyToTest,
                                                    request = req
                                                )
                                                val text = res.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                                                geminiTestMessage = if (text.isNotEmpty()) "Verified successfully!" else "Failed to verify."
                                            } catch (e: Exception) {
                                                geminiTestMessage = "Error: ${e.message}"
                                            } finally {
                                                isTestingGemini = false
                                            }
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
                                        color = if (geminiTestMessage.contains("success")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))
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
                                            text = "Model Management","""

if target in content:
    content = content.replace(target, replacement)
else:
    print("TARGET NOT FOUND!")

with open("app/src/main/java/com/usoy/papiro/ui/screens/SettingsScreen.kt", "w") as f:
    f.write(content)

