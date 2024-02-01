package com.example.memcards

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat



class EndScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_endscreen)

        val intent = this.intent
        val triesCt: Int = intent.getIntExtra("tries", -1)

        val backButton = findViewById<Button>(R.id.restartBtn)
        val tries = findViewById<TextView>(R.id.tries)
        tries.setText("Tries: $triesCt ‼️")

        backButton.setOnClickListener {
            val restart = Intent(this, MainActivity::class.java)
            startActivity(restart)
        }
    }
}
class Game : AppCompatActivity() {
    data class Card(
        var value: String,
        var index: Int = -1,
        var match: Int = -1,
        var matched: Boolean = false
    )

    private var cardsArray = mutableListOf<Card>()

    /* sets up list of cards and their matches */
    private fun setUpGameCards(gridSize: Int): MutableList<Card> {
        val allCardContents = resources.getStringArray(R.array.card_contents)
        val useCards = allCardContents.slice(0..<(gridSize * gridSize)/2)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        val intent = this.intent
        val gridSize: Int = intent.getIntExtra("level", 2)
        var openedCard = false
        var openedCardIndex: Int = -1

        setUpGameCards(gridSize)

        var faces = cardsArray.size/2

        val gridLayout = findViewById<GridLayout>(R.id.gameLayout)
        gridLayout.columnCount = gridSize
        gridLayout.rowCount = gridSize
        if (gridSize > 4) {
            val param = gridLayout.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0,0,0,0)
            gridLayout.layoutParams = param
            gridLayout.setPadding(1,1,1,1)
        }

        val tries = findViewById<TextView>(R.id.tries)
        var ct = 0
        tries.setText("Tries: $ct")

        cardsArray.forEachIndexed { idx, card ->
            val textView = TextView(this)
            val param = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                4.0f
            )
            textView.layoutParams = param
            textView.id = idx
            textView.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.border, null))
            textView.setText(R.string.card_back)
            //textView.setOnClickListener(textViewListener)
            textView.setOnClickListener {
                textView.setText(card.value)
                    if (openedCard) {
                        if (card.match == cardsArray[openedCardIndex].index) {
                            Toast.makeText(this, "Found a match!", Toast.LENGTH_SHORT).show()
                            card.matched = true
                            cardsArray[openedCardIndex].matched = true
                            textView.alpha = 0F
                            textView.isClickable = false
                            val firstCard = findViewById<TextView>(openedCardIndex)
                            firstCard.alpha = 0F
                            firstCard.isClickable = false
                            if (--faces == 0) {
                                val endScreen = Intent(this, EndScreen::class.java)
                                    .putExtra("tries", ++ct)
                                startActivity(endScreen)
                            }
                        } else {
                            Toast.makeText(this, "Nope, sorry.. That was ${card.value} by the way", Toast.LENGTH_SHORT).show()
                            cardsArray[openedCardIndex].matched = true
                            val firstCard = findViewById<TextView>(openedCardIndex)
                            firstCard.setText(R.string.card_back)
                            textView.setText(R.string.card_back)
                        }
                        openedCard = false
                        openedCardIndex = -1
                        tries.setText("Tries: ${++ct}")
                    } else {
                        openedCard = true
                        openedCardIndex = card.index
                        //Toast.makeText(this, "Where's the match?? (id: ${textView.id})", Toast.LENGTH_SHORT).show()
                    }
                }
            gridLayout.addView(textView)
        }
        val backButton = findViewById<Button>(R.id.backBtn)
        backButton.setOnClickListener(backButtonListener)
    }

    private val backButtonListener = View.OnClickListener {
        val main = Intent(this, MainActivity::class.java)
        startActivity(main)
    }
}