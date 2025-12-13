package com.example.pikephish_v2.data.scanner

import android.util.Log
import com.example.pikephish_v2.data.model.ScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.Date
import java.util.concurrent.TimeUnit

class UrlScanner {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val dnsAnalyzer = DnsAnalyzer()
    private val sslAnalyzer = SslAnalyzer()
    private val htmlAnalyzer = HtmlAnalyzer()

    companion object {
        private const val TAG = "UrlScanner"
    }

    /**
     * Основной метод сканирования URL
     */
    suspend fun scanUrl(urlInput: String): ScanResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<String>()

        // Нормализация URL
        val url = normalizeUrl(urlInput)
        val isHttps = url.startsWith("https", ignoreCase = true)

        var finalUrl: String? = null
        var statusCode: Int? = null
        var html: String? = null
        var domain: String? = null

        // Извлекаем домен
        try {
            domain = extractDomain(url)
        } catch (e: Exception) {
            errors.add("Domain extraction failed: ${e.message}")
        }

        // 1️⃣ HTTP запрос
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "PikePhish/1.0 (Android)")
                .build()

            val response = client.newCall(request).execute()
            statusCode = response.code
            finalUrl = response.request.url.toString()
            html = response.body?.string()

            Log.d(TAG, "✅ HTTP: $statusCode for $url")
        } catch (e: Exception) {
            errors.add("HTTP failed: ${e.message}")
            Log.e(TAG, "❌ HTTP error: ${e.message}")
        }

        // 2️⃣ Анализ HTML
        val htmlInfo = if (html != null) {
            try {
                htmlAnalyzer.analyze(html)
            } catch (e: Exception) {
                errors.add("HTML analysis failed: ${e.message}")
                null
            }
        } else null

        // 3️⃣ DNS анализ
        val dnsInfo = if (domain != null) {
            try {
                dnsAnalyzer.analyze(domain)
            } catch (e: Exception) {
                errors.add("DNS analysis failed: ${e.message}")
                null
            }
        } else null

        // 4️⃣ SSL анализ
        val sslInfo = if (isHttps && domain != null) {
            try {
                sslAnalyzer.analyze(domain)
            } catch (e: Exception) {
                errors.add("SSL analysis failed: ${e.message}")
                null
            }
        } else null

        // 5️⃣ Подсчет поддоменов
        val subdomainCount = domain?.split('.')?.size?.minus(2)?.coerceAtLeast(0)

        // 6️⃣ WebView misuse эвристика
        val webviewMisuse = when {
            htmlInfo?.hasLoginForm == true && htmlInfo.hasCsp == false -> "suspicious"
            htmlInfo?.hasLoginForm == false && htmlInfo.hasCsp == false -> "possible"
            else -> "unlikely"
        }

        val scanDuration = System.currentTimeMillis() - startTime

        ScanResult(
            url = urlInput,
            finalUrl = finalUrl,
            statusCode = statusCode,
            isHttps = isHttps,

            // SSL
            sslIssuer = sslInfo?.issuer,
            sslValidFrom = sslInfo?.validFrom,
            sslValidTo = sslInfo?.validTo,
            sslIsValid = sslInfo?.isValid,

            // DNS
            ip = dnsInfo?.ip,
            hasMx = dnsInfo?.hasMx,
            mxRecords = dnsInfo?.mxRecords,

            // Domain
            domain = domain,
            subdomainCount = subdomainCount,
            ageDays = null, // WHOIS не реализован (требует внешний API)
            registrar = null,

            // HTML
            hasLoginForm = htmlInfo?.hasLoginForm,
            hasIframe = htmlInfo?.hasIframe,
            jsEvalLike = htmlInfo?.jsEvalLike,
            overlayAttempt = htmlInfo?.overlayAttempt,
            notificationInjection = htmlInfo?.notificationInjection,

            // Security
            webviewMisuse = webviewMisuse,
            instantAppFlag = htmlInfo?.instantAppFlag,

            // Metadata
            collectedAt = System.currentTimeMillis(),
            scanDurationMs = scanDuration,
            errors = errors
        )
    }

    /**
     * Нормализация URL (добавляет https:// если не указано)
     */
    private fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim()
        return when {
            trimmed.startsWith("http://", ignoreCase = true) -> trimmed
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            else -> "https://$trimmed"
        }
    }

    /**
     * Извлечение домена из URL
     */
    private fun extractDomain(url: String): String? {
        return try {
            URI(url).host
        } catch (e: Exception) {
            null
        }
    }
}
