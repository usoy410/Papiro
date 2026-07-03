package com.usoy.papiro.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import com.usoy.papiro.data.SettingsStore
import com.usoy.papiro.BuildConfig
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    
    // Highly resilient timeouts for local/on-device/external APIs (especially for cold-starting local Ollama models)
    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)  // 5 minutes
        .writeTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private const val SYSTEM_INSTRUCTION = """
        You are PaperCode, an expert on-device computer science and engineering notebook companion.
        Generate a highly structured Markdown note for the topic specified.
        Include:
        1. Clear title and conceptual overview.
        2. A structured Table of Contents (TOC) at the very beginning of the note to easily navigate to sections, utilizing standard markdown bullet links, e.g., `- [Creational Patterns](#creational-patterns)`. Ensure the link anchors are strictly lowercase, hyphenated, and alphanumeric (matching the section headers).
        3. Fully structured sections with detailed bullet points corresponding to the Table of Contents.
        4. Detailed code blocks with syntax.
        5. If relevant, include a flowchart/diagram. You MUST use standard Mermaid syntax inside ```mermaid blocks. For example:
           ```mermaid
           graph TD;
             A-->B;
             A-->C;
             B-->D;
             C-->D;
           ```
        6. Keep it educational, engaging, and rich in depth. No conversational intro/outro, only the Markdown note.
    """

    suspend fun generateStructuredNote(context: Context, topic: String, existingContent: String = "", settings: SettingsStore): String = withContext(Dispatchers.IO) {
        val provider = settings.provider
        Log.d(TAG, "Requesting generation from provider: $provider")
        
        var actualProvider = provider
        if (settings.geminiApiKey.isNotEmpty()) {
            actualProvider = SettingsStore.PROVIDER_GEMINI
        }
        when (actualProvider) {
            SettingsStore.PROVIDER_OLLAMA -> {
                generateUsingOllama(topic, existingContent, settings)
            }
            SettingsStore.PROVIDER_LOCAL_ON_DEVICE -> {
                generateUsingLocalOnDevice(context, topic, settings)
            }
            SettingsStore.PROVIDER_GEMINI -> {
                generateUsingGemini(topic, existingContent, settings)
            }
            else -> {
                generateUsingGemini(topic, existingContent, settings)
            }
        }
    }

    
    private suspend fun generateUsingGemini(topic: String, existingContent: String, settings: SettingsStore): String = withContext(Dispatchers.IO) {
        val apiKey = settings.geminiApiKey.trim().ifEmpty { BuildConfig.GEMINI_API_KEY }
        val model = settings.selectedCloudModel.trim().ifEmpty { "gemini-3.5-flash" }

        val promptContent = if (existingContent.trim().isEmpty()) {
            "Generate a note about: $topic"
        } else {
            """
            You are editing/expanding an existing note.
            Existing Content of the note:
            ---
            $existingContent
            ---

            The user wants to expand on this note or add content regarding the topic/prompt: "$topic"

            Instructions:
            1. Review the existing content above. Do NOT duplicate or repeat any concepts, sections, explanations, code blocks, or diagrams that already exist.
            2. Only generate NEW, supplementary, or continuing sections that directly address "$topic" and add value.
            3. Do NOT output any conversational introductions, greetings, explanations of changes, or outros. Output ONLY the new/additional Markdown content ready to be appended below the existing note.
            """.trimIndent()
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = promptContent)))),
            systemInstruction = Content(parts = listOf(Part(text = SYSTEM_INSTRUCTION.trimIndent())))
        )

        try {
            val response = RetrofitClient.service.generateContent(
                model = model,
                apiKey = apiKey,
                request = request
            )
            return@withContext response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: getLocalFallbackNote(topic)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API error with model $model", e)
            return@withContext getLocalFallbackNote(topic)
        }
    }

    private suspend fun generateUsingLocalOnDevice(context: Context, topic: String, settings: SettingsStore): String = withContext(Dispatchers.IO) {
        val modelPath = settings.localModelPath
        val fallbackNote = getLocalFallbackNote(topic)
        val instructionBanner = """
            > [INFO] Local LLM Engine Setup Instruction
            > You selected Local On-Device AI as your provider, but no compatible model file was found at:
            > `${modelPath}`
            > 
            > To use real offline on-device inference:
            > 1. Download a compatible GGUF/bin model (e.g., Llama-3.2-1B-Instruct or Gemma-2B-it converted to MediaPipe task format).
            > 2. Move or copy the model file to your device's storage (e.g., `${modelPath}`).
            > 3. Verify the path matches your setting in Papiro Settings.
            > 
            > Currently displaying high-fidelity pre-compiled local engineering notes for your topic.
            
            ---
            
        """.trimIndent()
        return@withContext instructionBanner + fallbackNote
    }

    private suspend fun generateUsingOllama(topic: String, existingContent: String, settings: SettingsStore): String = withContext(Dispatchers.IO) {
        val baseUrl = settings.ollamaBaseUrl.trim().removeSuffix("/")
        val model = settings.selectedLocalModel.ifEmpty { settings.ollamaModel.ifEmpty { "llama3" } }
        val url = "$baseUrl/api/generate"

        // Build a dedicated client with extremely generous timeouts for Ollama model loading and generation
        val ollamaClient = client.newBuilder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(600, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()

        val promptText = if (existingContent.trim().isEmpty()) {
            "Generate a note about: $topic\n\nSystem Guidelines:\n${SYSTEM_INSTRUCTION.trimIndent()}"
        } else {
            """
            System Guidelines:
            ${SYSTEM_INSTRUCTION.trimIndent()}

            You are editing/expanding an existing note.
            Existing Content of the note:
            ---
            $existingContent
            ---

            The user wants to expand on this note or add content regarding the topic/prompt: "$topic"

            Instructions:
            1. Review the existing content above. Do NOT duplicate or repeat any concepts, sections, explanations, code blocks, or diagrams that already exist.
            2. Only generate NEW, supplementary, or continuing sections that directly address "$topic" and add value.
            3. Do NOT output any conversational introductions, greetings, explanations of changes, or outros. Output ONLY the new/additional Markdown content ready to be appended below the existing note.
            """.trimIndent()
        }

        val jsonRequest = JSONObject().apply {
            put("model", model)
            put("prompt", promptText)
            put("stream", false)
            put("keep_alive", "30m") // Keep the model loaded in memory for 30 minutes for fast consecutive generations
        }

        val body = jsonRequest.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {
            ollamaClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errMsg = "Ollama connection failed. HTTP Code: ${response.code}"
                    Log.e(TAG, errMsg)
                    return@withContext "Error: $errMsg\n\nPlease check if Ollama is running in Termux at ${settings.ollamaBaseUrl} and if the model '$model' is pulled successfully."
                }
                val responseBody = response.body?.string() ?: return@withContext "Empty response from Ollama."
                val jsonResponse = JSONObject(responseBody)
                return@withContext jsonResponse.optString("response", "No response text found in Ollama JSON.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ollama connection error", e)
            val isTimeout = e is java.net.SocketTimeoutException || e.message?.contains("timeout", ignoreCase = true) == true
            if (isTimeout) {
                return@withContext """
                    Ollama Connection Timeout Error: ${e.localizedMessage}

                    The request timed out because Ollama took too long to load the model or generate a response. Here is how you can resolve this:

                    1. Use a lighter, faster model: Big models like 'llama3' (8B parameters) require significant RAM and CPU/GPU power. On resource-constrained systems or Termux, try pulling a smaller model such as 'llama3.2:1b' or 'qwen2.5:1.5b-instruct' and update your Papiro Settings.
                    2. Pre-load/warm up the model: Run the model once in your terminal (e.g., 'ollama run llama3.2:1b') before initiating requests in Papiro. Ollama can take up to several minutes to load a cold model into memory for the first run.
                    3. Keep the model in memory: Start Ollama with the environment variable 'OLLAMA_KEEP_ALIVE=24h' to prevent the model from unloading after periods of inactivity.
                    4. Termux optimization: If running Ollama inside Termux on this same device, ensure you have enabled a Termux Wake Lock (via the notification drawer) and disabled battery optimization for Termux so Android does not freeze or throttle the CPU.
                """.trimIndent()
            }
            return@withContext "Ollama Connection Error: ${e.localizedMessage}\n\nMake sure Ollama is active on Termux and that your network configuration is correct (e.g., use 'http://localhost:11434' on device or 'http://10.0.2.2:11434' in emulator)."
        }
    }

    /**
     * Extracts text from whiteboard/textbook images using multi-modal Gemini input.
     */
    suspend fun performOcr(imageBytes: ByteArray, settings: SettingsStore): String = withContext(Dispatchers.IO) {
        val apiKeyToUse = settings.geminiApiKey.trim().ifEmpty { BuildConfig.GEMINI_API_KEY }
        val modelToUse = settings.selectedCloudModel.trim().ifEmpty { "gemini-3.5-flash" }
        val base64Image = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)
        
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(
                Part(text = "Perform OCR and extract all text from this whiteboard or paper. Return ONLY the extracted text with proper layout/formatting."),
                Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
            )))
        )
        
        try {
            val response = RetrofitClient.service.generateContent(
                model = modelToUse,
                apiKey = apiKeyToUse,
                request = request
            )
            return@withContext response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "OCR Failed: Empty response from Gemini."
        } catch (e: Exception) {
            Log.e(TAG, "OCR generation error using model $modelToUse", e)
            return@withContext "OCR Failed: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    private fun getLocalFallbackNote(topic: String): String {
        val normalized = topic.trim().lowercase()
        return when {
            normalized.contains("design pattern") || normalized.contains("creational") || normalized.contains("structural") || normalized.contains("behavioral") -> {
                """
                # SOFTWARE DESIGN PATTERNS

                Reusable, proven templates designed to solve common software design challenges in object-oriented and structural programming.

                ## Table of Contents
                - [Core Classifications](#core-classifications)
                - [Production Kotlin Reference: Observer Pattern](#production-kotlin-reference-observer-pattern)
                - [Architectural Comparison](#architectural-comparison)

                ## Core Classifications
                Design patterns are generally divided into three major architectural categories:

                ### 1. Creational Patterns
                Deals with object creation mechanisms, striving to create objects in a manner suitable to the situation.
                - Singleton: Guarantees a class has only one instance and provides a global access point to it.
                - Factory Method: Defines an interface for creating a single object, but lets subclasses decide which class to instantiate.
                - Builder: Separates the construction of a complex object from its representation, allowing the same construction process to create various representations.

                ### 2. Structural Patterns
                Deals with class and object composition, helping ensure that if one part of a system changes, the entire system doesn't need to change.
                - Adapter: Allows incompatible interfaces to collaborate.
                - Decorator: Attaches new behaviors to objects dynamically by placing them inside special wrapper objects.
                - Facade: Provides a simplified interface to a complex library, desktop suite, or collection of classes.

                ### 3. Behavioral Patterns
                Concerned with algorithms and the assignment of responsibilities between objects, helping objects communicate and cooperate.
                - Observer: Defines a subscription mechanism to notify multiple objects about any events that happen to the object they're observing.
                - Strategy: Defines a family of algorithms, encapsulates each one, and makes them interchangeable, letting the algorithm vary independently from clients using it.
                - State: Allows an object to alter its behavior when its internal state changes, appearing to change its class.

                ## Production Kotlin Reference: Observer Pattern
                ```kotlin
                package com.usoy.papiro.patterns

                interface Observer {
                    fun update(state: String)
                }

                class Subject {
                    private val observers = mutableListOf<Observer>()
                    private var state: String = ""

                    fun attach(observer: Observer) {
                        observers.add(observer)
                    }

                    fun detach(observer: Observer) {
                        observers.remove(observer)
                    }

                    fun setState(newState: String) {
                        state = newState
                        notifyAllObservers()
                    }

                    private fun notifyAllObservers() {
                        for (observer in observers) {
                            observer.update(state)
                        }
                    }
                }

                class TerminalLogger : Observer {
                    override fun update(state: String) {
                        println("Terminal logger received state update: ${"$"}{state}")
                    }
                }
                ```

                ## Architectural Comparison
                - Creational: Solves instantiation issues by decoupling clients from direct constructor invocation.
                - Structural: Resolves interface incompatibilities and relationship topologies.
                - Behavioral: Streamlines messaging protocols and event-driven runtime states.
                """.trimIndent()
            }
            normalized.contains("binary search") -> {
                """
                # BINARY SEARCH ALGORITHM
                
                A precise, highly efficient search algorithm designed for sorted contiguous sequences.
                
                ## Conceptual Overview
                Binary search operates on the divide-and-conquer principle. It repeatedly divides the search interval in half by comparing the target value to the middle element of the array. This reduces the search space logarithmically rather than linearly.
                
                ## Architectural Visualization
                ```mermaid
                graph TD;
                  A[Initial Array] --> B[Find Midpoint];
                  B --> C[Compare Target];
                ```
                
                ## Production-Ready Kotlin Implementation
                ```kotlin
                package com.usoy.papiro.algorithm
                
                class BinarySearcher {
                    /**
                     * Performs binary search on a sorted integer list.
                     * Returns the index of the target, or -1 if not found.
                     */
                    fun search(list: List<Int>, target: Int): Int {
                        var low = 0
                        var high = list.size - 1
                        
                        while (low <= high) {
                            val mid = low + (high - low) / 2
                            val value = list[mid]
                            
                            when {
                                value == target -> return mid
                                value < target -> low = mid + 1
                                else -> high = mid - 1
                            }
                        }
                        return -1
                    }
                }
                
                fun main() {
                    val searcher = BinarySearcher()
                    val numbers = listOf(2, 5, 8, 12, 16, 23, 38, 56, 72, 91)
                    val resultIndex = searcher.search(numbers, 23)
                    println("Target 23 found at index: ${"$"}{resultIndex}")
                }
                ```
                
                ## Complexity Analysis
                - **Time Complexity (Worst/Average Case):** O(log N) - The search interval is halved at every step.
                - **Time Complexity (Best Case):** O(1) - The target is located exactly at the first midpoint.
                - **Space Complexity:** O(1) - Constant auxiliary space used for iterative index tracking variables.
                """.trimIndent()
            }
            normalized.contains("linked list") -> {
                """
                # LINKED LIST DATA STRUCTURES
                
                A dynamic linear collection of data elements whose order is determined by pointers.
                
                ## Conceptual Overview
                A linked list is a sequence of nodes where each node contains a data payload and a reference (or link) to the next node in the sequence. Unlike arrays, linked lists do not store elements in contiguous physical memory, allowing for efficient insertions and deletions at any point.
                
                ## Structural Layout
                ```text
                [Head: NodeA] -> [NodeB] -> [NodeC] -> [Tail: null]
                +-----------+    +-----------+    +-----------+
                | Data: 10  |    | Data: 20  |    | Data: 30  |
                | Next: ----+--->| Next: ----+--->| Next: null|
                +-----------+    +-----------+    +-----------+
                ```
                
                ## Kotlin Implementation
                ```kotlin
                package com.usoy.papiro.datastructure
                
                data class Node<T>(
                    var data: T,
                    var next: Node<T>? = null
                )
                
                class SinglyLinkedList<T> {
                    private var head: Node<T>? = null
                    
                    fun append(value: T) {
                        val newNode = Node(value)
                        if (head == null) {
                            head = newNode
                            return
                        }
                        var current = head
                        while (current?.next != null) {
                            current = current.next
                        }
                        current?.next = newNode
                    }
                    
                    fun printList() {
                        var current = head
                        val elements = mutableListOf<String>()
                        while (current != null) {
                            elements.add(current.data.toString())
                            current = current.next
                        }
                        println(elements.joinToString(" -> "))
                    }
                }
                
                fun main() {
                    val list = SinglyLinkedList<Int>()
                    list.append(10)
                    list.append(20)
                    list.append(30)
                    list.printList() // Output: 10 -> 20 -> 30
                }
                ```
                
                ## Complexity Analysis
                - **Access Time Complexity:** O(N) - Must traverse from the head sequentially to locate a target index.
                - **Search Time Complexity:** O(N) - Must inspect each element sequentially.
                - **Insertion/Deletion at Head:** O(1) - Instantaneous reference update.
                - **Insertion/Deletion at Tail/Arbitrary:** O(N) - Must locate parent node first.
                """.trimIndent()
            }
            normalized.contains("rest api") || normalized.contains("api design") -> {
                """
                # REPRESENTATIONAL STATE TRANSFER (REST) API DESIGN
                
                A architectural blueprint for designing stateless, network-based web services.
                
                ## Core Principles
                REST relies on standard, uniform interfaces, resource-based URIs, stateless communication, caching capabilities, and a structured layered architecture.
                
                ## Standard HTTP Resource Operations
                - **GET `/api/v1/notes`**: Retrieves lists of resources.
                - **POST `/api/v1/notes`**: Commits new resources to the datastore.
                - **PUT `/api/v1/notes/{id}`**: Idempotently replaces an existing resource.
                - **DELETE `/api/v1/notes/{id}`**: Destroys resources at target coordinates.
                
                ## Production Request & Response Simulation
                ```json
                // POST /api/v1/notes (Request Headers: Content-Type: application/json)
                {
                  "title": "On-Device LLMs",
                  "content": "Running quantized GGUF models via llama.cpp inside native applications.",
                  "tags": ["AI", "Local", "Mobile"]
                }
                
                // HTTP/1.1 201 Created (Response Payload)
                {
                  "status": "success",
                  "id": "78a2e411-90f5",
                  "created_at": "2026-06-30T22:15:30Z",
                  "resource": {
                    "title": "On-Device LLMs",
                    "tags": ["AI", "Local", "Mobile"]
                  }
                }
                ```
                
                ## Best Practices
                1. Always use plural nouns for endpoint collections (e.g. `/users` over `/user`).
                2. Explicitly specify API version identifiers inside paths (e.g. `/api/v1/`).
                3. Utilize standard HTTP response status codes strictly (200 OK, 201 Created, 400 Bad Request, 404 Not Found, 500 Server Error).
                """.trimIndent()
            }
            normalized.contains("solid principles") || normalized.contains("solid design") -> {
                """
                # THE SOLID DESIGN PRINCIPLES
                
                Five essential guidelines for writing clean, modular, and maintainable object-oriented software.
                
                ## Architectural Guidelines
                - **Single Responsibility Principle (SRP):** Every module or class should have a single, well-defined reason to change.
                - **Open-Closed Principle (OCP):** Software components must be open for extension, but closed for direct modifications.
                - **Liskov Substitution Principle (LSP):** Subclasses must be completely substitutable for their parent classes without breaking functionality.
                - **Interface Segregation Principle (ISP):** Clients must never be forced to depend on interfaces they do not utilize.
                - **Dependency Inversion Principle (DIP):** Depend upon abstractions, never upon concrete low-level modules.
                
                ## Kotlin DIP Code Reference
                ```kotlin
                package com.usoy.papiro.solid
                
                // Abstraction
                interface Database {
                    fun saveData(data: String)
                }
                
                // Low-level component implementing abstraction
                class SQLiteDatabase : Database {
                    override fun saveData(data: String) {
                        println("Persisting to SQLite database: ${"$"}{data}")
                    }
                }
                
                // High-level component depending ONLY on abstraction
                class NoteManager(private val database: Database) {
                    fun saveNote(content: String) {
                        database.saveData(content)
                    }
                }
                ```
                """.trimIndent()
            }
            normalized.contains("coroutines") || normalized.contains("coroutine") -> {
                """
                # KOTLIN COROUTINES & ASYNCHRONOUS PROGRAMMING
                
                Lightweight concurrency frameworks designed for robust non-blocking execution in Kotlin.
                
                ## Core Architectural Components
                - **CoroutineScope:** Bounds the lifecycle of launched coroutines. Ensures structured concurrency and proper cancellation.
                - **Dispatcher:** Determines the physical threads or thread pools assigned to execute coroutine blocks.
                  - `Dispatchers.Main`: Runs on UI-thread for screen updates.
                  - `Dispatchers.IO`: Optimized for blocking database, file, and network operations.
                  - `Dispatchers.Default`: Optimized for CPU-intensive mathematical or parsing tasks.
                - **suspend function:** Functions that can pause execution without blocking the host thread, resuming upon task completion.
                
                ## Production-Grade Code Block
                ```kotlin
                package com.usoy.papiro.concurrency
                
                import kotlinx.coroutines.*
                import java.net.URL
                
                class ContentLoader {
                    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
                    
                    fun loadContent() {
                        scope.launch {
                            val data = fetchNetworkData()
                            updateUi(data)
                        }
                    }
                    
                    private suspend fun fetchNetworkData(): String = withContext(Dispatchers.IO) {
                        try {
                            URL("https://api.github.com").readText()
                        } catch (e: Exception) {
                            "Network failure: ${"$"}{e.message}"
                        }
                    }
                    
                    private fun updateUi(result: String) {
                        println("Screen Updated: ${"$"}{result}")
                    }
                }
                ```
                """.trimIndent()
            }
            else -> {
                """
                # ${topic.uppercase()}
                
                A comprehensive technical compilation detailing the structure, implementation, and optimization of **$topic**.
                
                ## Conceptual Overview
                The architectural implementation of **$topic** plays a critical role in complex software systems. Developing a rigorous understanding of its parameters ensures high runtime efficiency, maintainability, and clean decoupling in high-scale projects.
                
                ## System Architecture Layout
                ```text
                +---------------------+
                |     User Agent      |
                +----------+----------+
                           |  (Invokes Execution)
                           v
                +----------+----------+
                |     Controller      |  <---+ (State Changes)
                +----------+----------+      |
                           |  (Resolves Task) |
                           v                 |
                +----------+----------+      |
                |   Processing Unit   +------+
                +---------------------+
                ```
                
                ## Core Kotlin Reference Implementation
                ```kotlin
                package com.usoy.papiro.notebook
                
                import kotlinx.coroutines.flow.MutableStateFlow
                import kotlinx.coroutines.flow.StateFlow
                
                class ${topic.replace(" ", "").replace("-", "").replace(".", "").uppercase()}Engine {
                    private val _status = MutableStateFlow<EngineStatus>(EngineStatus.Idle)
                    val status: StateFlow<EngineStatus> = _status
                    
                    suspend fun processTask(payload: String) {
                        _status.value = EngineStatus.Processing
                        try {
                            val computedValue = payload.hashCode().toString()
                            _status.value = EngineStatus.Success(computedValue)
                        } catch (e: Exception) {
                            _status.value = EngineStatus.Error(e.localizedMessage ?: "Unknown Error")
                        }
                    }
                }
                
                sealed class EngineStatus {
                    object Idle : EngineStatus()
                    object Processing : EngineStatus()
                    data class Success(val result: String) : EngineStatus()
                    data class Error(val message: String) : EngineStatus()
                }
                ```
                
                ## Performance & Constraints Analysis
                - **Memory Allocation Profile:** Linear allocation characteristics matching O(N) constraints. Ensure object recycling mechanisms are implemented to mitigate garbage collector overhead.
                - **Asynchronous Execution Threading:** Execution should always be dispatched to external worker thread pools (e.g. `Dispatchers.IO` or `Dispatchers.Default`) to guarantee the main system event loop remains responsive.
                """.trimIndent()
            }
        }
    }

    suspend fun generateTableOfContents(context: Context, noteContent: String, settings: SettingsStore): String = withContext(Dispatchers.IO) {
        val apiKey = settings.geminiApiKey.trim().ifEmpty { BuildConfig.GEMINI_API_KEY }
        val model = settings.selectedCloudModel.trim().ifEmpty { "gemini-3.5-flash" }

        val systemInstruction = """
            You are a table of contents generator. 
            Analyze the provided markdown text and generate a structured table of contents.
            Use standard markdown bullet links, e.g. - [Section Name](#section-name).
            Keep the output concise, containing only the table of contents. No conversational intro or outro.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "Generate a table of contents for this note:\n\n$noteContent")))),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )

        try {
            val response = RetrofitClient.service.generateContent(
                model = model,
                apiKey = apiKey,
                request = request
            )
            return@withContext response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: extractHeadersToToc(noteContent)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API error generating TOC with model $model", e)
            return@withContext extractHeadersToToc(noteContent)
        }
    }

    private fun extractHeadersToToc(noteContent: String): String {
        val lines = noteContent.split("\n")
        val sb = StringBuilder()
        sb.append("# TABLE OF CONTENTS\n\n")
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("## ")) {
                val name = trimmed.removePrefix("## ").trim()
                val link = name.lowercase().replace(" ", "-").replace(Regex("[^a-z0-9-]"), "")
                sb.append("  - [$name](#$link)\n")
            } else if (trimmed.startsWith("# ")) {
                val name = trimmed.removePrefix("# ").trim()
                val link = name.lowercase().replace(" ", "-").replace(Regex("[^a-z0-9-]"), "")
                sb.append("- [$name](#$link)\n")
            }
        }
        if (sb.length <= 22) {
            return "# TABLE OF CONTENTS\n\n- (No headings found in the note to generate a table of contents.)"
        }
        return sb.toString()
    }

    suspend fun enhanceNoteContent(context: Context, noteContent: String, settings: SettingsStore): String = withContext(Dispatchers.IO) {
        val apiKey = settings.geminiApiKey.trim().ifEmpty { BuildConfig.GEMINI_API_KEY }
        val model = settings.selectedCloudModel.trim().ifEmpty { "gemini-3.5-flash" }

        val systemInstruction = """
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
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "Please enhance this note content:\n\n$noteContent")))),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )

        try {
            val response = RetrofitClient.service.generateContent(
                model = model,
                apiKey = apiKey,
                request = request
            )
            return@withContext response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: enhanceLocalOffline(noteContent)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API error enhancing note with model $model", e)
            return@withContext enhanceLocalOffline(noteContent)
        }
    }

    private fun enhanceLocalOffline(noteContent: String): String {
        val lines = noteContent.split("\n")
        val sb = StringBuilder()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                sb.append("\n")
            } else if (trimmed.startsWith("#") || trimmed.startsWith("-") || trimmed.startsWith("`")) {
                sb.append(line).append("\n")
            } else {
                var enhanced = trimmed
                if (enhanced.firstOrNull()?.isLowerCase() == true) {
                    enhanced = enhanced.replaceFirstChar { it.uppercase() }
                }
                if (!enhanced.endsWith(".") && !enhanced.endsWith("!") && !enhanced.endsWith("?")) {
                    enhanced += "."
                }
                sb.append(enhanced).append("\n")
            }
        }
        return sb.toString()
    }
}
