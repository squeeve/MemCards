package com.squeeve.memcards

import android.content.Context
import android.os.Handler
import android.util.Log
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

/* game levels */
internal const val EASY = 2
internal const val MED = 4
internal const val HARD = 6
internal val LEVELS = mapOf("EASY" to EASY, "MED" to MED, "HARD" to HARD)
internal val LEVELSTOSTRING = mapOf(EASY to "EASY", MED to "MEDIUM", HARD to "HARD")

class Game(private val context: Context, val gridSize: Int, val layout: GridLayout) {
    private val tag = "Game"

    internal var cardsArray = mutableListOf<Card>()
    internal var gridLayout = layout
    internal var tries: Int = 0      // number of attempted matches in this round
    private var openedCard: Boolean = false
    private var openedCardIndex: Int? = null
    private var handler = Handler()
    private var faces: Int = -1     // this should be overwritten in initialization
    lateinit var onGameEndListener: OnGameEndListener

    // Firebase
    private lateinit var db: DatabaseReference
    private lateinit var gameRef: DatabaseReference
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private fun setUpGameCards(gridSize: Int): MutableList<Card> {
        // 1. Pick out set of unique card faces, double them and shuffle them
        val allCardContents = context.resources.getStringArray(R.array.card_contents)
        val useCards = allCardContents.slice(0..<(gridSize * gridSize) / 2)
        val ordered = (useCards + useCards).shuffled()
        ordered.forEachIndexed { idx, face ->
            cardsArray.add(Card(face, idx))
        }
        // 2. Find the match of each card and save their match's index (bi-directional)
        useCards.forEach {
            val firstIndex = cardsArray.indexOfFirst { card -> it == card.value }
            val lastIndex = cardsArray.indexOfLast { card -> it == card.value }
            lastIndex.also { cardsArray[firstIndex].match = it }
            firstIndex.also { cardsArray[lastIndex].match = it }
        }
        // by now, the cardsArray order is set for this round.
        // To reset the game, just reset the game parameters, and rebuild from this list.
        return cardsArray
    }

    private fun onCardClick(textView: TextView, card: Card) {
        Log.d(tag, "onCardClick: Started. Tries: ${tries}. Card: ${card.value}. Opened: ${openedCard}")
        if (textView.text == card.value) { // clicked on something already open; don't count it.
            Log.d(tag, "onCardClick: clicked on open card; ignoring.")
            return
        }
        textView.text = card.value

        if (!openedCard) { // First card selection
            openedCardIndex = textView.id
            openedCard = true
        } else {
            tries++
            openedCard = false
            Log.d(tag, "onCardClick:: tries now ${tries}")
            // MATCH LOGIC
            if (card.match == cardsArray[openedCardIndex!!].index) {
                Log.d(tag, "onCardClick:: matched!")
                card.matched = true
                cardsArray[openedCardIndex!!].matched = true
                textView.apply {
                    alpha = 0F
                    isClickable = false
                }
                val firstCardView = gridLayout.findViewById<TextView>(openedCardIndex!!)
                firstCardView.apply {
                    alpha = 0F
                    isClickable = false
                }
                if (--faces == 0) {
                    onGameEndListener.onGameEnd()
                }
            // MISS LOGIC
            } else {
                Log.d(tag, "onCardClick:: didn't match!")
                flipBackCards(textView, openedCardIndex!!)
                openedCardIndex = null
            }
        }
        saveGameStateToFirebase()
        Log.d(tag, "onCardClick:: finished")
    }

    private fun flipBackCards(secondCardView: TextView, otherCard: Int) {
        handler.postDelayed({
            secondCardView.text = context.resources.getString(R.string.card_back)
            gridLayout.findViewById<TextView>(otherCard).setText(R.string.card_back)
        }, 1000)
    }

    internal fun drawGame(): GridLayout {
        gridLayout.columnCount = gridSize
        gridLayout.rowCount = gridSize
        if (gridSize > 4) {
            val param = gridLayout.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0,0,0,0)
            gridLayout.layoutParams = param
            gridLayout.setPadding(1,1,1,1)
        }

        cardsArray.forEachIndexed { idx, card ->
            val textView = TextView(context)
            val param = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                4.0f
            )
            textView.apply {
                layoutParams = param
                id = idx
                background = ResourcesCompat.getDrawable(context.resources, R.drawable.border, null)
                setText(context.resources.getString(R.string.card_back))
            }
            textView.setOnClickListener {
                onCardClick(textView, card)
            }
            gridLayout.addView(textView)
        }
        saveGameStateToFirebase()
        return gridLayout
    }

    private fun saveGameStateToFirebase() {
        val cardsArrayMap = cardsArray.map { card ->
            mapOf(
                "value" to card.value,
                "index" to card.index,
                "match" to card.match,
                "matched" to card.matched
            )
        }

        Log.d(tag, "saveGameStateToFirebase: tries: $tries")
        val gameStateMap = mapOf<String, Any>(
            "cardsArray" to cardsArrayMap,
            "tries" to tries,
        )
        gameRef.updateChildren(gameStateMap)
    }

    private fun initializeFirebase() {
        db = FirebaseDatabase.getInstance().reference
        val currentUser = auth.currentUser!!
        gameRef = db.child("Users").child(currentUser.uid).child("gameState")
    }

    internal fun initializeGame() {
        initializeFirebase()
        setUpGameCards(gridSize)
        faces = cardsArray.size / 2
        drawGame()
    }

    internal fun resetGame(reoriented: Boolean=false) {
        if (!reoriented) {  // reset happened because of Button click.
            cardsArray.forEachIndexed { _, card ->
                card.matched = false
            }
            faces = cardsArray.size / 2
            tries = 0
            gridLayout.removeAllViews()
        } else {        // reset happened because device was reoriented
            initializeFirebase()
            // TODO: put the cardsArray back the way it was, and replace faces and tries variables.
        }
        drawGame()
        Log.d(tag, "resetGame: Faces left: $faces. Tries: $tries.")
    }

    interface OnGameEndListener {
        fun onGameEnd()
    }

}
