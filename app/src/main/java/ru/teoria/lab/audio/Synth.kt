package ru.teoria.lab.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import ru.teoria.lab.core.midiToFrequency
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Синтез звука через AudioTrack: синусоида с огибающей, без внешних файлов.
 * Все вызовы выполняются в одном фоновом потоке, чтобы звуки не накладывались.
 */
object Synth {
    private const val SAMPLE_RATE = 44100
    private val executor = Executors.newSingleThreadExecutor()

    fun playNote(midi: Int, durationMs: Int = 600, gain: Double = 0.35) {
        playChord(listOf(midi), durationMs, gain)
    }

    fun playSequence(midis: List<Int>, noteMs: Int = 480, gapMs: Int = 70) {
        executor.execute {
            for (m in midis) {
                playBuffer(buildTone(midiToFrequency(m), noteMs / 1000.0, 0.35))
                Thread.sleep(gapMs.toLong())
            }
        }
    }

    fun playChord(midis: List<Int>, durationMs: Int = 900, gain: Double = 0.28) {
        executor.execute {
            playBuffer(buildChord(midis.map { midiToFrequency(it) }, durationMs / 1000.0, gain))
        }
    }

    /** Проигрывает готовый PCM-буфер (используется проигрывателем тем). */
    fun playSamples(samples: ShortArray) {
        if (samples.isEmpty()) return
        executor.execute { playBuffer(samples) }
    }

    /** Клик метронома. */
    fun click(accent: Boolean = false) {
        executor.execute {
            playBuffer(buildTone(if (accent) 1600.0 else 1200.0, 0.05, 0.4, attackMs = 1))
        }
    }

    fun playCadence(bpm: Int, beats: Int) {
        executor.execute {
            for (i in 0 until beats) {
                playBuffer(buildTone(if (i == 0) 1600.0 else 1200.0, 0.05, 0.4, attackMs = 1))
                Thread.sleep((60_000.0 / bpm).toLong())
            }
        }
    }

    private fun buildTone(freq: Double, seconds: Double, gain: Double, attackMs: Int = 15): ShortArray {
        val count = (SAMPLE_RATE * seconds).toInt().coerceAtLeast(1)
        val attack = (SAMPLE_RATE * attackMs / 1000.0).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            val attackEnv = if (i < attack) i.toDouble() / attack else 1.0
            val decay = exp(-3.2 * t / seconds.coerceAtLeast(0.01))
            val sample = sin(2 * PI * freq * t) * gain * attackEnv * decay
            out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
        }
        return out
    }

    private fun buildChord(freqs: List<Double>, seconds: Double, gain: Double): ShortArray {
        val count = (SAMPLE_RATE * seconds).toInt().coerceAtLeast(1)
        val attack = (SAMPLE_RATE * 0.015).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        val perVoice = gain / freqs.size.coerceAtLeast(1)
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            val attackEnv = if (i < attack) i.toDouble() / attack else 1.0
            val decay = exp(-2.2 * t / seconds.coerceAtLeast(0.01))
            var sum = 0.0
            for (f in freqs) {
                sum += sin(2 * PI * f * t)
                // мягкая вторая гармоника для «фортепианного» тембра
                sum += 0.18 * sin(4 * PI * f * t)
            }
            val sample = sum * perVoice * attackEnv * decay
            out[i] = (sample * Short.MAX_VALUE / 1.2).toInt().coerceIn(-32768, 32767).toShort()
        }
        return out
    }

    private fun playBuffer(samples: ShortArray) {
        var track: AudioTrack? = null
        try {
            val bufferSize = samples.size * 2
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(bufferSize)
                .build()
            track.write(samples, 0, samples.size)
            track.play()
            Thread.sleep((samples.size * 1000L / SAMPLE_RATE) + 40L)
            track.stop()
        } catch (t: Throwable) {
            // Звук не должен ломать упражнение (например, нет аудиоустройства).
        } finally {
            try {
                track?.release()
            } catch (_: Throwable) {
            }
        }
    }
}
