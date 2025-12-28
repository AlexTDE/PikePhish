package com.example.pikephish_v2

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.pikephish_v2.data.local.AppDatabase
import com.example.pikephish_v2.data.remote.PhishingApiService
import com.example.pikephish_v2.data.repository.PhishingRepository
import com.example.pikephish_v2.data.scanner.UrlScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class UrlMonitorService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val repository by lazy {
        val database = AppDatabase.getDatabase(applicationContext)
        PhishingRepository(
            apiService = PhishingApiService.create(useEmulator = true),
            urlScanner = UrlScanner(),
            historyDao = database.linkHistoryDao()
        )
    }

    companion object {
        private const val TAG = "UrlMonitorService"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "✅ Сервис защиты запущен")
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                   AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        setServiceInfo(info)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            val packageName = event.packageName?.toString() ?: return
            
            // Извлекаем URL из браузера или других приложений
            val url = extractUrl(event.source)
            
            if (!url.isNullOrEmpty() && isValidUrl(url)) {
                Log.d(TAG, "🔍 Обнаружен URL: $url")
                checkUrlInBackground(url)
            }
        }
    }

    private fun extractUrl(nodeInfo: AccessibilityNodeInfo?): String? {
        if (nodeInfo == null) return null
        
        // Ищем URL в адресной строке браузера
        for (i in 0 until nodeInfo.childCount) {
            val child = nodeInfo.getChild(i) ?: continue
            
            val text = child.text?.toString()
            if (text != null && (text.startsWith("http://") || text.startsWith("https://"))) {
                return text
            }
            
            // Рекурсивный поиск
            val childUrl = extractUrl(child)
            if (childUrl != null) return childUrl
        }
        return null
    }

    private fun isValidUrl(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }

    private fun checkUrlInBackground(url: String) {
        serviceScope.launch {
            val result = repository.checkUrl(url, source = "background")
            
            if (result.isSuccess) {
                val response = result.getOrNull()!!
                
                if (response.isPhishing) {
                    Log.w(TAG, "⚠️ ФИШИНГ ОБНАРУЖЕН: $url")
                    // TODO: Показать уведомление пользователю
                    // showPhishingNotification(url, response)
                } else {
                    Log.d(TAG, "✅ Ссылка безопасна: $url")
                }
            } else {
                Log.e(TAG, "❌ Ошибка проверки: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "❌ Сервис прерван")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "🛑 Сервис остановлен")
    }
}