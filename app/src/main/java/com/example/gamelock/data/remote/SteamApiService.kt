package com.example.gamelock.data.remote

import com.example.gamelock.domain.model.SteamReview
import retrofit2.http.GET
import retrofit2.http.Query

interface SteamApiService {

    @GET("api/reviews/{appid}")
    suspend fun getReviews(
        @retrofit2.http.Path("appid") appId: Int,
        @Query("json") json: Int = 1,
        @Query("language") language: String = "all",
        @Query("purchase_type") purchaseType: String = "all"
    ): SteamReviewResponse

    @GET("api/appdetails")
    suspend fun getAppDetails(
        @Query("appids") appIds: Int,
        @Query("l") language: String = "english"
    ): Map<String, SteamAppDetailsResult>

    @GET("api/storesearch")
    suspend fun searchStore(
        @Query("term") term: String,
        @Query("l") language: String = "english",
        @Query("cc") cc: String = "us"
    ): SteamSearchResponse
}

data class SteamSearchResponse(
    val items: List<SteamSearchItem>?,
    val total: Int?
)

data class SteamSearchItem(
    val id: Int,
    val name: String,
    val type: String?
)

data class SteamReviewResponse(
    val success: Int,
    val query_summary: QuerySummary?,
    val reviews: List<SteamReviewItemDto>?
)

data class SteamReviewItemDto(
    val review: String,
    @com.google.gson.annotations.SerializedName("timestamp_created") val timestampCreated: Long,
    val author: SteamReviewAuthorDto,
    @com.google.gson.annotations.SerializedName("voted_up") val votedUp: Boolean,
    @com.google.gson.annotations.SerializedName("votes_up") val votesUp: Int
)

data class SteamReviewAuthorDto(
    val personaname: String?,
    val avatar: String?
)

fun SteamReviewItemDto.toDomain(): SteamReview = SteamReview(
    text = review,
    authorName = author.personaname ?: "Anonymous",
    authorAvatar = author.avatar,
    votedUp = votedUp,
    votesUp = votesUp,
    timestamp = timestampCreated
)

data class QuerySummary(
    val num_reviews: Int,
    val review_score: Int,
    val review_score_desc: String,
    val total_positive: Int,
    val total_negative: Int,
    val total_reviews: Int
)

data class SteamAppDetailsResult(
    val success: Boolean,
    val data: SteamAppData?
)

data class SteamAppData(
    val about_the_game: String?,
    val detailed_description: String?,
    val short_description: String?
)
