package com.example.gamelock.data.remote

import com.example.gamelock.domain.model.Game
import com.google.gson.annotations.SerializedName

data class GamesResponse(
    val count: Int,
    val next: String?,
    val results: List<GameDto>
)

data class GameDto(
    val id: Int,
    val name: String,
    @SerializedName("background_image") val backgroundImage: String?,
    val rating: Double,
    val genres: List<GenreDto>?
)

data class GameDetailDto(
    val id: Int,
    val name: String,
    @SerializedName("background_image") val backgroundImage: String?,
    val rating: Double,
    @SerializedName("description_raw") val description: String?,
    val genres: List<GenreDto>?
)

data class GenreDto(val name: String)
data class ScreenshotsResponse(val results: List<ScreenshotDto>)
data class ScreenshotDto(val image: String)

fun GameDto.toDomain(): Game = Game(
    id = id, name = name, imageUrl = backgroundImage,
    rating = rating, description = null,
    genres = genres?.joinToString(", ") { it.name }
)

fun GameDetailDto.toDomain(screenshots: List<String> = emptyList()): Game = Game(
    id = id, name = name, imageUrl = backgroundImage,
    rating = rating, description = description,
    genres = genres?.joinToString(", ") { it.name },
    screenshots = screenshots
)
