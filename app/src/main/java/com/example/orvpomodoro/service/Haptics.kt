package com.example.orvpomodoro.service

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.orvpomodoro.model.Pulse
import com.example.orvpomodoro.model.VibrationLevel

object Haptics {

    /** El patrón completo del nivel (fin de fase). */
    fun play(context: Context, level: VibrationLevel) = playPulses(context, level.pulses)

    /** Solo el primer pulso del nivel: para las marcas a mitad de camino. */
    fun tick(context: Context, level: VibrationLevel) = playPulses(context, level.pulses.take(1))

    @Suppress("DEPRECATION")
    private fun playPulses(context: Context, pulses: List<Pulse>) {
        if (pulses.isEmpty()) return
        val vibrator = vibratorOf(context) ?: return
        if (!vibrator.hasVibrator()) return

        // Formato de Android: [pausa, vibra, pausa, vibra, ...] y una fuerza por cada tramo
        val timings = ArrayList<Long>().apply { add(0L) }
        val amplitudes = ArrayList<Int>().apply { add(0) }
        pulses.forEachIndexed { i, pulse ->
            timings.add(pulse.ms)
            amplitudes.add(pulse.strength.coerceIn(1, 255))
            if (i < pulses.lastIndex) {
                timings.add(pulse.gapAfterMs)
                amplitudes.add(0)
            }
        }
        val t = timings.toLongArray()
        val a = amplitudes.toIntArray()

        if (Build.VERSION.SDK_INT >= 26) {
            val effect = if (vibrator.hasAmplitudeControl()) {
                VibrationEffect.createWaveform(t, a, -1)
            } else {
                VibrationEffect.createWaveform(t, -1)   // sin control de fuerza: solo cuenta la duración
            }
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            vibrator.vibrate(effect, attributes)
        } else {
            vibrator.vibrate(t, -1)
        }
    }

    @Suppress("DEPRECATION")
    private fun vibratorOf(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
}