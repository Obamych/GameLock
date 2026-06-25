package com.example.gamelock.domain.model

data class SearchFilters(
    val storeIds: Set<Int> = emptySet(),
    val genreSlug: String? = null,
    val minRating: Double = 0.0,
    val sortBy: FilterSort = FilterSort.RELEVANCE
) {
    val activeCount: Int
        get() = listOf(
            storeIds.isNotEmpty(),
            genreSlug != null,
            minRating > 0.0,
            sortBy != FilterSort.RELEVANCE
        ).count { it }
}

enum class FilterSort(val apiValue: String?, val displayName: String) {
    RELEVANCE(null, "По релевантности"),
    RATING_DESC("-rating", "По рейтингу"),
    DATE_DESC("-released", "По дате"),
    NAME_ASC("name", "По имени")
}

enum class FilterStore(val id: Int, val displayName: String, val emoji: String) {
    STEAM(1, "Steam", "🎮"),
    EPIC(11, "Epic Games", "🕹️"),
    GOG(5, "GOG", "🔴"),
    XBOX(2, "Xbox", "🎯"),
    PLAYSTATION(3, "PlayStation", "🔵"),
    NINTENDO(6, "Nintendo", "🍄")
}

enum class FilterGenre(val slug: String, val displayName: String) {
    ACTION("action", "Экшн"),
    ADVENTURE("adventure", "Приключения"),
    RPG("role-playing-games-rpg", "RPG"),
    STRATEGY("strategy", "Стратегии"),
    SHOOTER("shooter", "Шутеры"),
    SIMULATION("simulation", "Симуляторы"),
    PUZZLE("puzzle", "Головоломки"),
    RACING("racing", "Гонки"),
    SPORTS("sports", "Спорт"),
    HORROR("horror", "Хоррор")
}
