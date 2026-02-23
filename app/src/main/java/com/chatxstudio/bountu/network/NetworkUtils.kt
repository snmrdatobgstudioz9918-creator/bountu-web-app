package com.chatxstudio.bountu.network

import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.content.Context
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {
    /** Return a list of local IPv4 addresses (excludes loopback). */
    fun getLocalIpAddresses(): List<String> {
        return try {
            val addresses = mutableListOf<String>()
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    val host = addr.hostAddress ?: continue
                    val isIPv4 = host.indexOf(':') < 0
                    if (isIPv4 && host != "127.0.0.1") {
                        addresses.add(host)
                    }
                }
            }
            addresses.distinct()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Best-effort primary IP (first found) or null. */
    fun getPrimaryIp(): String? = getLocalIpAddresses().firstOrNull()
}
