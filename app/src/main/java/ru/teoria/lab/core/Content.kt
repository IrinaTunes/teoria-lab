package ru.teoria.lab.core

/** Курсы приложения. */
enum class Course(val id: String, val title: String) {
    SOLFEGGIO("solfeggio", "Сольфеджио"),
    THEORY("theory", "Теория"),
    LITERATURE("literature", "Музлитература");

    companion object {
        fun byId(id: String): Course = values().firstOrNull { it.id == id } ?: SOLFEGGIO
    }
}

/** Типы упражнений. */
enum class ExerciseKind(val title: String) {
    FIND_NOTE("Найди ноту"),
    EAR_INTERVAL("Интервал на слух"),
    VISUAL_INTERVAL("Интервал по нотам"),
    CHORD_EAR("Аккорд"),
    KEY_SIGNS("Тональность и ключевые знаки"),
    RHYTHM("Ритм и метроном"),
    LITERATURE("Карточки музлитературы"),
    LISTEN("Угадай мелодию");

    companion object {
        fun byName(name: String): ExerciseKind? = values().firstOrNull { it.name == name }
    }
}

/** Тема курса: конспект + параметры генерации упражнений. */
data class TopicDef(
    val id: String,
    val course: Course,
    val title: String,
    val subtitle: String,
    val kind: ExerciseKind,
    val sectionId: String = "",
    val sectionTitle: String = "",
    val text: String = "",
    val filter: Map<String, String> = emptyMap()
)

data class ContentSection(
    val id: String,
    val title: String,
    val topics: List<TopicDef>
)

data class CourseContent(
    val course: Course,
    val title: String,
    val sections: List<ContentSection>
) {
    val topics: List<TopicDef> get() = sections.flatMap { it.topics }
    val topicCount: Int get() = sections.sumOf { it.topics.size }
}

data class LitCard(
    val id: Int,
    val composer: String,
    val epoch: String,
    val work: String,
    val genre: String,
    val note: String = ""
)

data class KeySpec(
    val ru: String,
    val latin: String,
    val sharps: Int,
    val flats: Int,
    val minor: Boolean
) {
    val signCount: Int get() = if (sharps > 0) sharps else flats
    val hasSharps: Boolean get() = sharps > 0
}

data class IntervalSpec(val name: String, val semitones: Int, val title: String)

data class ChordSpec(val name: String, val offsets: List<Int>, val title: String = "")

/** Параметры генерации из generators.json. */
data class Generators(
    val keys: List<KeySpec>,
    val intervals: List<IntervalSpec>,
    val directions: List<String>,
    val triads: List<ChordSpec>,
    val sevenths: List<ChordSpec>,
    val meters: List<String>,
    val rhythmValues: List<String>,
    val beats: List<Int>,
    val tempos: List<Int>,
    val defaultSession: Int
) {
    val chords: List<ChordSpec> get() = triads + sevenths
    val simpleIntervals: List<IntervalSpec> get() = intervals.filter { it.semitones <= 12 }
    val majorKeys: List<KeySpec> get() = keys.filter { !it.minor }
    val minorKeys: List<KeySpec> get() = keys.filter { it.minor }

    companion object {
        val EMPTY = Generators(
            keys = emptyList(),
            intervals = emptyList(),
            directions = listOf("вверх"),
            triads = emptyList(),
            sevenths = emptyList(),
            meters = listOf("4/4"),
            rhythmValues = emptyList(),
            beats = listOf(4),
            tempos = listOf(90),
            defaultSession = 8
        )
    }
}

/**
 * Единая точка доступа к контенту. Данные лежат в assets/content/*.json,
 * загрузчик передаётся снаружи: Android читает assets, тесты — файлы модуля.
 */
object ContentRepo {
    private var courses: Map<Course, CourseContent> = emptyMap()
    private var cardsList: List<LitCard> = emptyList()
    private var generatorsValue: Generators = Generators.EMPTY
    private var loaded = false
    private var loadError: String? = null

    fun init(loader: (String) -> String) {
        if (loaded) return
        try {
            val theory = parseCourse(loader("theory.json"), Course.THEORY)
            val solfege = parseCourse(loader("solfege.json"), Course.SOLFEGGIO)
            val literatureJson = loader("literature.json")
            val literature = parseCourse(literatureJson, Course.LITERATURE)
            cardsList = parseCards(literatureJson)
            generatorsValue = parseGenerators(loader("generators.json"))
            ThemeRepo.init { loader("themes.json") }
            courses = mapOf(
                Course.THEORY to theory,
                Course.SOLFEGGIO to solfege,
                Course.LITERATURE to literature
            )
            loaded = true
        } catch (t: Throwable) {
            loadError = t.message
            throw t
        }
    }

    private fun parseCourse(text: String, fallback: Course): CourseContent {
        val root = parseJson(text)
        val course = Course.byId(root.str("course", fallback.id))
        val sections = root.array("sections").map { sectionJson ->
            val sectionId = sectionJson.str("id")
            val sectionTitle = sectionJson.str("title")
            val topics = sectionJson.array("topics").map { topicJson ->
                TopicDef(
                    id = topicJson.str("id"),
                    course = course,
                    title = topicJson.str("title"),
                    subtitle = topicJson.str("summary"),
                    kind = ExerciseKind.byName(topicJson.str("kind")) ?: ExerciseKind.FIND_NOTE,
                    sectionId = sectionId,
                    sectionTitle = sectionTitle,
                    text = topicJson.str("text"),
                    filter = topicJson.opt("filter")?.fieldMap() ?: emptyMap()
                )
            }
            ContentSection(sectionId, sectionTitle, topics)
        }
        return CourseContent(course, root.str("title", fallback.title), sections)
    }

    private fun parseCards(text: String): List<LitCard> {
        val root = parseJson(text)
        return root.array("cards").map { card ->
            LitCard(
                id = card.int("id"),
                composer = card.str("composer"),
                epoch = card.str("epoch"),
                work = card.str("work"),
                genre = card.str("genre"),
                note = card.str("note")
            )
        }
    }

    private fun parseGenerators(text: String): Generators {
        val root = parseJson(text)
        val keys = root.array("keys").map {
            KeySpec(
                ru = it.str("ru"),
                latin = it.str("latin"),
                sharps = it.int("sharps"),
                flats = it.int("flats"),
                minor = it.bool("minor")
            )
        }
        val intervals = root.array("intervals").map {
            IntervalSpec(it.str("name"), it.int("semitones"), it.str("title"))
        }
        val triads = root.array("triads").map {
            ChordSpec(it.str("name"), it.array("offsets").map { off -> off.asInt() }, it.str("name"))
        }
        val sevenths = root.array("sevenths").map {
            ChordSpec(it.str("name"), it.array("offsets").map { off -> off.asInt() }, it.str("title"))
        }
        val directions = root.array("directions").map { it.asString() }
        val meters = root.array("meters").map { it.asString() }
        val rhythm = root.array("rhythm_values").map { it.asString() }
        val beats = root.array("beats").map { it.asInt() }
        val tempos = root.array("tempos").map { it.asInt() }
        return Generators(
            keys = keys,
            intervals = intervals,
            directions = directions.ifEmpty { listOf("вверх") },
            triads = triads,
            sevenths = sevenths,
            meters = meters.ifEmpty { listOf("4/4") },
            rhythmValues = rhythm,
            beats = beats.ifEmpty { listOf(4) },
            tempos = tempos.ifEmpty { listOf(90) },
            defaultSession = root.int("default_session", 8)
        )
    }

    fun isLoaded(): Boolean = loaded
    fun error(): String? = loadError
    fun course(course: Course): CourseContent = courses[course] ?: CourseContent(course, course.title, emptyList())
    fun courses(): List<CourseContent> = Course.values().map { course(it) }
    fun cards(): List<LitCard> = cardsList
    fun generators(): Generators = generatorsValue
    fun allTopics(): List<TopicDef> = Course.values().flatMap { course(it).topics }
    fun topic(id: String): TopicDef? = allTopics().firstOrNull { it.id == id }
    fun topicsOf(course: Course): List<TopicDef> = course(course).topics

    fun topicCount(): Int = allTopics().size
    fun sectionCount(): Int = Course.values().sumOf { course(it).sections.size }
    fun cardsCount(): Int = cardsList.size
}

/** Обратная совместимость: темы берутся из контента. */
object Topics {
    val ALL: List<TopicDef> get() = ContentRepo.allTopics()
    fun byId(id: String): TopicDef = ContentRepo.topic(id) ?: ALL.first()
    fun byCourse(course: Course): List<TopicDef> = ContentRepo.topicsOf(course)
    fun topicsCount(): Int = ALL.size
}
