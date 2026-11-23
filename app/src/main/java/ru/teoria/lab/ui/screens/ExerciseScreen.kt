package ru.teoria.lab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.teoria.lab.audio.Synth
import ru.teoria.lab.audio.ThemePlayer
import ru.teoria.lab.audio.Timbre
import ru.teoria.lab.core.Chords
import ru.teoria.lab.core.Guitar
import ru.teoria.lab.core.Intervals
import ru.teoria.lab.core.Question
import ru.teoria.lab.core.Rhythm
import ru.teoria.lab.core.Topics
import ru.teoria.lab.core.Violin
import ru.teoria.lab.core.correctLabel
import ru.teoria.lab.core.signLabel
import ru.teoria.lab.ui.Accent
import ru.teoria.lab.ui.AppViewModel
import ru.teoria.lab.ui.Blue
import ru.teoria.lab.ui.Green
import ru.teoria.lab.ui.Red
import ru.teoria.lab.ui.Surface1
import ru.teoria.lab.ui.Surface2
import ru.teoria.lab.ui.components.Fretboard
import ru.teoria.lab.ui.components.KeySignBadge
import ru.teoria.lab.ui.components.NotePicker
import ru.teoria.lab.ui.components.OptionButton
import ru.teoria.lab.ui.components.OptionState
import ru.teoria.lab.ui.components.PianoKeyboard
import ru.teoria.lab.ui.components.PrimaryButton
import ru.teoria.lab.ui.components.SectionCard
import ru.teoria.lab.ui.components.SelectChip
import ru.teoria.lab.ui.components.StaffView

@Composable
fun ExerciseScreen(vm: AppViewModel, topicId: String, onBack: () -> Unit, onOpenComposer: (String) -> Unit = {}) {
    val topic = Topics.byId(topicId)
    LaunchedEffect(topicId) { vm.startTopic(topic) }
    var showSummary by remember(topicId) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("‹ Назад", fontSize = 14.sp, color = Blue, modifier = Modifier.clickable { onBack() })
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(topic.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(topic.course.title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "${(vm.sessionAnswered + if (vm.answered) 0 else 1).coerceAtMost(vm.sessionSize)} / ${vm.sessionSize}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { vm.sessionAnswered.toFloat() / vm.sessionSize },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = Accent,
            trackColor = Surface2
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (showSummary) {
                SessionSummary(vm) {
                    showSummary = false
                    vm.restartSession()
                }
            } else {
                val q = vm.question
                if (q == null) {
                    Text("Готовим задание…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(q.prompt, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(14.dp))
                    ExerciseBody(vm, q, onOpenComposer)
                }
            }
        }

        if (!showSummary) {
            FeedbackBar(
                vm = vm,
                onNext = {
                    if (vm.sessionFinished) showSummary = true else vm.nextQuestion()
                }
            )
        }
    }
}

@Composable
private fun FeedbackBar(vm: AppViewModel, onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface1)
            .padding(16.dp)
    ) {
        if (vm.answered) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(24.dp).clip(CircleShape)
                        .background(if (vm.wasCorrect) Green else Red),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (vm.wasCorrect) "✓" else "✕", color = Color(0xFF101219), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(
                        if (vm.wasCorrect) "Верно! +${vm.gainedXp} XP" else "Ошибка · +0 XP",
                        color = if (vm.wasCorrect) Green else Red,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        "Ответ: ${vm.answerText}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            PrimaryButton(
                text = if (vm.sessionFinished) "Итоги сессии" else "Далее",
                onClick = onNext
            )
        } else {
            Text(
                "Выбери ответ — обратная связь и звук мгновенные",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SessionSummary(vm: AppViewModel, onRestart: () -> Unit) {
    val accuracy = if (vm.sessionAnswered == 0) 0 else vm.sessionCorrect * 100 / vm.sessionAnswered
    SectionCard {
        Text("Сессия завершена", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Верных: ${vm.sessionCorrect} из ${vm.sessionAnswered}", fontSize = 15.sp)
        Text("Точность: $accuracy%", fontSize = 15.sp, color = if (accuracy >= 70) Green else Red)
        Spacer(Modifier.height(6.dp))
        Text(
            "Интервал повторения темы обновлён: при ошибке он сбрасывается на 1 день, при верных ответах растёт 1 → 3 → 7 → 14 → 30.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Ещё раз", onRestart)
    }
}

@Composable
private fun ExerciseBody(vm: AppViewModel, q: Question, onOpenComposer: (String) -> Unit) {
    when (q) {
        is Question.FindNote -> FindNoteBody(vm, q)
        is Question.EarInterval -> EarIntervalBody(vm, q)
        is Question.VisualInterval -> VisualIntervalBody(vm, q)
        is Question.ChordEar -> ChordBody(vm, q)
        is Question.KeySigns -> KeySignsBody(vm, q)
        is Question.RhythmTask -> RhythmBody(vm, q)
        is Question.Lit -> LitBody(vm, q)
        is Question.Listen -> ListenBody(vm, q, onOpenComposer)
    }
}

@Composable
private fun FindNoteBody(vm: AppViewModel, q: Question.FindNote) {
    val target = q.target
    val answered = vm.answered
    Column {
        when (vm.instrument) {
            "piano" -> {
                PianoKeyboard(
                    startMidi = 60,
                    octaves = 2,
                    highlight = if (answered) setOf(target.midi) else emptySet(),
                    enabled = !answered
                ) { midi ->
                    Synth.playNote(midi, 420)
                    vm.submit(midi == target.midi)
                }
                Caption("Тапни клавишу: две октавы, от до первой до си второй")
            }
            "guitar" -> {
                Fretboard(
                    openStrings = Guitar.OPEN.toList(),
                    stringLabels = Guitar.STRING_NAMES,
                    frets = Guitar.FRETS,
                    highlight = if (answered) Guitar.positionsFor(target.midi) else emptyList(),
                    enabled = !answered
                ) { string, fret ->
                    val midi = Guitar.OPEN[string] + fret
                    Synth.playNote(midi, 500)
                    vm.submit(midi == target.midi)
                }
                Caption("Строй E2 A2 D3 G3 B3 E4. Подсказка: один лад = один полутон")
            }
            "violin" -> {
                Fretboard(
                    openStrings = Violin.OPEN.toList(),
                    stringLabels = Violin.STRING_NAMES,
                    frets = Violin.POSITIONS,
                    highlight = if (answered) Violin.positionsFor(target.midi) else emptyList(),
                    enabled = !answered
                ) { string, step ->
                    val midi = Violin.OPEN[string] + step
                    Synth.playNote(midi, 500)
                    vm.submit(midi == target.midi)
                }
                Caption("Струны G3 D4 A4 E5, позиции 0–12 полутонов")
            }
            else -> {
                StaffView(notes = if (answered) listOf(target) else emptyList(), caption = "нотоносец, скрипичный ключ")
                Spacer(Modifier.height(8.dp))
                NotePicker(enabled = !answered) { note ->
                    Synth.playNote(note.midi, 420)
                    vm.submit(note.midi == target.midi)
                }
            }
        }
        if (answered) {
            Spacer(Modifier.height(12.dp))
            SectionCard {
                Text("Правильный ответ: ${target.fullRu}", fontSize = 13.sp, color = Green)
                Text(
                    "MIDI ${target.midi} · ${String.format("%.1f", ru.teoria.lab.core.midiToFrequency(target.midi))} Гц",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Caption(text: String) {
    Text(
        text,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun EarIntervalBody(vm: AppViewModel, q: Question.EarInterval) {
    LaunchedEffect(vm.questionKey) {
        delay(150)
        Synth.playSequence(listOf(q.low.midi, q.high.midi), noteMs = 620, gapMs = 120)
    }
    var selected by remember(vm.questionKey) { mutableStateOf<String?>(null) }
    Column {
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Две ноты, последовательно", fontSize = 13.sp)
                    Text(
                        "Низкая: ${q.low.shortRu} · высокая: ${q.high.shortRu}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "▶ ещё раз",
                    color = Accent,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable {
                        Synth.playSequence(listOf(q.low.midi, q.high.midi), noteMs = 620, gapMs = 120)
                    }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        q.options.forEach { option ->
            val state = optionState(vm.answered, option, q.answer, selected)
            OptionButton(option, enabled = !vm.answered, state = state) {
                selected = option
                vm.submit(option == q.answer)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun optionState(answered: Boolean, option: String, correct: String, selected: String?): OptionState = when {
    !answered && option == selected -> OptionState.SELECTED
    !answered -> OptionState.NEUTRAL
    option == correct -> OptionState.CORRECT
    option == selected -> OptionState.WRONG
    else -> OptionState.NEUTRAL
}

@Composable
private fun VisualIntervalBody(vm: AppViewModel, q: Question.VisualInterval) {
    var selected by remember(vm.questionKey) { mutableStateOf<String?>(null) }
    Column {
        SectionCard {
            StaffView(notes = listOf(q.low, q.high), caption = "две ноты: ${q.low.shortRu} — ${q.high.shortRu}")
            Spacer(Modifier.height(4.dp))
            Text(
                "Полутонов: ${Intervals.semitonesBetween(q.low, q.high)}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(12.dp))
        q.options.forEach { option ->
            OptionButton(option, enabled = !vm.answered, state = optionState(vm.answered, option, q.answer, selected)) {
                selected = option
                vm.submit(option == q.answer)
                Synth.playSequence(listOf(q.low.midi, q.high.midi), noteMs = 500)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ChordBody(vm: AppViewModel, q: Question.ChordEar) {
    LaunchedEffect(vm.questionKey) {
        delay(150)
        Synth.playChord(Chords.midiFromOffsets(q.root.midi, q.offsets), 1100)
    }
    var selected by remember(vm.questionKey) { mutableStateOf<String?>(null) }
    Column {
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Аккорд от ${q.root.shortRu}", fontSize = 13.sp)
                    Text(
                        "Мажор 0-4-7 · минор 0-3-7 · ум. 0-3-6 · ув. 0-4-8",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "▶ ещё раз",
                    color = Accent,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable {
                        Synth.playChord(Chords.midiFromOffsets(q.root.midi, q.offsets), 1100)
                    }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        q.options.forEach { option ->
            OptionButton(option, enabled = !vm.answered, state = optionState(vm.answered, option, q.type, selected)) {
                selected = option
                vm.submit(option == q.type)
            }
            Spacer(Modifier.height(8.dp))
        }
        if (vm.answered) {
            Spacer(Modifier.height(4.dp))
            SectionCard {
                Text("${q.type}: ${q.offsets.joinToString("-")} полутонов", fontSize = 12.sp, color = Green)
                when (vm.instrument) {
                    "guitar" -> {
                        val shape = Chords.midiFromOffsets(q.root.midi, q.offsets)
                            .mapNotNull { midi -> Guitar.positionsFor(midi).firstOrNull() }
                        if (shape.isNotEmpty()) {
                            Fretboard(
                                openStrings = Guitar.OPEN.toList(),
                                stringLabels = Guitar.STRING_NAMES,
                                highlight = shape,
                                enabled = false
                            ) { _, _ -> }
                            Text(
                                "Аппликатура: " + shape.joinToString(" · ") { (s, f) ->
                                    "${Guitar.STRING_NAMES[s]}:${if (f == 0) "открытая" else "$f лад"}"
                                },
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    "piano" -> {
                        PianoKeyboard(
                            startMidi = 60,
                            octaves = 2,
                            highlight = Chords.midiFromOffsets(q.root.midi, q.offsets).toSet(),
                            enabled = false
                        ) { }
                    }
                    else -> {
                        StaffView(notes = Chords.midiFromOffsets(q.root.midi, q.offsets).map { ru.teoria.lab.core.Note.fromMidi(it) }, height = 170)
                    }
                }
            }
        }
    }
}

@Composable
private fun KeySignsBody(vm: AppViewModel, q: Question.KeySigns) {
    var selected by remember(vm.questionKey) { mutableStateOf<String?>(null) }
    val correct = if (q.askCount) signLabel(q.key.signCount, q.key.hasSharps) else q.key.nameRu
    Column {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            if (q.askCount) {
                Text(q.key.nameRu, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Accent)
            } else {
                Text(signLabel(q.key.signCount, q.key.hasSharps), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Accent)
                Text("знаки тональности", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            KeySignBadge(q.key)
        }
        Spacer(Modifier.height(16.dp))
        q.options.forEach { option ->
            OptionButton(option, enabled = !vm.answered, state = optionState(vm.answered, option, correct, selected)) {
                selected = option
                vm.submit(option == correct)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RhythmBody(vm: AppViewModel, q: Question.RhythmTask) {
    var startAt by remember(vm.questionKey) { mutableStateOf(0L) }
    var taps by remember(vm.questionKey) { mutableStateOf(listOf<Long>()) }
    val expected = remember(startAt, q) {
        if (startAt == 0L) emptyList() else Rhythm.beatTimes(q.bpm, q.beats, startAt)
    }
    LaunchedEffect(startAt) {
        if (startAt > 0L) {
            delay(700)
            Synth.playCadence(q.bpm, q.beats)
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${q.bpm} уд/мин · ${q.meter} · ${q.beats} доли", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (i in 0 until q.beats) {
                val filled = taps.size > i
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (filled) Accent else Surface2),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${i + 1}", color = if (filled) Color(0xFF231A00) else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .size(170.dp)
                .clip(CircleShape)
                .background(if (vm.answered) Surface2 else Accent)
                .clickable(enabled = !vm.answered) {
                    if (startAt == 0L) {
                        startAt = System.currentTimeMillis() + 700
                        taps = emptyList()
                        return@clickable
                    }
                    val now = System.currentTimeMillis()
                    val newTaps = taps + now
                    taps = newTaps
                    if (newTaps.size >= q.beats) {
                        val hits = Rhythm.scoreTaps(newTaps, expected)
                        vm.submit(hits >= q.beats - 1)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                when {
                    vm.answered -> "готово"
                    startAt == 0L -> "Старт\nметронома"
                    else -> "ТАП\n${taps.size}/${q.beats}"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (vm.answered) MaterialTheme.colorScheme.onSurface else Color(0xFF231A00),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Допуск ±${Rhythm.TOLERANCE_MS} мс. После «Старт» прозвучат ${q.beats} клика — стучи в такт большим кругом.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (expected.isNotEmpty() && taps.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Попаданий: ${Rhythm.scoreTaps(taps, expected)} из ${q.beats}",
                fontSize = 12.sp,
                color = if (!vm.answered) Blue else if (vm.wasCorrect) Green else Red
            )
        }
    }
}

@Composable
private fun LitBody(vm: AppViewModel, q: Question.Lit) {
    var selected by remember(vm.questionKey) { mutableStateOf<String?>(null) }
    Column {
        q.options.forEach { option ->
            OptionButton(option, enabled = !vm.answered, state = optionState(vm.answered, option, q.answer, selected)) {
                selected = option
                vm.submit(option == q.answer)
            }
            Spacer(Modifier.height(8.dp))
        }
        if (vm.answered) {
            Spacer(Modifier.height(4.dp))
            SectionCard {
                Text(q.card.composer, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Эпоха: ${q.card.epoch}", fontSize = 12.sp, color = Accent)
                Text("Произведение: ${q.card.work}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Жанр: ${q.card.genre}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ListenBody(vm: AppViewModel, q: Question.Listen, onOpenComposer: (String) -> Unit) {
    val theme = vm.themeById(q.themeId)
    var selected by remember(vm.questionKey) { mutableStateOf<String?>(null) }
    var timbre by remember(vm.questionKey) { mutableStateOf(Timbre.PIANO) }
    var fragmentNotes by remember(vm.questionKey) { mutableStateOf(16) }
    LaunchedEffect(vm.questionKey) {
        if (theme != null && theme.notes.isNotEmpty()) {
            delay(250)
            ThemePlayer.play(theme, bpm = 100, timbre = timbre, maxNotes = fragmentNotes)
        }
    }
    Column {
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (theme == null) "Тема недоступна" else "Тема · ${theme.notes.size} нот",
                        fontSize = 13.sp
                    )
                    Text(
                        "синтез из нотных данных, не запись",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "▶ ещё раз",
                    color = Accent,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable {
                        theme?.let { ThemePlayer.play(it, bpm = 100, timbre = timbre, maxNotes = fragmentNotes) }
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectChip("4 такта", fragmentNotes == 16, Modifier.weight(1f)) { fragmentNotes = 16 }
                SelectChip("8 тактов", fragmentNotes == 32, Modifier.weight(1f)) { fragmentNotes = 32 }
                SelectChip("струнные", timbre == Timbre.STRINGS, Modifier.weight(1f)) {
                    timbre = if (timbre == Timbre.STRINGS) Timbre.PIANO else Timbre.STRINGS
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        q.options.forEach { option ->
            OptionButton(option, enabled = !vm.answered, state = optionState(vm.answered, option, q.answer, selected)) {
                selected = option
                vm.submit(option == q.answer)
            }
            Spacer(Modifier.height(8.dp))
        }
        if (vm.answered) {
            Spacer(Modifier.height(4.dp))
            SectionCard {
                Text(q.work, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(q.composer, fontSize = 12.sp, color = Accent)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectChip("▶ послушать ещё раз", false, Modifier.weight(1f)) {
                        theme?.let { ThemePlayer.play(it, bpm = 100, timbre = timbre) }
                    }
                    SelectChip("В карточку композитора", false, Modifier.weight(1f)) {
                        onOpenComposer(q.composer)
                    }
                }
            }
        }
    }
}
