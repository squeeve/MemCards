package com.squeeve.memcards

import android.content.Context

data class Card(
    var value: String,
    var index: Int = -1,
    var match: Int = -1,
    var matched: Boolean = false
)

/* game levels */
internal const val EASY = 2
internal const val MED = 4
internal const val HARD = 6

/* sets up list of cards and their matches */
internal fun setUpGameCards(context: Context, gridSize: Int, cardsArray: MutableList<Card>): MutableList<Card> {
    val allCardContents = context.resources.getStringArray(R.array.card_contents)
    val useCards = allCardContents.slice(0..<(gridSize * gridSize) / 2)
    val ordered = (useCards + useCards).shuffled()
    ordered.forEachIndexed { idx, face ->
        cardsArray.add(Card(face, idx))
    }

    useCards.forEach {
        val firstIndex = cardsArray.indexOfFirst { card -> it == card.value }
        val lastIndex = cardsArray.indexOfLast { card -> it == card.value }
        lastIndex.also { cardsArray[firstIndex].match = it }
        firstIndex.also { cardsArray[lastIndex].match = it }
    }
    //Toast.makeText(this, "level: $gridSize; length: ${useCards.size}", Toast.LENGTH_SHORT).show()
    return cardsArray
}