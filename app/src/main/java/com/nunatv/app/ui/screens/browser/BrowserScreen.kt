package com.nunatv.app.ui.screens.browser

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.nunatv.app.data.preferences.AppPreferences
import com.nunatv.app.data.preferences.VideoResolution
import com.nunatv.app.downloader.DownloadService
import com.nunatv.app.ui.components.DownloadPromptBanner
import com.nunatv.app.util.DeviceUtils

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    appPreferences: AppPreferences,
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    downloadedVideoIds: Set<String> = emptySet()
) {
    val context = LocalContext.current
    val isTv = remember { DeviceUtils.isTvDevice(context) }

    var promptVideoUrl by remember { mutableStateOf<String?>(null) }
    var lastPromptedVideoId by remember { mutableStateOf<String?>(null) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    val latestDownloadedIds by rememberUpdatedState(downloadedVideoIds)

    val defaultResolution by appPreferences.selectedResolution.collectAsState(initial = VideoResolution.HIGH)

    fun showDownloadBannerForUrl(url: String) {
        val videoId = YoutubeWebViewClient.extractVideoId(url) ?: url
        // Jangan munculkan popup download jika video sudah tersimpan atau sedang diunduh
        if (videoId in latestDownloadedIds) {
            return
        }
        if (videoId != lastPromptedVideoId) {
            lastPromptedVideoId = videoId
            promptVideoUrl = url
        }
    }

    // Tangani Pause/Resume & Visibilitas WebView agar tidak mencuri fokus D-Pad saat berpindah menu
    LaunchedEffect(isActive, webViewInstance) {
        webViewInstance?.let { webView ->
            if (isActive) {
                webView.visibility = View.VISIBLE
                webView.isFocusable = true
                webView.isFocusableInTouchMode = true
                webView.onResume()
            } else {
                webView.onPause()
                webView.isFocusable = false
                webView.isFocusableInTouchMode = false
                webView.clearFocus()
                webView.visibility = View.GONE
                promptVideoUrl = null
            }
        }
    }

    // Tombol Back fisik / remote untuk riwayat navigasi WebView YouTube
    BackHandler(enabled = isActive && (promptVideoUrl != null || webViewInstance?.canGoBack() == true)) {
        if (promptVideoUrl != null) {
            promptVideoUrl = null
        } else {
            webViewInstance?.goBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusProperties { canFocus = isActive }
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .focusProperties { canFocus = isActive },
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    isFocusable = isActive
                    isFocusableInTouchMode = isActive
                    isClickable = true
                    visibility = if (isActive) View.VISIBLE else View.GONE

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        cacheMode = WebSettings.LOAD_DEFAULT

                        // Gunakan User-Agent Chrome modern agar YouTube (m.youtube.com) mendukung WebView penuh
                        userAgentString = if (isTv) {
                            "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
                        } else {
                            "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                        }
                    }

                    // JavaScript Bridge Interface untuk menangkap klik SPA YouTube sambil tetap memutar video
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onVideoUrlDetected(url: String) {
                            Handler(Looper.getMainLooper()).post {
                                showDownloadBannerForUrl(url)
                            }
                        }
                    }, "NunaBridge")

                    // WebViewClient: Tampilkan banner download di bawah TANPA memblokir pemutaran video
                    webViewClient = YoutubeWebViewClient { url ->
                        showDownloadBannerForUrl(url)
                        false // Biarkan WebView langsung memuat & memutar video
                    }

                    webChromeClient = WebChromeClient()

                    setOnKeyListener { _, keyCode, event ->
                        if (event.action == KeyEvent.ACTION_DOWN) {
                            when (keyCode) {
                                KeyEvent.KEYCODE_BACK -> {
                                    if (promptVideoUrl != null) {
                                        promptVideoUrl = null
                                        true
                                    } else if (canGoBack()) {
                                        goBack()
                                        true
                                    } else {
                                        false
                                    }
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    }

                    loadUrl("https://m.youtube.com")
                    webViewInstance = this
                }
            },
            update = { webView ->
                webViewInstance = webView
            }
        )

        // Banner Download non-blocking di sisi bawah (hilang otomatis dalam 5 detik)
        val activeUrl = promptVideoUrl
        DownloadPromptBanner(
            visible = isActive && activeUrl != null,
            videoTitle = activeUrl?.let { "YouTube (${YoutubeWebViewClient.extractVideoId(it) ?: "Video"})" } ?: "",
            resolutionLabel = defaultResolution.label,
            requestFocusOnShow = isTv,
            onDownload = {
                activeUrl?.let { url ->
                    DownloadService.start(
                        context = context,
                        videoUrl = url,
                        resolutionName = defaultResolution.name
                    )
                    Toast.makeText(
                        context,
                        "Memulai download (${defaultResolution.label})...",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                promptVideoUrl = null
            },
            onDismiss = {
                promptVideoUrl = null
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
