package ru.teoria.lab.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import ru.teoria.lab.audio.ThemePlayer
import ru.teoria.lab.audio.Timbre

/**
 * Целостность тем и правовая проверка: только общественное достояние
 * (композиторы, умершие до 1955 года), корректные нотные данные.
 */
class ThemesIntegrityTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun load() {
            TestContent.init()
        }
    }

    @Test
    fun atLeastThirtyThemesByAtLeastTwelveComposers() {
        assertTrue("тем: ${ThemeRepo.count()}", ThemeRepo.count() >= 30)
        assertTrue("композиторов: ${ThemeRepo.composerCount()}", ThemeRepo.composerCount() >= 12)
    }

    @Test
    fun everyThemeIsStructurallyValid() {
        val ids = mutableSetOf<String>()
        ThemeRepo.all().forEach { theme ->
            assertTrue("пустой id", theme.id.isNotBlank())
            assertTrue("дубликат id ${theme.id}", ids.add(theme.id))
            assertTrue("нет композитора ${theme.id}", theme.composer.isNotBlank())
            assertTrue("нет произведения ${theme.id}", theme.work.isNotBlank())
            assertTrue("нет эпохи ${theme.id}", theme.epoch.isNotBlank())
            assertTrue("мало нот ${theme.id}: ${theme.notes.size}", theme.notes.size >= 8)
            assertTrue("нет источника ${theme.id}", theme.source.isNotBlank())
            theme.notes.forEach { note ->
                assertTrue("midi ${note.midi} вне 36..96 в ${theme.id}", note.midi in 36..96)
                assertTrue("beats ${note.beats} в ${theme.id}", note.beats > 0.0)
            }
        }
    }

    @Test
    fun onlyPublicDomainComposers() {
        ThemeRepo.all().forEach { theme ->
            assertTrue("publicDomain=false в ${theme.id}", theme.publicDomain)
            val death = theme.deathYear
            assertNotNull("не разобраны годы жизни: ${theme.composer} «${theme.composerYears}»", death)
            assertTrue("${theme.composer} умер в $death — не PD", death!! < 1955)
        }
    }

    @Test
    fun yearParserHandlesFormats() {
        assertEquals(1827, parseDeathYear("1770–1827"))
        assertEquals(1893, parseDeathYear("1840-1893"))
        assertEquals(1750, parseDeathYear("1685 — 1750"))
        assertEquals(null, parseDeathYear("неизвестно"))
    }

    @Test
    fun melodyDurationIsReasonable() {
        ThemeRepo.all().forEach { theme ->
            assertTrue("короткая тема ${theme.id}", theme.durationBeats >= 4.0)
            assertTrue("слишком длинная тема ${theme.id}", theme.durationBeats <= 64.0)
        }
    }

    @Test
    fun playingEmptyOrMissingThemeDoesNotCrash() {
        // Пустой список нот — синтез возвращает пустой буфер, падения нет.
        val empty = ThemePlayer.buildMelody(emptyList(), bpm = 100, timbre = Timbre.PIANO, transposeSemitones = 0)
        assertEquals(0, empty.size)
        ThemePlayer.playNotes(emptyList())
        ThemePlayer.playAll(emptyList())

        // Тема с пустыми нотами тоже безопасна.
        val silent = MusicTheme(
            id = "silent", composer = "Тест", composerYears = "1900–1950",
            work = "Тишина", epoch = "XX век", publicDomain = true,
            source = "тест", notes = emptyList()
        )
        assertEquals(0, ThemePlayer.buildMelody(silent.notes, 100, Timbre.STRINGS, 0).size)
        ThemePlayer.play(silent)
    }

    @Test
    fun melodyBufferHasExpectedLength() {
        val theme = ThemeRepo.all().first { it.notes.size >= 8 }
        val bpm = 120
        val samples = ThemePlayer.buildMelody(theme.notes, bpm, Timbre.PIANO, 0)
        val expected = (theme.notes.sumOf { it.beats } * 60.0 / bpm * 44100).toInt()
        assertTrue("буфер ${samples.size} против $expected", Math.abs(samples.size - expected) < 2000)
        assertTrue(samples.any { it.toInt() != 0 })
        // транспозиция не меняет длину
        val transposed = ThemePlayer.buildMelody(theme.notes, bpm, Timbre.PLUCK, 3)
        assertEquals(samples.size, transposed.size)
    }

    @Test
    fun composersHaveMultipleThemesAvailable() {
        val composers = ThemeRepo.composers()
        assertTrue(composers.size >= 12)
        assertTrue(composers.all { ThemeRepo.byComposer(it).isNotEmpty() })
        val withSeveral = composers.count { ThemeRepo.byComposer(it).size >= 2 }
        assertTrue("композиторов с 2+ темами: $withSeveral", withSeveral >= 5)
    }
}
