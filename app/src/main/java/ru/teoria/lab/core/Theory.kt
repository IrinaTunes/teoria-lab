package ru.teoria.lab.core

/**
 * Чистая теория музыки: ноты, интервалы, тональности, аккорды, инструменты.
 * Никаких зависимостей от Android — всё покрывается JVM-тестами.
 */

val LETTER_RU = listOf("до", "ре", "ми", "фа", "соль", "ля", "си")
val LETTER_LATIN = listOf("C", "D", "E", "F", "G", "A", "B")
/** Полутоны от C внутри октавы для каждой буквы. */
val LETTER_SEMITONES = intArrayOf(0, 2, 4, 5, 7, 9, 11)

/** Названия октав в родительном падеже: «до первой октавы». */
fun octaveRu(octave: Int): String = when (octave) {
    0 -> "субконтроктавы"
    1 -> "контроктавы"
    2 -> "большой октавы"
    3 -> "малой октавы"
    4 -> "первой октавы"
    5 -> "второй октавы"
    6 -> "третьей октавы"
    else -> "$octave-й октавы"
}

data class Note(
    val letter: Int,
    val accidental: Int = 0,
    val octave: Int = 4
) {
    init {
        require(letter in 0..6) { "letter must be 0..6" }
        require(accidental in -1..1) { "accidental must be -1..1" }
    }

    /** MIDI-номер: C4 = 60 (до первой октавы). */
    val midi: Int get() = (octave + 1) * 12 + LETTER_SEMITONES[letter] + accidental
    val pitchClass: Int get() = ((LETTER_SEMITONES[letter] + accidental) % 12 + 12) % 12
    val isBlack: Boolean get() = accidental != 0 || LETTER_SEMITONES[letter] in intArrayOf(1, 3, 6, 8, 10)

    /** «фа-диез» */
    val nameRu: String
        get() = when (accidental) {
            1 -> LETTER_RU[letter] + "-диез"
            -1 -> LETTER_RU[letter] + "-бемоль"
            else -> LETTER_RU[letter]
        }

    /** «фа♯» — компактно для клавиш и кнопок. */
    val shortRu: String
        get() = LETTER_RU[letter] + when (accidental) {
            1 -> "♯"
            -1 -> "♭"
            else -> ""
        }

    val latin: String
        get() = LETTER_LATIN[letter] + when (accidental) {
            1 -> "#"
            -1 -> "b"
            else -> ""
        }

    /** «до-диез первой октавы» */
    val fullRu: String get() = "$nameRu ${octaveRu(octave)}"

    /** Диатонический индекс для размещения на нотоносце. */
    val diatonicIndex: Int get() = octave * 7 + letter

    fun transposed(semitones: Int): Note {
        val target = midi + semitones
        // Подбираем ближайшую диатоническую букву, чтобы название осталось осмысленным.
        var best = this
        var bestPenalty = Int.MAX_VALUE
        for (letter in 0..6) {
            for (acc in -1..1) {
                for (oct in 2..6) {
                    val candidate = Note(letter, acc, oct)
                    if (candidate.midi != target) continue
                    val penalty = if (candidate.letter == this.letter) 0 else 1
                    if (penalty < bestPenalty) {
                        bestPenalty = penalty
                        best = candidate
                    }
                }
            }
        }
        return best
    }

    override fun toString(): String = "$latin$octave"

    companion object {
        val C4 = Note(0, 0, 4)

        fun fromMidi(midi: Int): Note {
            val pc = ((midi % 12) + 12) % 12
            val octave = midi / 12 - 1
            // Предпочитаем диезы.
            val sharpLetters = mapOf(0 to 0, 1 to 0, 2 to 1, 3 to 1, 4 to 2, 5 to 3, 6 to 3, 7 to 4, 8 to 4, 9 to 5, 10 to 5, 11 to 6)
            val accs = mapOf(0 to 0, 1 to 1, 2 to 0, 3 to 1, 4 to 0, 5 to 0, 6 to 1, 7 to 0, 8 to 1, 9 to 0, 10 to 1, 11 to 0)
            return Note(sharpLetters.getValue(pc), accs.getValue(pc), octave)
        }

        /** Парсит «C#4», «Bb3», «F4». */
        fun parse(text: String): Note {
            val t = text.trim()
            val letter = LETTER_LATIN.indexOfFirst { it.equals(t[0].toString(), ignoreCase = true) }
            require(letter >= 0) { "bad note: $text" }
            var acc = 0
            var octaveStart = 1
            if (t.length > 1 && (t[1] == '#' || t[1] == 'b')) {
                acc = if (t[1] == '#') 1 else -1
                octaveStart = 2
            }
            val octave = t.substring(octaveStart).toIntOrNull() ?: 4
            return Note(letter, acc, octave)
        }
    }
}

/** Интервалы: полутоны -> обозначение. */
object Intervals {
    val BY_SEMITONES: Map<Int, String> = linkedMapOf(
        1 to "м2",
        2 to "б2",
        3 to "м3",
        4 to "б3",
        5 to "ч4",
        6 to "тритон",
        7 to "ч5",
        8 to "м6",
        9 to "б6",
        10 to "м7",
        11 to "б7",
        12 to "ч8"
    )

    val ORDER: List<String> = BY_SEMITONES.values.toList()

    val TITLES: Map<String, String> = mapOf(
        "м2" to "малая секунда",
        "б2" to "большая секунда",
        "м3" to "малая терция",
        "б3" to "большая терция",
        "ч4" to "чистая кварта",
        "тритон" to "тритон",
        "ч5" to "чистая квинта",
        "м6" to "малая секста",
        "б6" to "большая секста",
        "м7" to "малая септима",
        "б7" to "большая септима",
        "ч8" to "чистая октава"
    )

    fun nameFor(semitones: Int): String? = BY_SEMITONES[semitones]
    fun semitonesOf(name: String): Int? = BY_SEMITONES.entries.firstOrNull { it.value == name }?.key
    fun titleOf(name: String): String = TITLES[name] ?: name

    /** Четыре варианта ответа, включая правильный, детерминированно по seed. */
    fun optionsFor(correct: String, seed: Long, rnd: kotlin.random.Random = kotlin.random.Random(seed)): List<String> {
        val idx = ORDER.indexOf(correct)
        require(idx >= 0) { "unknown interval $correct" }
        val start = (idx - rnd.nextInt(0, 2)).coerceIn(0, ORDER.size - 4)
        val window = ORDER.subList(start, start + 4).toMutableList()
        if (!window.contains(correct)) {
            window.removeAt(0)
            window.add(correct)
        }
        // Детерминированная перестановка Фишера–Йетса.
        for (i in window.size - 1 downTo 1) {
            val j = rnd.nextInt(0, i + 1)
            val tmp = window[i]
            window[i] = window[j]
            window[j] = tmp
        }
        return window
    }

    /** Примеры интервалов: пары нот, у которых полутоны строго совпадают с таблицей. */
    val EXAMPLES: Map<String, List<Pair<Note, Note>>> = mapOf(
        "м2" to listOf(Note.parse("E4") to Note.parse("F4"), Note.parse("B4") to Note.parse("C5"), Note.parse("A4") to Note.parse("Bb4")),
        "б2" to listOf(Note.parse("C4") to Note.parse("D4"), Note.parse("F4") to Note.parse("G4"), Note.parse("G4") to Note.parse("A4")),
        "м3" to listOf(Note.parse("D4") to Note.parse("F4"), Note.parse("E4") to Note.parse("G4"), Note.parse("A4") to Note.parse("C5")),
        "б3" to listOf(Note.parse("C4") to Note.parse("E4"), Note.parse("F4") to Note.parse("A4"), Note.parse("G4") to Note.parse("B4")),
        "ч4" to listOf(Note.parse("C4") to Note.parse("F4"), Note.parse("D4") to Note.parse("G4"), Note.parse("E4") to Note.parse("A4")),
        "тритон" to listOf(Note.parse("F4") to Note.parse("B4"), Note.parse("B4") to Note.parse("F5"), Note.parse("C4") to Note.parse("F#4")),
        "ч5" to listOf(Note.parse("C4") to Note.parse("G4"), Note.parse("D4") to Note.parse("A4"), Note.parse("E4") to Note.parse("B4")),
        "м6" to listOf(Note.parse("E4") to Note.parse("C5"), Note.parse("A4") to Note.parse("F5"), Note.parse("B4") to Note.parse("G5")),
        "б6" to listOf(Note.parse("C4") to Note.parse("A4"), Note.parse("D4") to Note.parse("B4"), Note.parse("F4") to Note.parse("D5")),
        "м7" to listOf(Note.parse("D4") to Note.parse("C5"), Note.parse("E4") to Note.parse("D5"), Note.parse("G4") to Note.parse("F5")),
        "б7" to listOf(Note.parse("C4") to Note.parse("B4"), Note.parse("F4") to Note.parse("E5"), Note.parse("G4") to Note.parse("F#5")),
        "ч8" to listOf(Note.parse("C4") to Note.parse("C5"), Note.parse("D4") to Note.parse("D5"), Note.parse("G4") to Note.parse("G5"))
    )

    /** Проверка соответствия пары нот обозначению интервала. */
    fun semitonesBetween(a: Note, b: Note): Int = b.midi - a.midi
}

/** Порядок знаков: диезы — фа, до, соль, ре, ля, ми, си; бемоли — обратный. */
val SHARP_ORDER = listOf("фа", "до", "соль", "ре", "ля", "ми", "си")
val FLAT_ORDER = SHARP_ORDER.reversed()

data class MusicKey(
    val tonic: String,
    val latin: String,
    val sharps: Int,
    val flats: Int,
    val pitchClass: Int,
    val degree: Int,
    val minor: Boolean = false
) {
    val accidentals: Int get() = sharps - flats
    val signCount: Int get() = if (sharps > 0) sharps else flats
    val nameRu: String get() = "$tonic ${if (minor) "минор" else "мажор"}"
    val signList: List<String> get() = if (sharps > 0) SHARP_ORDER.take(sharps) else FLAT_ORDER.take(flats)
    val hasSharps: Boolean get() = sharps > 0
}

object Keys {
    /** 15 мажорных тональностей: C и по 7 диезных/бемольных. */
    val MAJOR: List<MusicKey> = listOf(
        MusicKey("до", "C", 0, 0, 0, 1),
        MusicKey("соль", "G", 1, 0, 7, 2),
        MusicKey("ре", "D", 2, 0, 2, 3),
        MusicKey("ля", "A", 3, 0, 9, 4),
        MusicKey("ми", "E", 4, 0, 4, 5),
        MusicKey("си", "B", 5, 0, 11, 6),
        MusicKey("фа-диез", "F#", 6, 0, 6, 7),
        MusicKey("до-диез", "C#", 7, 0, 1, 8),
        MusicKey("фа", "F", 0, 1, 5, 8),
        MusicKey("си-бемоль", "Bb", 0, 2, 10, 7),
        MusicKey("ми-бемоль", "Eb", 0, 3, 3, 6),
        MusicKey("ля-бемоль", "Ab", 0, 4, 8, 5),
        MusicKey("ре-бемоль", "Db", 0, 5, 1, 4),
        MusicKey("соль-бемоль", "Gb", 0, 6, 6, 3),
        MusicKey("до-бемоль", "Cb", 0, 7, 11, 2)
    )

    /** 15 минорных тональностей: ля минор и по 7 диезных/бемольных. */
    val MINOR: List<MusicKey> = listOf(
        MusicKey("ля", "Am", 0, 0, 9, 1, true),
        MusicKey("ми", "Em", 1, 0, 4, 5, true),
        MusicKey("си", "Bm", 2, 0, 11, 6, true),
        MusicKey("фа-диез", "F#m", 3, 0, 6, 7, true),
        MusicKey("до-диез", "C#m", 4, 0, 1, 8, true),
        MusicKey("соль-диез", "G#m", 5, 0, 8, 2, true),
        MusicKey("ре-диез", "D#m", 6, 0, 3, 3, true),
        MusicKey("ля-диез", "A#m", 7, 0, 10, 4, true),
        MusicKey("ре", "Dm", 0, 1, 2, 3, true),
        MusicKey("соль", "Gm", 0, 2, 7, 2, true),
        MusicKey("до", "Cm", 0, 3, 0, 1, true),
        MusicKey("фа", "Fm", 0, 4, 5, 8, true),
        MusicKey("си-бемоль", "Bbm", 0, 5, 10, 7, true),
        MusicKey("ми-бемоль", "Ebm", 0, 6, 3, 6, true),
        MusicKey("ля-бемоль", "Abm", 0, 7, 8, 5, true)
    )

    val ALL: List<MusicKey> get() = MAJOR + MINOR

    val SHARP_KEYS: List<MusicKey> = listOf("G", "D", "A", "E", "B", "F#", "C#").mapNotNull { l -> MAJOR.firstOrNull { it.latin == l } }
    val FLAT_KEYS: List<MusicKey> = listOf("F", "Bb", "Eb", "Ab", "Db", "Gb", "Cb").mapNotNull { l -> MAJOR.firstOrNull { it.latin == l } }

    fun byLatin(latin: String): MusicKey? = MAJOR.firstOrNull { it.latin.equals(latin, ignoreCase = true) }
    fun byTonicRu(tonic: String): MusicKey? = MAJOR.firstOrNull { it.tonic == tonic }

    /** Сколько знаков (диезов или бемолей, всегда неотрицательное) в тональности. */
    fun accidentals(tonic: String): Int = byTonicRu(tonic)?.signCount ?: 0

    /** Порядок знаков для тональности. */
    fun signOrder(tonic: String): List<String> = byTonicRu(tonic)?.signList ?: emptyList()

    /** Тональности с заданным числом знаков (без диезных/бемольных синонимов). */
    fun keysWithAccidentals(count: Int, sharps: Boolean): MusicKey? =
        MAJOR.firstOrNull { if (sharps) it.sharps == count else it.flats == count }
}

object Chords {
    val MAJOR = listOf(0, 4, 7)
    val MINOR = listOf(0, 3, 7)
    val DIMINISHED = listOf(0, 3, 6)
    val AUGMENTED = listOf(0, 4, 8)
    val TYPES = listOf("мажор", "минор", "уменьшённый", "увеличенный")

    fun offsets(type: String): List<Int> = when (type) {
        "мажор" -> MAJOR
        "минор" -> MINOR
        "уменьшённый" -> DIMINISHED
        "увеличенный" -> AUGMENTED
        else -> MAJOR
    }

    /** Тип аккорда по полутоновым смещениям от основы. */
    fun typeByOffsets(offsets: List<Int>): String? {
        val sorted = offsets.sorted()
        if (sorted.size < 3) return null
        val norm = listOf(sorted[0], sorted[1], sorted[2]).map { it - sorted[0] }
        return when (norm) {
            MAJOR -> "мажор"
            MINOR -> "минор"
            DIMINISHED -> "уменьшённый"
            AUGMENTED -> "увеличенный"
            else -> null
        }
    }

    fun midiNotes(rootMidi: Int, type: String): List<Int> = offsets(type).map { rootMidi + it }

    fun midiFromOffsets(rootMidi: Int, offsets: List<Int>): List<Int> = offsets.map { rootMidi + it }

    fun optionsFor(correct: String, seed: Long): List<String> {
        val rnd = kotlin.random.Random(seed)
        val list = TYPES.toMutableList()
        for (i in list.size - 1 downTo 1) {
            val j = rnd.nextInt(0, i + 1)
            val t = list[i]; list[i] = list[j]; list[j] = t
        }
        return list
    }
}

object Guitar {
    /** Стандартный строй E2 A2 D3 G3 B3 E4 (от 6-й струны к 1-й). */
    val OPEN: IntArray = intArrayOf(40, 45, 50, 55, 59, 64)
    val STRING_NAMES = listOf("E2", "A2", "D3", "G3", "B3", "E4")
    const val FRETS = 12

    fun stringCount(): Int = OPEN.size

    /** Все позиции (струна 0..5 от низкой, лад 0..12) для заданного MIDI. */
    fun positionsFor(midi: Int): List<Pair<Int, Int>> {
        val result = mutableListOf<Pair<Int, Int>>()
        OPEN.forEachIndexed { string, open ->
            val fret = midi - open
            if (fret in 0..FRETS) result.add(string to fret)
        }
        return result
    }

    /** Расстояние в ладах между двумя позициями (только в пределах одной струны). */
    fun fretDistance(a: Pair<Int, Int>, b: Pair<Int, Int>): Int? =
        if (a.first == b.first) kotlin.math.abs(a.second - b.second) else null

    /** Аппликатура простого трезвучия: основной тон на 5-й или 6-й струне. */
    fun chordShape(rootMidi: Int, type: String): List<Pair<Int, Int>> {
        val offsets = Chords.offsets(type)
        val rootPositions = positionsFor(rootMidi).filter { it.second in 0..7 }
        if (rootPositions.isEmpty()) return emptyList()
        val rootPos = rootPositions.maxByOrNull { it.first } ?: rootPositions.first()
        val shape = mutableListOf(rootPos)
        var lastString = rootPos.first
        var lastFret = rootPos.second
        for (i in 1 until offsets.size) {
            val target = rootMidi + offsets[i]
            val candidates = positionsFor(target).filter { it.first < lastString && it.second >= lastFret - 2 && it.second <= lastFret + 4 }
            val chosen = candidates.maxByOrNull { it.second } ?: positionsFor(target).firstOrNull { it.first < lastString }
            if (chosen != null) {
                shape.add(chosen)
                lastString = chosen.first
                lastFret = chosen.second
            }
        }
        return shape
    }

    /** Подсказка «интервал = N ладов» для пары позиций. */
    fun hintForInterval(semitones: Int): String = "интервал = $semitones ладов"
}

object Violin {
    /** Строй G3 D4 A4 E5. */
    val OPEN = intArrayOf(55, 62, 69, 76)
    val STRING_NAMES = listOf("G3", "D4", "A4", "E5")
    const val POSITIONS = 12

    fun stringCount(): Int = OPEN.size

    fun positionsFor(midi: Int): List<Pair<Int, Int>> {
        val result = mutableListOf<Pair<Int, Int>>()
        OPEN.forEachIndexed { string, open ->
            val step = midi - open
            if (step in 0..POSITIONS) result.add(string to step)
        }
        return result
    }
}

object Instruments {
    val ALL = listOf(
        Instrument("piano", "Фортепиано", "Клавиатура на две октавы"),
        Instrument("guitar", "Гитара", "Гриф 6 струн × 12 ладов"),
        Instrument("violin", "Скрипка", "Четыре струны G · D · A · E"),
        Instrument("vocal", "Вокал", "Нотоносец и лестница ступеней"),
        Instrument("universal", "Универсальный", "Только нотоносец")
    )

    fun byId(id: String): Instrument = ALL.firstOrNull { it.id == id } ?: ALL[0]

    fun title(id: String): String = byId(id).title
}

data class Instrument(val id: String, val title: String, val hint: String)

/** Частота в герцах для MIDI-ноты (A4 = 440). */
fun midiToFrequency(midi: Int): Double = 440.0 * Math.pow(2.0, (midi - 69) / 12.0)

/** Лестница ступеней мажорной гаммы (полутоны от тоники). */
val MAJOR_STEPS = listOf(0, 2, 4, 5, 7, 9, 11)
val DEGREE_RU = listOf("I", "II", "III", "IV", "V", "VI", "VII")
