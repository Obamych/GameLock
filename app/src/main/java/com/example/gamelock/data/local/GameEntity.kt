package com.example.gamelock.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.gamelock.domain.model.Game
import com.example.gamelock.domain.model.GameStatus

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val imageUrl: String?,
    val rating: Double,
    val description: String?,            // English original (cleaned)
    val descriptionRu: String? = null,    // Russian translation (ML Kit cache)
    val genres: String?,
    val screenshotsJson: String = "",
    val userStatus: String = "NONE",
    val userRating: Float = 0f,
    val userReview: String = "",
    val overallRating: Double = 0.0,
    val descriptionOriginal: String? = null,
    val lastAccessed: Long? = null
)

fun GameEntity.toDomain(): Game = Game(
    id = id, name = name, imageUrl = imageUrl,
    rating = rating,
    description = descriptionRu ?: description,
    genres = genres,
    screenshots = if (screenshotsJson.isEmpty()) emptyList()
    else screenshotsJson.split(","),
    userStatus = GameStatus.valueOf(userStatus),
    userRating = userRating, userReview = userReview,
    overallRating = if (overallRating == 0.0) rating * 2 else overallRating,
    descriptionOriginal = descriptionOriginal ?: description,
    lastAccessed = lastAccessed
)

fun Game.toEntity(): GameEntity = GameEntity(
    id = id, name = name, imageUrl = imageUrl,
    rating = rating,
    description = descriptionOriginal ?: description,
    descriptionRu = if (description != (descriptionOriginal ?: description)) description else null,
    genres = genres,
    screenshotsJson = screenshots.joinToString(","),
    userStatus = userStatus.name,
    userRating = userRating, userReview = userReview,
    overallRating = overallRating,
    descriptionOriginal = descriptionOriginal,
    lastAccessed = lastAccessed
)
