package ru.teoria.lab.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.teoria.lab.core.Course
import ru.teoria.lab.core.Instruments
import ru.teoria.lab.core.Question
import ru.teoria.lab.core.QuizGenerator
import ru.teoria.lab.core.TopicDef
import ru.teoria.lab.core.Topics
import ru.teoria.lab.core.correctLabel
import ru.teoria.lab.data.ProgressSnapshot
import ru.teoria.lab.data.TeoriaRepository

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TeoriaRepository(app)
    private val prefs = app.getSharedPreferences("teoria_prefs", android.content.Context.MODE_PRIVATE)
    private var seedCounter: Long = System.currentTimeMillis()

    var instrumentsLoaded by mutableStateOf(false)
        private set
    var instrument by mutableStateOf(repo.instrumentId())
        private set
    var onboarded by mutableStateOf(prefs.getBoolean("onboarded", false))
        private set
    var snapshot by mutableStateOf<ProgressSnapshot?>(null)
        private set
    var selectedCourse by mutableStateOf(Course.SOLFEGGIO)

    // Текущая тренировка
    var topic by mutableStateOf<TopicDef?>(null)
        private set
    var question by mutableStateOf<Question?>(null)
        private set
    var answered by mutableStateOf(false)
        private set
    var wasCorrect by mutableStateOf(false)
        private set
    var answerText by mutableStateOf("")
        private set
    var gainedXp by mutableStateOf(0)
        private set
    var sessionAnswered by mutableStateOf(0)
        private set
    var sessionCorrect by mutableStateOf(0)
        private set
    val sessionSize = 8
    var sessionFinished by mutableStateOf(false)
        private set
    var questionKey by mutableStateOf(0)
        private set

    init {
        viewModelScope.launch {
            repo.seedIfNeeded()
            refreshSync()
            instrumentsLoaded = true
        }
    }

    private suspend fun refreshSync() {
        snapshot = repo.snapshot()
    }

    fun refresh() {
        viewModelScope.launch { refreshSync() }
    }

    fun selectInstrument(id: String) {
        instrument = id
        viewModelScope.launch {
            repo.setInstrument(id)
            refreshSync()
        }
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean("onboarded", true).apply()
        onboarded = true
    }

    fun startTopic(t: TopicDef) {
        topic = t
        sessionAnswered = 0
        sessionCorrect = 0
        sessionFinished = false
        nextQuestion()
    }

    fun nextQuestion() {
        val t = topic ?: return
        seedCounter += 1
        question = QuizGenerator.generate(t, seedCounter, instrument)
        answered = false
        wasCorrect = false
        answerText = ""
        gainedXp = 0
        questionKey += 1
    }

    /** Принимает ответ; correct считается на стороне экрана. */
    fun submit(correct: Boolean) {
        if (answered) return
        val t = topic ?: return
        val q = question ?: return
        answered = true
        wasCorrect = correct
        answerText = q.correctLabel()
        sessionAnswered += 1
        if (correct) sessionCorrect += 1
        viewModelScope.launch {
            val result = repo.recordAnswer(t, correct)
            gainedXp = result.xpGained
            refreshSync()
        }
        if (sessionAnswered >= sessionSize) sessionFinished = true
    }

    fun restartSession() {
        sessionAnswered = 0
        sessionCorrect = 0
        sessionFinished = false
        nextQuestion()
    }

    fun masteryOf(topicId: String): Double = snapshot?.mastery?.get(topicId) ?: 0.0

    fun isDue(topicId: String): Boolean {
        val srs = snapshot?.srs?.get(topicId) ?: return true
        return srs.dueAt <= System.currentTimeMillis()
    }

    fun dueCount(): Int = Topics.ALL.count { isDue(it.id) }

    fun instrumentTitle(): String = Instruments.title(instrument)

    fun nextDueTopic(): TopicDef {
        val dueId = snapshot?.dueTopicIds?.firstOrNull()
        return dueId?.let { Topics.byId(it) } ?: Topics.ALL.first()
    }
}
