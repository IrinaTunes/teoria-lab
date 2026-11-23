package ru.teoria.lab.audio

import ru.teoria.lab.core.MusicTheme
import ru.teoria.lab.core.ThemeNote
import ru.teoria.lab.core.midiToFrequency
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Тембры синтезатора для тем. */
enum class Timbre(val title: String) {
    PIANO("Фортепиано"),
    STRINGS("Струнные"),
    PLUCK("Щипковый")
}

/**
 * Проигрывание мелодических тем (нотных данных) через общий синтезатор.
 * Записей нет: звук строится из MIDI-номеров и длительностей.
 */
object ThemePlayer {
    private const val SAMPLE_RATE = 44100
    private val executor = Executors.newSingleThreadExecutor()

    fun play(
        theme: MusicTheme,
        bpm: Int = 104,
        timbre: Timbre = Timbre.PIANO,
        transposeSemitones: Int = 0,
        maxNotes: Int? = null
    ) {
        val notes = if (maxNotes != null) theme.notes.take(maxNotes) else theme.notes
        playNotes(notes, bpm, timbre, transposeSemitones)
    }

    /** Проигрывает все темы подряд (для кнопки «▶ все темы»). */
    fun playAll(themes: List<MusicTheme>, bpm: Int = 104, timbre: Timbre = Timbre.PIANO) {
        if (themes.isEmpty()) return
        executor.execute {
            for (theme in themes) {
                if (theme.notes.isEmpty()) continue
                playBuffer(buildMelody(theme.notes, bpm, timbre, 0))
                Thread.sleep(500)
            }
        }
    }

    /** Безопасно проигрывает список нот; пустой список — тишина, без падения. */
    fun playNotes(
        notes: List<ThemeNote>,
        bpm: Int = 104,
        timbre: Timbre = Timbre.PIANO,
        transposeSemitones: Int = 0
    ) {
        if (notes.isEmpty()) return
        val samples = buildMelody(notes, bpm, timbre, transposeSemitones)
        if (samples.isEmpty()) return
        executor.execute { playBuffer(samples) }
    }

    fun buildMelody(
        notes: List<ThemeNote>,
        bpm: Int,
        timbre: Timbre,
        transposeSemitones: Int
    ): ShortArray {
        if (notes.isEmpty()) return ShortArray(0)
        val beatSeconds = 60.0 / bpm.coerceAtLeast(30)
        var totalSamples = 0
        val segments = ArrayList<ShortArray>(notes.size)
        for (note in notes) {
            val seconds = (note.beats * beatSeconds).coerceAtLeast(0.05)
            val count = (SAMPLE_RATE * seconds).toInt().coerceAtLeast(1)
            val buffer = ShortArray(count)
            val freq = midiToFrequency(note.midi + transposeSemitones)
            for (i in 0 until count) {
                val t = i.toDouble() / SAMPLE_RATE
                val attack = when (timbre) {
                    Timbre.PIANO -> (i.toDouble() / (SAMPLE_RATE * 0.01)).coerceAtMost(1.0)
                    Timbre.STRINGS -> (i.toDouble() / (SAMPLE_RATE * 0.08)).coerceAtMost(1.0)
                    Timbre.PLUCK -> (i.toDouble() / (SAMPLE_RATE * 0.004)).coerceAtMost(1.0)
                }
                val decay = when (timbre) {
                    Timbre.PIANO -> exp(-2.0 * t / seconds.coerceAtLeast(0.05))
                    Timbre.STRINGS -> 0.75 + 0.25 * exp(-1.0 * t / seconds.coerceAtLeast(0.05))
                    Timbre.PLUCK -> exp(-4.5 * t / seconds.coerceAtLeast(0.05))
                }
                val fundamental = sin(2 * PI * freq * t)
                val harmonics = when (timbre) {
                    Timbre.PIANO -> 0.30 * sin(4 * PI * freq * t) + 0.12 * sin(6 * PI * freq * t)
                    Timbre.STRINGS -> 0.45 * sin(4 * PI * freq * t) + 0.18 * sin(6 * PI * freq * t)
                    Timbre.PLUCK -> 0.20 * sin(4 * PI * freq * t) + 0.35 * sin(6 * PI * freq * t)
                }
                val sample = (fundamental + harmonics) * 0.30 * attack * decay
                buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
            }
            segments.add(buffer)
            totalSamples += buffer.size
        }
        val out = ShortArray(totalSamples)
        var offset = 0
        for (segment in segments) {
            System.arraycopy(segment, 0, out, offset, segment.size)
            offset += segment.size
        }
        return out
    }

    private fun playBuffer(samples: ShortArray) {
        Synth.playSamples(samples)
    }
}
