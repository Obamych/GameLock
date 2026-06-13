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
    val description: String?,
    val genres: String?,
    val screenshotsJson: String = "",
    val userStatus: String = "NONE",
    val userRating: Float = 0f,
    val userReview: String = ""
)

fun GameEntity.toDomain(): Game = Game(
    id = id, name = name, imageUrl = imageUrl,
    rating = rating, description = description, genres = genres,
    screenshots = if (screenshotsJson.isEmpty()) emptyList()
    else screenshotsJson.split(","),
    userStatus = GameStatus.valueOf(userStatus),
    userRating = userRating, userReview = userReview
)

fun Game.toEntity(): GameEntity = GameEntity(
    id = id, name = name, imageUrl = imageUrl,
    rating = rating, description = description, genres = genres,
    screenshotsJson = screenshots.joinToString(","),
    userStatus = userStatus.name,
    userRating = userRating, userReview = userReview
)
