package com.example.gamelock.domain.model

data class SteamReview(
    val text: String,
    val authorName: String,
    val authorAvatar: String?,
    val votedUp: Boolean,
    val votesUp: Int,
    val timestamp: Long
)
