package com.example.gamelock.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RawgApiService {

    @GET("games")
    suspend fun getPopularGames(
        @Query("key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("ordering") ordering: String = "-rating",
        @Query("stores") stores: String? = null,
        @Query("genres") genres: String? = null
    ): GamesResponse

    @GET("games")
    suspend fun searchGames(
        @Query("search") query: String,
        @Query("key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("stores") stores: String? = null,
        @Query("genres") genres: String? = null,
        @Query("ordering") ordering: String? = null
    ): GamesResponse

    @GET("games/{id}")
    suspend fun getGameDetails(
        @Path("id") id: Int,
        @Query("key") apiKey: String,
        @Query("lang") lang: String? = null
    ): GameDetailDto

    @GET("games/{id}/screenshots")
    suspend fun getScreenshots(
        @Path("id") id: Int,
        @Query("key") apiKey: String
    ): ScreenshotsResponse
}