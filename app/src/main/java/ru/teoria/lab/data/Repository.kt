package ru.teoria.lab.data

import android.content.Context
import ru.teoria.lab.core.Days
import ru.teoria.lab.core.Leveling
import ru.teoria.lab.core.SpacedRepetition
import ru.teoria.lab.core.TopicDef
import ru.teoria.lab.core.Topics

/** Снимок прогресса для UI. */
data class ProgressSnapshot(
    val profile: ProfileEntity,
    val mastery: Map<String, Double>,
    val srs: Map<String, SrsEntity>,
    val dueTopicIds: List<String>,
    val xpByDay: Map<String, Int>,
    val attemptsByTopic: Map<String, List<Boolean>>
)

class TeoriaRepository(context: Context) {
    private val dao = TeoriaDb.get(context).dao()
    private val prefs = context.getSharedPreferences("teoria_prefs", Context.MODE_PRIVATE)

    suspend fun seedIfNeeded() {
        if (dao.topicCount() == 0) {
            dao.insertTopics(Topics.ALL.map {
                TopicEntity(it.id, it.course.id, it.title, it.subtitle, it.kind.name)
            })
        }
        if (dao.profile() == null) {
            dao.upsertProfile(ProfileEntity(instrument = instrumentId()))
        }
    }

    fun instrumentId(): String = prefs.getString("instrument", "piano") ?: "piano"

    fun saveInstrument(id: String) {
        prefs.edit().putString("instrument", id).apply()
    }

    suspend fun profile(): ProfileEntity = dao.profile() ?: ProfileEntity(instrument = instrumentId())

    suspend fun setInstrument(id: String) {
        saveInstrument(id)
        val current = profile()
        dao.upsertProfile(current.copy(instrument = id))
    }

    suspend fun snapshot(now: Long = System.currentTimeMillis()): ProgressSnapshot {
        val profile = profile()
        val attempts = dao.allAttempts()
        val srsList = dao.allSrs()
        val byTopic = attempts.groupBy { it.topicId }
        val mastery = Topics.ALL.associate { topic ->
            val list = byTopic[topic.id]?.map { it.correct } ?: emptyList()
            topic.id to SpacedRepetition.mastery(list)
        }
        val srsMap = srsList.associateBy { it.topicId }
        val due = Topics.ALL
            .map { topic -> topic to srsMap[topic.id] }
            .sortedByDescending { (topic, srs) ->
                SpacedRepetition.priority(
                    dueAt = srs?.dueAt ?: 0L,
                    mastery = mastery[topic.id] ?: 0.0,
                    now = now
                )
            }
            .map { it.first.id }
        val xpByDay = mutableMapOf<String, Int>()
        for (a in attempts) {
            if (a.xp <= 0) continue
            val key = Days.dayKey(a.timestamp)
            xpByDay[key] = (xpByDay[key] ?: 0) + a.xp
        }
        return ProgressSnapshot(
            profile = profile,
            mastery = mastery,
            srs = srsMap,
            dueTopicIds = due,
            xpByDay = xpByDay,
            attemptsByTopic = byTopic.mapValues { entry -> entry.value.map { it.correct } }
        )
    }

    /** Возвращает тему, которую стоит предложить следующей (просроченные и слабые — первыми). */
    suspend fun nextTopicId(now: Long = System.currentTimeMillis()): String {
        val snapshot = snapshot(now)
        return snapshot.dueTopicIds.firstOrNull() ?: Topics.ALL.first().id
    }

    suspend fun recordAnswer(
        topic: TopicDef,
        correct: Boolean,
        now: Long = System.currentTimeMillis(),
        dayKey: String = Days.dayKey(now)
    ): RecordResult {
        val current = profile()
        // Серия дней
        val newStreak = Days.nextStreak(current.streak, current.lastDay.ifBlank { null }, dayKey)
        val todayCount = if (current.todayDate == dayKey) current.todayCount + 1 else 1
        // Серия верных ответов внутри темы
        val topicAttempts = dao.attemptsFor(topic.id)
        var streakCorrect = 0
        for (i in topicAttempts.indices.reversed()) {
            if (topicAttempts[i].correct) streakCorrect++ else break
        }
        val xpGained = Leveling.xpForAnswer(correct, streakCorrect)
        dao.insertAttempt(AttemptEntity(topicId = topic.id, correct = correct, timestamp = now, xp = xpGained))

        val newXp = current.xp + xpGained
        val newProfile = current.copy(
            xp = newXp,
            level = Leveling.levelForXp(newXp),
            streak = newStreak,
            lastDay = dayKey,
            todayCount = todayCount,
            todayDate = dayKey
        )
        dao.upsertProfile(newProfile)

        val existing = dao.srsFor(topic.id)
        val newInterval = SpacedRepetition.nextInterval(existing?.intervalDays ?: 0, correct)
        dao.upsertSrs(
            SrsEntity(
                topicId = topic.id,
                dueAt = SpacedRepetition.nextDue(now, newInterval),
                intervalDays = newInterval,
                lastSeen = now
            )
        )
        return RecordResult(newProfile, xpGained, newInterval)
    }

    suspend fun dueCount(now: Long = System.currentTimeMillis()): Int {
        val snapshot = snapshot(now)
        return snapshot.dueTopicIds.count { id ->
            val srs = snapshot.srs[id] ?: return@count true
            SpacedRepetition.isDue(srs.dueAt, now)
        }
    }
}

/** Результат записи ответа: профиль после ответа, полученный XP и новый интервал повторения. */
data class RecordResult(val profile: ProfileEntity, val xpGained: Int, val intervalDays: Int)

/** Простая запись ответа для чистых тестов и UI-подсказок. */
data class Attempt(val topicId: String, val correct: Boolean, val timestamp: Long)
