package com.example.gamelock.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SteamApiService {

    @GET("api/reviews/{appid}")
    suspend fun getReviews(
        @Path("appid") appId: Int,
        @Query("json") json: Int = 1,
        @Query("language") language: String = "all",
        @Query("purchase_type") purchaseType: String = "all"
    ): SteamReviewResponse
}

data class SteamReviewResponse(
    val success: Int,
    val query_summary: QuerySummary?
)

data class QuerySummary(
    val num_reviews: Int,
    val review_score: Int,
    val review_score_desc: String,
    val total_positive: Int,
    val total_negative: Int,
    val total_reviews: Int
)
