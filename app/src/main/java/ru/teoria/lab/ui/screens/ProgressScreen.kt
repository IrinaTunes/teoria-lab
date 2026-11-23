package ru.teoria.lab.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.teoria.lab.core.Days
import ru.teoria.lab.core.Leveling
import ru.teoria.lab.core.SpacedRepetition
import ru.teoria.lab.core.Topics
import ru.teoria.lab.ui.Accent
import ru.teoria.lab.ui.AppViewModel
import ru.teoria.lab.ui.Blue
import ru.teoria.lab.ui.Green
import ru.teoria.lab.ui.Red
import ru.teoria.lab.ui.Surface2
import ru.teoria.lab.ui.components.MasteryRing
import ru.teoria.lab.ui.components.SectionCard
import ru.teoria.lab.ui.components.drawLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

@Composable
fun ProgressScreen(vm: AppViewModel, onBack: () -> Unit) {
    val snapshot = vm.snapshot
    val profile = snapshot?.profile
    val xp = profile?.xp ?: 0
    val level = Leveling.levelForXp(xp)
    val totalAttempts = snapshot?.attemptsByTopic?.values?.sumOf { it.size } ?: 0
    val totalCorrect = snapshot?.attemptsByTopic?.values?.sumOf { list -> list.count { it } } ?: 0
    val accuracy = if (totalAttempts == 0) 0 else totalCorrect * 100 / totalAttempts

    val now = System.currentTimeMillis()
    val dayLabels = remember(now) {
        val fmt = SimpleDateFormat("d.M", Locale("ru"))
        (13 downTo 0).map { back ->
            val ts = now - back * SpacedRepetition.DAY_MS
            if (back == 0) "сег" else fmt.format(Date(ts))
        }
    }
    val values = remember(snapshot?.xpByDay) {
        (13 downTo 0).map { back ->
            val key = Days.dayKey(System.currentTimeMillis() - back * SpacedRepetition.DAY_MS)
            snapshot?.xpByDay?.get(key) ?: 0
        }
    }
    val maxValue = (values.maxOrNull() ?: 0).coerceAtLeast(10)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹ Назад", color = Blue, fontSize = 14.sp, modifier = Modifier.clickable { onBack() })
            Text(
                "  Прогресс",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(Modifier.height(14.dp))

        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MasteryRing(Leveling.progressInLevel(xp), "ур. $level", sizeDp = 74, color = Accent)
                Column(Modifier.padding(start = 14.dp).weight(1f)) {
                    Text("${Leveling.levelTitle(level)} · $xp XP", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "уровень $level из ${Leveling.MAX_LEVEL}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { Leveling.progressInLevel(xp) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = Accent,
                        trackColor = Surface2
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStat("серия", "${profile?.streak ?: 0} дн.", Accent, Modifier.weight(1f))
                MiniStat("ответов", "$totalAttempts", Blue, Modifier.weight(1f))
                MiniStat("точность", "$accuracy%", if (accuracy >= 70) Green else Red, Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Порог следующего уровня = 100 × уровень. XP: +10 за верный ответ, ×1,5 при серии из трёх и более.",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(12.dp))
        SectionCard {
            Text("XP за 14 дней", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                "всего за период: ${values.sum()} XP",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                val count = values.size
                val slot = size.width / count
                val baseY = size.height - 18.dp.toPx()
                val usable = baseY - 8.dp.toPx()
                val muted = Color(0xFF7C8296)
                for (i in 0 until count) {
                    val value = values[i]
                    val h = if (maxValue == 0) 0f else (value.toFloat() / maxValue) * usable
                    val barW = slot * 0.62f
                    val x = i * slot + (slot - barW) / 2
                    drawRoundRect(
                        color = if (i == count - 1) Accent else Accent.copy(alpha = 0.55f),
                        topLeft = Offset(x, baseY - h.coerceAtLeast(2f)),
                        size = Size(barW, h.coerceAtLeast(2f)),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    if (i % 3 == 0 || i == count - 1) {
                        drawLabel(dayLabels[i], i * slot + slot / 2, size.height - 4.dp.toPx(), 8.sp.toPx(), muted)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionCard {
            Text("Mastery по темам", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(
                "% верных за последние 10 попыток; пока попыток меньше — знаменатель всё равно 10.",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Topics.ALL.forEach { topic ->
                val mastery = (snapshot?.mastery?.get(topic.id) ?: 0.0).toFloat()
                val srs = snapshot?.srs?.get(topic.id)
                val dueText = when {
                    srs == null -> "не начато"
                    srs.dueAt <= now -> "к повторению"
                    else -> "через ${ceil((srs.dueAt - now).toDouble() / SpacedRepetition.DAY_MS).toInt()} дн."
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(topic.title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "${topic.course.title} · $dueText · интервал ${srs?.intervalDays ?: 0} дн.",
                            fontSize = 10.sp,
                            color = if (srs != null && srs.dueAt <= now) Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    MasteryRing(
                        progress = mastery,
                        label = "${(mastery * 100).toInt()}%",
                        sizeDp = 38,
                        color = if (mastery >= 0.8f) Green else if (mastery >= 0.5f) Accent else Red
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionCard {
            Text("Интервальное повторение", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Верный ответ переводит тему на следующий интервал: 1 → 3 → 7 → 14 → 30 дней. Ошибка сбрасывает интервал на 1 день. В очередь упражнений первыми попадают просроченные и слабые темы.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
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
