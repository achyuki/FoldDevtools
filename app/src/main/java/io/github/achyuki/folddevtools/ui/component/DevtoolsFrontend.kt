package io.github.achyuki.folddevtools.ui.component

import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.github.achyuki.folddevtools.TAG
import java.io.*
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevtoolsFrontend(url: String) {
    val context = LocalContext.current

    Log.i(TAG, "loadweb $url")
    ComposeWebView(
        url,
        modifier = Modifier
            .fillMaxSize()
    )
}

@Composable
fun ComposeWebView(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context as ComponentActivity

    AndroidView(
        factory = {
            WebView(it).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = WebViewClient()
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    allowContentAccess = true
                    allowFileAccess = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                setOnKeyListener { _, keyCode, event ->
                    if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                        if (canGoBack()) {
                            goBack()
                        } else {
                            activity.onBackPressedDispatcher.onBackPressed()
                        }
                        true
                    } else {
                        false
                    }
                }

                loadUrl(url)
            }
        },
        modifier = modifier,
        update = {}
    )
}
