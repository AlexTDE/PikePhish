package com.example.pikephish_v2.data.repository

import android.util.Log
import com.example.pikephish_v2.data.local.LinkHistoryDao
import com.example.pikephish_v2.data.local.LinkHistoryEntity
import com.example.pikephish_v2.data.model.ScanResult
import com.example.pikephish_v2.data.remote.PhishingApiService
import com.example.pikephish_v2.data.remote.PhishingCheckRequest
import com.example.pikephish_v2.data.remote.PhishingCheckResponse
import com.example.pikephish_v2.data.scanner.UrlScanner
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Locale

class PhishingRepository(
    private val apiService: PhishingApiService,
    private val urlScanner: UrlScanner,
    private val historyDao: LinkHistoryDao
) {

    companion object {
        private const val TAG = "PhishingRepository"
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    }

    /**
     * Проверка URL с кэшированием в БД
     * 1. Нормализует URL (удаляет путь после домена)
     * 2. Проверяет наличие в локальной БД
     * 3. Если есть в БД - возвращает кэшированный результат
     * 4. Если нет - запрашивает с сервера и сохраняет только при успехе
     */
    suspend fun checkUrlWithCache(url: String, source: String = "manual"): Result<PhishingCheckResponse> {
        return try {
            // Нормализация URL (удаляем путь)
            val normalizedUrl = normalizeUrl(url)
            Log.d(TAG, "🔍 Начинаем проверку: $url")
            Log.d(TAG, "✂️ Нормализованный URL: $normalizedUrl")

            // Проверяем кэш в БД
            val cachedResult = historyDao.getLinkByUrl(normalizedUrl)
            if (cachedResult != null) {
                Log.d(TAG, "💾 Найдено в кэше: $normalizedUrl")
                val response = PhishingCheckResponse(
                    isPhishing = cachedResult.isPhishing,
                    confidence = cachedResult.confidence,
                    prediction = cachedResult.prediction,
                    reason = cachedResult.reason ?: "Из кэша"
                )
                return Result.success(response)
            }

            Log.d(TAG, "🌐 Кэш не найден, запрашиваем сервер...")

            // 1. Сканируем URL локально
            val scanResult = urlScanner.scanUrl(normalizedUrl)

            // 2. Формируем запрос для сервера
            val request = scanResultToRequest(scanResult)

            Log.d(TAG, "📤 Отправляем данные на сервер...")

            // 3. Отправляем на сервер
            val response = apiService.checkUrl(request)

            if (response.isSuccessful && response.body() != null) {
                val result = response.body()!!
                Log.d(TAG, "✅ Получен ответ: isPhishing=${result.isPhishing}, confidence=${result.confidence}")

                // 💾 Сохраняем в историю только при успешном ответе
                saveToHistory(result, scanResult, source)

                // 🗑️ Автоматически удаляем старые записи (оставляем 15)
                historyDao.keepOnlyRecent(15)

                Result.success(result)
            } else {
                val error = "Ошибка сервера: ${response.code()} ${response.message()}"
                Log.e(TAG, "❌ $error")
                // НЕ сохраняем в БД при ошибке
                Result.failure(Exception(error))
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка проверки: ${e.message}", e)
            // НЕ сохраняем в БД при исключении
            Result.failure(e)
        }
    }

    /**
     * Полная проверка URL с сохранением в историю (без кэша)
     * Оставлен для обратной совместимости
     */
    suspend fun checkUrl(url: String, source: String = "manual"): Result<PhishingCheckResponse> {
        return try {
            // Нормализация URL
            val normalizedUrl = normalizeUrl(url)
            Log.d(TAG, "🔍 Начинаем сканирование: $url")
            Log.d(TAG, "✂️ Нормализованный URL: $normalizedUrl")

            // 1. Сканируем URL локально
            val scanResult = urlScanner.scanUrl(normalizedUrl)

            // 2. Формируем запрос для сервера
            val request = scanResultToRequest(scanResult)

            Log.d(TAG, "📤 Отправляем данные на сервер...")

            // 3. Отправляем на сервер
            val response = apiService.checkUrl(request)

            if (response.isSuccessful && response.body() != null) {
                val result = response.body()!!
                Log.d(TAG, "✅ Получен ответ: isPhishing=${result.isPhishing}, confidence=${result.confidence}")

                // 💾 Сохраняем в историю только при успехе
                saveToHistory(result, scanResult, source)

                // 🗑️ Автоматически удаляем старые записи (оставляем 15)
                historyDao.keepOnlyRecent(15)

                Result.success(result)
            } else {
                val error = "Ошибка сервера: ${response.code()} ${response.message()}"
                Log.e(TAG, "❌ $error")
                Result.failure(Exception(error))
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка проверки: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Нормализация URL - удаляет путь, оставляет только протокол и домен
     * Примеры:
     * - https://example.com/path/info -> https://example.com
     * - http://test.com/page -> http://test.com
     * - example.com/path -> https://example.com
     */
    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        
        // Определяем протокол
        val protocol = when {
            trimmed.startsWith("https://", ignoreCase = true) -> "https://"
            trimmed.startsWith("http://", ignoreCase = true) -> "http://"
            else -> "https://"
        }
        
        // Убираем протокол для обработки
        val withoutProtocol = trimmed
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("HTTPS://")
            .removePrefix("HTTP://")
        
        // Находим первый слеш и обрезаем всё после него
        val domain = withoutProtocol.split("/")[0]
        
        return "$protocol$domain"
    }

    /**
     * Сохранение результата в историю
     * Используем URL и domain из scanResult, а результат классификации из response
     */
    private suspend fun saveToHistory(
        response: PhishingCheckResponse,
        scanResult: ScanResult,  // ← Добавили scanResult
        source: String
    ) {
        val entity = LinkHistoryEntity(
            url = scanResult.url,              // ← Из scanResult!
            domain = scanResult.domain,        // ← Из scanResult!
            isPhishing = response.isPhishing,
            confidence = response.confidence,
            prediction = response.prediction,
            reason = response.reason,
            checkedAt = System.currentTimeMillis(),
            source = source
        )

        historyDao.insertLink(entity)
        Log.d(TAG, "💾 Сохранено в историю: ${scanResult.url} (domain: ${scanResult.domain})")
    }

    /**
     * Получить последние 15 ссылок
     */
    fun getRecentLinks(): Flow<List<LinkHistoryEntity>> {
        return historyDao.getRecentLinks(15)
    }

    /**
     * Получить все ссылки
     */
    fun getAllLinks(): Flow<List<LinkHistoryEntity>> {
        return historyDao.getAllLinks()
    }

    /**
     * Очистить историю
     */
    suspend fun clearHistory() {
        historyDao.clearAll()
        Log.d(TAG, "🗑️ История очищена")
    }

    /**
     * Удалить одну запись
     */
    suspend fun deleteLink(id: Long) {
        historyDao.deleteLink(id)
        Log.d(TAG, "🗑️ Удалена запись: $id")
    }

    /**
     * Конвертация ScanResult в запрос для сервера
     */
    private fun scanResultToRequest(scan: ScanResult): PhishingCheckRequest {
        return PhishingCheckRequest(
            url = scan.url,
            finalUrl = scan.finalUrl,
            statusCode = scan.statusCode?.toString(),
            isHttps = scan.isHttps.toString(),

            sslIssuer = scan.sslIssuer,
            sslValidFrom = scan.sslValidFrom?.let { dateFormat.format(it) },
            sslValidTo = scan.sslValidTo?.let { dateFormat.format(it) },

            ageDays = scan.ageDays?.toString(),
            registrar = scan.registrar,

            hasMx = scan.hasMx?.toString(),
            mxRecordsJson = scan.mxRecords?.joinToString(";"),

            ip = scan.ip,

            subdomainCount = scan.subdomainCount?.toString(),

            hasLoginForm = scan.hasLoginForm?.toString(),
            hasIframe = scan.hasIframe?.toString(),
            jsEvalLike = scan.jsEvalLike?.toString(),
            overlayAttempt = scan.overlayAttempt?.toString(),
            notificationInjection = scan.notificationInjection?.toString(),

            webviewMisuse = scan.webviewMisuse,
            instantAppFlag = scan.instantAppFlag?.toString(),

            collectedAt = scan.collectedAt.toString()
        )
    }
}
