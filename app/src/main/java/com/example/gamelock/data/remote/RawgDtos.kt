package com.example.gamelock.data.remote

import com.example.gamelock.domain.model.Game
import com.example.gamelock.domain.model.RedditPost
import com.example.gamelock.domain.model.StoreUrl
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
    val genres: List<GenreDto>?,
    val tags: List<TagDto>?
)

data class TagDto(val name: String, val slug: String)

data class GameDetailDto(
    val id: Int,
    val name: String,
    @SerializedName("background_image") val backgroundImage: String?,
    val rating: Double,
    @SerializedName("description_raw") val description: String?,
    val genres: List<GenreDto>?,
    val metacritic: Int?,
    val stores: List<StoreDto>?,
    @SerializedName("released") val released: String?,
    val website: String?,
    val tags: List<TagDto>?,
    val developers: List<CompanyDto>?,
    val publishers: List<CompanyDto>?,
    val esrb_rating: EsrbDto?,
    val clip: ClipDto?
)

data class StoreDto(
    val id: Int,
    val url: String?,
    val store: StoreInfo
)

data class StoreInfo(
    val id: Int,
    val name: String,
    val slug: String
)

data class GenreDto(val name: String)
data class CompanyDto(val id: Int, val name: String, val slug: String)
data class EsrbDto(val id: Int?, val name: String?, val slug: String?)
data class ClipDto(
    val clip: String?,
    val clips: Map<String, String>?,
    val video: String?,
    val preview: String?
)
data class ScreenshotsResponse(val results: List<ScreenshotDto>)
data class ScreenshotDto(val image: String)

data class RedditResponse(val results: List<RedditPostDto>)
data class RedditPostDto(
    val name: String,
    val text: String?,
    val url: String,
    val image: String?,
    val username: String,
    val created: String
)

fun RedditPostDto.toDomain(): RedditPost = RedditPost(
    title = name, text = text, url = url,
    image = image, username = username, created = created
)

fun GameDto.toDomain(): Game = Game(
    id = id, name = name, imageUrl = backgroundImage,
    rating = rating, description = null,
    genres = genres?.joinToString(", ") { it.name },
    tags = tags?.map { it.name } ?: emptyList()
)

fun GameDetailDto.toDomain(screenshots: List<String> = emptyList()): Game = Game(
    id = id, name = name, imageUrl = backgroundImage,
    rating = rating, description = description,
    genres = genres?.joinToString(", ") { it.name },
    screenshots = screenshots,
    metacritic = metacritic,
    storeUrls = stores?.map { StoreUrl(it.store.name, "") } ?: emptyList(),
    released = released,
    website = website,
    tags = tags?.map { it.name } ?: emptyList(),
    developers = developers?.map { it.name } ?: emptyList(),
    publishers = publishers?.map { it.name } ?: emptyList(),
    esrbRating = esrb_rating?.name,
    trailerUrl = clip?.video,
    clipPreview = clip?.preview
)
