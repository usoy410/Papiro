import sys

with open("app/src/main/java/com/usoy/papiro/data/GeminiService.kt", "r") as f:
    content = f.read()

# update generateTableOfContents
old_toc = """    suspend fun generateTableOfContents(context: Context, noteContent: String, settings: SettingsStore): String = withContext(Dispatchers.IO) {
        val apiKey = settings.customApiKey.trim()
        if (apiKey.isEmpty()) {
            return@withContext extractHeadersToToc(noteContent)
        }"""

new_toc = """    suspend fun generateTableOfContents(context: Context, noteContent: String, settings: SettingsStore): String = withContext(Dispatchers.IO) {
        if (settings.geminiApiKey.isNotEmpty() || settings.provider == SettingsStore.PROVIDER_GEMINI) {
            val apiKeyToUse = settings.geminiApiKey.trim().ifEmpty { BuildConfig.GEMINI_API_KEY }
            val systemInstruction = \"\"\"
                You are a table of contents generator. 
                Analyze the provided markdown text and generate a structured table of contents.
                Use standard markdown bullet links, e.g. - [Section Name](#section-name).
                Keep the output concise, containing only the table of contents. No conversational intro or outro.
            \"\"\".trimIndent()
            
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = "Generate a table of contents for this note:\n\n$noteContent")))),
                systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
            )
            
            try {
                val response = RetrofitClient.service.generateContent(
                    model = "gemini-1.5-flash",
                    apiKey = apiKeyToUse,
                    request = request
                )
                return@withContext response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: extractHeadersToToc(noteContent)
            } catch (e: Exception) {
                return@withContext extractHeadersToToc(noteContent)
            }
        }

        val apiKey = settings.customApiKey.trim()
        if (apiKey.isEmpty()) {
            return@withContext extractHeadersToToc(noteContent)
        }"""
content = content.replace(old_toc, new_toc)

# update enhanceNoteContent
old_enhance = """    suspend fun enhanceNoteContent(context: Context, noteContent: String, settings: SettingsStore): String = withContext(Dispatchers.IO) {
        val apiKey = settings.customApiKey.trim()
        if (apiKey.isEmpty()) {
            return@withContext enhanceLocalOffline(noteContent)
        }"""

new_enhance = """    suspend fun enhanceNoteContent(context: Context, noteContent: String, settings: SettingsStore): String = withContext(Dispatchers.IO) {
        if (settings.geminiApiKey.isNotEmpty() || settings.provider == SettingsStore.PROVIDER_GEMINI) {
            val apiKeyToUse = settings.geminiApiKey.trim().ifEmpty { BuildConfig.GEMINI_API_KEY }
            val systemInstruction = \"\"\"
                You are an expert AI note assistant and enhancer.
                Your task is to analyze and enhance the note content, understanding its purpose and structural blocks.
                Depending on the type of content present in the note, apply the following guidance:
                1. Raw text or prose:
                   - Fix any typos, spelling errors, and grammar mistakes.
                   - Improve clarity, terminology, and flow.
                   - Simplify overly complex phrasing to make it clear, concise, and direct.
                   - If the content is lacking detail or context, reasonably expand on key concepts to make the note more comprehensive.
                   - Format with professional markdown hierarchy (headings, subheadings, bullet points, bold key terms).
                2. Diagrams (e.g., Mermaid diagrams or textual flowcharts):
                   - Inspect the syntax of any diagram (such as Mermaid syntax). If it is incorrect, incomplete, or unfinished, fix it completely.
                   - Ensure Mermaid blocks are correctly enclosed in ```mermaid and ``` and have valid syntax.
                   - Enhance flow, layout, or details in the diagram if the representation is lacking.
                3. Code blocks:
                   - Detect any programming language, syntax errors, or unfinished snippets. Fix syntax errors, write missing parts of the code, and format/beautify it.
                   - Use correct syntax highlighting block labels (e.g., ```kotlin, ```java, ```python).
                   - Add short, clear code comments to explain the code's purpose if they are lacking.
                Maintain all original technical details, correctness, and intent.
                Return ONLY the final enhanced markdown content. Do NOT include any conversational introduction, conversational explanation, or conversational outro before or after the markdown.
            \"\"\".trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = "Please enhance this note content:\n\n$noteContent")))),
                systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
            )
            
            try {
                val response = RetrofitClient.service.generateContent(
                    model = "gemini-1.5-flash",
                    apiKey = apiKeyToUse,
                    request = request
                )
                return@withContext response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: enhanceLocalOffline(noteContent)
            } catch (e: Exception) {
                return@withContext enhanceLocalOffline(noteContent)
            }
        }

        val apiKey = settings.customApiKey.trim()
        if (apiKey.isEmpty()) {
            return@withContext enhanceLocalOffline(noteContent)
        }"""
content = content.replace(old_enhance, new_enhance)

with open("app/src/main/java/com/usoy/papiro/data/GeminiService.kt", "w") as f:
    f.write(content)
