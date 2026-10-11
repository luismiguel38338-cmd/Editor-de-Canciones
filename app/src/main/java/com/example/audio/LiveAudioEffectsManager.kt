package com.example.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.util.Log

class LiveAudioEffectsManager {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var currentSessionId: Int = 0

    fun attachToSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == currentSessionId) return
        release()
        currentSessionId = audioSessionId

        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
        } catch (e: Exception) {
            Log.w("LiveAudioEffects", "No se pudo instanciar Equalizer para sesión $audioSessionId: ${e.message}")
            equalizer = null
        }

        try {
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = true
            }
        } catch (e: Exception) {
            Log.w("LiveAudioEffects", "No se pudo instanciar BassBoost para sesión $audioSessionId: ${e.message}")
            bassBoost = null
        }
    }

    fun applyConfig(config: EqualizerConfig) {
        // 1. Equalizer bands
        equalizer?.let { eq ->
            try {
                val numBands = eq.numberOfBands.toInt()
                val minLevel = eq.bandLevelRange[0]
                val maxLevel = eq.bandLevelRange[1]

                for (band in 0 until numBands) {
                    val centerFreqHz = eq.getCenterFreq(band.toShort()) / 1000 // Convertir a Hz

                    val targetGainDb = when {
                        centerFreqHz < 350 -> config.bassGainDb
                        centerFreqHz in 350..2500 -> config.midGainDb
                        else -> config.trebleGainDb
                    }

                    // Convertir dB a milibelios (1 dB = 100 mB)
                    val targetMb = (targetGainDb * 100f).toInt().toShort()
                    val clampedMb = targetMb.coerceIn(minLevel, maxLevel)
                    eq.setBandLevel(band.toShort(), clampedMb)
                }
            } catch (e: Exception) {
                Log.w("LiveAudioEffects", "Error aplicando bandas de EQ: ${e.message}")
            }
        }

        // 2. Bass Boost strength (0 .. 1000)
        bassBoost?.let { bb ->
            try {
                if (bb.strengthSupported) {
                    val strength = (config.bassBoostPercent * 10).coerceIn(0, 1000).toShort()
                    bb.setStrength(strength)
                }
            } catch (e: Exception) {
                Log.w("LiveAudioEffects", "Error aplicando Bass Boost: ${e.message}")
            }
        }
    }

    fun release() {
        try {
            equalizer?.release()
        } catch (_: Exception) {}
        equalizer = null

        try {
            bassBoost?.release()
        } catch (_: Exception) {}
        bassBoost = null

        currentSessionId = 0
    }
}
