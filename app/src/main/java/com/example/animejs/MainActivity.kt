package com.example.animejs

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

private const val HOME_URL = "https://animejs.com/"

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private var showingOfflinePage = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(12, 14, 18))
        }
        webView = WebView(this).apply {
            setBackgroundColor(Color.rgb(12, 14, 18))
            setLayerType(View.LAYER_TYPE_NONE, null)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadsImagesAutomatically = true
                useWideViewPort = true
                loadWithOverviewMode = false
                cacheMode = if (hasValidatedInternet()) {
                    WebSettings.LOAD_DEFAULT
                } else {
                    WebSettings.LOAD_CACHE_ELSE_NETWORK
                }
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    // Keep normal website navigation inside this app.
                    return request.url.scheme != "https" && request.url.scheme != "http"
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    super.onReceivedError(view, request, error)
                    if (request.isForMainFrame && !showingOfflinePage) {
                        showOfflinePage()
                    }
                }

                override fun onRenderProcessGone(
                    view: WebView,
                    detail: RenderProcessGoneDetail
                ): Boolean {
                    // Recover from a crashed WebView renderer rather than leaving a blank screen.
                    (view.parent as? android.view.ViewGroup)?.removeView(view)
                    view.destroy()
                    recreateWebView(root)
                    return true
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
                    progressBar.progress = newProgress
                }
            }
        }

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            max = 100
            progress = 0
        }
        root.addView(
            webView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        root.addView(
            progressBar,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(3),
                Gravity.TOP
            )
        )
        setContentView(root)

        if (savedInstanceState != null && webView.restoreState(savedInstanceState) != null) {
            // Restore the last page when Android recreates the activity.
        } else {
            loadHome()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        })
    }

    private fun hasValidatedInternet(): Boolean {
        val manager = getSystemService(ConnectivityManager::class.java)
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun loadHome() {
        showingOfflinePage = false
        webView.settings.cacheMode = if (hasValidatedInternet()) {
            WebSettings.LOAD_DEFAULT
        } else {
            WebSettings.LOAD_CACHE_ELSE_NETWORK
        }
        webView.loadUrl(HOME_URL)
    }

    private fun showOfflinePage() {
        showingOfflinePage = true
        val html = """
            <!doctype html>
            <html>
            <head>
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <meta name="color-scheme" content="dark">
              <style>
                body { margin:0; min-height:100vh; display:grid; place-items:center;
                  background:#0c0e12; color:#f4f4f5; font:16px system-ui,sans-serif; }
                main { max-width:420px; padding:28px; text-align:center; }
                p { color:#a1a1aa; line-height:1.55; }
                button { border:0; border-radius:10px; padding:12px 18px;
                  background:#f4f4f5; color:#111827; font-weight:600; font-size:16px; }
              </style>
            </head>
            <body><main>
              <h2>Anime.js is unavailable</h2>
              <p>You may be offline, or this page is not cached yet. Connect to the internet
              and load Anime.js once so the app can cache resources for later use.</p>
              <button onclick="window.location.href='https://animejs.com/'">Try again</button>
            </main></body></html>
        """.trimIndent()
        webView.loadDataWithBaseURL(HOME_URL, html, "text/html", "UTF-8", null)
    }

    private fun recreateWebView(root: FrameLayout) {
        webView = WebView(this).apply {
            setBackgroundColor(Color.rgb(12, 14, 18))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = if (hasValidatedInternet()) {
                WebSettings.LOAD_DEFAULT
            } else {
                WebSettings.LOAD_CACHE_ELSE_NETWORK
            }
            webViewClient = object : WebViewClient() {
                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    if (request.isForMainFrame) showOfflinePage()
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
                    progressBar.progress = newProgress
                }
            }
        }
        root.addView(
            webView,
            0,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        loadHome()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            (webView.parent as? android.view.ViewGroup)?.removeView(webView)
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }
}
