package com.example.pikephish_v2.data.whitelist

import android.content.Context
import android.content.SharedPreferences

/**
 * Менеджер доверенных доменов (whitelist)
 * Содержит предустановленный список + пользовательские домены
 */
class TrustedDomains(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "trusted_domains", 
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_CUSTOM_DOMAINS = "custom_domains"
        
        /**
         * Предустановленный whitelist популярных доменов
         */
        private val DEFAULT_WHITELIST = setOf(
            // Поисковики и почта
            "google.com", "gmail.com", "google.ru",
            "yandex.ru", "yandex.com", "ya.ru",
            "mail.ru", "rambler.ru",
            
            // Социальные сети
            "vk.com", "vk.ru",
            "ok.ru", "odnoklassniki.ru",
            "facebook.com", "fb.com",
            "instagram.com",
            "twitter.com", "x.com",
            "linkedin.com",
            "tiktok.com",
            "telegram.org",
            
            // Видео и медиа
            "youtube.com", "youtu.be",
            "rutube.ru",
            "twitch.tv",
            "vimeo.com",
            
            // Разработка
            "github.com", "gitlab.com",
            "stackoverflow.com", "stackexchange.com",
            "habr.com",
            
            // Онлайн-сервисы
            "wikipedia.org",
            "amazon.com", "amazon.ru",
            "ozon.ru", "wildberries.ru",
            "avito.ru",
            "aliexpress.com", "aliexpress.ru",
            
            // Банки (основные российские)
            "sberbank.ru", "sbrf.ru",
            "tinkoff.ru",
            "alfabank.ru",
            "vtb.ru",
            
            // Госуслуги
            "gosuslugi.ru", "esia.gosuslugi.ru",
            
            // Облачные хранилища
            "drive.google.com",
            "dropbox.com",
            "cloud.mail.ru",
            "disk.yandex.ru",
            
            // Мессенджеры
            "web.whatsapp.com",
            "web.telegram.org",
            
            // Другое
            "microsoft.com", "office.com",
            "apple.com", "icloud.com",
            "reddit.com",
            "zoom.us"
        )
    }

    /**
     * Проверяет, является ли домен доверенным
     */
    fun isTrusted(domain: String?): Boolean {
        if (domain.isNullOrBlank()) return false
        
        val normalizedDomain = domain.lowercase().trim()
        
        // Проверяем в дефолтном списке
        if (isInDefaultWhitelist(normalizedDomain)) {
            return true
        }
        
        // Проверяем в пользовательском списке
        return isInCustomWhitelist(normalizedDomain)
    }

    /**
     * Проверка в предустановленном whitelist
     */
    private fun isInDefaultWhitelist(domain: String): Boolean {
        // Точное совпадение или поддомен
        return DEFAULT_WHITELIST.any { trusted ->
            domain == trusted || domain.endsWith(".$trusted")
        }
    }

    /**
     * Проверка в пользовательском whitelist
     */
    private fun isInCustomWhitelist(domain: String): Boolean {
        val customDomains = getCustomDomains()
        return customDomains.any { trusted ->
            domain == trusted || domain.endsWith(".$trusted")
        }
    }

    /**
     * Получить список пользовательских доменов
     */
    fun getCustomDomains(): Set<String> {
        val saved = prefs.getStringSet(KEY_CUSTOM_DOMAINS, emptySet()) ?: emptySet()
        return saved.toSet()
    }

    /**
     * Добавить домен в whitelist
     */
    fun addCustomDomain(domain: String): Boolean {
        val normalized = domain.lowercase().trim()
        if (normalized.isBlank()) return false
        
        val current = getCustomDomains().toMutableSet()
        val added = current.add(normalized)
        
        if (added) {
            prefs.edit().putStringSet(KEY_CUSTOM_DOMAINS, current).apply()
        }
        
        return added
    }

    /**
     * Удалить домен из whitelist
     */
    fun removeCustomDomain(domain: String): Boolean {
        val current = getCustomDomains().toMutableSet()
        val removed = current.remove(domain.lowercase().trim())
        
        if (removed) {
            prefs.edit().putStringSet(KEY_CUSTOM_DOMAINS, current).apply()
        }
        
        return removed
    }

    /**
     * Очистить пользовательский whitelist
     */
    fun clearCustomDomains() {
        prefs.edit().remove(KEY_CUSTOM_DOMAINS).apply()
    }

    /**
     * Получить все доверенные домены (дефолтные + пользовательские)
     */
    fun getAllTrustedDomains(): Set<String> {
        return DEFAULT_WHITELIST + getCustomDomains()
    }
}
