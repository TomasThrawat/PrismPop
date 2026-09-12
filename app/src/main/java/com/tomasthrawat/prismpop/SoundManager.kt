package com.tomasthrawat.prismpop

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

/**
 * All sound effects are synthesized PCM16 sine tones at playback time — there
 * are no audio asset files in this project, mirroring the "everything
 * procedurally generated" approach used for the art (Engine MCP).
 */
object SoundManager {

    private const val SAMPLE_RATE = 44100
    var enabled: Boolean = true

    private fun tone(freqHz: Double, durationMs: Int, volume: Double = 0.3): ShortArray {
        val sampleCount = (SAMPLE_RATE * durationMs / 1000.0).toInt().coerceAtLeast(1)
        val fadeSamples = (sampleCount / 6).coerceAtLeast(1)
        val buffer = ShortArray(sampleCount)
        for (i in 0 until sampleCount) {
            val angle = 2.0 * PI * i * freqHz / SAMPLE_RATE
            var amp = volume
            if (i < fadeSamples) amp *= i.toDouble() / fadeSamples
            if (i > sampleCount - fadeSamples) amp *= (sampleCount - i).toDouble() / fadeSamples
            buffer[i] = (sin(angle) * amp * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun buildTrack(samples: ShortArray): AudioTrack {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(samples, 0, samples.size)
        return track
    }

    private fun playAsync(vararg tones: ShortArray) {
        if (!enabled) return
        thread(isDaemon = true) {
            for (samples in tones) {
                val track = buildTrack(samples)
                track.play()
                Thread.sleep(samples.size * 1000L / SAMPLE_RATE)
                track.release()
            }
        }
    }

    fun playSwap() = playAsync(tone(440.0, 90))
    fun playInvalidSwap() = playAsync(tone(160.0, 140, volume = 0.25))
    fun playMatch(comboIndex: Int) = playAsync(tone(560.0 + comboIndex * 90.0, 110))
    fun playLevelWin() = playAsync(tone(523.0, 120), tone(659.0, 120), tone(784.0, 120), tone(1047.0, 160))
    fun playLevelLose() = playAsync(tone(400.0, 150, volume = 0.28), tone(300.0, 150, volume = 0.28), tone(200.0, 200, volume = 0.28))
}
