package com.example.pikephish_v2.data.model

import java.util.Date

data class ScanResult(
    val url: String,
    val finalUrl: String?,
    val statusCode: Int?,
    val isHttps: Boolean,

    // SSL информация
    val sslIssuer: String?,
    val sslValidFrom: Date?,
    val sslValidTo: Date?,
    val sslIsValid: Boolean?,

    // DNS информация
    val ip: String?,
    val hasMx: Boolean?,
    val mxRecords: List<String>?,

    // Домен
    val domain: String?,
    val subdomainCount: Int?,
    val ageDays: Int?,
    val registrar: String?,

    // HTML анализ
    val hasLoginForm: Boolean?,
    val hasIframe: Boolean?,
    val jsEvalLike: Boolean?,
    val overlayAttempt: Boolean?,
    val notificationInjection: Boolean?,

    // Безопасность
    val webviewMisuse: String?,
    val instantAppFlag: Boolean?,

    // Метаданные
    val collectedAt: Long = System.currentTimeMillis(),
    val scanDurationMs: Long = 0,
    val errors: List<String> = emptyList()
)
