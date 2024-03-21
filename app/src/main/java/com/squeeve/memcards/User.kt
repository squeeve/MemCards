package com.squeeve.memcards


data class User(
    var username: String = "",
    var email: String = "",
    var profilePicture: String? = null,
    var scoreHistory: MutableList<Int> = mutableListOf(),
)
