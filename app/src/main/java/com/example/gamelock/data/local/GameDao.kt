package com.example.gamelock.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Query("SELECT * FROM games WHERE userStatus != 'NONE'")
    fun getAllLibraryGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE userStatus = :status")
    fun getGamesByStatus(status: String): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getGameById(id: Int): GameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity)

    @Query("UPDATE games SET userStatus=:status, userRating=:rating, userReview=:review WHERE id=:id")
    suspend fun updateUserData(id: Int, status: String, rating: Float, review: String)

    @Query("UPDATE games SET lastAccessed = :ts WHERE id = :id")
    suspend fun updateLastAccessed(id: Int, ts: Long)

    @Delete
    suspend fun deleteGame(game: GameEntity)
}
