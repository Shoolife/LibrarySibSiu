package com.example.libsibsiu.models

data class BookSelected(
    val id: String,            // ID книги
    val title: String,         // Название книги
    val coverUrl: String,      // URL обложки
    var isFavorite: Boolean,   // Состояние избранного
    val favoriteId: String     // ID избранного для управления записями в Firebase
)

