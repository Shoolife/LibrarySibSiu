package com.example.libsibsiu.models

data class News(
    val title: String = "",
    var date: String = "",
    var imageUrl: String = "",
    var id: String = "",
    val description: String = "",
    var likeCount: Int = 0,
    var dislikeCount: Int = 0,
    var tags: List<String> = emptyList(),
    var searchKeywords: List<String> = emptyList()
)