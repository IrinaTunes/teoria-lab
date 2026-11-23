package ru.teoria.lab.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TheoryTest {

    @Test
    fun midiNumbersFollowConcertPitch() {
        assertEquals(60, Note(0, 0, 4).midi)
        assertEquals(69, Note(5, 0, 4).midi)
        assertEquals(61, Note(0, 1, 4).midi)
        assertEquals(72, Note(0, 0, 5).midi)
        assertEquals(40, Note(2, 0, 2).midi)
        assertEquals(76, Note(2, 0, 5).midi)
    }

    @Test
    fun noteNamesAreRussian() {
        assertEquals("фа-диез", Note(3, 1, 4).nameRu)
        assertEquals("фа-диез первой октавы", Note(3, 1, 4).fullRu)
        assertEquals("си-бемоль малой октавы", Note(6, -1, 3).fullRu)
        assertEquals("до первой октавы", Note(0, 0, 4).fullRu)
        assertEquals("соль♯", Note(4, 1, 4).shortRu)
    }

    @Test
    fun notesParseAndRoundTrip() {
        assertEquals(Note(0, 1, 4), Note.parse("C#4"))
        assertEquals(Note(6, -1, 3), Note.parse("Bb3"))
        assertEquals(Note(2, 0, 4), Note.fromMidi(64))
        assertEquals("до-диез", Note.fromMidi(61).nameRu)
        assertEquals("ре-бемоль", Note(1, -1, 4).nameRu)
    }

    @Test
    fun accidentalOrderIsCorrect() {
        assertEquals(listOf("фа", "до", "соль", "ре", "ля", "ми", "си"), SHARP_ORDER)
        assertEquals(listOf("си", "ми", "ля", "ре", "соль", "до", "фа"), FLAT_ORDER)
        assertEquals(SHARP_ORDER.reversed(), FLAT_ORDER)
    }

    @Test
    fun sharpKeysHaveExpectedCounts() {
        assertEquals(1, Keys.accidentals("соль"))
        assertEquals(2, Keys.accidentals("ре"))
        assertEquals(3, Keys.accidentals("ля"))
        assertEquals(4, Keys.accidentals("ми"))
        assertEquals(5, Keys.accidentals("си"))
        assertEquals(6, Keys.accidentals("фа-диез"))
        assertEquals(7, Keys.accidentals("до-диез"))
        assertEquals(0, Keys.accidentals("до"))
        assertEquals(listOf("фа"), Keys.signOrder("соль"))
        assertEquals(listOf("фа", "до", "соль"), Keys.signOrder("ля"))
    }

    @Test
    fun flatKeysHaveExpectedCounts() {
        assertEquals(1, Keys.accidentals("фа"))
        assertEquals(2, Keys.accidentals("си-бемоль"))
        assertEquals(3, Keys.accidentals("ми-бемоль"))
        assertEquals(4, Keys.accidentals("ля-бемоль"))
        assertEquals(5, Keys.accidentals("ре-бемоль"))
        assertEquals(6, Keys.accidentals("соль-бемоль"))
        assertEquals(7, Keys.accidentals("до-бемоль"))
        assertEquals(listOf("си", "ми", "ля"), Keys.signOrder("ми-бемоль"))
    }

    @Test
    fun allThirtyKeysExistInCodeAndData() {
        assertEquals(15, Keys.MAJOR.size)
        assertEquals(15, Keys.MINOR.size)
        assertEquals(30, Keys.ALL.size)
        assertEquals(7, Keys.SHARP_KEYS.size)
        assertEquals(7, Keys.FLAT_KEYS.size)
        assertEquals(3, Keys.byLatin("A")?.sharps)
        assertEquals(2, Keys.byLatin("Bb")?.flats)
        assertEquals("ля минор", Keys.MINOR.first().nameRu)
    }

    @Test
    fun intervalSemitoneTableIsExact() {
        val expected = mapOf(
            "м2" to 1, "б2" to 2, "м3" to 3, "б3" to 4,
            "ч4" to 5, "тритон" to 6, "ч5" to 7,
            "м6" to 8, "б6" to 9, "м7" to 10, "б7" to 11, "ч8" to 12
        )
        assertEquals(12, Intervals.BY_SEMITONES.size)
        expected.forEach { (name, semitones) ->
            assertEquals("semitones of $name", semitones, Intervals.semitonesOf(name))
            assertEquals("name of $semitones", name, Intervals.nameFor(semitones))
        }
        assertEquals("малая терция", Intervals.titleOf("м3"))
    }

    @Test
    fun everyIntervalExampleMatchesItsSemitoneCount() {
        Intervals.EXAMPLES.forEach { (name, pairs) ->
            val expected = Intervals.semitonesOf(name)!!
            pairs.forEach { (a, b) ->
                assertEquals("$name ${a.latin}–${b.latin}", expected, Intervals.semitonesBetween(a, b))
            }
        }
    }

    @Test
    fun intervalOptionsAlwaysContainAnswerWithFourDistinct() {
        Intervals.ORDER.forEachIndexed { index, name ->
            val options = Intervals.optionsFor(name, index.toLong())
            assertEquals(4, options.size)
            assertEquals(4, options.distinct().size)
            assertTrue(options.contains(name))
        }
    }

    @Test
    fun chordsUseRequiredSemitoneFormulas() {
        assertEquals(listOf(0, 4, 7), Chords.MAJOR)
        assertEquals(listOf(0, 3, 7), Chords.MINOR)
        assertEquals(listOf(0, 3, 6), Chords.DIMINISHED)
        assertEquals(listOf(0, 4, 8), Chords.AUGMENTED)
        assertEquals("мажор", Chords.typeByOffsets(listOf(0, 4, 7)))
        assertEquals("минор", Chords.typeByOffsets(listOf(7, 12, 15)))
        assertEquals("уменьшённый", Chords.typeByOffsets(listOf(0, 3, 6)))
        assertEquals("увеличенный", Chords.typeByOffsets(listOf(0, 4, 8)))
        assertNull(Chords.typeByOffsets(listOf(0, 5, 7)))
        assertEquals(listOf(60, 64, 67), Chords.midiNotes(60, "мажор"))
        assertEquals(listOf(57, 60, 64), Chords.midiNotes(57, "минор"))
        assertEquals(listOf(60, 64, 67, 70), Chords.midiFromOffsets(60, listOf(0, 4, 7, 10)))
    }

    @Test
    fun guitarTuningAndPositionsAreCorrect() {
        assertEquals(listOf(40, 45, 50, 55, 59, 64), Guitar.OPEN.toList())
        assertEquals(listOf("E2", "A2", "D3", "G3", "B3", "E4"), Guitar.STRING_NAMES)
        assertTrue(Guitar.positionsFor(40).contains(0 to 0))
        assertTrue(Guitar.positionsFor(69).contains(5 to 5))
        assertEquals(2, Guitar.positionsFor(69).size)
        assertEquals(4, Guitar.fretDistance(0 to 2, 0 to 6))
        assertNull(Guitar.fretDistance(0 to 2, 1 to 3))
        assertTrue(Guitar.chordShape(40, "мажор").isNotEmpty())
    }

    @Test
    fun violinStringsAreG3D4A4E5() {
        assertEquals(listOf(55, 62, 69, 76), Violin.OPEN.toList())
        assertEquals(listOf("G3", "D4", "A4", "E5"), Violin.STRING_NAMES)
        assertTrue(Violin.positionsFor(69).contains(2 to 0))
        assertTrue(Violin.positionsFor(64).contains(1 to 2))
    }

    @Test
    fun jsonParserHandlesContentShapes() {
        val json = parseJson("""{"a":[1,2.5,-3],"b":{"c":"строка"},"d":true,"e":null}""")
        assertEquals(3, json.array("a").size)
        assertEquals(2, json.array("a")[1].asInt())
        assertEquals("строка", json.opt("b")?.str("c"))
        assertTrue(json.bool("d"))
        assertNotNull(json.opt("e"))
        assertFalse(json.bool("f", true) == false)
    }
}

class ProgressTest {

    @Test
    fun levelThresholdsGrowLinearly() {
        assertEquals(0, Leveling.xpForLevelStart(1))
        assertEquals(100, Leveling.xpForLevelStart(2))
        assertEquals(300, Leveling.xpForLevelStart(3))
        assertEquals(600, Leveling.xpForLevelStart(4))
        assertEquals(19000, Leveling.xpForLevelStart(20))
    }

    @Test
    fun levelIsDerivedFromXp() {
        assertEquals(1, Leveling.levelForXp(0))
        assertEquals(1, Leveling.levelForXp(99))
        assertEquals(2, Leveling.levelForXp(100))
        assertEquals(2, Leveling.levelForXp(299))
        assertEquals(3, Leveling.levelForXp(300))
        assertEquals(20, Leveling.levelForXp(1_000_000))
        assertEquals(0f, Leveling.progressInLevel(0), 0.001f)
        assertEquals(0.25f, Leveling.progressInLevel(150), 0.001f)
        assertEquals(1f, Leveling.progressInLevel(1_000_000), 0.001f)
    }

    @Test
    fun xpAwardsTenOrFifteen() {
        assertEquals(10, Leveling.xpForAnswer(true, 0))
        assertEquals(10, Leveling.xpForAnswer(true, 2))
        assertEquals(15, Leveling.xpForAnswer(true, 3))
        assertEquals(15, Leveling.xpForAnswer(true, 7))
        assertEquals(0, Leveling.xpForAnswer(false, 5))
    }

    @Test
    fun srsIntervalSequenceAndReset() {
        assertEquals(1, SpacedRepetition.nextInterval(0, true))
        assertEquals(3, SpacedRepetition.nextInterval(1, true))
        assertEquals(7, SpacedRepetition.nextInterval(3, true))
        assertEquals(14, SpacedRepetition.nextInterval(7, true))
        assertEquals(30, SpacedRepetition.nextInterval(14, true))
        assertEquals(30, SpacedRepetition.nextInterval(30, true))
        assertEquals(1, SpacedRepetition.nextInterval(14, false))
        assertEquals(1, SpacedRepetition.nextInterval(0, false))
        assertEquals(listOf(1, 3, 7, 14, 30), SpacedRepetition.INTERVALS)
    }

    @Test
    fun srsDueAndPriority() {
        val now = 10_000_000L
        assertEquals(now + 3 * SpacedRepetition.DAY_MS, SpacedRepetition.nextDue(now, 3))
        assertTrue(SpacedRepetition.isDue(now - 1, now))
        assertFalse(SpacedRepetition.isDue(now + 1, now))
        val overdue = SpacedRepetition.priority(now - 5 * SpacedRepetition.DAY_MS, 0.5, now)
        val fresh = SpacedRepetition.priority(now + 5 * SpacedRepetition.DAY_MS, 0.9, now)
        assertTrue(overdue > fresh)
    }

    @Test
    fun masteryUsesWindowOfTen() {
        assertEquals(0.0, SpacedRepetition.mastery(emptyList()), 0.0001)
        assertEquals(0.3, SpacedRepetition.mastery(listOf(true, true, true)), 0.0001)
        val ten = List(10) { it < 8 }
        assertEquals(0.8, SpacedRepetition.mastery(ten), 0.0001)
        val fourteen = List(4) { false } + List(10) { true }
        assertEquals(1.0, SpacedRepetition.mastery(fourteen), 0.0001)
    }

    @Test
    fun rhythmTapsRespectTolerance() {
        assertTrue(Rhythm.matchesTap(1000, 1000))
        assertTrue(Rhythm.matchesTap(1120, 1000))
        assertTrue(Rhythm.matchesTap(880, 1000))
        assertFalse(Rhythm.matchesTap(1121, 1000))
        assertFalse(Rhythm.matchesTap(878, 1000))
        assertEquals(120L, Rhythm.TOLERANCE_MS)
    }

    @Test
    fun rhythmBeatsAndScoring() {
        assertEquals(500L, Rhythm.beatIntervalMs(120))
        val beats = Rhythm.beatTimes(120, 4, 1000)
        assertEquals(listOf(1000L, 1500L, 2000L, 2500L), beats)
        assertEquals(4, Rhythm.scoreTaps(listOf(1010, 1510, 1990, 2515), beats))
        assertEquals(2, Rhythm.scoreTaps(listOf(1010, 1700, 1990, 2900), beats))
        assertEquals(0.5, Rhythm.hitRate(listOf(1010, 1700, 1990, 2900), beats), 0.001)
    }

    @Test
    fun streakCountingFollowsCalendar() {
        val day1 = Days.dayKey(0)
        val day2 = Days.dayKey(SpacedRepetition.DAY_MS)
        assertEquals(1, Days.nextStreak(0, null, day1))
        assertEquals(2, Days.nextStreak(1, day1, day2))
        assertEquals(1, Days.nextStreak(5, day1, Days.dayKey(5 * SpacedRepetition.DAY_MS)))
        assertEquals(3, Days.nextStreak(3, day2, day2))
    }
}
