package ru.teoria.lab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.teoria.lab.ui.Accent
import ru.teoria.lab.ui.Surface1

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1)
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun StatPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Surface1, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, fontSize = 10.sp, color = MaterialThemeColors.muted)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

/** Небольшой держатель цветов, чтобы не тянуть MaterialTheme в каждый вызов. */
object MaterialThemeColors {
    val muted = Color(0xFF9AA0B0)
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Accent
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color(0xFF231A00),
            disabledContainerColor = Color(0xFF33374A),
            disabledContentColor = Color(0xFF7C8296)
        )
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
fun OptionButton(
    text: String,
    enabled: Boolean = true,
    state: OptionState = OptionState.NEUTRAL,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = when (state) {
        OptionState.NEUTRAL -> Color(0xFF232733)
        OptionState.CORRECT -> Color(0xFF1F4B38)
        OptionState.WRONG -> Color(0xFF4B2327)
        OptionState.SELECTED -> Color(0xFF2E3446)
    }
    val border = when (state) {
        OptionState.NEUTRAL -> Color(0xFF343A4C)
        OptionState.CORRECT -> Color(0xFF52C08A)
        OptionState.WRONG -> Color(0xFFE06C6C)
        OptionState.SELECTED -> Accent
    }
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, border),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = bg, contentColor = Color(0xFFECEDF2))
    ) {
        Text(text, fontSize = 15.sp)
    }
}

enum class OptionState { NEUTRAL, SELECTED, CORRECT, WRONG }

@Composable
fun RowOfPills(vararg items: Pair<String, String>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { (label, value) ->
            StatPill(label, value, Color(0xFFECEDF2), Modifier.weight(1f))
        }
    }
}
