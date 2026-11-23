package ru.teoria.lab.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class QuizGenerationTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun load() {
            TestContent.init()
        }

        private fun topicOf(kind: ExerciseKind): TopicDef =
            ContentRepo.allTopics().first { it.kind == kind }
    }

    @Test
    fun generationIsDeterministicBySeed() {
        assertTrue(ContentRepo.allTopics().size >= 180)
        ContentRepo.allTopics().take(30).forEach { topic ->
            val a = QuizGenerator.generate(topic, 42L, "piano")
            val b = QuizGenerator.generate(topic, 42L, "piano")
            assertEquals("topic ${topic.id}", a, b)
        }
    }

    @Test
    fun findNoteTargetsArePlayableOnEachInstrument() {
        val topic = topicOf(ExerciseKind.FIND_NOTE)
        listOf("piano", "guitar", "violin", "vocal", "universal").forEach { instrument ->
            for (seed in 1L..60L) {
                val q = QuizGenerator.generate(topic, seed, instrument) as Question.FindNote
                val midi = q.target.midi
                assertTrue("$instrument $midi", QuizGenerator.playableMidi(instrument).contains(midi))
                if (instrument == "guitar") assertTrue(Guitar.positionsFor(midi).isNotEmpty())
                if (instrument == "violin") assertTrue(Violin.positionsFor(midi).isNotEmpty())
            }
        }
    }

    fun correctKeyAnswer(q: Question.KeySigns): String =
        if (q.askCount) signLabel(q.key.signCount, q.key.hasSharps) else q.key.nameRu

    @Test
    fun keySignQuestionsAreValidAndCoverAllThirtyKeys() {
        val topic = topicOf(ExerciseKind.KEY_SIGNS)
        val seen = mutableSetOf<String>()
        for (seed in 1L..400L) {
            val q = QuizGenerator.generate(topic, seed, "piano") as Question.KeySigns
            assertEquals(4, q.options.size)
            assertEquals("дубликаты ${q.options}", 4, q.options.distinct().size)
            assertTrue("${q.prompt} → ответа нет в ${q.options}", q.options.contains(correctKeyAnswer(q)))
            seen.add(q.key.latin)
        }
        assertEquals("тональностей в вопросах: $seen", 30, seen.size)
        assertTrue(seen.any { it.endsWith("m") })
    }

    @Test
    fun intervalQuestionsMatchGeneratorTableAndCoverAll() {
        val topic = topicOf(ExerciseKind.VISUAL_INTERVAL)
        val specs = ContentRepo.generators().intervals.associateBy { it.name }
        val seen = mutableSetOf<String>()
        for (seed in 1L..500L) {
            val q = QuizGenerator.generate(topic, seed, "piano")
            val (low, high, answer) = when (q) {
                is Question.EarInterval -> Triple(q.low, q.high, q.answer)
                is Question.VisualInterval -> Triple(q.low, q.high, q.answer)
                else -> throw AssertionError("unexpected ${q.kind}")
            }
            val options = when (q) {
                is Question.EarInterval -> q.options
                is Question.VisualInterval -> q.options
                else -> emptyList()
            }
            val spec = specs[answer]
            assertNotNull("нет спеки для $answer", spec)
            assertEquals("полутоны $answer", spec!!.semitones, Intervals.semitonesBetween(low, high))
            assertTrue("нет ответа в вариантах", options.contains(answer))
            assertEquals(4, options.size)
            assertTrue("midi вне диапазона", low.midi in 30..90 && high.midi in 30..90)
            seen.add(answer)
        }
        assertEquals("покрытие интервалов: $seen", specs.size, seen.size)
    }

    @Test
    fun chordQuestionsUseGeneratorSpecsAndCoverAll() {
        val topic = topicOf(ExerciseKind.CHORD_EAR)
        val specs = ContentRepo.generators().chords.associateBy { it.name }
        val seen = mutableSetOf<String>()
        for (seed in 1L..400L) {
            val q = QuizGenerator.generate(topic, seed, "piano") as Question.ChordEar
            val spec = specs[q.type]
            assertNotNull("неизвестный аккорд ${q.type}", spec)
            assertEquals("смещения ${q.type}", spec!!.offsets, q.offsets)
            assertEquals(4, q.options.size)
            assertEquals(4, q.options.distinct().size)
            assertTrue(q.options.contains(q.type))
            seen.add(q.type)
        }
        assertEquals("покрытие аккордов: $seen", specs.size, seen.size)
        assertTrue(seen.containsAll(listOf("D7", "D65", "D43", "D2", "VII7", "умVII7")))
    }

    @Test
    fun rhythmQuestionsUseConfiguredMeters() {
        val topic = topicOf(ExerciseKind.RHYTHM)
        val meters = ContentRepo.generators().meters.toSet()
        val seen = mutableSetOf<String>()
        for (seed in 1L..300L) {
            val q = QuizGenerator.generate(topic, seed, "piano") as Question.RhythmTask
            assertTrue("размер ${q.meter}", meters.contains(q.meter))
            assertTrue(q.bpm in 40..200)
            assertTrue(q.beats >= 2)
            seen.add(q.meter)
        }
        assertEquals("покрытие размеров: $seen", meters.size, seen.size)
    }

    @Test
    fun literatureQuestionsAlwaysHaveAnswerAmongFourOptions() {
        ContentRepo.allTopics().filter { it.kind == ExerciseKind.LITERATURE }.forEach { topic ->
            for (seed in 1L..25L) {
                val q = QuizGenerator.generate(topic, seed, "piano") as Question.Lit
                assertEquals("дубликаты ${q.options}", 4, q.options.distinct().size)
                assertTrue("${q.prompt} → ${q.answer} нет в ${q.options}", q.options.contains(q.answer))
                assertEquals(q.answer, q.correctLabel())
            }
        }
    }

    @Test
    fun listenQuestionsReferenceRealThemesAndBothModes() {
        val modes = mutableSetOf<String>()
        ContentRepo.allTopics().filter { it.kind == ExerciseKind.LISTEN }.forEach { topic ->
            for (seed in 1L..40L) {
                val q = QuizGenerator.generate(topic, seed, "piano") as Question.Listen
                val theme = ThemeRepo.byId(q.themeId)
                assertNotNull("тема ${q.themeId} не найдена", theme)
                assertEquals(4, q.options.size)
                assertEquals(4, q.options.distinct().size)
                assertTrue(q.options.contains(q.answer))
                modes.add(q.mode)
            }
        }
        assertTrue("режимы: $modes", modes.containsAll(listOf("composer", "work")))
    }

    @Test
    fun everyTopicProducesAQuestion() {
        ContentRepo.allTopics().forEach { topic ->
            val q = QuizGenerator.generate(topic, 7L, "guitar")
            assertEquals(topic.kind, q.kind)
            assertTrue(q.prompt.isNotBlank())
        }
    }
}
