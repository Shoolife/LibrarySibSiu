package com.example.libsibsiu.models

data class Video(
    val title: String = "",
    var date: String = "",
    var imageUrl: String = "",
    var id: String = "",
    val description: String = "",
    val videoUrl: String = "",
    var likeCount: Int = 0,
    var dislikeCount: Int = 0,
    var popularity: Int = 0,
    var categoryId: String = "",
    var searchKeywords: List<String> = emptyList()
)