package com.artillery.tdoa

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TemplateManagerActivity : AppCompatActivity() {
    private lateinit var btnCapture: Button
    private lateinit var btnLoad: Button
    private lateinit var etThreshold: EditText
    private lateinit var btnSetThresh: Button
    private lateinit var btnBack: Button
    private val matcher = MfccDtwMatcher()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_template_manager)

        btnCapture = findViewById(R.id.btn_capture)
        btnLoad = findViewById(R.id.btn_load)
        etThreshold = findViewById(R.id.et_threshold)
        btnSetThresh = findViewById(R.id.btn_set_thresh)
        btnBack = findViewById(R.id.btn_back)

        btnCapture.setOnClickListener { captureTemplate() }

        btnLoad.setOnClickListener {
            val ok = matcher.loadTemplate()
            if (ok) {
                Toast.makeText(this, "模板加载成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "模板文件不存在", Toast.LENGTH_SHORT).show()
            }
        }

        btnSetThresh.setOnClickListener {
            val str = etThreshold.text.toString().toDoubleOrNull()
            if (str == null || str <= 0.0) {
                Toast.makeText(this, "阈值非法", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            matcher.setThreshold(str)
            Toast.makeText(this, "阈值已设置：$str", Toast.LENGTH_SHORT).show()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun captureTemplate() {
        val bufSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 4
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufSize
        )
        val buf = ShortArray(16000 * 2)
        recorder.startRecording()
        var idx = 0
        while (idx < buf.size) {
            val r = recorder.read(buf, idx, buf.size - idx)
            if (r <= 0) break
            idx += r
        }
        recorder.stop()
        recorder.release()
        matcher.recordTemplate(buf, idx)
        Toast.makeText(this, "模板录制完成（VAD自动裁剪）", Toast.LENGTH_SHORT).show()
    }
}
