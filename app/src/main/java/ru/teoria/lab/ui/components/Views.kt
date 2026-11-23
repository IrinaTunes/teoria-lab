package ru.teoria.lab.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.teoria.lab.core.LETTER_RU
import ru.teoria.lab.core.LETTER_SEMITONES
import ru.teoria.lab.core.MAJOR_STEPS
import ru.teoria.lab.core.Note
import ru.teoria.lab.core.octaveRu
import ru.teoria.lab.ui.Accent
import ru.teoria.lab.ui.Blue
import ru.teoria.lab.ui.Green
import ru.teoria.lab.ui.Surface1
import ru.teoria.lab.ui.Surface2
import kotlin.math.abs
import kotlin.math.floor

fun DrawScope.drawLabel(
    text: String,
    x: Float,
    y: Float,
    sizePx: Float,
    color: Color,
    bold: Boolean = false,
    center: Boolean = true
) {
    val paint = android.graphics.Paint().apply {
        this.color = color.toArgb()
        textSize = sizePx
        textAlign = if (center) android.graphics.Paint.Align.CENTER else android.graphics.Paint.Align.LEFT
        isAntiAlias = true
        typeface = if (bold) {
            android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        } else {
            android.graphics.Typeface.DEFAULT
        }
    }
    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
}

private val WHITE_PCS = setOf(0, 2, 4, 5, 7, 9, 11)

/** Клавиатура на две октавы: белые и чёрные клавиши, тап по клавише отдаёт MIDI. */
@Composable
fun PianoKeyboard(
    modifier: Modifier = Modifier,
    startMidi: Int = 60,
    octaves: Int = 2,
    highlight: Set<Int> = emptySet(),
    enabled: Boolean = true,
    onKey: (Int) -> Unit
) {
    val whiteNotes = remember(startMidi, octaves) {
        (startMidi until startMidi + octaves * 12).filter { it % 12 in WHITE_PCS }
    }
    val blackNotes = remember(startMidi, octaves) {
        val list = mutableListOf<Pair<Int, Int>>()
        for (m in startMidi until startMidi + octaves * 12) {
            if (m % 12 !in WHITE_PCS) {
                val idx = whiteNotes.indexOfLast { it < m }
                list.add(m to idx)
            }
        }
        list
    }
    val onSurface = MaterialTheme.colorScheme.onSurface
    val keyLabel = MaterialTheme.colorScheme.onSurfaceVariant
    val blackKeyColor = Color(0xFF0E1016)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(enabled, whiteNotes) {
                detectTapGestures { offset ->
                    if (!enabled || whiteNotes.isEmpty()) return@detectTapGestures
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val whiteW = w / whiteNotes.size
                    val blackW = whiteW * 0.62f
                    val blackH = h * 0.62f
                    if (offset.y <= blackH) {
                        for ((midi, idx) in blackNotes) {
                            val cx = (idx + 1) * whiteW
                            if (offset.x >= cx - blackW / 2 && offset.x <= cx + blackW / 2) {
                                onKey(midi)
                                return@detectTapGestures
                            }
                        }
                    }
                    val index = floor(offset.x / whiteW).toInt().coerceIn(0, whiteNotes.size - 1)
                    onKey(whiteNotes[index])
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val whiteW = w / whiteNotes.size
        whiteNotes.forEachIndexed { i, midi ->
            val x = i * whiteW
            val isHi = midi in highlight
            drawRoundRect(
                color = if (isHi) Accent else Color(0xFFF4F4F6),
                topLeft = Offset(x, 0f),
                size = Size(whiteW - 2f, h),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = Color(0xFF3A3E4C),
                topLeft = Offset(x, 0f),
                size = Size(whiteW - 2f, h),
                cornerRadius = CornerRadius(6f, 6f),
                style = Stroke(width = 2f)
            )
            val note = Note.fromMidi(midi)
            if (note.accidental == 0) {
                drawLabel(note.shortRu, x + whiteW / 2, h - 10f, 11.sp.toPx(), if (isHi) Color(0xFF231A00) else keyLabel)
            }
        }
        blackNotes.forEach { (midi, idx) ->
            val cx = (idx + 1) * whiteW
            val isHi = midi in highlight
            drawRoundRect(
                color = if (isHi) Accent else blackKeyColor,
                topLeft = Offset(cx - whiteW * 0.31f, 0f),
                size = Size(whiteW * 0.62f, h * 0.62f),
                cornerRadius = CornerRadius(5f, 5f)
            )
            drawRoundRect(
                color = Color(0xFF2C3040),
                topLeft = Offset(cx - whiteW * 0.31f, 0f),
                size = Size(whiteW * 0.62f, h * 0.62f),
                cornerRadius = CornerRadius(5f, 5f),
                style = Stroke(width = 2f)
            )
        }
        drawLabel(
            "до первой октавы → до третьей",
            w / 2,
            h - 1f,
            8.sp.toPx(),
            onSurface.copy(alpha = 0.0f)
        )
    }
}

/** Гриф: 6 (или 4) струн × 12 ладов, сетка точек, тап по ближайшей точке. */
@Composable
fun Fretboard(
    openStrings: List<Int>,
    stringLabels: List<String>,
    modifier: Modifier = Modifier,
    frets: Int = 12,
    highlight: List<Pair<Int, Int>> = emptyList(),
    enabled: Boolean = true,
    onPick: (Int, Int) -> Unit
) {
    val stringCount = openStrings.size
    val height = (stringCount * 30 + 46).dp
    val onSurface = MaterialTheme.colorScheme.onSurfaceVariant
    val boardColor = Color(0xFF2A2E3A)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(enabled, openStrings, frets) {
                detectTapGestures { offset ->
                    if (!enabled) return@detectTapGestures
                    val left = 34.dp.toPx()
                    val right = 10.dp.toPx()
                    val top = 16.dp.toPx()
                    val bottom = 26.dp.toPx()
                    val w = size.width - left - right
                    val h = size.height - top - bottom
                    val fretW = w / (frets + 1)
                    var best = Pair(0, 0)
                    var bestDist = Float.MAX_VALUE
                    for (s in 0 until stringCount) {
                        for (f in 0..frets) {
                            val cx = left + (f + 0.5f) * fretW
                            val cy = top + h * s / (stringCount - 1).coerceAtLeast(1)
                            val d = abs(offset.x - cx) + abs(offset.y - cy)
                            if (d < bestDist) {
                                bestDist = d
                                best = s to f
                            }
                        }
                    }
                    onPick(best.first, best.second)
                }
            }
    ) {
        val left = 34.dp.toPx()
        val right = 10.dp.toPx()
        val top = 16.dp.toPx()
        val bottom = 26.dp.toPx()
        val w = size.width - left - right
        val h = size.height - top - bottom
        val fretW = w / (frets + 1)

        // корпус
        drawRoundRect(
            color = Color(0xFF191C25),
            topLeft = Offset(left, top - 8f),
            size = Size(w + 4f, h + 16f),
            cornerRadius = CornerRadius(8f, 8f)
        )
        // ладовые порожки
        for (f in 0..frets) {
            val x = left + f * fretW
            drawLine(
                color = if (f == 0) Color(0xFFD7D9E2) else boardColor,
                start = Offset(x, top),
                end = Offset(x, top + h),
                strokeWidth = if (f == 0) 6f else 2f
            )
        }
        // точки-маркеры ладов
        for (f in listOf(3, 5, 7, 9, 12)) {
            val x = left + (f + 0.5f) * fretW
            drawCircle(Color(0xFF3C4254), radius = 4f, center = Offset(x, top + h / 2))
        }
        // струны
        for (s in 0 until stringCount) {
            val y = top + h * s / (stringCount - 1).coerceAtLeast(1)
            drawLine(color = Color(0xFFBFC4D2), start = Offset(left, y), end = Offset(left + w, y), strokeWidth = 2f)
            drawLabel(stringLabels.getOrElse(s) { "" }, left / 2 - 2.dp.toPx(), y + 4.dp.toPx(), 10.sp.toPx(), onSurface)
        }
        // номера ладов
        for (f in 1..frets) {
            val x = left + (f - 0.5f) * fretW
            drawLabel(f.toString(), x, size.height - 6.dp.toPx(), 8.sp.toPx(), onSurface.copy(alpha = 0.6f))
        }
        // подсветка позиций
        highlight.forEach { (s, f) ->
            if (s in 0 until stringCount && f in 0..frets) {
                val cx = left + (f + 0.5f) * fretW
                val cy = top + h * s / (stringCount - 1).coerceAtLeast(1)
                drawCircle(Accent, radius = 9f, center = Offset(cx, cy))
            }
        }
        drawLabel("тап по точке = струна + лад", size.width / 2, size.height - 6.dp.toPx(), 8.sp.toPx(), onSurface.copy(alpha = 0.35f))
    }
}

/** Нотоносец: 5 линеек, скрипичный ключ, ноты с диезами/бемолями. */
@Composable
fun StaffView(
    notes: List<Note>,
    modifier: Modifier = Modifier,
    height: Int = 190,
    caption: String? = null
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val accent = Accent

    Canvas(modifier = modifier.fillMaxWidth().height(height.dp)) {
        val spacing = 12.dp.toPx()
        val staffTop = size.height * 0.22f
        val staffBottom = staffTop + spacing * 4
        val leftPad = 46.dp.toPx()
        val rightPad = 16.dp.toPx()
        for (i in 0..4) {
            val y = staffTop + i * spacing
            drawLine(lineColor, Offset(leftPad, y), Offset(size.width - rightPad, y), strokeWidth = 1.6f)
        }
        // скрипичный ключ — стилизованный росчерк
        val clefX = leftPad - 22.dp.toPx()
        val clefPath = Path().apply {
            moveTo(clefX, staffBottom)
            cubicTo(clefX - 10.dp.toPx(), staffTop + spacing * 2f, clefX + 12.dp.toPx(), staffTop + spacing * 1.2f, clefX + 4.dp.toPx(), staffTop + spacing * 3.2f)
            cubicTo(clefX - 2.dp.toPx(), staffTop + spacing * 4.4f, clefX + 12.dp.toPx(), staffBottom + spacing * 0.6f, clefX + 4.dp.toPx(), staffBottom)
            cubicTo(clefX - 2.dp.toPx(), staffBottom + spacing * 0.2f, clefX - 6.dp.toPx(), staffTop + spacing * 2f, clefX, staffTop + spacing * 1.6f)
        }
        drawPath(clefPath, color = onSurface, style = Stroke(width = 2.4f))
        drawCircle(onSurface, radius = 2.6f, center = Offset(clefX + 1.dp.toPx(), staffBottom - spacing * 0.2f))

        if (notes.isEmpty()) {
            drawLabel("?", size.width / 2, staffTop + spacing * 2.2f, 24.sp.toPx(), onSurface.copy(alpha = 0.4f))
        }

        val usableWidth = size.width - leftPad - rightPad - 40.dp.toPx()
        val step = if (notes.size > 1) usableWidth / (notes.size) else usableWidth / 2
        notes.forEachIndexed { index, note ->
            val x = leftPad + 46.dp.toPx() + index * step
            val y = staffBottom - (note.diatonicIndex - 30) * spacing / 2f
            // добавочные линейки
            val diff = note.diatonicIndex - 30
            if (diff < 0) {
                var d = -2
                while (d >= diff) {
                    val ly = staffBottom - d * spacing / 2f
                    drawLine(lineColor, Offset(x - 13.dp.toPx(), ly), Offset(x + 13.dp.toPx(), ly), strokeWidth = 1.4f)
                    d -= 2
                }
            } else if (diff > 8) {
                var d = 10
                while (d <= diff) {
                    val ly = staffBottom - d * spacing / 2f
                    drawLine(lineColor, Offset(x - 13.dp.toPx(), ly), Offset(x + 13.dp.toPx(), ly), strokeWidth = 1.4f)
                    d += 2
                }
            }
            // нота
            drawOval(
                color = accent,
                topLeft = Offset(x - 7.dp.toPx(), y - 4.8.dp.toPx()),
                size = Size(14.dp.toPx(), 9.6.dp.toPx())
            )
            drawLine(accent, Offset(x + 6.5.dp.toPx(), y), Offset(x + 6.5.dp.toPx(), y - 34.dp.toPx()), strokeWidth = 2f)
            if (note.accidental != 0) {
                val sign = if (note.accidental > 0) "♯" else "♭"
                drawLabel(sign, x - 15.dp.toPx(), y + 5.dp.toPx(), 14.sp.toPx(), onSurface)
            }
            if (notes.size <= 3) {
                drawLabel(note.shortRu, x, staffBottom + spacing * 2.4f, 9.sp.toPx(), onSurface.copy(alpha = 0.65f))
            }
        }
        caption?.let {
            drawLabel(it, size.width / 2, size.height - 4.dp.toPx(), 9.sp.toPx(), onSurface.copy(alpha = 0.5f))
        }
    }
}

/** Лестница ступеней I–VII для вокала и универсального инструмента. */
@Composable
fun ScaleLadder(
    rootMidi: Int = 60,
    highlightPitchClass: Int? = null,
    modifier: Modifier = Modifier
) {
    val rows = MAJOR_STEPS.size
    val height = (rows * 30 + 8).dp
    val onSurface = MaterialTheme.colorScheme.onSurface
    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val rowH = size.height / rows
        for (i in (0 until rows).reversed()) {
            val degree = i
            val y = (rows - 1 - i) * rowH
            val pc = (rootMidi + MAJOR_STEPS[degree]) % 12
            val active = highlightPitchClass != null && highlightPitchClass == pc
            val barW = size.width * (0.42f + 0.08f * degree)
            drawRoundRect(
                color = if (active) Accent else Surface2,
                topLeft = Offset(size.width - barW, y + 3f),
                size = Size(barW, rowH - 6f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawLabel(
                ru.teoria.lab.core.DEGREE_RU[degree],
                size.width - barW + 12.dp.toPx(), y + rowH / 2 + 5.dp.toPx(), 12.sp.toPx(),
                if (active) Color(0xFF231A00) else onSurface.copy(alpha = 0.85f), center = false
            )
            val note = Note.fromMidi(rootMidi + MAJOR_STEPS[degree])
            drawLabel(note.shortRu, size.width - 12.dp.toPx(), y + rowH / 2 + 5.dp.toPx(), 11.sp.toPx(),
                if (active) Color(0xFF231A00) else onSurface.copy(alpha = 0.6f))
        }
        drawLabel("ступени", 8.dp.toPx(), 14.dp.toPx(), 9.sp.toPx(), onSurface.copy(alpha = 0.4f), center = false)
    }
}

/** Выбор ноты кнопками: буква + альтерация + октава (для вокала/универсального). */
@Composable
fun NotePicker(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onPick: (Note) -> Unit
) {
    var accidental by remember { mutableStateOf(0) }
    var octave by remember { mutableStateOf(4) }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(-1 to "♭", 0 to "♮", 1 to "♯").forEach { (value, label) ->
                SelectChip(
                    text = label,
                    selected = accidental == value,
                    modifier = Modifier.weight(1f),
                    onClick = { accidental = value }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            LETTER_RU.forEachIndexed { index, name ->
                val midi = (octave + 1) * 12 + LETTER_SEMITONES[index] + accidental
                SelectChip(
                    text = name,
                    selected = false,
                    modifier = Modifier.weight(1f),
                    onClick = { if (enabled) onPick(Note(index, accidental, octave)) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(3, 4, 5).forEach { oct ->
                SelectChip(
                    text = if (oct == 3) "малая" else if (oct == 4) "первая" else "вторая",
                    selected = octave == oct,
                    modifier = Modifier.weight(1f),
                    onClick = { octave = oct }
                )
            }
        }
    }
}

@Composable
fun SelectChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Accent else Surface2)
            .border(1.dp, if (selected) Accent else Color(0xFF3A3F4E), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) Color(0xFF231A00) else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

/** Кольцо mastery в процентах. */
@Composable
fun MasteryRing(
    progress: Float,
    label: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 46,
    color: Color = Green
) {
    Box(modifier = modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(sizeDp.dp)) {
            val stroke = 5.dp.toPx()
            drawArc(
                color = Surface2,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke),
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(this.size.width - stroke, this.size.height - stroke)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(this.size.width - stroke, this.size.height - stroke)
            )
        }
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Знаки тональности в виде маленьких «♯ ♯ ♯». */
@Composable
fun KeySignBadge(key: ru.teoria.lab.core.MusicKey, modifier: Modifier = Modifier) {
    val text = if (key.signCount == 0) "нет знаков" else {
        (if (key.hasSharps) "♯" else "♭").repeat(key.signCount)
    }
    Text(
        text,
        modifier = modifier,
        color = if (key.hasSharps) Blue else Accent,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
    )
}
