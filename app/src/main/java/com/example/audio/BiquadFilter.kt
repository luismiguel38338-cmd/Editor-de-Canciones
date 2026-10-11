package com.example.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Representa la configuración del ecualizador de 3 bandas y ganancia.
 * - bassGainDb: Ganancia de graves (Low Shelf a 200 Hz), en dB [-12dB .. +12dB]
 * - midGainDb: Ganancia de medios (Peaking EQ a 1000 Hz), en dB [-12dB .. +12dB]
 * - trebleGainDb: Ganancia de agudos (High Shelf a 4000 Hz), en dB [-12dB .. +12dB]
 * - bassBoostPercent: Refuerzo adicional de graves [0 .. 100%]
 */
data class EqualizerConfig(
    val bassGainDb: Float = 0f,
    val midGainDb: Float = 0f,
    val trebleGainDb: Float = 0f,
    val bassBoostPercent: Int = 0
) {
    val isFlat: Boolean
        get() = bassGainDb == 0f && midGainDb == 0f && trebleGainDb == 0f && bassBoostPercent == 0
}

/**
 * Filtro IIR Biquad digital de precisión directa (Direct Form I / II).
 * Implementa las fórmulas del Audio EQ Cookbook de Robert Bristow-Johnson.
 */
class BiquadFilter {
    private var a0 = 1.0
    private var a1 = 0.0
    private var a2 = 0.0
    private var b0 = 1.0
    private var b1 = 0.0
    private var b2 = 0.0

    // Estados de retardo por canal (hasta 2 canales estéreo)
    private var x1L = 0.0
    private var x2L = 0.0
    private var y1L = 0.0
    private var y2L = 0.0

    private var x1R = 0.0
    private var x2R = 0.0
    private var y1R = 0.0
    private var y2R = 0.0

    fun reset() {
        x1L = 0.0
        x2L = 0.0
        y1L = 0.0
        y2L = 0.0
        x1R = 0.0
        x2R = 0.0
        y1R = 0.0
        y2R = 0.0
    }

    /**
     * Configura filtro Low-Shelf (para Graves).
     */
    fun configureLowShelf(sampleRate: Int, cutoffFreq: Double, gainDb: Double) {
        val a = Math.pow(10.0, gainDb / 40.0)
        val w0 = 2.0 * PI * cutoffFreq / sampleRate.coerceAtLeast(8000)
        val cosW0 = cos(w0)
        val sinW0 = sin(w0)
        val s = 1.0
        val alpha = sinW0 / 2.0 * sqrt((a + 1.0 / a) * (1.0 / s - 1.0) + 2.0)
        val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha

        val b0Raw = a * ((a + 1.0) - (a - 1.0) * cosW0 + twoSqrtAAlpha)
        val b1Raw = 2.0 * a * ((a - 1.0) - (a + 1.0) * cosW0)
        val b2Raw = a * ((a + 1.0) - (a - 1.0) * cosW0 - twoSqrtAAlpha)
        val a0Raw = (a + 1.0) + (a - 1.0) * cosW0 + twoSqrtAAlpha
        val a1Raw = -2.0 * ((a - 1.0) + (a + 1.0) * cosW0)
        val a2Raw = (a + 1.0) + (a - 1.0) * cosW0 - twoSqrtAAlpha

        normalizeCoefficients(b0Raw, b1Raw, b2Raw, a0Raw, a1Raw, a2Raw)
    }

    /**
     * Configura filtro Peaking EQ (para Medios).
     */
    fun configurePeaking(sampleRate: Int, centerFreq: Double, gainDb: Double, q: Double = 1.0) {
        val a = Math.pow(10.0, gainDb / 40.0)
        val w0 = 2.0 * PI * centerFreq / sampleRate.coerceAtLeast(8000)
        val cosW0 = cos(w0)
        val sinW0 = sin(w0)
        val alpha = sinW0 / (2.0 * q)

        val b0Raw = 1.0 + alpha * a
        val b1Raw = -2.0 * cosW0
        val b2Raw = 1.0 - alpha * a
        val a0Raw = 1.0 + alpha / a
        val a1Raw = -2.0 * cosW0
        val a2Raw = 1.0 - alpha / a

        normalizeCoefficients(b0Raw, b1Raw, b2Raw, a0Raw, a1Raw, a2Raw)
    }

    /**
     * Configura filtro High-Shelf (para Agudos).
     */
    fun configureHighShelf(sampleRate: Int, cutoffFreq: Double, gainDb: Double) {
        val a = Math.pow(10.0, gainDb / 40.0)
        val w0 = 2.0 * PI * cutoffFreq / sampleRate.coerceAtLeast(8000)
        val cosW0 = cos(w0)
        val sinW0 = sin(w0)
        val s = 1.0
        val alpha = sinW0 / 2.0 * sqrt((a + 1.0 / a) * (1.0 / s - 1.0) + 2.0)
        val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha

        val b0Raw = a * ((a + 1.0) + (a - 1.0) * cosW0 + twoSqrtAAlpha)
        val b1Raw = -2.0 * a * ((a - 1.0) + (a + 1.0) * cosW0)
        val b2Raw = a * ((a + 1.0) + (a - 1.0) * cosW0 - twoSqrtAAlpha)
        val a0Raw = (a + 1.0) - (a - 1.0) * cosW0 + twoSqrtAAlpha
        val a1Raw = 2.0 * ((a - 1.0) - (a + 1.0) * cosW0)
        val a2Raw = (a + 1.0) - (a - 1.0) * cosW0 - twoSqrtAAlpha

        normalizeCoefficients(b0Raw, b1Raw, b2Raw, a0Raw, a1Raw, a2Raw)
    }

    private fun normalizeCoefficients(
        b0R: Double, b1R: Double, b2R: Double,
        a0R: Double, a1R: Double, a2R: Double
    ) {
        val invA0 = if (a0R != 0.0) 1.0 / a0R else 1.0
        b0 = b0R * invA0
        b1 = b1R * invA0
        b2 = b2R * invA0
        a1 = a1R * invA0
        a2 = a2R * invA0
        a0 = 1.0
    }

    fun processSampleLeft(x: Double): Double {
        val y = b0 * x + b1 * x1L + b2 * x2L - a1 * y1L - a2 * y2L
        x2L = x1L
        x1L = x
        y2L = y1L
        y1L = y
        return y
    }

    fun processSampleRight(x: Double): Double {
        val y = b0 * x + b1 * x1R + b2 * x2R - a1 * y1R - a2 * y2R
        x2R = x1R
        x1R = x
        y2R = y1R
        y1R = y
        return y
    }
}

/**
 * Procesador de ecualización digital de 3 bandas en PCM 16-bit.
 */
class PcmEqualizer(sampleRate: Int) {
    private val bassFilter = BiquadFilter()
    private val midFilter = BiquadFilter()
    private val trebleFilter = BiquadFilter()
    private var currentSampleRate = sampleRate
    private var config = EqualizerConfig()

    init {
        updateFilters(currentSampleRate, config)
    }

    fun updateConfig(sampleRate: Int, newConfig: EqualizerConfig) {
        currentSampleRate = sampleRate
        config = newConfig
        updateFilters(sampleRate, newConfig)
    }

    private fun updateFilters(sampleRate: Int, cfg: EqualizerConfig) {
        val totalBassGain = cfg.bassGainDb.toDouble() + (cfg.bassBoostPercent * 0.12) // hasta +12dB boost adicional
        bassFilter.configureLowShelf(sampleRate, 220.0, totalBassGain)
        midFilter.configurePeaking(sampleRate, 1100.0, cfg.midGainDb.toDouble(), 1.0)
        trebleFilter.configureHighShelf(sampleRate, 4000.0, cfg.trebleGainDb.toDouble())
    }

    fun processPcm16(pcmBytes: ByteArray, channels: Int) {
        if (config.isFlat) return // Sin modificaciones si todos los valores están en 0

        val shortCount = pcmBytes.size / 2
        var byteIndex = 0

        for (i in 0 until shortCount) {
            val low = pcmBytes[byteIndex].toInt() and 0xFF
            val high = pcmBytes[byteIndex + 1].toInt()
            val sample = ((high shl 8) or low).toShort().toDouble()

            val isRightChannel = (channels == 2 && (i % 2 == 1))
            val filtered = if (isRightChannel) {
                var s = bassFilter.processSampleRight(sample)
                s = midFilter.processSampleRight(s)
                trebleFilter.processSampleRight(s)
            } else {
                var s = bassFilter.processSampleLeft(sample)
                s = midFilter.processSampleLeft(s)
                trebleFilter.processSampleLeft(s)
            }

            // Soft saturation / clamping a 16-bit
            val clamped = filtered.coerceIn(-32767.0, 32767.0).toInt().toShort()
            pcmBytes[byteIndex] = (clamped.toInt() and 0xFF).toByte()
            pcmBytes[byteIndex + 1] = ((clamped.toInt() shr 8) and 0xFF).toByte()

            byteIndex += 2
        }
    }
}
