package com.example.pikephish_v2.data.scanner

import android.util.Log
import org.jsoup.Jsoup

data class HtmlInfo(
    val hasLoginForm: Boolean,
    val hasIframe: Boolean,
    val jsEvalLike: Boolean,
    val overlayAttempt: Boolean,
    val notificationInjection: Boolean,
    val instantAppFlag: Boolean,
    val hasCsp: Boolean,
    val hasXfo: Boolean
)

class HtmlAnalyzer {

    companion object {
        private const val TAG = "HtmlAnalyzer"
    }

    fun analyze(html: String): HtmlInfo {
        val doc = Jsoup.parse(html)

        val hasLoginForm = doc.select("input[type=password]").isNotEmpty()
        val hasIframe = doc.select("iframe").isNotEmpty()
        val jsEvalLike = doc.select("script").any {
            it.html().contains("eval(", ignoreCase = true)
        }

        // Эвристика на overlay атаки
        val overlayAttempt = doc.select("div[style*='position:fixed']").isNotEmpty() ||
                doc.select("div[style*='z-index']").any {
                    val zIndex = it.attr("style").substringAfter("z-index:").trim().takeWhile { c -> c.isDigit() }
                    zIndex.toIntOrNull()?.let { it > 1000 } ?: false
                }

        // Notification injection
        val notificationInjection = doc.select("script").any {
            it.html().contains("Notification.requestPermission", ignoreCase = true)
        }

        // Instant App flag
        val instantAppFlag = html.contains("instantapps", ignoreCase = true)

        // Security headers (эмуляция через meta tags, т.к. headers недоступны здесь)
        val hasCsp = doc.select("meta[http-equiv='Content-Security-Policy']").isNotEmpty()
        val hasXfo = doc.select("meta[http-equiv='X-Frame-Options']").isNotEmpty()

        Log.d(TAG, "✅ HTML: Login=$hasLoginForm, Iframe=$hasIframe, Eval=$jsEvalLike")

        return HtmlInfo(
            hasLoginForm = hasLoginForm,
            hasIframe = hasIframe,
            jsEvalLike = jsEvalLike,
            overlayAttempt = overlayAttempt,
            notificationInjection = notificationInjection,
            instantAppFlag = instantAppFlag,
            hasCsp = hasCsp,
            hasXfo = hasXfo
        )
    }
}
