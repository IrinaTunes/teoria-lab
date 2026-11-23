package ru.teoria.lab.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * Полнота контента. Тест падает, если в JSON не хватает разделов, тем, карточек
 * или параметров генерации.
 */
class ContentCoverageTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun load() {
            TestContent.init()
        }
    }

    @Test
    fun theoryHasTwelveSectionsAndAtLeast120Topics() {
        val theory = ContentRepo.course(Course.THEORY)
        assertEquals(12, theory.sections.size)
        assertTrue("тем теории: ${theory.topicCount}", theory.topicCount >= 120)
        theory.sections.forEach { section ->
            assertTrue("${section.title}: ${section.topics.size}", section.topics.size >= 8)
            assertTrue(section.title.isNotBlank())
        }
    }

    @Test
    fun solfegeHasNineSectionsAndAtLeast60Topics() {
        val solfege = ContentRepo.course(Course.SOLFEGGIO)
        assertEquals(9, solfege.sections.size)
        assertTrue("тем сольфеджио: ${solfege.topicCount}", solfege.topicCount >= 60)
        solfege.sections.forEach { section ->
            assertTrue("${section.title}: ${section.topics.size}", section.topics.size >= 6)
        }
    }

    @Test
    fun everyTopicHasSummaryTextAndLinkedSection() {
        val all = ContentRepo.allTopics()
        assertTrue(all.size >= 180)
        assertEquals("дубликаты id", all.size, all.map { it.id }.distinct().size)
        all.forEach { topic ->
            assertTrue("пустой id", topic.id.isNotBlank())
            assertTrue("пустой заголовок ${topic.id}", topic.title.isNotBlank())
            assertTrue("пустой конспект ${topic.id}", topic.text.length >= 40)
            assertTrue("нет раздела ${topic.id}", topic.sectionId.isNotBlank())
            assertTrue("нет привязки к разделу ${topic.id}", topic.sectionTitle.isNotBlank())
            assertNotNull("нет типа упражнения ${topic.id}", topic.kind)
        }
    }

    @Test
    fun theorySectionsCoverRequiredProgram() {
        val titles = ContentRepo.course(Course.THEORY).sections.map { it.title.lowercase() }
        listOf(
            "звук", "нотация", "ритм", "лад", "интервал", "аккорд",
            "гармони", "мелоди", "форм", "транспозиц", "инструментов", "терминологи"
        ).forEach { required ->
            assertTrue("нет раздела про $required: $titles", titles.any { it.contains(required) })
        }
    }

    @Test
    fun solfegeSectionsCoverRequiredProgram() {
        val titles = ContentRepo.course(Course.SOLFEGGIO).sections.map { it.title.lowercase() }
        listOf("интонац", "гамм", "ступен", "интервал", "аккорд", "ритм", "слухов", "листа", "двухголос")
            .forEach { required ->
                assertTrue("нет раздела про $required: $titles", titles.any { it.contains(required) })
            }
    }

    @Test
    fun literatureHasAtLeast200CardsAndRequiredFacts() {
        val cards = ContentRepo.cards()
        assertTrue("карточек: ${cards.size}", cards.size >= 200)
        assertTrue(cards.all { it.composer.isNotBlank() && it.work.isNotBlank() && it.epoch.isNotBlank() })

        val composers = cards.map { it.composer }.toSet()
        listOf(
            "Иоганн Себастьян Бах", "Георг Фридрих Гендель", "Йозеф Гайдн", "Вольфганг Амадей Моцарт",
            "Людвиг ван Бетховен", "Франц Шуберт", "Фридерик Шопен", "Ференц Лист", "Рихард Вагнер",
            "Джузеппе Верди", "Жорж Бизе", "Иоганнес Брамс", "Клод Дебюсси", "Морис Равель",
            "Игорь Стравинский", "Михаил Глинка", "Модест Мусоргский", "Пётр Ильич Чайковский",
            "Сергей Рахманинов", "Сергей Прокофьев", "Дмитрий Шостакович", "Георгий Свиридов"
        ).forEach { required ->
            assertTrue("нет композитора $required", composers.contains(required))
        }

        val epochs = cards.map { it.epoch }.toSet()
        listOf("Средневековье", "Возрождение", "Барокко", "Классицизм", "Романтизм", "Импрессионизм", "XX век")
            .forEach { assertTrue("нет эпохи $it", epochs.contains(it)) }

        val genres = cards.map { it.genre }.toSet()
        listOf("опера", "балет", "симфония", "соната", "концерт", "квартет", "прелюдия", "фуга", "этюд")
            .forEach { assertTrue("нет жанра $it", genres.contains(it)) }
    }

    @Test
    fun literatureFactsAreCorrect() {
        assertEquals("Барокко", Literature.epochOf("Иоганн Себастьян Бах"))
        assertEquals("Классицизм", Literature.epochOf("Вольфганг Амадей Моцарт"))
        assertEquals("Классицизм", Literature.epochOf("Людвиг ван Бетховен"))
        assertEquals("Романтизм", Literature.epochOf("Пётр Ильич Чайковский"))
        assertEquals("XX век", Literature.epochOf("Сергей Прокофьев"))
        assertEquals("XX век", Literature.epochOf("Дмитрий Шостакович"))
        assertEquals("Пётр Ильич Чайковский", Literature.composerOfWork("Балет «Щелкунчик»"))
        assertEquals("Жорж Бизе", Literature.composerOfWork("Опера «Кармен»"))
        assertEquals("Модест Мусоргский", Literature.composerOfWork("Опера «Борис Годунов»"))
    }

    @Test
    fun generatorsCoverAllThirtyKeys() {
        val keys = ContentRepo.generators().keys
        assertEquals(30, keys.size)
        assertEquals(15, keys.count { !it.minor })
        assertEquals(15, keys.count { it.minor })
        val expectedMajor = mapOf(
            "C" to 0, "G" to 1, "D" to 2, "A" to 3, "E" to 4, "B" to 5, "F#" to 6, "C#" to 7,
            "F" to -1, "Bb" to -2, "Eb" to -3, "Ab" to -4, "Db" to -5, "Gb" to -6, "Cb" to -7
        )
        keys.filter { !it.minor }.forEach { key ->
            val expected = expectedMajor[key.latin]
            assertNotNull("неизвестная тональность ${key.latin}", expected)
            val actual = if (key.sharps > 0) key.sharps else -key.flats
            assertEquals("${key.latin}: знаки", expected, actual)
        }
        val minorSigns = mapOf(
            "Am" to 0, "Em" to 1, "Bm" to 2, "F#m" to 3, "C#m" to 4, "G#m" to 5, "D#m" to 6, "A#m" to 7,
            "Dm" to -1, "Gm" to -2, "Cm" to -3, "Fm" to -4, "Bbm" to -5, "Ebm" to -6, "Abm" to -7
        )
        keys.filter { it.minor }.forEach { key ->
            val expected = minorSigns[key.latin]
            assertNotNull("неизвестный минор ${key.latin}", expected)
            val actual = if (key.sharps > 0) key.sharps else -key.flats
            assertEquals("${key.latin}: знаки", expected, actual)
        }
    }

    @Test
    fun generatorsCoverIntervalsChordsAndMeters() {
        val g = ContentRepo.generators()
        val required = mapOf(
            "ч1" to 0, "м2" to 1, "б2" to 2, "м3" to 3, "б3" to 4, "ч4" to 5,
            "тритон" to 6, "ч5" to 7, "м6" to 8, "б6" to 9, "м7" to 10, "б7" to 11, "ч8" to 12,
            "м9" to 13, "б9" to 14, "м10" to 15, "б10" to 16, "ч11" to 17,
            "ч12" to 19, "м13" to 20, "б13" to 21
        )
        required.forEach { (name, semitones) ->
            val spec = g.intervals.firstOrNull { it.name == name }
            assertNotNull("нет интервала $name", spec)
            assertEquals("полутоны $name", semitones, spec!!.semitones)
        }
        assertEquals(4, g.triads.size)
        assertEquals(listOf(0, 4, 7), g.triads.first { it.name.contains("мажор") }.offsets)
        assertEquals(listOf(0, 3, 6), g.triads.first { it.name.contains("уменьш") }.offsets)
        assertEquals(listOf(0, 4, 8), g.triads.first { it.name.contains("увеличен") }.offsets)

        assertEquals(7, g.sevenths.size)
        listOf("D7", "D65", "D43", "D2", "SII7", "VII7", "умVII7").forEach { name ->
            assertTrue("нет септаккорда $name", g.sevenths.any { it.name == name })
        }
        assertEquals(listOf(0, 4, 7, 10), g.sevenths.first { it.name == "D7" }.offsets)
        assertEquals(listOf(0, 3, 6, 8), g.sevenths.first { it.name == "D65" }.offsets)
        assertEquals(listOf(0, 3, 5, 8), g.sevenths.first { it.name == "D43" }.offsets)
        assertEquals(listOf(0, 2, 5, 9), g.sevenths.first { it.name == "D2" }.offsets)

        listOf("2/4", "3/4", "4/4", "6/8", "9/8", "12/8", "5/4", "7/8").forEach {
            assertTrue("нет размера $it", g.meters.contains(it))
        }
        assertTrue(g.tempos.isNotEmpty())
        assertEquals(8, g.defaultSession)
    }

    @Test
    fun contentCountersMatchReadmeNumbers() {
        assertEquals(12, ContentRepo.course(Course.THEORY).sections.size)
        assertEquals(120, ContentRepo.course(Course.THEORY).topicCount)
        assertEquals(9, ContentRepo.course(Course.SOLFEGGIO).sections.size)
        assertEquals(63, ContentRepo.course(Course.SOLFEGGIO).topicCount)
        assertEquals(244, ContentRepo.cardsCount())
        assertTrue(ThemeRepo.count() >= 30)
    }
}
