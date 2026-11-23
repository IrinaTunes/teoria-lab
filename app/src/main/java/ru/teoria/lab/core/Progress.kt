package ru.teoria.lab.core

/** Шкала знаний: XP, уровни, серия верных ответов, mastery, интервальное повторение. */
object Leveling {
    const val MAX_LEVEL = 20
    const val XP_CORRECT = 10
    const val STREAK_MULTIPLIER = 1.5
    /** С третьего верного подряд срабатывает множитель серии. */
    const val STREAK_THRESHOLD = 3

    /** Суммарный XP, при котором начинается уровень. Порог уровня N+1 = 100 × N. */
    fun xpForLevelStart(level: Int): Int {
        if (level <= 1) return 0
        val l = level.coerceAtMost(MAX_LEVEL)
        return 100 * (l - 1) * l / 2
    }

    fun levelForXp(xp: Int): Int {
        var level = 1
        while (level < MAX_LEVEL && xp >= xpForLevelStart(level + 1)) level++
        return level
    }

    /** Прогресс внутри текущего уровня, 0f..1f. */
    fun progressInLevel(xp: Int): Float {
        val level = levelForXp(xp)
        if (level >= MAX_LEVEL) return 1f
        val start = xpForLevelStart(level)
        val next = xpForLevelStart(level + 1)
        val span = (next - start).coerceAtLeast(1)
        return ((xp - start).toFloat() / span).coerceIn(0f, 1f)
    }

    /** XP до следующего уровня. */
    fun xpToNextLevel(xp: Int): Int {
        val level = levelForXp(xp)
        if (level >= MAX_LEVEL) return 0
        return xpForLevelStart(level + 1) - xp
    }

    /** +10 за верный ответ, ×1.5 при серии; за ошибку 0. */
    fun xpForAnswer(correct: Boolean, streakBefore: Int): Int {
        if (!correct) return 0
        val base = XP_CORRECT
        return if (streakBefore >= STREAK_THRESHOLD) (base * STREAK_MULTIPLIER).toInt() else base
    }

    fun levelTitle(level: Int): String = when {
        level <= 2 -> "Новичок"
        level <= 5 -> "Ученик"
        level <= 9 -> "Практик"
        level <= 13 -> "Знаток"
        level <= 17 -> "Мастер"
        else -> "Виртуоз"
    }
}

object SpacedRepetition {
    const val DAY_MS = 24L * 60L * 60L * 1000L
    /** Интервалы повторения в днях. */
    val INTERVALS = listOf(1, 3, 7, 14, 30)
    const val WINDOW = 10
    const val DAILY_GOAL = 20

    /** Следующий интервал: шаг вперёд при верном ответе, сброс на 1 день при ошибке. */
    fun nextInterval(currentDays: Int, correct: Boolean): Int {
        if (!correct) return INTERVALS.first()
        val idx = INTERVALS.indexOf(currentDays)
        if (idx < 0) {
            // Нестандартное значение — берём первый интервал, который больше текущего.
            return INTERVALS.firstOrNull { it > currentDays } ?: INTERVALS.last()
        }
        return INTERVALS[(idx + 1).coerceAtMost(INTERVALS.size - 1)]
    }

    fun nextDue(now: Long, intervalDays: Int): Long = now + intervalDays * DAY_MS

    fun isDue(dueAt: Long, now: Long): Boolean = dueAt <= now

    /** Доля верных за последние 10 попыток; знаменатель не меньше 10. */
    fun mastery(attempts: List<Boolean>): Double {
        if (attempts.isEmpty()) return 0.0
        val last = attempts.takeLast(WINDOW)
        val denominator = maxOf(WINDOW, last.size)
        return (last.count { it }.toDouble() / denominator).coerceIn(0.0, 1.0)
    }

    /** Приоритет темы в очереди: сначала просроченные, затем слабые. */
    fun priority(dueAt: Long, mastery: Double, now: Long): Double {
        val overdueDays = ((now - dueAt).toDouble() / DAY_MS).coerceAtLeast(0.0)
        val weakness = (1.0 - mastery) * 3.0
        val dueBonus = if (isDue(dueAt, now)) 10.0 else 0.0
        return dueBonus + overdueDays * 2.0 + weakness
    }
}

object Rhythm {
    const val TOLERANCE_MS = 120L

    fun beatIntervalMs(bpm: Int): Long = (60_000.0 / bpm.coerceAtLeast(30)).toLong()

    fun beatTimes(bpm: Int, beats: Int, startMs: Long): List<Long> {
        val step = beatIntervalMs(bpm)
        return (0 until beats).map { startMs + it * step }
    }

    fun matchesTap(tapMs: Long, expectedMs: Long, tolerance: Long = TOLERANCE_MS): Boolean =
        kotlin.math.abs(tapMs - expectedMs) <= tolerance

    /** Сколько тапов попало в допуск. */
    fun scoreTaps(taps: List<Long>, expected: List<Long>): Int {
        if (taps.isEmpty()) return 0
        var hits = 0
        for (i in expected.indices) {
            val tap = taps.getOrNull(i) ?: break
            if (matchesTap(tap, expected[i])) hits++
        }
        return hits
    }

    fun hitRate(taps: List<Long>, expected: List<Long>): Double =
        if (expected.isEmpty()) 0.0 else scoreTaps(taps, expected).toDouble() / expected.size
}

/** Календарные утилиты для серии дней (без Android). */
object Days {
    fun dayKey(timestamp: Long, zoneOffsetMinutes: Int = 0): String {
        val day = (timestamp + zoneOffsetMinutes * 60_000L) / SpacedRepetition.DAY_MS
        return day.toString()
    }

    fun dayIndex(timestamp: Long, zoneOffsetMinutes: Int = 0): Long =
        Math.floorDiv(timestamp + zoneOffsetMinutes * 60_000L, SpacedRepetition.DAY_MS)

    fun isNextDay(previous: String?, current: String): Boolean {
        val p = previous?.toLongOrNull() ?: return false
        val c = current.toLongOrNull() ?: return false
        return c - p == 1L
    }

    fun isSameDay(previous: String?, current: String): Boolean = previous == current

    /** Новая серия: продолжается, если день следующий, сбрасывается иначе. */
    fun nextStreak(streak: Int, lastDay: String?, today: String): Int = when {
        isSameDay(lastDay, today) -> streak.coerceAtLeast(1)
        isNextDay(lastDay, today) -> streak + 1
        else -> 1
    }
}
