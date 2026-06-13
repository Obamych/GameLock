package com.example.gamelock.domain.model

data class Game(
    val id: Int,
    val name: String,
    val imageUrl: String?,
    val rating: Double,
    val description: String?,
    val genres: String?,
    val screenshots: List<String> = emptyList(),
    val userStatus: GameStatus = GameStatus.NONE,
    val userRating: Float = 0f,
    val userReview: String = ""
)

enum class GameStatus {
    NONE,       // не добавлено
    PLAYING,    // играю
    COMPLETED,  // прошёл
    BACKLOG     // в планах
}
