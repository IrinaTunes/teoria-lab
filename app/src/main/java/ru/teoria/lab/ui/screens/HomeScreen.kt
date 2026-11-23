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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.teoria.lab.core.Course
import ru.teoria.lab.core.Days
import ru.teoria.lab.core.Instruments
import ru.teoria.lab.core.Instrument
import ru.teoria.lab.core.Leveling
import ru.teoria.lab.core.SpacedRepetition
import ru.teoria.lab.core.TopicDef
import ru.teoria.lab.core.Topics
import ru.teoria.lab.ui.Accent
import ru.teoria.lab.ui.AppViewModel
import ru.teoria.lab.ui.Blue
import ru.teoria.lab.ui.Green
import ru.teoria.lab.ui.Red
import ru.teoria.lab.ui.Surface1
import ru.teoria.lab.ui.Surface2
import ru.teoria.lab.ui.components.MasteryRing
import ru.teoria.lab.ui.components.PrimaryButton
import ru.teoria.lab.ui.components.SectionCard
import ru.teoria.lab.ui.components.SelectChip

@Composable
fun InstrumentScreen(vm: AppViewModel, onDone: () -> Unit, firstRun: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        if (!firstRun) {
            Text(
                "← Назад",
                color = Blue,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onDone() }
            )
            Spacer(Modifier.height(12.dp))
        }
        Text("Твой инструмент", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(6.dp))
        Text(
            "Упражнения и подсказки подстроятся под инструмент. Выбор можно изменить в любой момент в настройках.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(18.dp))
        Instruments.ALL.forEach { instrument ->
            InstrumentRow(
                instrument = instrument,
                selected = vm.instrument == instrument.id,
                onClick = { vm.selectInstrument(instrument.id) }
            )
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(10.dp))
        PrimaryButton(
            text = if (firstRun) "Начать заниматься" else "Готово",
            onClick = {
                if (firstRun) vm.completeOnboarding()
                onDone()
            }
        )
        if (firstRun) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Всё работает офлайн: звук синтезируется на устройстве, интернет не нужен.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InstrumentRow(instrument: Instrument, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color(0xFF2A2A1C) else Surface1)
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (selected) Accent else Surface2),
            contentAlignment = Alignment.Center
        ) {
            Text(
                instrument.title.take(1),
                color = if (selected) Color(0xFF231A00) else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(instrument.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(instrument.hint, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(if (selected) "выбрано" else "", fontSize = 11.sp, color = Accent)
    }
}

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onOpenTopic: (TopicDef) -> Unit,
    onProgress: () -> Unit,
    onSettings: () -> Unit
) {
    val snapshot = vm.snapshot
    val profile = snapshot?.profile
    val level = profile?.level ?: 1
    val xp = profile?.xp ?: 0
    val progress = Leveling.progressInLevel(xp)
    val todayKey = Days.dayKey(System.currentTimeMillis())
    val todayCount = if (profile?.todayDate == todayKey) profile?.todayCount ?: 0 else 0
    val goal = profile?.dailyGoal ?: SpacedRepetition.DAILY_GOAL
    val due = vm.dueCount()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Теория", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    "сольфеджио · теория · музлитература",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "⚙",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { onSettings() }.padding(8.dp)
            )
        }
        Spacer(Modifier.height(14.dp))

        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MasteryRing(
                    progress = progress,
                    label = "ур. $level",
                    sizeDp = 62,
                    color = Accent
                )
                Column(Modifier.padding(start = 14.dp).weight(1f)) {
                    Text(
                        "${Leveling.levelTitle(level)} · $xp XP",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = Accent,
                        trackColor = Surface2
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "до уровня ${(level + 1).coerceAtMost(Leveling.MAX_LEVEL)}: ${Leveling.xpToNextLevel(xp)} XP",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("серия", "${profile?.streak ?: 0} дн.", Accent, Modifier.weight(1f))
                StatChip("сегодня", "$todayCount / $goal", Green, Modifier.weight(1f))
                StatChip("к повторению", "$due", if (due > 0) Red else Blue, Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionCard {
            Text("Умная очередь", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Сначала просроченные темы и те, где mastery ниже. Интервалы: 1 · 3 · 7 · 14 · 30 дней.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            val next = vm.nextDueTopic()
            PrimaryButton(
                text = "Тренировка по очереди → ${next.title}",
                onClick = { onOpenTopic(next) }
            )
        }

        Spacer(Modifier.height(16.dp))
        Text("Курсы", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Course.values().forEach { course ->
                SelectChip(
                    text = course.title,
                    selected = vm.selectedCourse == course,
                    modifier = Modifier.weight(1f),
                    onClick = { vm.selectedCourse = course }
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        Topics.byCourse(vm.selectedCourse).forEach { topic ->
            val mastery = vm.masteryOf(topic.id).toFloat()
            val dueTopic = vm.isDue(topic.id)
            TopicRow(topic, mastery, dueTopic) { onOpenTopic(topic) }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedAction("Прогресс", Modifier.weight(1f)) { onProgress() }
            OutlinedAction("Настройки", Modifier.weight(1f)) { onSettings() }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Инструмент: ${Instruments.title(vm.instrument)}",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Surface2.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun TopicRow(topic: TopicDef, mastery: Float, due: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(topic.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                if (due) {
                    Text(
                        "  к повторению",
                        fontSize = 10.sp,
                        color = Red,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
            Text(topic.subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { mastery },
                modifier = Modifier.fillMaxWidth(0.9f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = when {
                    mastery >= 0.8f -> Green
                    mastery >= 0.5f -> Accent
                    else -> Red
                },
                trackColor = Surface2
            )
        }
        MasteryRing(
            progress = mastery,
            label = "${(mastery * 100).toInt()}%",
            sizeDp = 44,
            color = if (mastery >= 0.8f) Green else if (mastery >= 0.5f) Accent else Red
        )
    }
}

@Composable
private fun OutlinedAction(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Surface2)
    ) {
        Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
