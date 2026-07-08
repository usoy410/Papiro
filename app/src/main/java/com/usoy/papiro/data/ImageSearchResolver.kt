package com.usoy.papiro.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap

object ImageSearchResolver {
    private const val TAG = "ImageSearchResolver"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val inProgressResolutions = ConcurrentHashMap<String, Deferred<String>>()
    private val scope = CoroutineScope(Dispatchers.IO)

    private var lastWarmupTime = 0L
    private const val WARMUP_COOLDOWN_MS = 5 * 60 * 1000L // 5 minutes

    fun warmup() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastWarmupTime < WARMUP_COOLDOWN_MS) {
            Log.d(TAG, "Warmup skipped (cooldown active)")
            return
        }
        lastWarmupTime = currentTime

        scope.launch {
            try {
                Log.d(TAG, "Starting Render service warmup...")
                val url = "https://papiro-image-retrieval.onrender.com/"
                val request = Request.Builder()
                    .url(url)
                    .build()
                client.newCall(request).execute().use { response ->
                    Log.d(TAG, "Render service warmup response code: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Render service warmup failed", e)
            }
        }
    }

    suspend fun resolveImage(context: android.content.Context, path: String, altText: String): String = withContext(Dispatchers.IO) {
        val isUrlOrUri = path.startsWith("http://") || path.startsWith("https://") || 
                         path.startsWith("file://") || path.startsWith("content://") || 
                         path.startsWith("/") || path.startsWith("data:image")
                         
        val isPlaceholder = path.contains("loremflickr.com") || path.contains("placeimg.com") || path.contains("unsplash.it")
        
        // We resolve if it is a known placeholder, OR if it's NOT a valid direct URL/URI (meaning it's likely just a keyword)
        if (isUrlOrUri && !isPlaceholder) {
            // Already a direct image URL or local path
            return@withContext path
        }

        // Check persistent cache
        val prefs = context.getSharedPreferences("resolved_images_cache", android.content.Context.MODE_PRIVATE)
        val cachedUrl = prefs.getString(path, null)
        if (!cachedUrl.isNullOrEmpty()) {
            Log.d(TAG, "Cache hit for path: $path -> $cachedUrl")
            return@withContext cachedUrl
        }

        // Deduplicate concurrent requests
        val existingDeferred = inProgressResolutions[path]
        if (existingDeferred != null) {
            Log.d(TAG, "Waiting for existing resolution of path: $path")
            return@withContext existingDeferred.await()
        }

        val deferred = scope.async {
            performResolution(path, altText, prefs)
        }
        inProgressResolutions[path] = deferred
        
        try {
            return@withContext deferred.await()
        } finally {
            inProgressResolutions.remove(path)
        }
    }

    private fun performResolution(path: String, altText: String, prefs: android.content.SharedPreferences): String {
        // Extract keyword
        var query = extractKeyword(path)
        if (query.isEmpty() || query == "TAG" || query.length < 2) {
            query = altText
        }
        
        if (query.isEmpty()) {
            return path // Return original placeholder if no query can be determined
        }

        val queryType = determineQueryType(query)
        Log.d(TAG, "Resolving image for query: $query (Type: $queryType)")

        // 1. Try the dedicated Papiro Image Retrieval SaaS
        try {
            val resolvedUrl = searchPapiroService(query, queryType)
            if (resolvedUrl != null) {
                Log.d(TAG, "Successfully resolved via Papiro SaaS ($queryType): $resolvedUrl")
                prefs.edit().putString(path, resolvedUrl).apply()
                return resolvedUrl
            }
        } catch (e: Exception) {
            Log.e(TAG, "Papiro SaaS search failed", e)
        }

        // 2. Fallback to original path
        Log.d(TAG, "All search retrievals failed or not configured, falling back to original: $path")
        return path
    }

    private fun extractKeyword(path: String): String {
        try {
            val uri = android.net.Uri.parse(path)
            val segments = uri.pathSegments
            if (segments.isNotEmpty()) {
                val lastSegment = segments.last()
                // Decodes any URL encoding (e.g., %20)
                return java.net.URLDecoder.decode(lastSegment, "UTF-8")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing path segment from $path", e)
        }
        return ""
    }

    private fun determineQueryType(query: String): String {
        val q = query.lowercase()
        // If it already has scientific/diagram context, mark as diagram
        if (q.contains("diagram") || q.contains("map") || q.contains("anatomy") || 
            q.contains("chart") || q.contains("structure") || q.contains("labeled") || 
            q.contains("system") || q.contains("schematic")) {
            return "diagram"
        }
        
        // These subjects represent highly visual/educational concepts that benefit from a labeled diagram
        val scientificKeywords = listOf(
            "skeleton", "bone", "skull", "brain", "heart", "kidney", "lung", "liver", "cell", "neuron",
            "atom", "molecule", "earth", "planet", "solar", "moon", "volcano", "leaf", "plant", "flower",
            "muscle", "stomach", "intestine", "eye", "ear", "skin", "digestive", "nervous", "circulatory",
            "respiratory", "endocrine", "immune", "lymphatic", "skeletal", "geography", "anatomy",
            "chromosome", "dna", "mitochondria", "photosynthesis"
        )
        
        for (keyword in scientificKeywords) {
            if (q.contains(keyword)) {
                return "diagram"
            }
        }
        
        return "standard"
    }

    private fun searchPapiroService(query: String, type: String): String? {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val encodedType = URLEncoder.encode(type, "UTF-8")
        val url = "https://papiro-image-retrieval.onrender.com/api/images?query=$encodedQuery&type=$encodedType"
        
        val request = Request.Builder()
            .url(url)
            .build()
            
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(TAG, "Papiro Image Service error: Code ${response.code}")
                return null
            }
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            return json.optString("image_url").takeIf { it.isNotEmpty() }
        }
        return null
    }

}
