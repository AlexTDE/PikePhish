package com.example.pikephish_v2.data.remote

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

data class PhishingCheckRequest(
    @SerializedName("url")
    val url: String,

    @SerializedName("finalUrl")
    val finalUrl: String? = null,

    @SerializedName("statusCode")
    val statusCode: String? = null,  // ← Изменено на String!

    @SerializedName("isHttps")
    val isHttps: String? = null,     // ← Изменено на String!

    @SerializedName("sslIssuer")
    val sslIssuer: String? = null,

    @SerializedName("sslValidFrom")
    val sslValidFrom: String? = null,  // ← Дата как String

    @SerializedName("sslValidTo")
    val sslValidTo: String? = null,    // ← Дата как String

    @SerializedName("ageDays")
    val ageDays: String? = null,       // ← Изменено на String!

    @SerializedName("registrar")
    val registrar: String? = null,

    @SerializedName("hasMx")
    val hasMx: String? = null,         // ← Изменено на String!

    @SerializedName("mxRecordsJson")
    val mxRecordsJson: String? = null,

    @SerializedName("ip")
    val ip: String? = null,

    @SerializedName("subdomainCount")
    val subdomainCount: String? = null,  // ← Изменено на String!

    @SerializedName("hasLoginForm")
    val hasLoginForm: String? = null,    // ← Изменено на String!

    @SerializedName("hasIframe")
    val hasIframe: String? = null,       // ← Изменено на String!

    @SerializedName("jsEvalLike")
    val jsEvalLike: String? = null,      // ← Изменено на String!

    @SerializedName("overlayAttempt")
    val overlayAttempt: String? = null,  // ← Изменено на String!

    @SerializedName("notificationInjection")
    val notificationInjection: String? = null,  // ← Изменено на String!

    @SerializedName("webviewMisuse")
    val webviewMisuse: String? = null,

    @SerializedName("instantAppFlag")
    val instantAppFlag: String? = null,  // ← Изменено на String!

    @SerializedName("collectedAt")
    val collectedAt: String? = null      // ← Изменено на String!
)

/**
 * Ответ от сервера
 */
data class PhishingCheckResponse(
    val url: String,
    val isPhishing: Boolean,
    val confidence: Double,
    val prediction: String,  // "phishing" или "legitimate"
    val reason: String?
)

interface PhishingApiService {

    @POST("predict")
    suspend fun checkUrl(@Body request: PhishingCheckRequest): Response<PhishingCheckResponse>

    companion object {
        // Для эмулятора Android Studio
        private const val BASE_URL_EMULATOR = "http://10.0.2.2:8000/"

        // Для физического устройства (замените на свой IP)
        private const val BASE_URL_DEVICE = "http://192.168.1.100:8000/"

        fun create(useEmulator: Boolean = true): PhishingApiService {
            val baseUrl = if (useEmulator) BASE_URL_EMULATOR else BASE_URL_DEVICE

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(PhishingApiService::class.java)
        }
    }
}
