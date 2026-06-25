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
    val userReview: String = "",
    val steamReviewPct: Int? = null,
    val steamReviewDesc: String? = null,
    val metacritic: Int? = null,
    val storeUrls: List<StoreUrl> = emptyList(),
    val ratingSource: String = "RAWG",
    val overallRating: Double = rating * 2,
    val storeRatings: List<StoreRating> = emptyList(),
    val descriptionOriginal: String? = null,
    val descriptionSource: String = "RAWG",
    val redditPosts: List<RedditPost> = emptyList(),
    val steamReviews: List<SteamReview> = emptyList(),
    val released: String? = null,
    val website: String? = null,
    val tags: List<String> = emptyList(),
    val developers: List<String> = emptyList(),
    val publishers: List<String> = emptyList(),
    val esrbRating: String? = null,
    val lastAccessed: Long? = null,
    val trailerUrl: String? = null,
    val clipPreview: String? = null
)

enum class GameStatus {
    NONE,       // не добавлено
    PLAYING,    // играю
    COMPLETED,  // прошёл
    BACKLOG     // в планах
}
