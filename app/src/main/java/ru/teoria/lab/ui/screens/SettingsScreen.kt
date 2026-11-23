package ru.teoria.lab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import ru.teoria.lab.core.Instruments
import ru.teoria.lab.ui.Accent
import ru.teoria.lab.ui.AppViewModel
import ru.teoria.lab.ui.Blue
import ru.teoria.lab.ui.Surface1
import ru.teoria.lab.ui.components.SectionCard

@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit, onOpenInstrument: () -> Unit) {
    var soundOn by remember { mutableStateOf(true) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹ Назад", color = Blue, fontSize = 14.sp, modifier = Modifier.clickable { onBack() })
            Text("  Настройки", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(14.dp))

        SectionCard {
            Text("Инструмент", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(
                "Влияет на клавиатуру, гриф, струны, лестницу ступеней и подсказки во всех упражнениях.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Column {
                Instruments.ALL.forEach { instrument ->
                    val selected = vm.instrument == instrument.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) Color(0xFF2A2A1C) else Surface1)
                            .clickable { vm.selectInstrument(instrument.id) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (selected) "●" else "○",
                            color = if (selected) Accent else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                        Column(Modifier.padding(start = 10.dp).weight(1f)) {
                            Text(instrument.title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(instrument.hint, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Открыть экран выбора инструмента",
                fontSize = 12.sp,
                color = Accent,
                modifier = Modifier.clickable { onOpenInstrument() }
            )
        }

        Spacer(Modifier.height(12.dp))
        SectionCard {
            Text("Занятия", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Цель на день", fontSize = 14.sp)
                    Text("20 ответов в день", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("20", color = Accent, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Звук упражнений", fontSize = 14.sp)
                    Text(
                        "Синтез AudioTrack, офлайн. Выключите, если занимаетесь без звука.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = soundOn,
                    onCheckedChange = { soundOn = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Accent)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionCard {
            Text("О приложении", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
            Text("«Теория» 1.0 — тренажёр сольфеджио, теории музыки и музлитературы.", fontSize = 12.sp)
            Text("Работает офлайн, без сети. Разрешение POST_NOTIFICATIONS объявлено, но уведомления не отправляются.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Данные прогресса хранятся локально (Room + SharedPreferences).", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text("Сборка: GitHub Actions, debug APK.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(30.dp))
    }
}
