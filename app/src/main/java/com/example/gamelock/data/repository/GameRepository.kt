package com.example.gamelock.data.repository

import android.util.Log
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
import com.example.gamelock.domain.model.RedditPost
import com.example.gamelock.domain.model.SearchFilters
import com.example.gamelock.domain.model.StoreRating
import com.example.gamelock.utils.Constants
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameRepository(database: AppDatabase) {

    companion object {
        private const val TAG = "GameRepository"
    }

    private val dao = database.gameDao()
    private val api = RetrofitClient.api
    private val steamApi = RetrofitClient.steamApi

    private fun extractSteamAppId(url: String?): Int? {
        if (url == null) return null
        url.substringAfter("/app/").substringBefore("/").toIntOrNull()?.let { return it }
        url.substringAfter("/sub/").substringBefore("/").toIntOrNull()?.let { return it }
        url.substringAfter("app/").substringBefore("/").toIntOrNull()?.let { return it }
        val segments = url.trimEnd('/').split("/")
        segments.lastOrNull()?.toIntOrNull()?.let { return it }
        return null
    }

    private fun searchNameVariations(name: String): List<String> {
        val variations = mutableListOf(name.trim())
        val colonIdx = name.indexOf(":")
        if (colonIdx > 0) variations.add(name.substring(0, colonIdx).trim())
        val parenIdx = name.indexOf("(")
        if (parenIdx > 0) variations.add(name.substring(0, parenIdx).trim())
        val dashIdx = name.indexOf(" — ")
        if (dashIdx > 0) variations.add(name.substring(0, dashIdx).trim())
        val enDashIdx = name.indexOf(" – ")
        if (enDashIdx > 0) variations.add(name.substring(0, enDashIdx).trim())
        return variations.distinct()
    }

    suspend fun fetchPopularGames(genreSlug: String? = null, page: Int = 1): Result<Triple<List<Game>, Boolean, Int>> {
        return try {
            val response = api.getPopularGames(Constants.API_KEY, page = page, pageSize = 20, genres = genreSlug)
            val games = response.results.filter { !hasAdultContent(it) }.map { it.toDomain() }
            Result.success(Triple(games, response.next != null, response.count))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchGames(query: String): Result<Triple<List<Game>, Boolean, Int>> =
        searchGames(query, SearchFilters())

    suspend fun searchGames(query: String, filters: SearchFilters, page: Int = 1): Result<Triple<List<Game>, Boolean, Int>> {
        return try {
            val storesParam = filters.storeIds.takeIf { it.isNotEmpty() }?.joinToString(",")
            val response = api.searchGames(
                query, Constants.API_KEY,
                page = page, pageSize = 20,
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
            Result.success(Triple(games, response.next != null, response.count))
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

    suspend fun getGameDetails(id: Int, userId: Int = 0): Result<Game> {
        return try {
            coroutineScope {
                val detailDef = async { api.getGameDetails(id, Constants.API_KEY) }
                val screensDef = async { api.getScreenshots(id, Constants.API_KEY).results.map { it.image } }
                val redditDef = async {
                    try {
                        api.getGameRedditPosts(id, Constants.API_KEY).results.map { it.toDomain() }
                    } catch (e: Exception) {
                        Log.w(TAG, "Reddit posts failed for game $id: ${e.message}")
                        emptyList()
                    }
                }

                val detail = detailDef.await()
                val screenshots = screensDef.await()
                val redditPosts = redditDef.await()

                val local = dao.getGameById(id, userId)

                val steamStoreUrl = detail.stores?.firstOrNull { it.store.name.equals("Steam", ignoreCase = true) }?.url
                val steamAppIdFromUrl = extractSteamAppId(steamStoreUrl)
                if (steamAppIdFromUrl != null) {
                    Log.d(TAG, "Steam App ID from URL: $steamAppIdFromUrl for ${detail.name}")
                }

                val steamSearchDef = if (steamAppIdFromUrl == null) async {
                    var foundId: Int? = null
                    for (searchName in searchNameVariations(detail.name)) {
                        if (foundId != null) break
                        try {
                            Log.d(TAG, "Searching Steam for: $searchName")
                            val searchResult = steamApi.searchStore(searchName)
                            foundId = searchResult.items?.firstOrNull { it.id > 0 }?.id
                        } catch (e: Exception) {
                            Log.w(TAG, "Steam search failed for '$searchName': ${e.message}")
                        }
                    }
                    if (foundId != null) Log.d(TAG, "Steam App ID from search: $foundId")
                    foundId
                } else null

                val resolvedAppId = steamAppIdFromUrl ?: steamSearchDef?.await()
                if (resolvedAppId == null) {
                    Log.w(TAG, "No Steam App ID found for ${detail.name}")
                }

                val steamDescDef = if (resolvedAppId != null) async {
                    try {
                        val result = steamApi.getAppDetails(resolvedAppId)
                        result[resolvedAppId.toString()]?.data
                    } catch (e: Exception) {
                        Log.w(TAG, "Steam description failed for app $resolvedAppId: ${e.message}")
                        null
                    }
                } else null

                val rawgDescription = detail.description?.let { cleanDescription(it) }

                var descSource = "RAWG"
                var primaryOriginal: String? = rawgDescription

                val steamData = steamDescDef?.await()
                if (steamData != null) {
                    val steamDesc = steamData.about_the_game?.takeIf { it.isNotBlank() }
                        ?: steamData.detailed_description?.takeIf { it.isNotBlank() }
                        ?: steamData.short_description?.takeIf { it.isNotBlank() }
                    if (steamDesc != null) {
                        val cleaned = cleanDescription(stripHtml(steamDesc))
                        if (cleaned.isNotBlank()) {
                            primaryOriginal = cleaned
                            descSource = "Steam"
                        }
                    }
                }

                var translatedRu: String? = local?.descriptionRu
                val lang = if (!primaryOriginal.isNullOrBlank())
                    TranslationService.detectLanguage(primaryOriginal) else SourceLang.OTHER
                if (translatedRu == null && !primaryOriginal.isNullOrBlank() && lang != SourceLang.OTHER) {
                    val result = TranslationService.translate(primaryOriginal, lang)
                    translatedRu = result.getOrNull()
                    if (translatedRu != null) {
                val entity = (local ?: GameEntity(
                    id = detail.id, userId = userId, name = detail.name,
                    imageUrl = detail.backgroundImage,
                    rating = detail.rating, description = primaryOriginal,
                    descriptionRu = translatedRu,
                    genres = detail.genres?.joinToString(", ") { it.name },
                    descriptionOriginal = primaryOriginal
                )).copy(descriptionRu = translatedRu)
                        dao.insertGame(entity)
                    }
                }

                var steamReviewPct: Int? = null
                var steamReviewDesc: String? = null
                var steamReviews: List<com.example.gamelock.domain.model.SteamReview> = emptyList()
                if (resolvedAppId != null) {
                    try {
                        val reviewResponse = steamApi.getReviews(resolvedAppId)
                        Log.d(TAG, "Steam reviews response success=${reviewResponse.success} for app $resolvedAppId")
                        if (reviewResponse.success == 1) {
                            val summary = reviewResponse.query_summary
                            if (summary != null && summary.total_reviews > 0) {
                                steamReviewPct = (summary.total_positive.toDouble() / summary.total_reviews * 100).toInt()
                                steamReviewDesc = summary.review_score_desc
                                steamReviews = reviewResponse.reviews?.map { it.toDomain() } ?: emptyList()
                                Log.d(TAG, "Loaded ${steamReviews.size} Steam reviews, $steamReviewPct% positive")
                            } else {
                                Log.d(TAG, "No Steam reviews (total_reviews = ${summary?.total_reviews})")
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Steam reviews failed for app $resolvedAppId: ${e.message}")
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
                    description = translatedRu ?: primaryOriginal,
                    descriptionOriginal = primaryOriginal,
                    descriptionSource = descSource,
                    steamReviewPct = steamReviewPct,
                    steamReviewDesc = steamReviewDesc,
                    ratingSource = ratingSource,
                    storeRatings = storeRatings,
                    overallRating = overallRating,
                    userStatus = local?.let { GameStatus.valueOf(it.userStatus) } ?: GameStatus.NONE,
                    userRating = local?.userRating ?: 0f,
                    userReview = local?.userReview ?: "",
                    redditPosts = redditPosts,
                    steamReviews = steamReviews
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

    private fun stripHtml(html: String): String {
        return html.replace(Regex("<[^>]*>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun getLibraryGames(userId: Int): Flow<List<Game>> =
        dao.getAllLibraryGames(userId).map { list -> list.map { it.toDomain() } }

    fun getGamesByStatus(userId: Int, status: GameStatus): Flow<List<Game>> =
        dao.getGamesByStatus(userId, status.name).map { list -> list.map { it.toDomain() } }

    suspend fun saveGame(game: Game, userId: Int = 0) = dao.insertGame(game.toEntity(userId))

    suspend fun updateLastAccessed(gameId: Int, userId: Int = 0) =
        dao.updateLastAccessed(gameId, userId, System.currentTimeMillis())

    suspend fun updateUserData(game: Game, userId: Int = 0) =
        dao.updateUserData(game.id, userId, game.userStatus.name, game.userRating, game.userReview)

    suspend fun deleteGame(game: Game, userId: Int = 0) = dao.deleteGame(game.toEntity(userId))
}
