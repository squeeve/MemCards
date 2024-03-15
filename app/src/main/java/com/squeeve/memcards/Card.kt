package com.squeeve.memcards


data class Card(
    var value: String,
    var index: Int = -1,
    var match: Int = -1,
    var matched: Boolean = false
)

