package com.artillery.tdoa

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class HistoryDb(private val ctx: Context) {
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun addRecord(x: Double, y: Double, ts: Long) {
        val f = File(ctx.filesDir, "history.csv")
        FileWriter(f, true).use {
            it.append("${sdf.format(ts)},$x,$y\n")
        }
    }

    fun readAll(): String {
        val f = File(ctx.filesDir, "history.csv")
        if (!f.exists()) return "暂无记录"
        return f.readText()
    }

    fun clear() {
        File(ctx.filesDir, "history.csv").delete()
    }

    fun exportToDownload(): String {
        val src = File(ctx.filesDir, "history.csv")
        if (!src.exists()) return ""
        val dst = File(ctx.getExternalFilesDir(null), "tdoa_history.csv")
        src.copyTo(dst, overwrite = true)
        return dst.absolutePath
    }
}
