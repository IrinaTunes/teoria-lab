package ru.teoria.lab.core

/** Нота темы: высота в MIDI и длительность в четвертях. */
data class ThemeNote(val midi: Int, val beats: Double)

/** Мелодическая тема — синтезированная модель, не запись. */
data class MusicTheme(
    val id: String,
    val composer: String,
    val composerYears: String,
    val work: String,
    val epoch: String,
    val publicDomain: Boolean,
    val source: String,
    val notes: List<ThemeNote>
) {
    val durationBeats: Double get() = notes.sumOf { it.beats }
    val deathYear: Int? get() = parseDeathYear(composerYears)
}

/** Годы жизни «1770–1827» → 1827. */
fun parseDeathYear(years: String): Int? {
    val parts = years.split('–', '-', '—').map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.size < 2) return parts.firstOrNull()?.toIntOrNull()
    return parts.last().takeWhile { it.isDigit() }.toIntOrNull()
}

object ThemeRepo {
    private var themesList: List<MusicTheme> = emptyList()
    private var loaded = false

    fun init(loader: () -> String) {
        if (loaded) return
        val root = parseJson(loader())
        themesList = root.asArray().map { theme ->
            MusicTheme(
                id = theme.str("id"),
                composer = theme.str("composer"),
                composerYears = theme.str("composerYears"),
                work = theme.str("work"),
                epoch = theme.str("epoch"),
                publicDomain = theme.bool("publicDomain"),
                source = theme.str("source"),
                notes = theme.array("notes").map { n ->
                    ThemeNote(n.int("midi"), n.double("beats"))
                }
            )
        }
        loaded = true
    }

    fun all(): List<MusicTheme> = themesList
    fun byId(id: String): MusicTheme? = themesList.firstOrNull { it.id == id }
    fun byComposer(composer: String): List<MusicTheme> = themesList.filter { it.composer == composer }
    fun composers(): List<String> = themesList.map { it.composer }.distinct().sorted()
    fun epochOf(composer: String): String = byComposer(composer).firstOrNull()?.epoch ?: ""
    fun yearsOf(composer: String): String = byComposer(composer).firstOrNull()?.composerYears ?: ""
    fun isLoaded(): Boolean = loaded
    fun count(): Int = themesList.size
    fun composerCount(): Int = composers().size
}
