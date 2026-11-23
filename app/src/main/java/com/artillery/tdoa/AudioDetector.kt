package com.artillery.tdoa

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.max

class AudioDetector(private val ctx: Context) {
    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }

    var onTrigger: (() -> Unit)? = null
    private var audioRecord: AudioRecord? = null
    private var thread: Thread? = null
    private var run = false
    private val matcher = MfccDtwMatcher()
    private val threshold = 0.25

    fun start() {
        if (run) return
        run = true
        val bufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL, ENCODING, bufSize)
        audioRecord?.startRecording()

        thread = Thread {
            val buffer = ShortArray(bufSize)
            while (run) {
                val readLen = audioRecord?.read(buffer, 0, bufSize) ?: 0
                if (readLen > 0) {
                    val rms = calcRms(buffer, readLen)
                    if (rms > 0.08) {
                        val dist = matcher.match(buffer, readLen)
                        if (dist < threshold) {
                            onTrigger?.invoke()
                        }
                    }
                }
            }
        }
        thread?.start()
    }

    fun stop() {
        run = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        thread?.interrupt()
    }

    private fun calcRms(buf: ShortArray, len: Int): Double {
        var sum = 0.0
        for (i in 0 until len) {
            val v = buf[i].toDouble() / 32768.0
            sum += v * v
        }
        return kotlin.math.sqrt(sum / len)
    }
}
