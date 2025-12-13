package com.example.pikephish_v2.data.scanner

import android.util.Log
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class SslInfo(
    val issuer: String?,
    val validFrom: Date?,
    val validTo: Date?,
    val isValid: Boolean
)

class SslAnalyzer {

    companion object {
        private const val TAG = "SslAnalyzer"
    }

    fun analyze(domain: String): SslInfo? {
        return try {
            // Создаём TrustManager, который принимает все сертификаты
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            val factory = sslContext.socketFactory
            val socket = factory.createSocket(domain, 443) as javax.net.ssl.SSLSocket

            socket.startHandshake()
            val session = socket.session
            val certs = session.peerCertificates

            if (certs.isNotEmpty()) {
                val cert = certs[0] as X509Certificate

                val issuer = cert.issuerDN.name
                val validFrom = cert.notBefore
                val validTo = cert.notAfter
                val isValid = try {
                    cert.checkValidity()
                    true
                } catch (e: Exception) {
                    false
                }

                socket.close()

                Log.d(TAG, "✅ SSL: Issuer=$issuer, Valid=$isValid")

                return SslInfo(
                    issuer = issuer,
                    validFrom = validFrom,
                    validTo = validTo,
                    isValid = isValid
                )
            }

            socket.close()
            null
        } catch (e: Exception) {
            Log.e(TAG, "❌ SSL analysis failed: ${e.message}")
            null
        }
    }
}
