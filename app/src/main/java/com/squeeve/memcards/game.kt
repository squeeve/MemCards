package com.squeeve.memcards

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
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

        val backButton = findViewById<Button>(R.id.restartBtn)

        backButton.setOnClickListener {
            val restart = Intent(this, MainActivity::class.java)
            startActivity(restart)
        }
    }
}
class Game : AppCompatActivity() {
    private var cardsArray = mutableListOf<Card>()
    private val tag: String = "Game"
     private fun showConfirmationDialog() {
        Log.d(tag, "Entered showConfirmationDialog")
        val builder = AlertDialog.Builder(this@Game)
        builder.setTitle("Quitting affects your history...")
        builder.setMessage("Are you really quitting now?")

        builder.setPositiveButton("Yes") { _, _ ->
            val restart = Intent(this@Game, MainActivity2::class.java)
            startActivity(restart)
            finish()
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        val intent = this.intent
        val gridSize: Int = intent.getIntExtra("level", 2)
        var openedCard = false
        var openedCardIndex: Int = -1

        cardsArray = setUpGameCards(this, gridSize, cardsArray)

        var faces = cardsArray.size / 2

        val gridLayout = findViewById<GridLayout>(R.id.gameLayout)
        gridLayout.columnCount = gridSize
        gridLayout.rowCount = gridSize
        if (gridSize > 4) {
            val param = gridLayout.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, 0, 0, 0)
            gridLayout.layoutParams = param
            gridLayout.setPadding(1, 1, 1, 1)
        }

        //val tries = findViewById<TextView>(R.id.tries)
        //var ct = 0
        //tries.setText("Tries: $ct")

        cardsArray.forEachIndexed { idx, card ->
            val textView = TextView(this)
            val param = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                4.0f
            )
            textView.layoutParams = param
            textView.id = idx
            textView.background = ResourcesCompat.getDrawable(getResources(), R.drawable.border, null)
            textView.setText(R.string.card_back)
            //textView.setOnClickListener(textViewListener)
            textView.setOnClickListener {
                textView.text = card.value
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
                                //.putExtra("tries", ++ct)
                            startActivity(endScreen)
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "Nope, sorry.. That was ${card.value} by the way",
                            Toast.LENGTH_SHORT
                        ).show()
                        cardsArray[openedCardIndex].matched = true
                        val firstCard = findViewById<TextView>(openedCardIndex)
                        firstCard.setText(R.string.card_back)
                        textView.setText(R.string.card_back)
                    }
                    openedCard = false
                    openedCardIndex = -1
                    //tries.setText("Tries: ${++ct}")
                } else {
                    openedCard = true
                    openedCardIndex = card.index
                    //Toast.makeText(this, "Where's the match?? (id: ${textView.id})", Toast.LENGTH_SHORT).show()
                }
            }
            gridLayout.addView(textView)
        }
        val backButton = findViewById<Button>(R.id.backBtn)
        backButton.setOnClickListener {
            showConfirmationDialog()
        }
    }
}