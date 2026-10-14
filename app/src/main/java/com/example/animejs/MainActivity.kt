package com.example.animejs

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.animejs.ui.theme.AnimeJSTheme

private const val HOME_URL = "https://animejs.com/"

class MainActivity : ComponentActivity() {
    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AnimeJSTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    AnimeJsWebView(
                        modifier = Modifier.padding(padding),
                        savedState = savedInstanceState,
                        onWebViewCreated = { webView = it }
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView?.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
    }

    override fun onPause() {
        webView?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        webView?.apply {
            stopLoading()
            webChromeClient = null
            webViewClient = WebViewClient()
            destroy()
        }
        webView = null
        super.onDestroy()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun AnimeJsWebView(
    modifier: Modifier = Modifier,
    savedState: Bundle?,
    onWebViewCreated: (WebView) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var currentWebView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }

    BackHandler(enabled = canGoBack) {
        currentWebView?.goBack()
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFF4F1EF))) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    currentWebView = this
                    onWebViewCreated(this)
                    setBackgroundColor(android.graphics.Color.WHITE)
                    // Work around stale GPU-composited frames seen on some phones while the
                    // Anime.js page runs scroll-synchronized transforms. Software rendering is
                    // slower, but it forces the WebView to repaint the current animation frame.
                    setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                    overScrollMode = View.OVER_SCROLL_NEVER

                    settings.apply {
                        // Keep the layout viewport aligned with the real phone viewport.
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        loadsImagesAutomatically = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        useWideViewPort = true
                        loadWithOverviewMode = false
                        textZoom = 100
                        layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
                        builtInZoomControls = false
                        displayZoomControls = false
                        allowFileAccess = false
                        allowContentAccess = true
                        javaScriptCanOpenWindowsAutomatically = false
                        setSupportMultipleWindows(false)
                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        mediaPlaybackRequiresUserGesture = true
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                            hasError = false
                            progress = 0
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                            progress = 100
                            canGoBack = view?.canGoBack() == true

                            // Preserve the site's own responsive and animation lifecycle.
                            // Synthetic resize events here can restart layout/animation sequences
                            // and produce overlapping text on mobile WebView.
                            view?.evaluateJavascript(
                                "document.documentElement.style.webkitTextSizeAdjust = '100%';",
                                null
                            )
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            if (request?.isForMainFrame == true) {
                                isLoading = false
                                hasError = true
                            }
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val uri: Uri = request?.url ?: return false
                            val host = uri.host.orEmpty().lowercase()
                            val isAnimeJs = host == "animejs.com" || host.endsWith(".animejs.com")

                            if (uri.scheme == "https" && isAnimeJs) return false

                            if (uri.scheme == "http" || uri.scheme == "https") {
                                return try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    true
                                } catch (_: ActivityNotFoundException) {
                                    false
                                }
                            }
                            return true
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                            isLoading = newProgress < 100
                            canGoBack = view?.canGoBack() == true
                        }
                    }

                    val restored = savedState?.let { restoreState(it) } != null
                    if (!restored) loadUrl(HOME_URL)
                }
            },
            update = { view ->
                currentWebView = view
                canGoBack = view.canGoBack()
            }
        )

        if (isLoading && !hasError) {
            Column(
                modifier = Modifier.align(Alignment.Center).background(Color(0xFFF4F1EF)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    progress = { (progress.coerceIn(0, 100) / 100f).coerceAtLeast(0.08f) }
                )
                Text(
                    text = if (progress > 0) "Loading Anime.js… $progress%" else "Connecting to Anime.js…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF292827)
                )
            }
        }

        if (hasError) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Unable to load Anime.js. Check your internet connection and retry.",
                    color = Color(0xFF292827)
                )
                Button(onClick = {
                    hasError = false
                    isLoading = true
                    progress = 0
                    currentWebView?.reload() ?: currentWebView?.loadUrl(HOME_URL)
                }) {
                    Text("Retry")
                }
            }
        }
    }
}
