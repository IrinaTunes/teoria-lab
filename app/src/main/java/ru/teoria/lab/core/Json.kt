package ru.teoria.lab.core

/**
 * Минимальный JSON-парсер без зависимостей: работает и на Android, и в JVM-тестах.
 * Поддерживает объекты, массивы, строки с escape-последовательностями, числа, true/false/null.
 */
sealed class Json {
    object Null : Json()
    data class Bool(val value: Boolean) : Json()
    data class Num(val value: Double) : Json()
    data class Str(val value: String) : Json()
    data class Arr(val items: List<Json>) : Json()
    data class Obj(val fields: Map<String, Json>) : Json()

    fun opt(key: String): Json? = (this as? Obj)?.fields?.get(key)
    fun str(key: String, fallback: String = ""): String = (opt(key) as? Str)?.value ?: fallback
    fun int(key: String, fallback: Int = 0): Int = (opt(key) as? Num)?.value?.toInt() ?: fallback
    fun double(key: String, fallback: Double = 0.0): Double = (opt(key) as? Num)?.value ?: fallback
    fun bool(key: String, fallback: Boolean = false): Boolean = (opt(key) as? Bool)?.value ?: fallback
    fun array(key: String): List<Json> = (opt(key) as? Arr)?.items ?: emptyList()
    fun asArray(): List<Json> = (this as? Arr)?.items ?: emptyList()
    fun asString(): String = (this as? Str)?.value ?: ""
    fun asInt(): Int = (this as? Num)?.value?.toInt() ?: 0
    fun asDouble(): Double = (this as? Num)?.value ?: 0.0
    fun fieldMap(): Map<String, String> {
        val obj = this as? Obj ?: return emptyMap()
        return obj.fields.mapNotNull { (k, v) -> (v as? Str)?.let { k to it.value } }.toMap()
    }
}

class JsonFormatException(message: String) : RuntimeException(message)

class JsonReader(private val text: String) {
    private var pos = 0

    fun read(): Json {
        skipWhitespace()
        val value = readValue()
        skipWhitespace()
        if (pos < text.length) throw JsonFormatException("Лишние символы в позиции $pos")
        return value
    }

    private fun readValue(): Json {
        if (pos >= text.length) throw JsonFormatException("Неожиданный конец JSON")
        return when (val c = text[pos]) {
            '{' -> readObject()
            '[' -> readArray()
            '"' -> Json.Str(readString())
            't' -> { expect("true"); Json.Bool(true) }
            'f' -> { expect("false"); Json.Bool(false) }
            'n' -> { expect("null"); Json.Null }
            else -> {
                if (c == '-' || c.isDigit()) Json.Num(readNumber())
                else throw JsonFormatException("Неожиданный символ '$c' в позиции $pos")
            }
        }
    }

    private fun readObject(): Json.Obj {
        pos++ // {
        val fields = LinkedHashMap<String, Json>()
        skipWhitespace()
        if (peek() == '}') { pos++; return Json.Obj(fields) }
        while (true) {
            skipWhitespace()
            if (peek() != '"') throw JsonFormatException("Ожидался ключ в позиции $pos")
            val key = readString()
            skipWhitespace()
            if (peek() != ':') throw JsonFormatException("Ожидалось ':' в позиции $pos")
            pos++
            skipWhitespace()
            fields[key] = readValue()
            skipWhitespace()
            when (peek()) {
                ',' -> { pos++; continue }
                '}' -> { pos++; return Json.Obj(fields) }
                else -> throw JsonFormatException("Ожидалось ',' или '}' в позиции $pos")
            }
        }
    }

    private fun readArray(): Json.Arr {
        pos++ // [
        val items = mutableListOf<Json>()
        skipWhitespace()
        if (peek() == ']') { pos++; return Json.Arr(items) }
        while (true) {
            skipWhitespace()
            items.add(readValue())
            skipWhitespace()
            when (peek()) {
                ',' -> { pos++; continue }
                ']' -> { pos++; return Json.Arr(items) }
                else -> throw JsonFormatException("Ожидалось ',' или ']' в позиции $pos")
            }
        }
    }

    private fun readString(): String {
        pos++ // "
        val sb = StringBuilder()
        while (true) {
            if (pos >= text.length) throw JsonFormatException("Незакрытая строка")
            val c = text[pos++]
            when (c) {
                '"' -> return sb.toString()
                '\\' -> {
                    if (pos >= text.length) throw JsonFormatException("Незавершённый escape")
                    when (val esc = text[pos++]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            val hex = text.substring(pos, pos + 4)
                            sb.append(hex.toInt(16).toChar())
                            pos += 4
                        }
                        else -> throw JsonFormatException("Неизвестный escape \\$esc")
                    }
                }
                else -> sb.append(c)
            }
        }
    }

    private fun readNumber(): Double {
        val start = pos
        if (peek() == '-') pos++
        while (pos < text.length && text[pos].isDigit()) pos++
        if (pos < text.length && text[pos] == '.') {
            pos++
            while (pos < text.length && text[pos].isDigit()) pos++
        }
        if (pos < text.length && (text[pos] == 'e' || text[pos] == 'E')) {
            pos++
            if (pos < text.length && (text[pos] == '+' || text[pos] == '-')) pos++
            while (pos < text.length && text[pos].isDigit()) pos++
        }
        val raw = text.substring(start, pos)
        return raw.toDoubleOrNull() ?: throw JsonFormatException("Плохое число '$raw'")
    }

    private fun peek(): Char = if (pos < text.length) text[pos] else '\u0000'

    private fun skipWhitespace() {
        while (pos < text.length && text[pos].isWhitespace()) pos++
    }

    private fun expect(word: String) {
        if (!text.startsWith(word, pos)) throw JsonFormatException("Ожидалось '$word' в позиции $pos")
        pos += word.length
    }
}

fun parseJson(text: String): Json = JsonReader(text).read()
