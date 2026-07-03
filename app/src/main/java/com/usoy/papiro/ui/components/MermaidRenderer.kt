package com.usoy.papiro.ui.components

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp

class WebAppInterface(private val onResize: (Float) -> Unit) {
    @JavascriptInterface
    fun resize(height: Float) {
        onResize(height)
    }
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun MermaidRenderer(
    mermaidCode: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    zoomable: Boolean = false
) {
    var webViewHeight by remember { mutableStateOf(200.dp) }
    val density = LocalDensity.current

    val theme = if (isDark) "dark" else "default"
    
    // We escape backticks, dollars and scripts carefully
    val safeCode = mermaidCode
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    val viewportMeta = if (zoomable) {
        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=yes\">"
    } else {
        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1\">"
    }

    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
          $viewportMeta
          <script type="module">
            import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs';
            mermaid.initialize({ startOnLoad: true, theme: '$theme' });
            
            const resizeObserver = new ResizeObserver(entries => {
                for (let entry of entries) {
                    const height = entry.contentRect.height;
                    if (window.AndroidInterface && height > 0) {
                        window.AndroidInterface.resize(height + 20);
                    }
                }
            });
            
            setTimeout(() => {
                const el = document.querySelector('.mermaid');
                if (el) {
                    resizeObserver.observe(el);
                    if (window.AndroidInterface) {
                       window.AndroidInterface.resize(document.body.scrollHeight + 20);
                    }
                }
            }, 500);
          </script>
          <style>
            body, html { margin: 0; padding: 0; background: transparent; overflow: hidden; display: flex; justify-content: center; }
            .mermaid { width: 100%; text-align: center; }
          </style>
        </head>
        <body>
          <div class="mermaid">
            $safeCode
          </div>
        </body>
        </html>
    """.trimIndent()

    val finalModifier = if (zoomable) modifier.fillMaxSize() else modifier.fillMaxWidth().height(webViewHeight)

    AndroidView(
        modifier = finalModifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                if (zoomable) {
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.setSupportZoom(true)
                }
                webViewClient = WebViewClient()
                setBackgroundColor(0x00000000) // transparent
                addJavascriptInterface(WebAppInterface { height ->
                    post {
                        with(density) {
                            webViewHeight = height.dp
                        }
                    }
                }, "AndroidInterface")
                loadDataWithBaseURL("https://cdn.jsdelivr.net", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://cdn.jsdelivr.net", htmlContent, "text/html", "UTF-8", null)
        }
    )
}
