package com.chatxstudio.bountu.ssh

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.chatxstudio.bountu.network.NetworkUtils

/**
 * Helper to assemble SSH connection details and integrate with Termius (via ssh:// URI).
 */
object SshInfo {
    data class Details(
        val host: String,
        val port: Int = 22,
        val username: String = "user",
        val password: String? = null // optional
    ) {
        fun toUri(): Uri {
            val cred = if (password.isNullOrEmpty()) "$username@" else "$username:$password@"
            return Uri.parse("ssh://" + cred + "$host:$port")
        }
        fun toCli(): String = buildString {
            append("ssh ")
            append("$username@$host")
            if (port != 22) append(" -p $port")
        }
    }

    /** Build Details using current device IP for hosting or a known server host. */
    fun currentDeviceAsServer(username: String = "user", port: Int = 22, password: String? = null): Details? {
        val ip = NetworkUtils.getPrimaryIp() ?: return null
        return Details(host = ip, port = port, username = username, password = password)
    }

    /** Try open Termius (or any SSH handler) using ssh:// URI, fallback to share sheet. */
    fun openInTermius(context: Context, details: Details) {
        val intent = Intent(Intent.ACTION_VIEW, details.toUri())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback share
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, details.toCli())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(share, "Share SSH command"))
            Toast.makeText(context, "No SSH app found. Shared SSH command.", Toast.LENGTH_SHORT).show()
        }
    }
}
