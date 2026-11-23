package com.artillery.tdoa

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class HistoryActivity : AppCompatActivity() {
    private lateinit var tvHistory: TextView
    private lateinit var btnExportCsv: Button
    private lateinit var btnClear: Button
    private lateinit var btnBack: Button
    private lateinit var historyDb: HistoryDb

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        tvHistory = findViewById(R.id.tv_history)
        btnExportCsv = findViewById(R.id.btn_export_csv)
        btnClear = findViewById(R.id.btn_clear)
        btnBack = findViewById(R.id.btn_back)
        historyDb = HistoryDb(this)

        refreshText()

        btnExportCsv.setOnClickListener {
            val path = historyDb.exportToDownload()
            if (path.isNotEmpty()) {
                Toast.makeText(this, "导出成功：$path", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "无历史记录", Toast.LENGTH_SHORT).show()
            }
        }

        btnClear.setOnClickListener {
            historyDb.clear()
            refreshText()
            Toast.makeText(this, "已清空记录", Toast.LENGTH_SHORT).show()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun refreshText() {
        tvHistory.text = historyDb.readAll()
    }
}
