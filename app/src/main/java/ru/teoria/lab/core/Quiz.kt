package ru.teoria.lab.core

enum class Course(val id: String, val title: String) {
    SOLFEGGIO("solfeggio", "Сольфеджио"),
    THEORY("theory", "Теория"),
    LITERATURE("literature", "Музлитература");

    companion object {
        fun byId(id: String): Course = values().firstOrNull { it.id == id } ?: SOLFEGGIO
    }
}

enum class ExerciseKind(val title: String) {
    FIND_NOTE("Найди ноту"),
    EAR_INTERVAL("Интервал на слух"),
    VISUAL_INTERVAL("Интервал по нотам"),
    CHORD_EAR("Аккорд на слух"),
    KEY_SIGNS("Тональность и ключевые знаки"),
    RHYTHM("Ритм и метроном"),
    LITERATURE("Карточки музлитературы")
}

data class TopicDef(
    val id: String,
    val course: Course,
    val title: String,
    val subtitle: String,
    val kind: ExerciseKind
)

object Topics {
    val ALL: List<TopicDef> = listOf(
        // Сольфеджио
        TopicDef("solf-notes", Course.SOLFEGGIO, "Ноты и октавы", "Названия нот, октавы, диезы и бемоли", ExerciseKind.FIND_NOTE),
        TopicDef("solf-ear", Course.SOLFEGGIO, "Интервалы на слух", "Две ноты — определи интервал", ExerciseKind.EAR_INTERVAL),
        TopicDef("solf-visual", Course.SOLFEGGIO, "Интервалы по нотам", "Две ноты на нотоносце", ExerciseKind.VISUAL_INTERVAL),
        TopicDef("solf-rhythm", Course.SOLFEGGIO, "Ритм и метроном", "Попади в долю с допуском ±120 мс", ExerciseKind.RHYTHM),
        // Теория
        TopicDef("theory-sharps", Course.THEORY, "Диезные тональности", "Порядок диезов: фа до соль ре ля ми си", ExerciseKind.KEY_SIGNS),
        TopicDef("theory-flats", Course.THEORY, "Бемольные тональности", "Порядок бемолей — обратный", ExerciseKind.KEY_SIGNS),
        TopicDef("theory-chords", Course.THEORY, "Аккорды", "Мажор, минор, уменьшённый, увеличенный", ExerciseKind.CHORD_EAR),
        TopicDef("theory-degrees", Course.THEORY, "Ступени и лад", "I–VII в мажоре, тоника и устойчивость", ExerciseKind.FIND_NOTE),
        // Музлитература
        TopicDef("lit-epoch", Course.LITERATURE, "Композитор и эпоха", "Барокко, классицизм, романтизм, XX век", ExerciseKind.LITERATURE),
        TopicDef("lit-stage", Course.LITERATURE, "Опера и балет", "Кто автор сценического произведения", ExerciseKind.LITERATURE),
        TopicDef("lit-works", Course.LITERATURE, "Композитор и произведение", "Что написал этот композитор", ExerciseKind.LITERATURE),
        TopicDef("lit-russian", Course.LITERATURE, "Русская и советская музыка", "Чайковский, Мусоргский, Прокофьев, Шостакович", ExerciseKind.LITERATURE)
    )

    fun byId(id: String): TopicDef = ALL.firstOrNull { it.id == id } ?: ALL[0]
    fun byCourse(course: Course): List<TopicDef> = ALL.filter { it.course == course }
    fun topicsCount(): Int = ALL.size
}

sealed class Question {
    abstract val kind: ExerciseKind
    abstract val prompt: String

    data class FindNote(
        val target: Note,
        val options: List<Note>
    ) : Question() {
        override val kind: ExerciseKind get() = ExerciseKind.FIND_NOTE
        override val prompt: String get() = "Найди ноту: ${target.nameRu} ${octaveRu(target.octave)}"
    }

    data class EarInterval(
        val low: Note,
        val high: Note,
        val answer: String,
        val options: List<String>
    ) : Question() {
        override val kind: ExerciseKind get() = ExerciseKind.EAR_INTERVAL
        override val prompt: String get() = "Послушай две ноты и определи интервал"
    }

    data class VisualInterval(
        val low: Note,
        val high: Note,
        val answer: String,
        val options: List<String>
    ) : Question() {
        override val kind: ExerciseKind get() = ExerciseKind.VISUAL_INTERVAL
        override val prompt: String get() = "Какой интервал между нотами?"
    }

    data class ChordEar(
        val root: Note,
        val type: String,
        val options: List<String>
    ) : Question() {
        override val kind: ExerciseKind get() = ExerciseKind.CHORD_EAR
        override val prompt: String get() = "Определи тип аккорда на слух"
    }

    data class KeySigns(
        val key: MusicKey,
        val askCount: Boolean,
        val options: List<String>
    ) : Question() {
        override val kind: ExerciseKind get() = ExerciseKind.KEY_SIGNS
        override val prompt: String get() = if (askCount) {
            "Сколько знаков в тональности ${key.nameRu}?"
        } else {
            val label = signLabel(key.signCount, key.hasSharps, genitive = true)
            "В какой тональности $label?"
        }
    }

    data class RhythmTask(
        val bpm: Int,
        val beats: Int,
        val pattern: List<Int>
    ) : Question() {
        override val kind: ExerciseKind get() = ExerciseKind.RHYTHM
        override val prompt: String get() = "Повтори ритм: ${beats} доли при $bpm уд/мин"
    }

    data class Lit(
        val promptText: String,
        val answer: String,
        val options: List<String>,
        val card: LitCard,
        val mode: String
    ) : Question() {
        override val kind: ExerciseKind get() = ExerciseKind.LITERATURE
        override val prompt: String get() = promptText
    }
}

/** «3 диеза», «1 бемоль», «нет знаков». */
fun signLabel(count: Int, sharps: Boolean, genitive: Boolean = false): String {
    if (count == 0) return if (genitive) "нет знаков" else "0 знаков"
    val plural = when {
        count % 10 == 1 && count % 100 != 11 -> if (sharps) "диез" else "бемоль"
        count % 10 in 2..4 && count % 100 !in 12..14 -> if (sharps) "диеза" else "бемоля"
        else -> if (sharps) "диезов" else "бемолей"
    }
    return "$count $plural"
}

object QuizGenerator {
    /** MIDI-диапазон, доступный на выбранном инструменте. */
    fun rangeFor(instrumentId: String): IntRange = when (instrumentId) {
        "guitar" -> 40..76
        "violin" -> 55..83
        "vocal" -> 60..79
        else -> 60..83
    }

    /** Ноты, реально исполнимые на инструменте. */
    fun playableMidi(instrumentId: String): List<Int> {
        val range = rangeFor(instrumentId)
        return when (instrumentId) {
            "guitar" -> range.filter { Guitar.positionsFor(it).isNotEmpty() }
            "violin" -> range.filter { Violin.positionsFor(it).isNotEmpty() }
            else -> range.toList()
        }
    }

    fun generate(topic: TopicDef, seed: Long, instrumentId: String): Question {
        val rnd = kotlin.random.Random(seed * 31 + topic.id.hashCode())
        return when (topic.kind) {
            ExerciseKind.FIND_NOTE -> findNote(topic, seed, rnd, instrumentId)
            ExerciseKind.EAR_INTERVAL -> earInterval(rnd)
            ExerciseKind.VISUAL_INTERVAL -> visualInterval(rnd)
            ExerciseKind.CHORD_EAR -> chordEar(rnd)
            ExerciseKind.KEY_SIGNS -> keySigns(topic, rnd)
            ExerciseKind.RHYTHM -> rhythm(rnd)
            ExerciseKind.LITERATURE -> literature(topic, rnd)
        }
    }

    private fun findNote(topic: TopicDef, seed: Long, rnd: kotlin.random.Random, instrumentId: String): Question.FindNote {
        val pool = playableMidi(instrumentId)
        val midi = pool[rnd.nextInt(pool.size)]
        // Название всегда соответствует точной высоте: «до-диез», «си-бемоль» и т. д.
        val target = Note.fromMidi(midi)
        return Question.FindNote(target, emptyList())
    }

    private fun randomIntervalName(rnd: kotlin.random.Random): String {
        val simple = listOf("м2", "б2", "м3", "б3", "ч4", "ч5", "м6", "б6", "м7", "б7", "ч8")
        return if (rnd.nextInt(6) == 0) "тритон" else simple[rnd.nextInt(simple.size)]
    }

    private fun earInterval(rnd: kotlin.random.Random): Question.EarInterval {
        val name = randomIntervalName(rnd)
        val pair = Intervals.EXAMPLES.getValue(name).let { it[rnd.nextInt(it.size)] }
        return Question.EarInterval(pair.first, pair.second, name, Intervals.optionsFor(name, rnd.nextLong()))
    }

    private fun visualInterval(rnd: kotlin.random.Random): Question.VisualInterval {
        val name = randomIntervalName(rnd)
        val pair = Intervals.EXAMPLES.getValue(name).let { it[rnd.nextInt(it.size)] }
        return Question.VisualInterval(pair.first, pair.second, name, Intervals.optionsFor(name, rnd.nextLong()))
    }

    private fun chordEar(rnd: kotlin.random.Random): Question.ChordEar {
        val type = Chords.TYPES[rnd.nextInt(Chords.TYPES.size)]
        val rootMidi = 55 + rnd.nextInt(8)
        val root = Note.fromMidi(rootMidi)
        return Question.ChordEar(root, type, Chords.optionsFor(type, rnd.nextLong()))
    }

    private fun keySigns(topic: TopicDef, rnd: kotlin.random.Random): Question.KeySigns {
        val sharpOnly = topic.id == "theory-sharps"
        val flatOnly = topic.id == "theory-flats"
        val pool = when {
            sharpOnly -> Keys.MAJOR.filter { it.sharps > 0 }
            flatOnly -> Keys.MAJOR.filter { it.flats > 0 }
            else -> Keys.MAJOR
        }
        val key = pool[rnd.nextInt(pool.size)]
        val askCount = rnd.nextInt(100) < 55 || sharpOnly || flatOnly
        if (askCount) {
            val correct = signLabel(key.signCount, key.hasSharps)
            val universe = (0..7)
                .flatMap { listOf(signLabel(it, true), signLabel(it, false)) }
                .distinct()
                .filter { it != correct }
            val options = (listOf(correct) + universe.shuffled(rnd).take(3)).toMutableList()
            shuffle(options, rnd)
            return Question.KeySigns(key, true, options)
        }
        val correct = key.nameRu
        val others = Keys.MAJOR.filter { it.tonic != key.tonic }.shuffled(rnd).take(3).map { it.nameRu }
        val options = (listOf(correct) + others).toMutableList()
        shuffle(options, rnd)
        return Question.KeySigns(key, false, options)
    }

    private fun rhythm(rnd: kotlin.random.Random): Question.RhythmTask {
        val bpm = listOf(80, 90, 100, 110)[rnd.nextInt(4)]
        val beats = 4
        val pattern = (0 until beats).map { 1 }
        return Question.RhythmTask(bpm, beats, pattern)
    }

    private fun literature(topic: TopicDef, rnd: kotlin.random.Random): Question.Lit {
        return when (topic.id) {
            "lit-epoch" -> {
                val card = Literature.CARDS[rnd.nextInt(Literature.CARDS.size)]
                val epoch = card.epoch
                val others = Literature.EPOCHS.filter { it != epoch }.shuffled(rnd).take(3)
                val options = (listOf(epoch) + others).toMutableList()
                shuffle(options, rnd)
                Question.Lit(
                    "В какую эпоху творил ${card.composer}?",
                    epoch, options, card, "epoch"
                )
            }
            "lit-stage" -> {
                val pool = Literature.stageWorks()
                val card = pool[rnd.nextInt(pool.size)]
                val answer = card.composer
                val others = Literature.composers().filter { it != answer }.shuffled(rnd).take(3)
                val options = (listOf(answer) + others).toMutableList()
                shuffle(options, rnd)
                Question.Lit("Кто автор «${card.work}»?", answer, options, card, "work")
            }
            "lit-russian" -> {
                val pool = Literature.russianWorks()
                val card = pool[rnd.nextInt(pool.size)]
                if (rnd.nextBoolean()) {
                    val answer = card.epoch
                    val others = Literature.EPOCHS.filter { it != answer }.shuffled(rnd).take(3)
                    val options = (listOf(answer) + others).toMutableList()
                    shuffle(options, rnd)
                    Question.Lit("Какой эпохе близко творчество ${card.composer}?", answer, options, card, "epoch")
                } else {
                    val answer = card.composer
                    val others = Literature.russianWorks().map { it.composer }.distinct()
                        .filter { it != answer }.shuffled(rnd).take(3)
                    val options = (listOf(answer) + others).toMutableList()
                    shuffle(options, rnd)
                    Question.Lit("Кто автор «${card.work}»?", answer, options, card, "work")
                }
            }
            else -> {
                val card = Literature.CARDS[rnd.nextInt(Literature.CARDS.size)]
                val answer = card.work
                val others = Literature.CARDS.filter { it.work != answer }.shuffled(rnd).take(3).map { it.work }
                val options = (listOf(answer) + others).toMutableList()
                shuffle(options, rnd)
                Question.Lit("Какое произведение написал ${card.composer}?", answer, options, card, "composer")
            }
        }
    }

    private fun <T> shuffle(list: MutableList<T>, rnd: kotlin.random.Random) {
        for (i in list.size - 1 downTo 1) {
            val j = rnd.nextInt(0, i + 1)
            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp
        }
    }
}

/** Текст правильного ответа для обратной связи. */
fun Question.correctLabel(): String = when (this) {
    is Question.FindNote -> target.fullRu
    is Question.EarInterval -> "$answer — ${Intervals.titleOf(answer)}"
    is Question.VisualInterval -> "$answer — ${Intervals.titleOf(answer)}"
    is Question.ChordEar -> type
    is Question.KeySigns -> if (askCount) {
        signLabel(key.signCount, key.hasSharps)
    } else {
        "${key.nameRu} — ${signLabel(key.signCount, key.hasSharps, genitive = true)}"
    }
    is Question.RhythmTask -> "$beats доли при $bpm уд/мин"
    is Question.Lit -> answer
}
