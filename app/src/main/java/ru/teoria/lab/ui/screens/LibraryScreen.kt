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
import ru.teoria.lab.audio.ThemePlayer
import ru.teoria.lab.audio.Timbre
import ru.teoria.lab.core.ContentRepo
import ru.teoria.lab.core.Course
import ru.teoria.lab.core.MusicTheme
import ru.teoria.lab.core.ThemeRepo
import ru.teoria.lab.ui.Accent
import ru.teoria.lab.ui.AppViewModel
import ru.teoria.lab.ui.Blue
import ru.teoria.lab.ui.Green
import ru.teoria.lab.ui.Red
import ru.teoria.lab.ui.Surface1
import ru.teoria.lab.ui.Surface2
import ru.teoria.lab.ui.components.MasteryRing
import ru.teoria.lab.ui.components.SectionCard
import ru.teoria.lab.ui.components.SelectChip

@Composable
fun LibraryScreen(
    vm: AppViewModel,
    onOpenTopic: (String) -> Unit,
    onBack: () -> Unit
) {
    var expanded by remember { mutableStateOf<Set<String>>(emptySet()) }
    val courseContent = ContentRepo.course(vm.selectedCourse)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹ Назад", color = Blue, fontSize = 14.sp, modifier = Modifier.clickable { onBack() })
            Text("  Библиотека", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Полный курс: ${ContentRepo.sectionCount()} разделов, ${ContentRepo.topicCount()} тем, ${ContentRepo.cardsCount()} карточек музлитературы",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
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
        Spacer(Modifier.height(8.dp))
        Text(
            "${courseContent.sections.size} разделов · ${courseContent.topicCount} тем",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        courseContent.sections.forEach { section ->
            val isOpen = expanded.contains(section.id)
            val avg = section.topics.map { vm.masteryOf(it.id) }.average().takeIf { !it.isNaN() } ?: 0.0
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface1)
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expanded = if (isOpen) expanded - section.id else expanded + section.id
                        }
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(section.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            "${section.topics.size} тем · mastery ${(avg * 100).toInt()}%",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(if (isOpen) "▾" else "▸", color = Accent, fontSize = 16.sp)
                }
                if (isOpen) {
                    Spacer(Modifier.height(8.dp))
                    section.topics.forEach { topic ->
                        val mastery = vm.masteryOf(topic.id).toFloat()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onOpenTopic(topic.id) }
                                .padding(vertical = 7.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(topic.title, fontSize = 13.sp)
                                Text(
                                    topic.kind.title,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            MasteryRing(
                                progress = mastery,
                                label = "${(mastery * 100).toInt()}%",
                                sizeDp = 34,
                                color = if (mastery >= 0.8f) Green else if (mastery >= 0.5f) Accent else Red
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun TopicScreen(
    vm: AppViewModel,
    topicId: String,
    onTrain: (String) -> Unit,
    onBack: () -> Unit
) {
    val topic = ContentRepo.topic(topicId)
    if (topic == null) {
        Text("Тема не найдена", modifier = Modifier.padding(16.dp))
        return
    }
    val mastery = vm.masteryOf(topic.id).toFloat()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹ Назад", color = Blue, fontSize = 14.sp, modifier = Modifier.clickable { onBack() })
            Text("  ${topic.course.title}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(10.dp))
        Text(topic.title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "${topic.sectionTitle} · ${topic.kind.title}",
            fontSize = 11.sp,
            color = Accent
        )
        Spacer(Modifier.height(12.dp))
        SectionCard {
            topic.text.split("\n").filter { it.isNotBlank() }.forEach { paragraph ->
                Text(paragraph, fontSize = 14.sp, lineHeight = 21.sp)
                Spacer(Modifier.height(6.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MasteryRing(
                    progress = mastery,
                    label = "${(mastery * 100).toInt()}%",
                    sizeDp = 52,
                    color = if (mastery >= 0.8f) Green else if (mastery >= 0.5f) Accent else Red
                )
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text("Mastery темы", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        "доля верных за последние 10 попыток",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { mastery },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Accent,
                        trackColor = Surface2
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            SelectChip(
                text = "Тренировать эту тему",
                selected = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onTrain(topic.id) }
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

private val COMPOSER_ABOUT = mapOf(
    "Бах" to "Барокко: полифония, фуга, хорал; органист и кантор. Соединил немецкую традицию с итальянской и французской.",
    "Моцарт" to "Классицизм: ясность формы, мелодическая щедрость, опера и симфония. Писал во всех жанрах своей эпохи.",
    "Бетховен" to "Классицизм на границе с романтизмом: героический пафос, развитие мотивной работы, расширение формы.",
    "Чайковский" to "Романтизм: лиризм, балет, симфония, опера. Мелодия и оркестровая драматургия — основа стиля.",
    "Григ" to "Романтизм: норвежский колорит, лирические пьесы, музыка к «Пер Гюнту».",
    "Брамс" to "Романтизм с опорой на классические формы: плотная фактура, развитая гармония.",
    "Гайдн" to "Классицизм: отец симфонии и квартета, ясная логика формы, юмор.",
    "Мендельсон" to "Романтизм: прозрачная оркестровка, программная увертюра, музыка к Шекспиру.",
    "Вагнер" to "Романтизм: музыкальная драма, лейтмотивы, расширенная тональность.",
    "Бизе" to "Романтизм: французская опера, яркая сценическая характерность.",
    "Верди" to "Романтизм: итальянская опера, вокальная выразительность, драматизм.",
    "Римский-Корсаков" to "Романтизм: русская школа, оркестровая красочность, сказочные сюжеты.",
    "Шуберт" to "Романтизм: песня и вокальный цикл, лирическая мелодия.",
    "Шопен" to "Романтизм: фортепиано, мазурки, ноктюрны, этюды; польская национальная традиция.",
    "Сен-Санс" to "Романтизм: блеск, юмор, «Карнавал животных», французская школа.",
    "Дворжак" to "Романтизм: чешский фольклор, симфония «Из Нового Света».",
    "Гендель" to "Барокко: оратория, опера-seria, кончерто гроссо; работал в Англии.",
    "Вивальди" to "Барокко: скрипичный концерт, «Времена года», кончерто гроссо.",
    "Штраус" to "Романтизм: венский вальс и оперетта.",
    "Равель" to "Импрессионизм: оркестровая красочность, «Болеро», точность формы.",
    "Дебюсси" to "Импрессионизм: целотоновость, колорит, фортепианные прелюдии.",
    "Глинка" to "Русская классическая школа: опера, романс, симфонические фантазии.",
)

fun composerAbout(composer: String, epoch: String): String =
    COMPOSER_ABOUT[composer] ?: "Эпоха: $epoch. Темы композитора в приложении синтезируются из нотных данных."

@Composable
fun ComposersScreen(onOpen: (String) -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹ Назад", color = Blue, fontSize = 14.sp, modifier = Modifier.clickable { onBack() })
            Text("  Прослушать автора", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Только общественное достояние: композиторы, умершие до 1955 года. ${ThemeRepo.count()} тем, ${ThemeRepo.composerCount()} авторов.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        ThemeRepo.composers().forEach { composer ->
            val themes = ThemeRepo.byComposer(composer)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface1)
                    .clickable { onOpen(composer) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(Surface2),
                    contentAlignment = Alignment.Center
                ) {
                    Text(composer.take(1), color = Accent, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(composer, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${themes.firstOrNull()?.epoch ?: ""} · ${themes.firstOrNull()?.composerYears ?: ""} · ${themes.size} тем",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("▸", color = Accent)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun ComposerScreen(
    composer: String,
    onGuess: (String) -> Unit,
    onBack: () -> Unit
) {
    val themes = ThemeRepo.byComposer(composer)
    val epoch = themes.firstOrNull()?.epoch ?: ""
    val years = themes.firstOrNull()?.composerYears ?: ""
    var timbre by remember { mutableStateOf(Timbre.PIANO) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹ Назад", color = Blue, fontSize = 14.sp, modifier = Modifier.clickable { onBack() })
            Text("  Композитор", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(10.dp))
        Text(composer, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("$years · $epoch", fontSize = 12.sp, color = Accent)
        Spacer(Modifier.height(10.dp))
        SectionCard {
            Text(composerAbout(composer, epoch), fontSize = 13.sp, lineHeight = 19.sp)
            Spacer(Modifier.height(8.dp))
            Text("Темы в приложении: ${themes.size}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Timbre.values().forEach { option ->
                SelectChip(
                    text = option.title,
                    selected = timbre == option,
                    modifier = Modifier.weight(1f),
                    onClick = { timbre = option }
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        SelectChip(
            text = "▶ все темы подряд",
            selected = true,
            modifier = Modifier.fillMaxWidth(),
            onClick = { ThemePlayer.playAll(themes, bpm = 104, timbre = timbre) }
        )
        Spacer(Modifier.height(12.dp))
        themes.forEach { theme ->
            ThemeRow(theme, timbre)
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(8.dp))
        SelectChip(
            text = "Играть: угадай мелодию этого автора",
            selected = false,
            modifier = Modifier.fillMaxWidth(),
            onClick = { onGuess("lit-listen-composer") }
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Звук синтезируется из нотных данных — это модель исполнения, не запись. Правовой статус: общественное достояние.",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun ThemeRow(theme: MusicTheme, timbre: Timbre) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface1)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(theme.work, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                "${theme.notes.size} нот · ${"%.0f".format(theme.durationBeats)} долей",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "▶",
            color = Color(0xFF231A00),
            fontSize = 14.sp,
            modifier = Modifier
                .clip(CircleShape)
                .background(Accent)
                .clickable { ThemePlayer.play(theme, timbre = timbre) }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
