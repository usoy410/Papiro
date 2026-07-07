package com.usoy.papiro.ui.components

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
    
    val sanitizedCode = mermaidCode.replace("\r\n", "\n")
    val base64Code = android.util.Base64.encodeToString(
        sanitizedCode.toByteArray(Charsets.UTF_8),
        android.util.Base64.NO_WRAP
    )

    val viewportMeta = if (zoomable) {
        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=yes\">"
    } else {
        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1\">"
    }

    val htmlContent = if (zoomable) {
        """
        <!DOCTYPE html>
        <html>
        <head>
          $viewportMeta
          <style>
            html, body { 
                margin: 0; 
                padding: 0; 
                background: transparent; 
                overflow: auto; 
                width: 100%;
            }
            .scroll-container {
                box-sizing: border-box;
                padding: 16px;
                width: 100%;
            }
            .mermaid { 
                text-align: center;
                width: 100%;
                margin: 0 auto;
            }
            .mermaid svg {
                max-width: 100% !important; 
                height: auto !important;
            }
          </style>
        </head>
        <body>
          <div class="scroll-container">
            <div class="mermaid">Loading diagram...</div>
          </div>
          
          <script type="module">
            import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.esm.min.mjs';
            mermaid.initialize({ startOnLoad: false, theme: '$theme' });
            
            function b64DecodeUnicode(str) {
                return decodeURIComponent(atob(str).split('').map(function(c) {
                    return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
                }).join(''));
            }
            
            async function renderDiagram() {
                try {
                    const decoded = b64DecodeUnicode('$base64Code');
                    const el = document.querySelector('.mermaid');
                    if (el) {
                        const { svg, bindFunctions } = await mermaid.render('mermaid-svg', decoded);
                        el.innerHTML = svg;
                        if (bindFunctions) bindFunctions(el);
                    }
                } catch (e) {
                    console.error("Mermaid error:", e);
                    const el = document.querySelector('.mermaid');
                    if (el) el.textContent = "Error rendering diagram:\n" + e.message;
                }
            }
            
            renderDiagram();
          </script>
        </body>
        </html>
        """.trimIndent()
    } else {
        """
        <!DOCTYPE html>
        <html>
        <head>
          $viewportMeta
          <style>
            html, body { 
                margin: 0; 
                padding: 0; 
                background: transparent; 
                overflow: hidden; 
                height: auto;
                width: 100%;
            }
            .mermaid { 
                text-align: center;
                width: 100%;
                margin: 0;
                padding: 0;
            }
            .mermaid svg {
                max-width: 100% !important; 
                height: auto !important;
            }
          </style>
        </head>
        <body>
          <div class="mermaid">Loading diagram...</div>
          
          <script type="module">
            import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.esm.min.mjs';
            mermaid.initialize({ startOnLoad: false, theme: '$theme' });
            
            function b64DecodeUnicode(str) {
                return decodeURIComponent(atob(str).split('').map(function(c) {
                    return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
                }).join(''));
            }
            
            async function renderDiagram() {
                try {
                    const decoded = b64DecodeUnicode('$base64Code');
                    const el = document.querySelector('.mermaid');
                    if (el) {
                        const { svg, bindFunctions } = await mermaid.render('mermaid-svg', decoded);
                        el.innerHTML = svg;
                        if (bindFunctions) bindFunctions(el);
                        
                        const resizeObserver = new ResizeObserver(entries => {
                            for (let entry of entries) {
                                const height = entry.contentRect.height;
                                if (window.AndroidInterface && height > 0) {
                                    window.AndroidInterface.resize(height + 20);
                                }
                            }
                        });
                        resizeObserver.observe(el);
                        if (window.AndroidInterface) {
                           window.AndroidInterface.resize(document.body.scrollHeight + 20);
                        }
                    }
                } catch (e) {
                    console.error("Mermaid error:", e);
                    const el = document.querySelector('.mermaid');
                    if (el) el.textContent = "Error rendering diagram:\n" + e.message;
                }
            }
            
            renderDiagram();
          </script>
        </body>
        </html>
        """.trimIndent()
    }

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
                    isVerticalScrollBarEnabled = true
                    isHorizontalScrollBarEnabled = true
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
            }
        },
        update = { webView ->
            val lastLoaded = webView.tag as? String
            if (lastLoaded != htmlContent) {
                webView.tag = htmlContent
                webView.loadDataWithBaseURL("https://cdn.jsdelivr.net", htmlContent, "text/html", "UTF-8", null)
            }
        }
    )
}
