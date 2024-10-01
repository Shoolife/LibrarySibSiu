package com.example.libsibsiu.models

data class Book(
    var id: String = "",
    val author: String = "",
    val coverUrl: String = "",
    val isbn: String = "",
    val publishYear: String = "",
    val summary: String = "",
    val title: String = "",
    val aboutTheBook: String = "",
    val quote: String = "",
    val averageRating: Double = 0.0,
    var userRating: Float? = null,
    val pdfUrl: String = "",
    var isFavorite: Boolean = false
)