package com.example.gamelock.data.repository

import com.example.gamelock.data.local.AppDatabase
import com.example.gamelock.data.local.toDomain
import com.example.gamelock.data.local.toEntity
import com.example.gamelock.data.remote.RetrofitClient
import com.example.gamelock.data.remote.toDomain
import com.example.gamelock.domain.model.Game
import com.example.gamelock.domain.model.GameStatus
import com.example.gamelock.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameRepository(database: AppDatabase) {

    private val dao = database.gameDao()
    private val api = RetrofitClient.api

    suspend fun searchGames(query: String): Result<List<Game>> {
        return try {
            val response = api.searchGames(query, Constants.API_KEY)
            Result.success(response.results.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGameDetails(id: Int): Result<Game> {
        return try {
            val detail = api.getGameDetails(id, Constants.API_KEY)
            val screenshots = api.getScreenshots(id, Constants.API_KEY)
                .results.map { it.image }
            val local = dao.getGameById(id)
            val game = detail.toDomain(screenshots).copy(
                userStatus = local?.let { GameStatus.valueOf(it.userStatus) } ?: GameStatus.NONE,
                userRating = local?.userRating ?: 0f,
                userReview = local?.userReview ?: ""
            )
            Result.success(game)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getLibraryGames(): Flow<List<Game>> =
        dao.getAllLibraryGames().map { list -> list.map { it.toDomain() } }

    fun getGamesByStatus(status: GameStatus): Flow<List<Game>> =
        dao.getGamesByStatus(status.name).map { list -> list.map { it.toDomain() } }

    suspend fun saveGame(game: Game) = dao.insertGame(game.toEntity())

    suspend fun updateUserData(game: Game) =
        dao.updateUserData(game.id, game.userStatus.name, game.userRating, game.userReview)

    suspend fun deleteGame(game: Game) = dao.deleteGame(game.toEntity())
}
