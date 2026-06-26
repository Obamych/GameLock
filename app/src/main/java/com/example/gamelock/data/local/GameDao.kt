package com.example.gamelock.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Query("SELECT * FROM games WHERE userId = :userId AND userStatus != 'NONE'")
    fun getAllLibraryGames(userId: Int): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE userId = :userId AND userStatus = :status")
    fun getGamesByStatus(userId: Int, status: String): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :id AND userId = :userId")
    suspend fun getGameById(id: Int, userId: Int): GameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity)

    @Query("UPDATE games SET userStatus=:status, userRating=:rating, userReview=:review WHERE id=:id AND userId=:userId")
    suspend fun updateUserData(id: Int, userId: Int, status: String, rating: Float, review: String)

    @Query("UPDATE games SET lastAccessed = :ts WHERE id = :id AND userId = :userId")
    suspend fun updateLastAccessed(id: Int, userId: Int, ts: Long)

    @Query("SELECT COUNT(*) FROM games WHERE userId = :userId AND userStatus != 'NONE'")
    suspend fun countLibraryGames(userId: Int): Int

    @Query("SELECT COUNT(*) FROM games WHERE userId = :userId AND userStatus = :status")
    suspend fun countGamesByStatus(userId: Int, status: String): Int

    @Delete
    suspend fun deleteGame(game: GameEntity)
}
