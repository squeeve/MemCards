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

class Game(private val context: Context, private val gridSize: Int, val layout: GridLayout) {
    private val tag = "Game"
    private val fh = FileHelper(context)

    private var cardsArray = mutableListOf<Card>()
    private var gridLayout = layout
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
    private var gameStateFile = auth.currentUser!!.uid + ".gameState.json"

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
        // By now, the cardsArray order is set for this round.
        // To reset the game, just reset the game parameters. No need to call this function again.
        return cardsArray
    }

    private fun onCardClick(textView: TextView, card: Card) {
        Log.d(tag, "onCardClick: Started. Tries: ${tries}. Card: ${card.value}. Opened: $openedCard")
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
            Log.d(tag, "onCardClick:: tries now $tries")
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
        saveGameState()
        Log.d(tag, "onCardClick:: finished")
    }

    private fun flipBackCards(secondCardView: TextView, otherCard: Int) {
        handler.postDelayed({
            secondCardView.text = context.resources.getString(R.string.card_back)
            gridLayout.findViewById<TextView>(otherCard).setText(R.string.card_back)
        }, 1000)
    }

    private fun drawGame(): GridLayout {
        // I should note that this is only for initial rendering when Activity is created/recreated.
        // After that, onCardClick takes over the rest of the rendering. Hence, cardsArray needs to be
        // properly set to previous state.

        gridLayout.removeAllViews()
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
            if (card.matched) {
                textView.apply {
                    layoutParams = param
                    id = idx
                    alpha = 0F
                    isClickable = false
                }
            } else {
                textView.apply {
                    layoutParams = param
                    id = idx
                    background =
                        ResourcesCompat.getDrawable(context.resources, R.drawable.border, null)
                    text = if (idx != openedCardIndex) { context.resources.getString(R.string.card_back) } else { card.value }
                }
                textView.setOnClickListener {
                    onCardClick(textView, card)
                }
            }
            gridLayout.addView(textView)
        }
        saveGameState()
        return gridLayout
    }

    private fun saveGameState() {
        val cardsArrayMap = cardsArray.map { card ->
            mapOf(
                "value" to card.value,
                "index" to card.index,
                "match" to card.match,
                "matched" to card.matched
            )
        }

        Log.d(tag, "saveGameStateToFirebase: tries: $tries. faces: $faces")
        val gameStateMap = mapOf(
            "tries" to tries,
            "faces" to faces,
            "openedCard" to openedCard,
            "openedCardIndex" to openedCardIndex,
            "cardsArray" to cardsArrayMap
        )
        fh.saveToFile(gameStateMap, gameStateFile)

    }

    private fun initializeFirebase() {
        db = FirebaseDatabase.getInstance().reference
        val currentUser = auth.currentUser!!
        gameRef = db.child("Users").child(currentUser.uid).child("gameState")
    }

    internal fun initializeGame(shuffle: Boolean = false) {
        initializeFirebase()
        Log.d(tag, "Reinitializing game, with shuffle = $shuffle")
        if (shuffle) {
            setUpGameCards(gridSize)
        } else {
            cardsArray.forEachIndexed { _, card ->
                card.matched = false
            }
        }
        openedCard = false
        openedCardIndex = null
        faces = cardsArray.size / 2
        tries = 0
        drawGame()
    }

    internal fun resetGame(reoriented: Boolean) {
        val tag = "gameState" // makes debugging easier

        if (!reoriented) {  // reset happened because of Button click.
            initializeGame()
        } else {        // reset happened because device was reoriented
            initializeFirebase()
            val currState: Map<String,Any>? = fh.readFromFile(gameStateFile)
            if (currState != null) {
                for ((key, value) in currState) {
                    when (key) {
                        "tries" -> tries = (value as Number).toInt()
                        "faces" -> faces = (value as Number).toInt()
                        "openedCardIndex" -> openedCardIndex = (value as Number).toInt()
                        "openedCard" -> openedCard = value as Boolean
                        "cardsArray" -> {
                            val arrayCardList = value as? ArrayList<Map<String, Any>>
                            cardsArray = arrayCardList?.map { c ->
                                val cvalue = c["value"] as String
                                val index = (c["index"] as Double).toInt()
                                val match = (c["match"] as Double).toInt()
                                val matched = c["matched"] as Boolean
                                Log.d(tag, "Building card...")
                                Card(cvalue, index, match, matched)
                            }?.toMutableList() ?: setUpGameCards(gridSize)
                        }
                    }
                }
                Log.d(tag, "resetGame: Faces left: $faces. Tries: $tries. openedCardIndex: $openedCardIndex. openedCard = $openedCard")
            } else {
                Log.e(tag, "Something is wrong with reading your gameState file; returned null!")
            }
        }
        drawGame()
    }

    interface OnGameEndListener {
        fun onGameEnd()
    }

}
