package com.example.pikephish_v2.data.scanner

import android.util.Log
import org.xbill.DNS.Lookup
import org.xbill.DNS.Type
import java.net.InetAddress

data class DnsInfo(
    val ip: String?,
    val hasMx: Boolean,
    val mxRecords: List<String>?
)

class DnsAnalyzer {

    companion object {
        private const val TAG = "DnsAnalyzer"
    }

    fun analyze(domain: String): DnsInfo {
        val ip = resolveIp(domain)
        val (hasMx, mxRecords) = resolveMx(domain)

        Log.d(TAG, "✅ DNS: IP=$ip, MX=$hasMx for $domain")

        return DnsInfo(
            ip = ip,
            hasMx = hasMx,
            mxRecords = mxRecords
        )
    }

    private fun resolveIp(domain: String): String? {
        return try {
            InetAddress.getByName(domain).hostAddress
        } catch (e: Exception) {
            Log.e(TAG, "❌ IP resolution failed: ${e.message}")
            null
        }
    }

    private fun resolveMx(domain: String): Pair<Boolean, List<String>?> {
        return try {
            val lookup = Lookup(domain, Type.MX)
            val records = lookup.run()

            if (records != null && records.isNotEmpty()) {
                val mxList = records.map { it.rdataToString() }
                true to mxList
            } else {
                false to null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ MX lookup failed: ${e.message}")
            false to null
        }
    }
}
