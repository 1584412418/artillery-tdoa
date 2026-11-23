package com.artillery.tdoa

import android.os.Handler
import android.os.Looper
import java.net.InetAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NtpTimeSync {
    var onResult: ((ok: Boolean, ts: Long?) -> Unit)? = null
    private val handler = Handler(Looper.getMainLooper())
    private val ntpServer = "pool.ntp.org"

    fun sync() {
        Thread {
            try {
                val socket = Socket(InetAddress.getByName(ntpServer), 123)
                socket.close()
                val time = System.currentTimeMillis()
                handler.post { onResult?.invoke(true, time) }
            } catch (e: Exception) {
                handler.post { onResult?.invoke(false, null) }
            }
        }.start()
    }

    fun formatTime(ts: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return sdf.format(Date(ts))
    }
}
