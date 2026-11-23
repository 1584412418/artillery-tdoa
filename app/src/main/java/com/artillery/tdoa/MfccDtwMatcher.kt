package com.artillery.tdoa

import java.io.File
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.math.*

class MfccDtwMatcher {
    private var templateFeature: Array<DoubleArray>? = null
    private var threshold = 0.25

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val FRAME_LEN = 400
        private const val FRAME_STEP = 160
        private const val MEL_FILTER_NUM = 24
        private const val MFCC_DIM = 12
        private const val PRE_EMPH = 0.97
        private const val VAD_ENERGY_THRESH = 0.0008
        private const val VAD_SILENCE_FRAME = 5
    }

    private val templateFile = File("template.bin")

    fun setThreshold(v: Double) {
        threshold = v
    }

    fun recordTemplate(pcm: ShortArray, len: Int) {
        val feat = extractMfcc(pcm, len)
        templateFeature = feat
        saveTemplate()
    }

    private fun saveTemplate() {
        ObjectOutputStream(FileOutputStream(templateFile)).use {
            it.writeObject(templateFeature)
        }
    }

    fun loadTemplate(): Boolean {
        return try {
            ObjectInputStream(templateFile.inputStream()).use {
                templateFeature = it.readObject() as Array<DoubleArray>
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun match(pcm: ShortArray, length: Int): Double {
        val tpl = templateFeature ?: return 9999.0
        val feat = extractMfcc(pcm, length)
        if (feat.isEmpty()) return 9999.0
        return dtw(tpl, feat)
    }

    fun extractMfcc(pcm: ShortArray, len: Int): Array<DoubleArray> {
        val x = DoubleArray(len)
        for (n in 0 until len) {
            val s = pcm[n].toDouble() / 32768.0
            x[n] = if (n == 0) s else s - PRE_EMPH * x[n - 1]
        }

        val rawMfccList = mutableListOf<DoubleArray>()
        var start = 0
        var silenceCnt = 0
        var hasVoice = false

        while (start + FRAME_LEN <= len) {
            val frame = DoubleArray(FRAME_LEN)
            var energy = 0.0
            for (i in 0 until FRAME_LEN) {
                val v = x[start + i] * hannWindow(i, FRAME_LEN)
                frame[i] = v
                energy += v * v
            }
            energy /= FRAME_LEN

            if (energy > VAD_ENERGY_THRESH) {
                hasVoice = true
                silenceCnt = 0
                val fft = fftReal(frame)
                val powSpec = DoubleArray(FRAME_LEN / 2 + 1)
                for (k in powSpec.indices) {
                    powSpec[k] = fft[k].re * fft[k].re + fft[k].im * fft[k].im
                }
                val melBank = melFilterBank(powSpec, MEL_FILTER_NUM, SAMPLE_RATE)
                val logMel = melBank.map { ln(max(it, 1e-10)) }.toDoubleArray()
                val mfcc = dct(logMel, MFCC_DIM)
                rawMfccList.add(mfcc)
            } else {
                if (hasVoice) silenceCnt++
                if (silenceCnt >= VAD_SILENCE_FRAME) break
            }
            start += FRAME_STEP
        }

        if (rawMfccList.isEmpty()) return emptyArray()
        return cmvn(rawMfccList.toTypedArray())
    }

    private fun cmvn(feat: Array<DoubleArray>): Array<DoubleArray> {
        val dim = feat[0].size
        val mean = DoubleArray(dim)
        val std = DoubleArray(dim)
        val n = feat.size

        for (d in 0 until dim) {
            var sum = 0.0
            for (f in feat) sum += f[d]
            mean[d] = sum / n
        }
        for (d in 0 until dim) {
            var s = 0.0
            for (f in feat) s += (f[d] - mean[d]) * (f[d] - mean[d])
            std[d] = sqrt(max(s / n, 1e-10))
        }
        return Array(n) { fIdx ->
            DoubleArray(dim) { dIdx ->
                (feat[fIdx][dIdx] - mean[dIdx]) / std[dIdx]
            }
        }
    }

    private fun hannWindow(n: Int, N: Int): Double {
        return 0.5 * (1 - cos(2 * PI * n / (N - 1)))
    }

    private fun fftReal(x: DoubleArray): Array<Complex> {
        val n = 1 shl ceil(log2(x.size.toDouble())).toInt()
        val src = Array(n) { Complex(0.0, 0.0) }
        for (i in x.indices) src[i] = Complex(x[i], 0.0)
        fft(src, false)
        return src
    }

    private fun fft(a: Array<Complex>, invert: Boolean) {
        val n = a.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j >= bit) {
                j -= bit
                bit = bit shr 1
            }
            j += bit
            if (i < j) {
                val tmp = a[i]
                a[i] = a[j]
                a[j] = tmp
            }
        }
        var len = 2
        while (len <= n) {
            val ang = 2 * PI / len * if (invert) -1 else 1
            val wlen = Complex(cos(ang), sin(ang))
            var i = 0
            while (i < n) {
                var w = Complex(1.0, 0.0)
                for (j in 0 until len / 2) {
                    val u = a[i + j]
                    val v = a[i + j + len / 2] * w
                    a[i + j] = u + v
                    a[i + j + len / 2] = u - v
                    w *= wlen
                }
                i += len
            }
            len *= 2
        }
        if (invert) {
            for (i in 0 until n) a[i] /= n
        }
    }

    private data class Complex(var re: Double, var im: Double) {
        operator fun plus(b: Complex) = Complex(re + b.re, im + b.im)
        operator fun minus(b: Complex) = Complex(re - b.re, im - b.im)
        operator fun times(b: Complex) = Complex(re * b.re - im * b.im, re * b.im + im * b.re)
        operator fun div(s: Double) = Complex(re / s, im / s)
    }

    private fun melFilterBank(powSpec: DoubleArray, filterNum: Int, fs: Int): DoubleArray {
        val fftBins = powSpec.size
        val melMin = hz2mel(0.0)
        val melMax = hz2mel(fs / 2.0)
        val melPoints = DoubleArray(filterNum + 2)
        for (i in melPoints.indices) {
            melPoints[i] = melMin + i * (melMax - melMin) / (filterNum + 1)
        }
        val hzPoints = melPoints.map { mel2hz(it) }.toDoubleArray()
        val binPoints = hzPoints.map { it / fs * (fftBins - 1) }.map { round(it).toInt() }

        val out = DoubleArray(filterNum)
        for (m in 0 until filterNum) {
            var sum = 0.0
            val l = binPoints[m]
            val c = binPoints[m + 1]
            val r = binPoints[m + 2]
            for (k in l..r) {
                if (k >= fftBins) break
                val weight = when {
                    k < c -> (k - l).toDouble() / (c - l)
                    else -> (r - k).toDouble() / (r - c)
                }
                sum += powSpec[k] * weight
            }
            out[m] = sum
        }
        return out
    }

    private fun hz2mel(hz: Double): Double = 2595.0 * log10(1 + hz / 700.0)
    private fun mel2hz(mel: Double): Double = 700.0 * (10.0.pow(mel / 2595.0) - 1)

    private fun dct(x: DoubleArray, dim: Int): DoubleArray {
        val n = x.size
        val res = DoubleArray(dim)
        for (k in 0 until dim) {
            var sum = 0.0
            for (i in 0 until n) {
                sum += x[i] * cos(PI * (i + 0.5) * k / n)
            }
            res[k] = sum * sqrt(2.0 / n)
        }
        return res
    }

    private fun dtw(a: Array<DoubleArray>, b: Array<DoubleArray>): Double {
        val n = a.size
        val m = b.size
        if (n == 0 || m == 0) return 9999.0
        val d = Array(n) { DoubleArray(m) { 1e18 } }
        d[0][0] = dist(a[0], b[0])
        for (i in 0 until n) {
            for (j in 0 until m) {
                if (i == 0 && j == 0) continue
                var minCost = 1e18
                if (i > 0) minCost = min(minCost, d[i - 1][j])
                if (j > 0) minCost = min(minCost, d[i][j - 1])
                if (i > 0 && j > 0) minCost = min(minCost, d[i - 1][j - 1])
                d[i][j] = dist(a[i], b[j]) + minCost
            }
        }
        return d[n - 1][m - 1] / (n + m)
    }

    private fun dist(x: DoubleArray, y: DoubleArray): Double {
        var s = 0.0
        for (k in x.indices) s += (x[k] - y[k]) * (x[k] - y[k])
        return sqrt(s)
    }
}
