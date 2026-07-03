import sys

with open("app/src/main/java/com/usoy/papiro/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

target = """                    item {
                        Text("1. Free Custom APIs (OpenRouter)","""

replacement = """                    item {
                        Text("1. Google AI Studio (Gemini API)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Get a free API key from Google AI Studio to use the latest Gemini models like Gemini 1.5 Pro and Flash directly.", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(onClick = { uriHandler.openUri("https://aistudio.google.com/app/apikey") }) {
                            Text("Get Gemini API Key")
                        }
                    }
                    item {
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("2. Free Custom APIs (OpenRouter)","""

new_content = content.replace(target, replacement)
new_content = new_content.replace('Text("2. Recommended Free Models"', 'Text("3. Recommended Free Models"')
new_content = new_content.replace('Text("3. Local Ollama', 'Text("4. Local Ollama')

if target not in content:
    print("TARGET NOT FOUND!")

with open("app/src/main/java/com/usoy/papiro/ui/screens/SettingsScreen.kt", "w") as f:
    f.write(new_content)
