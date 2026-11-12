package com.nunatv.app.ui.screens.browser

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class YoutubeWebViewClient(
    private val onVideoIntercepted: (url: String) -> Boolean
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        Log.d(TAG, "shouldOverrideUrlLoading: $url")

        if (isVideoUrl(url)) {
            val handled = onVideoIntercepted(url)
            if (handled) return true
        }

        return false
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        Log.d(TAG, "onPageStarted: $url")
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        // Inject JS helper untuk menangkap klik link SPA (Single Page Application) YouTube
        val jsInjection = """
            (function() {
                if (window.nunaInjected) return;
                window.nunaInjected = true;

                function checkUrl(url) {
                    if (url && (url.indexOf('watch?v=') !== -1 || url.indexOf('/shorts/') !== -1 || url.indexOf('youtu.be/') !== -1)) {
                        if (window.NunaBridge) {
                            window.NunaBridge.onVideoUrlDetected(url);
                        }
                    }
                }

                // Monitor klik pada elemen <a>
                document.addEventListener('click', function(e) {
                    var target = e.target;
                    while (target && target.tagName !== 'A') {
                        target = target.parentElement;
                    }
                    if (target && target.href) {
                        checkUrl(target.href);
                    }
                }, true);

                // Monitor perubahan history / SPA navigation YouTube
                var pushState = history.pushState;
                history.pushState = function() {
                    pushState.apply(history, arguments);
                    checkUrl(window.location.href);
                };
            })();
        """.trimIndent()
        view?.evaluateJavascript(jsInjection, null)
    }

    companion object {
        private const val TAG = "YoutubeWebViewClient"

        fun isVideoUrl(url: String): Boolean {
            return (url.contains("watch?v=") || url.contains("youtu.be/") || url.contains("/shorts/"))
        }

        fun extractVideoId(url: String): String? {
            return try {
                val uri = Uri.parse(url)
                if (url.contains("youtu.be/")) {
                    uri.lastPathSegment
                } else if (url.contains("/shorts/")) {
                    uri.lastPathSegment
                } else {
                    uri.getQueryParameter("v")
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
