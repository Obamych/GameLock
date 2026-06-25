package com.example.gamelock.data.repository

import com.example.gamelock.data.local.AppDatabase
import com.example.gamelock.data.local.GameEntity
import com.example.gamelock.data.local.toDomain
import com.example.gamelock.data.local.toEntity
import com.example.gamelock.data.remote.RetrofitClient
import com.example.gamelock.data.remote.SourceLang
import com.example.gamelock.data.remote.TranslationService
import com.example.gamelock.data.remote.toDomain
import com.example.gamelock.domain.model.Game
import com.example.gamelock.domain.model.GameStatus
import com.example.gamelock.domain.model.SearchFilters
import com.example.gamelock.domain.model.StoreRating
import com.example.gamelock.utils.Constants
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameRepository(database: AppDatabase) {

    private val dao = database.gameDao()
    private val api = RetrofitClient.api
    private val steamApi = RetrofitClient.steamApi

    suspend fun fetchPopularGames(genreSlug: String? = null): Result<List<Game>> {
        return try {
            val response = api.getPopularGames(Constants.API_KEY, genres = genreSlug)
            val games = response.results.filter { !hasAdultContent(it) }.map { it.toDomain() }
            Result.success(games)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchGames(query: String): Result<List<Game>> =
        searchGames(query, SearchFilters())

    suspend fun searchGames(query: String, filters: SearchFilters): Result<List<Game>> {
        return try {
            val storesParam = filters.storeIds.takeIf { it.isNotEmpty() }?.joinToString(",")
            val response = api.searchGames(
                query, Constants.API_KEY,
                stores = storesParam,
                genres = filters.genreSlug,
                ordering = filters.sortBy.apiValue
            )
            var games = response.results
                .filter { !hasAdultContent(it) }
                .map { it.toDomain() }
            if (filters.minRating > 0.0) {
                games = games.filter { it.overallRating >= filters.minRating }
            }
            Result.success(games)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun hasAdultContent(dto: com.example.gamelock.data.remote.GameDto): Boolean {
        val adultKeywords = setOf(
            "porn", "porno", "pornographic", "nsfw", "erotic", "erotica",
            "nudity", "hentai", "adult", "sex", "sexual", "xxx",
            "порн", "эротик", "эротика", "хентай",
            "взросл", "18+", "18 plus"
        )

        val name = dto.name.lowercase()
        if (adultKeywords.any { name.contains(it) }) return true

        dto.genres?.let { genres ->
            if (genres.any { g ->
                val gn = g.name.lowercase()
                adultKeywords.any { gn.contains(it) }
            }) return true
        }

        dto.tags?.let { tags ->
            if (tags.any { t ->
                val tn = t.name.lowercase()
                val ts = t.slug.lowercase()
                adultKeywords.any { tn.contains(it) || ts.contains(it) }
            }) return true
        }

        return false
    }

    suspend fun getGameDetails(id: Int): Result<Game> {
        return try {
            coroutineScope {
                val detailDef = async { api.getGameDetails(id, Constants.API_KEY) }
                val screensDef = async { api.getScreenshots(id, Constants.API_KEY).results.map { it.image } }

                val detail = detailDef.await()
                val screenshots = screensDef.await()

                val local = dao.getGameById(id)

                val cleanEn = detail.description?.let { cleanDescription(it) }

                var translatedRu: String? = local?.descriptionRu
                val lang = if (!cleanEn.isNullOrBlank()) TranslationService.detectLanguage(cleanEn) else SourceLang.OTHER
                if (translatedRu == null && !cleanEn.isNullOrBlank() && lang != SourceLang.OTHER) {
                    val result = TranslationService.translate(cleanEn, lang)
                    translatedRu = result.getOrNull()
                    if (translatedRu != null) {
                        val entity = (local ?: GameEntity(
                            id = detail.id, name = detail.name,
                            imageUrl = detail.backgroundImage,
                            rating = detail.rating, description = cleanEn,
                            descriptionRu = translatedRu,
                            genres = detail.genres?.joinToString(", ") { it.name },
                            descriptionOriginal = cleanEn
                        )).copy(descriptionRu = translatedRu)
                        dao.insertGame(entity)
                    }
                }

                var steamReviewPct: Int? = null
                var steamReviewDesc: String? = null
                detail.stores?.firstOrNull { it.store.name == "Steam" }?.url?.let { url ->
                    val appId = url.substringAfter("/app/").substringBefore("/").toIntOrNull()
                    if (appId != null) {
                        try {
                            val reviewResponse = steamApi.getReviews(appId)
                            if (reviewResponse.success == 1) {
                                val summary = reviewResponse.query_summary
                                if (summary != null && summary.total_reviews > 0) {
                                    steamReviewPct = (summary.total_positive.toDouble() / summary.total_reviews * 100).toInt()
                                    steamReviewDesc = summary.review_score_desc
                                }
                            }
                        } catch (_: Exception) { }
                    }
                }

                val ratingSource = when {
                    steamReviewPct != null -> "Steam"
                    detail.metacritic != null -> "Metacritic"
                    else -> "RAWG"
                }

                val storeRatings = mutableListOf<StoreRating>()
                if (steamReviewPct != null) {
                    storeRatings.add(
                        StoreRating("Steam", steamReviewPct.toDouble() / 10.0, "${steamReviewPct}%", "🎮")
                    )
                }
                detail.metacritic?.let {
                    storeRatings.add(
                        StoreRating("Metacritic", it.toDouble() / 10.0, "$it", "🏆")
                    )
                }
                val rawgValue = detail.rating * 2
                storeRatings.add(
                    StoreRating("RAWG", rawgValue, String.format("%.1f", rawgValue), "⭐")
                )
                val overallRating = storeRatings.sumOf { it.value } / storeRatings.size

                val game = detail.toDomain(screenshots).copy(
                    description = translatedRu ?: cleanEn,
                    descriptionOriginal = cleanEn,
                    steamReviewPct = steamReviewPct,
                    steamReviewDesc = steamReviewDesc,
                    ratingSource = ratingSource,
                    storeRatings = storeRatings,
                    overallRating = overallRating,
                    userStatus = local?.let { GameStatus.valueOf(it.userStatus) } ?: GameStatus.NONE,
                    userRating = local?.userRating ?: 0f,
                    userReview = local?.userReview ?: ""
                )
                Result.success(game)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cleanDescription(text: String): String {
        val sourcePrefixes = setOf(
            "metacritic:", "gamespot:", "ign:", "pc gamer:", "gamesradar:",
            "eurogamer:", "destructoid:", "polygon:", "kotaku:",
            "rock, paper, shotgun:", "nintendo life:", "vandal:",
            "3djuegos:", "hobbyconsolas:", "meristation:", "game informer:",
            "from metacritic:", "from gamespot:", "from ign:"
        )
        var result = text.lines()
            .filterNot { line ->
                val t = line.trim().lowercase()
                sourcePrefixes.any { t.startsWith(it) }
            }
            .joinToString("\n")
            .replace(Regex("\\[Source:[^\\]]+\\]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*[-–]\\s*(Metacritic|GameSpot|IGN|PC Gamer|GamesRadar|Eurogamer|Destructoid|Polygon|Kotaku|Nintendo Life|Game Informer)\\s*$", RegexOption.IGNORE_CASE), "")
            .trim()

        if (result.isBlank()) return text
        return result
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
