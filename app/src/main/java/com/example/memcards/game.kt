package com.example.memcards

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat

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
        Toast.makeText(this, "level: $gridSize; length: ${useCards.size}", Toast.LENGTH_SHORT).show()
        return cardsArray
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        val intent = this.intent
        val gridSize: Int = intent.getIntExtra("level", 2)

        setUpGameCards(gridSize)

        val gridLayout = findViewById<GridLayout>(R.id.gameLayout)
        gridLayout.columnCount = gridSize
        gridLayout.rowCount = gridSize
        if (gridSize > 4) {
            val param = gridLayout.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0,0,0,0)
            gridLayout.layoutParams = param
            gridLayout.setPadding(1,1,1,1)
        }

        for (card in cardsArray) {
            val textView = TextView(this)
            val param = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                4.0f
            )
            textView.layoutParams = param
            textView.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.border, null))
            textView.setText(R.string.card_back)

            textView.setOnClickListener {

                textView.setText(card.value)
                Toast.makeText(this, "Something here!", Toast.LENGTH_SHORT).show()
            }

            gridLayout.addView(textView)
        }

        val backButton = findViewById<Button>(R.id.backBtn)
        backButton.setOnClickListener {
            val picker = Intent(this, MainActivity::class.java)
            startActivity(picker)
        }
    }
}