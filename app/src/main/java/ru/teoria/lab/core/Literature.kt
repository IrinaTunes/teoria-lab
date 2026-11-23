package ru.teoria.lab.core

/** Эпохи западноевропейской и русской музыки. */
object Literature {
    val EPOCHS = listOf("Средневековье", "Возрождение", "Барокко", "Классицизм", "Романтизм", "Импрессионизм", "XX век")

    val CARDS: List<LitCard> get() = ContentRepo.cards()

    fun byComposer(composer: String): List<LitCard> = CARDS.filter { it.composer == composer }
    fun composers(): List<String> = CARDS.map { it.composer }.distinct().sorted()
    fun epochs(): List<String> = CARDS.map { it.epoch }.distinct()
    fun epochOf(composer: String): String? = CARDS.firstOrNull { it.composer == composer }?.epoch
    fun epochOfWork(work: String): String? = CARDS.firstOrNull { it.work == work }?.epoch
    fun composerOfWork(work: String): String? = CARDS.firstOrNull { it.work == work }?.composer
    fun worksBy(composer: String): List<String> = byComposer(composer).map { it.work }
    fun genres(): List<String> = CARDS.map { it.genre }.distinct().sorted()
    fun stageWorks(): List<LitCard> = CARDS.filter { it.genre == "опера" || it.genre == "балет" }
    fun russianWorks(): List<LitCard> = CARDS.filter {
        it.composer.contains("Чайковский") || it.composer.contains("Мусоргский") ||
            it.composer.contains("Бородин") || it.composer.contains("Римский-Корсаков") ||
            it.composer.contains("Рахманинов") || it.composer.contains("Скрябин") ||
            it.composer.contains("Прокофьев") || it.composer.contains("Шостакович") ||
            it.composer.contains("Свиридов") || it.composer.contains("Хачатурян") ||
            it.composer.contains("Глинка") || it.composer.contains("Глазунов") ||
            it.composer.contains("Лядов") || it.composer.contains("Шнитке")
    }
}
