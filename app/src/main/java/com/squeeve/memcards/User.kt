package com.squeeve.memcards

import android.graphics.Picture

data class User(
    var username: String = "",
    var email: String = "",
    var profilePicture: String? = null,
    var scoreHistory: MutableList<Int> = mutableListOf(),
    var highestScore: Int = -1
)
