package com.usoy.papiro.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object ImageSearchResolver {
    private const val TAG = "ImageSearchResolver"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun resolveImage(context: android.content.Context, path: String, altText: String): String = withContext(Dispatchers.IO) {
        val isPlaceholder = path.contains("loremflickr.com") || path.contains("placeimg.com") || path.contains("unsplash.it")
        
        if (!isPlaceholder) {
            // Already a direct image, return as is
            return@withContext path
        }

        // Check persistent cache
        val prefs = context.getSharedPreferences("resolved_images_cache", android.content.Context.MODE_PRIVATE)
        val cachedUrl = prefs.getString(path, null)
        if (!cachedUrl.isNullOrEmpty()) {
            Log.d(TAG, "Cache hit for path: $path -> $cachedUrl")
            return@withContext cachedUrl
        }

        // Extract keyword
        var query = extractKeyword(path)
        if (query.isEmpty() || query == "TAG" || query.length < 2) {
            query = altText
        }
        
        if (query.isEmpty()) {
            return@withContext path // Return original placeholder if no query can be determined
        }

        val optimizedQuery = getOptimizedQuery(query)
        Log.d(TAG, "Resolving image for query: $query (Optimized: $optimizedQuery)")

        // 1. Try the dedicated Papiro Image Retrieval SaaS
        try {
            val resolvedUrl = searchPapiroService(query)
            if (resolvedUrl != null) {
                Log.d(TAG, "Successfully resolved via Papiro SaaS: $resolvedUrl")
                prefs.edit().putString(path, resolvedUrl).apply()
                return@withContext resolvedUrl
            }
        } catch (e: Exception) {
            Log.e(TAG, "Papiro SaaS search failed", e)
        }

        // 2. Try Wikimedia Commons Search as standard robust free search
        // Try with optimized query + bitmap constraint first
        try {
            val resolvedUrl = searchWikimediaImages(optimizedQuery)
            if (resolvedUrl != null) {
                Log.d(TAG, "Successfully resolved via Wikimedia Commons (Optimized Bitmap): $resolvedUrl")
                prefs.edit().putString(path, resolvedUrl).apply()
                return@withContext resolvedUrl
            }
        } catch (e: Exception) {
            Log.e(TAG, "Wikimedia Commons optimized search failed", e)
        }

        // Try raw query + bitmap constraint
        try {
            val resolvedUrl = searchWikimediaImages(query)
            if (resolvedUrl != null) {
                Log.d(TAG, "Successfully resolved via Wikimedia Commons (Raw Bitmap): $resolvedUrl")
                prefs.edit().putString(path, resolvedUrl).apply()
                return@withContext resolvedUrl
            }
        } catch (e: Exception) {
            Log.e(TAG, "Wikimedia Commons raw search failed", e)
        }

        // Try optimized query with general search (allows SVG diagrams, structures, blueprints)
        try {
            val resolvedUrl = searchWikimediaImagesGeneral(optimizedQuery)
            if (resolvedUrl != null) {
                Log.d(TAG, "Successfully resolved via Wikimedia Commons General (Optimized): $resolvedUrl")
                prefs.edit().putString(path, resolvedUrl).apply()
                return@withContext resolvedUrl
            }
        } catch (e: Exception) {
            Log.e(TAG, "Wikimedia Commons general optimized search failed", e)
        }

        // Try raw query with general search
        try {
            val resolvedUrl = searchWikimediaImagesGeneral(query)
            if (resolvedUrl != null) {
                Log.d(TAG, "Successfully resolved via Wikimedia Commons General (Raw): $resolvedUrl")
                prefs.edit().putString(path, resolvedUrl).apply()
                return@withContext resolvedUrl
            }
        } catch (e: Exception) {
            Log.e(TAG, "Wikimedia Commons general raw search failed", e)
        }

        // 4. Fallback to original path
        Log.d(TAG, "All search retrievals failed or not configured, falling back to original: $path")
        return@withContext path
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

    private fun getOptimizedQuery(query: String): String {
        val q = query.lowercase()
        // If it already has scientific/diagram context, leave it
        if (q.contains("diagram") || q.contains("map") || q.contains("illustration") || 
            q.contains("anatomy") || q.contains("chart") || q.contains("structure") ||
            q.contains("labeled") || q.contains("system")) {
            return query
        }
        
        // These subjects represent highly visual/educational concepts that benefit from a labeled diagram
        val scientificKeywords = listOf(
            "skeleton", "bone", "skull", "brain", "heart", "kidney", "lung", "liver", "cell", "neuron",
            "atom", "molecule", "earth", "planet", "solar", "moon", "volcano", "leaf", "plant", "flower",
            "muscle", "stomach", "intestine", "eye", "ear", "skin", "digestive", "nervous", "circulatory",
            "respiratory", "endocrine", "immune", "lymphatic", "skeletal", "geography", "map", "anatomy",
            "chromosome", "dna", "mitochondria", "photosynthesis"
        )
        
        for (keyword in scientificKeywords) {
            if (q.contains(keyword)) {
                return "$query diagram"
            }
        }
        
        return query
    }

    private fun searchPapiroService(query: String): String? {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://papiro-image-retrieval.onrender.com/api/images?query=$encodedQuery"
        
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

    private fun searchWikimediaImages(query: String): String? {
        // We look for bitmap files (JPG, PNG, WEBP) in File namespace (ns 6)
        val searchString = "$query filetype:bitmap"
        val encodedQuery = URLEncoder.encode(searchString, "UTF-8")
        val url = "https://commons.wikimedia.org/w/api.php?action=query&format=json&prop=imageinfo&generator=search&gsrnamespace=6&gsrsearch=$encodedQuery&iiprop=url&iiurlwidth=800&gsrlimit=5"
        
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "PapiroNotebook/1.0 (alfortearjay0@gmail.com; Android App)")
            .build()
            
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(TAG, "Wikimedia API error: Code ${response.code}")
                return null
            }
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val queryObj = json.optJSONObject("query") ?: return null
            val pagesObj = queryObj.optJSONObject("pages") ?: return null
            
            val keys = pagesObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val page = pagesObj.getJSONObject(key)
                val imageInfoArray = page.optJSONArray("imageinfo")
                if (imageInfoArray != null && imageInfoArray.length() > 0) {
                    val info = imageInfoArray.getJSONObject(0)
                    
                    // Prefer the optimized 800px thumbnail PNG/JPG URL
                    val thumbUrl = info.optString("thumburl")
                    if (thumbUrl.isNotEmpty()) {
                        return thumbUrl
                    }
                    
                    // Fallback to original URL
                    val originalUrl = info.optString("url")
                    if (originalUrl.isNotEmpty() && !originalUrl.endsWith(".svg", ignoreCase = true)) {
                        return originalUrl
                    }
                }
            }
        }
        return null
    }

    private fun searchWikimediaImagesGeneral(query: String): String? {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://commons.wikimedia.org/w/api.php?action=query&format=json&prop=imageinfo&generator=search&gsrnamespace=6&gsrsearch=$encodedQuery&iiprop=url&iiurlwidth=800&gsrlimit=5"
        
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "PapiroNotebook/1.0 (alfortearjay0@gmail.com; Android App)")
            .build()
            
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(TAG, "Wikimedia API General error: Code ${response.code}")
                return null
            }
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val queryObj = json.optJSONObject("query") ?: return null
            val pagesObj = queryObj.optJSONObject("pages") ?: return null
            
            val keys = pagesObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val page = pagesObj.getJSONObject(key)
                val imageInfoArray = page.optJSONArray("imageinfo")
                if (imageInfoArray != null && imageInfoArray.length() > 0) {
                    val info = imageInfoArray.getJSONObject(0)
                    
                    val thumbUrl = info.optString("thumburl")
                    if (thumbUrl.isNotEmpty()) {
                        return thumbUrl
                    }
                    
                    val originalUrl = info.optString("url")
                    if (originalUrl.isNotEmpty()) {
                        return originalUrl
                    }
                }
            }
        }
        return null
    }
}
