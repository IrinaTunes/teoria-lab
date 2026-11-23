package ru.teoria.lab.core

sealed class Question {
    abstract val kind: ExerciseKind
    abstract val prompt: String

    data class FindNote(
        val target: Note,
        val options: List<Note> = emptyList()
    ) : Question() {
        override val kind: ExerciseKind get() = ExerciseKind.FIND_NOTE
        override val prompt: String get() = "Найди ноту: ${target.nameRu} ${octaveRu(target.octave)}"
    }

    data class EarInterval(
        val low: Note,
        val high: Note,
        val answer: String,
        val options: List<String>,
        val direction: String = "вверх"
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
        val title: String,
        val offsets: List<Int>,
        val options: List<String>
    ) : Question() {
        override val kind: ExerciseKind get() = ExerciseKind.CHORD_EAR
        override val prompt: String get() = "Определи аккорд на слух"
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
            val mode = if (key.minor) "минорной" else "мажорной"
            "В какой $mode тональности ${signLabel(key.signCount, key.hasSharps)}?"
        }
    }

    data class RhythmTask(
        val bpm: Int,
        val beats: Int,
        val meter: String
    ) : Question() {
        override val kind: ExerciseKind get() = ExerciseKind.RHYTHM
        override val prompt: String get() = "Повтори ритм: $beats доли при $bpm уд/мин"
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

    data class Listen(
        val themeId: String,
        val mode: String,
        val answer: String,
        val options: List<String>,
        val composer: String,
        val work: String
    ) : Question() {
        override val kind: ExerciseKind get() = ExerciseKind.LISTEN
        override val prompt: String get() = if (mode == "composer") {
            "Звучит тема — кто её автор?"
        } else {
            "Звучит тема — что это за произведение?"
        }
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

fun keyFromSpec(spec: KeySpec): MusicKey {
    val letter = LETTER_LATIN.indexOfFirst { it.equals(spec.latin.take(1), ignoreCase = true) }
    val acc = when {
        spec.latin.contains("#") -> 1
        spec.latin.contains("b") -> -1
        else -> 0
    }
    val pc = if (letter >= 0) (LETTER_SEMITONES[letter] + acc + 12) % 12 else 0
    return MusicKey(spec.ru, spec.latin, spec.sharps, spec.flats, pc, 1, spec.minor)
}

object QuizGenerator {

    fun rangeFor(instrumentId: String): IntRange = when (instrumentId) {
        "guitar" -> 40..76
        "violin" -> 55..83
        "vocal" -> 60..79
        else -> 60..83
    }

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
            ExerciseKind.FIND_NOTE -> findNote(rnd, instrumentId)
            ExerciseKind.EAR_INTERVAL -> intervalQuestion(rnd, visual = false)
            ExerciseKind.VISUAL_INTERVAL -> intervalQuestion(rnd, visual = true)
            ExerciseKind.CHORD_EAR -> chordQuestion(rnd)
            ExerciseKind.KEY_SIGNS -> keySigns(rnd, topic)
            ExerciseKind.RHYTHM -> rhythm(rnd)
            ExerciseKind.LITERATURE -> literature(rnd, topic)
            ExerciseKind.LISTEN -> listen(rnd, topic)
        }
    }

    // ---------- нота ----------
    fun findNote(rnd: kotlin.random.Random, instrumentId: String): Question.FindNote {
        val pool = playableMidi(instrumentId)
        val midi = pool[rnd.nextInt(pool.size)]
        return Question.FindNote(Note.fromMidi(midi))
    }

    // ---------- интервалы ----------
    fun intervalQuestion(rnd: kotlin.random.Random, visual: Boolean): Question {
        val specs = ContentRepo.generators().intervals.ifEmpty {
            Intervals.BY_SEMITONES.map { (s, n) -> IntervalSpec(n, s, Intervals.titleOf(n)) }
        }
        val spec = specs[rnd.nextInt(specs.size)]
        val direction = if (rnd.nextBoolean()) "вверх" else "вниз"
        val highMidi = (48 + spec.semitones).coerceAtMost(88)
        val lowMidi = highMidi - spec.semitones
        val low = Note.fromMidi(lowMidi)
        val high = Note.fromMidi(highMidi)
        val options = fourOptions(specs.map { it.name }, spec.name, rnd)
        return if (visual) {
            Question.VisualInterval(low, high, spec.name, options)
        } else {
            Question.EarInterval(low, high, spec.name, options, direction)
        }
    }

    // ---------- аккорды ----------
    fun chordQuestion(rnd: kotlin.random.Random): Question.ChordEar {
        val specs = ContentRepo.generators().chords.ifEmpty {
            Chords.TYPES.map { ChordSpec(it, Chords.offsets(it), it) }
        }
        val spec = specs[rnd.nextInt(specs.size)]
        val rootMidi = 52 + rnd.nextInt(10)
        val root = Note.fromMidi(rootMidi)
        return Question.ChordEar(
            root, spec.name, spec.title, spec.offsets,
            fourOptions(specs.map { it.name }, spec.name, rnd)
        )
    }

    // ---------- тональности ----------
    fun keySigns(rnd: kotlin.random.Random, topic: TopicDef): Question.KeySigns {
        val configKeys = ContentRepo.generators().keys
        val allKeys: List<MusicKey> = if (configKeys.isEmpty()) Keys.ALL else configKeys.map { keyFromSpec(it) }
        val pool = when (topic.filter["mode"]) {
            "sharp" -> allKeys.filter { it.sharps > 0 }
            "flat" -> allKeys.filter { it.flats > 0 }
            "minor" -> allKeys.filter { it.minor }
            "major" -> allKeys.filter { !it.minor }
            else -> allKeys
        }.ifEmpty { allKeys }
        val key = pool[rnd.nextInt(pool.size)]
        val askCount = rnd.nextInt(100) < 55
        return if (askCount) {
            val correct = signLabel(key.signCount, key.hasSharps)
            val universe = (0..7)
                .flatMap { listOf(signLabel(it, true), signLabel(it, false)) }
                .distinct()
                .filter { it != correct }
            val options = (listOf(correct) + universe.shuffled(rnd).take(3)).toMutableList()
            options.shuffleInPlace(rnd)
            Question.KeySigns(key, true, options)
        } else {
            val correct = key.nameRu
            val others = allKeys.filter { it.minor == key.minor && it.latin != key.latin }
                .shuffled(rnd).take(3).map { it.nameRu }
            val options = (listOf(correct) + others).toMutableList()
            options.shuffleInPlace(rnd)
            Question.KeySigns(key, false, options)
        }
    }

    // ---------- ритм ----------
    fun rhythm(rnd: kotlin.random.Random): Question.RhythmTask {
        val g = ContentRepo.generators()
        val tempos = g.tempos.ifEmpty { listOf(80, 90, 100, 110) }
        val meters = g.meters.ifEmpty { listOf("2/4", "3/4", "4/4", "6/8") }
        val meter = meters[rnd.nextInt(meters.size)]
        val beats = meter.substringBefore('/').toIntOrNull()?.coerceIn(2, 6) ?: 4
        return Question.RhythmTask(tempos[rnd.nextInt(tempos.size)], beats, meter)
    }

    // ---------- музлитература ----------
    fun cardsFor(topic: TopicDef): List<LitCard> {
        val filtered = ContentRepo.cards().filter { card ->
            topic.filter.all { (key, value) ->
                when (key) {
                    "composer" -> card.composer == value
                    "epoch" -> card.epoch == value
                    "genre" -> card.genre == value
                    "work" -> card.work == value
                    else -> true
                }
            }
        }
        return filtered.ifEmpty { ContentRepo.cards() }
    }

    fun literature(rnd: kotlin.random.Random, topic: TopicDef): Question.Lit {
        val cards = cardsFor(topic)
        if (cards.isEmpty()) {
            return Question.Lit("Нет карточек", "", emptyList(), LitCard(0, "", "", "", ""), "none")
        }
        val allCards = ContentRepo.cards()
        val mode = when {
            topic.filter["epoch"] != null -> "epochComposer"
            topic.filter["genre"] != null -> "work"
            topic.filter["composer"] != null -> "composer"
            else -> listOf("epoch", "work", "composer")[rnd.nextInt(3)]
        }
        return when (mode) {
            "epochComposer" -> {
                val card = cards[rnd.nextInt(cards.size)]
                val pool = allCards.filter { it.epoch != card.epoch }.map { it.composer }.distinct()
                Question.Lit(
                    "Кто из композиторов — представитель эпохи «${card.epoch}»?",
                    card.composer, fourOptions(pool, card.composer, rnd), card, "epoch"
                )
            }
            "epoch" -> {
                val card = cards[rnd.nextInt(cards.size)]
                val pool = allCards.map { it.epoch }.distinct()
                Question.Lit(
                    "В какую эпоху творил ${card.composer}?",
                    card.epoch, fourOptions(pool, card.epoch, rnd), card, "epoch"
                )
            }
            "composer" -> {
                val card = cards[rnd.nextInt(cards.size)]
                val pool = allCards.map { it.work }.distinct()
                Question.Lit(
                    "Какое произведение написал ${card.composer}?",
                    card.work, fourOptions(pool, card.work, rnd), card, "composer"
                )
            }
            else -> {
                val card = cards[rnd.nextInt(cards.size)]
                val pool = allCards.map { it.composer }.distinct()
                Question.Lit(
                    "Кто автор «${card.work}»?",
                    card.composer, fourOptions(pool, card.composer, rnd), card, "work"
                )
            }
        }
    }

    // ---------- угадай мелодию ----------
    fun listen(rnd: kotlin.random.Random, topic: TopicDef): Question.Listen {
        val themes = ThemeRepo.all()
        if (themes.isEmpty()) {
            return Question.Listen("", "composer", "", emptyList(), "", "")
        }
        val theme = themes[rnd.nextInt(themes.size)]
        val mode = topic.filter["mode"] ?: if (rnd.nextBoolean()) "composer" else "work"
        return if (mode == "work") {
            val answer = theme.work
            val others = themes.filter { it.work != answer }.map { it.work }.distinct()
                .shuffled(rnd).take(3)
            val options = (listOf(answer) + others).toMutableList().also { it.shuffleInPlace(rnd) }
            Question.Listen(theme.id, "work", answer, options, theme.composer, theme.work)
        } else {
            val answer = theme.composer
            val others = ThemeRepo.composers().filter { it != answer }.shuffled(rnd).take(3)
            val options = (listOf(answer) + others).toMutableList().also { it.shuffleInPlace(rnd) }
            Question.Listen(theme.id, "composer", answer, options, theme.composer, theme.work)
        }
    }

    /** Четыре различных варианта, среди них обязательно правильный. */
    fun fourOptions(pool: List<String>, correct: String, rnd: kotlin.random.Random): List<String> {
        val distinct = pool.distinct()
        val others = distinct.filter { it != correct }.shuffled(rnd).take(3)
        val options = (listOf(correct) + others).toMutableList()
        while (options.size < 4) options.add("—")
        options.shuffleInPlace(rnd)
        return options
    }

    private fun MutableList<String>.shuffleInPlace(rnd: kotlin.random.Random) {
        for (i in size - 1 downTo 1) {
            val j = rnd.nextInt(0, i + 1)
            val tmp = this[i]
            this[i] = this[j]
            this[j] = tmp
        }
    }
}

/** Текст правильного ответа для обратной связи. */
fun Question.correctLabel(): String = when (this) {
    is Question.FindNote -> target.fullRu
    is Question.EarInterval -> "$answer — ${Intervals.titleOf(answer)}"
    is Question.VisualInterval -> "$answer — ${Intervals.titleOf(answer)}"
    is Question.ChordEar -> "$type — $title"
    is Question.KeySigns -> if (askCount) {
        signLabel(key.signCount, key.hasSharps)
    } else {
        "${key.nameRu} — ${signLabel(key.signCount, key.hasSharps, genitive = true)}"
    }
    is Question.RhythmTask -> "$beats доли, $meter, $bpm уд/мин"
    is Question.Lit -> answer
    is Question.Listen -> if (mode == "composer") composer else work
}
