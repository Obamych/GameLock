package com.example.gamelock.domain.model

data class RedditPost(
    val title: String,
    val text: String?,
    val url: String,
    val image: String?,
    val username: String,
    val created: String
)
