package ru.teoria.lab.core

import java.io.File

/** Загружает assets/content/*.json из рабочей папки модуля для JVM-тестов. */
object TestContent {
    private var initialized = false

    fun init() {
        if (initialized) return
        ContentRepo.init { name ->
            val candidates = listOf(
                File("src/main/assets/content/$name"),
                File("app/src/main/assets/content/$name")
            )
            val file = candidates.firstOrNull { it.exists() }
                ?: throw IllegalStateException("Не найден assets/content/$name (cwd=${File(".").absolutePath})")
            file.readText()
        }
        initialized = true
    }
}
