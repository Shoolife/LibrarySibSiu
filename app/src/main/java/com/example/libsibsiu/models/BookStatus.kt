package com.example.libsibsiu.models

import java.util.Date

data class BookStatus(
    val bookId: String = "", // Значения по умолчанию
    val title: String = "",
    var status: String = "",
    val quantity: Int = 0,
    val checkoutDate: Date = Date(0), // Используйте Date(0) для указания начальной даты
    val dueDate: Date = Date(0),
    val coverUrl: String = ""
)