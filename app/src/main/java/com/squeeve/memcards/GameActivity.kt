package com.squeeve.memcards

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue


class GameActivity : AppCompatActivity(), Game.OnGameEndListener {
    private val tag: String = "GameActivity"
    private var level: Int = 0
    private lateinit var game: Game
    private var elapsedSeconds: Long = 0
    private lateinit var gameTimerView: TextView
    private lateinit var countUpTimer: CountUpTimer
    private lateinit var db: DatabaseReference
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

     private fun showConfirmationDialog(quit: Boolean = true) {
        Log.d(tag, "Entered showConfirmationDialog")
        val builder = AlertDialog.Builder(this@GameActivity)
        if (quit) {     // Quit button
            builder.setTitle("Are you sure?")
            builder.setMessage("Do you really want to quit?")

            builder.setPositiveButton("Yes") { _, _ ->
                val restart = Intent(this@GameActivity, MainActivity2::class.java)
                startActivity(restart)
                finish()
            }
            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
        } else {        // Reset button
            builder.setTitle("Are you sure?")
            builder.setMessage("Do you want to start this round over?")
            builder.setPositiveButton("Yes") { _,_ ->
                elapsedSeconds = 0
                startCountUpTimer()
                game.resetGame()
            }
            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        val intent = this.intent
        level = intent.getIntExtra("level", 2)
        val gridLayout = findViewById<GridLayout>(R.id.gameLayout)

        game = Game(this, level, gridLayout)
        game.onGameEndListener = this
        db = FirebaseDatabase.getInstance().reference

        if (savedInstanceState != null) {
            elapsedSeconds = savedInstanceState.getLong("elapsedSeconds")
            startCountUpTimer()
            game.resetGame(reoriented=true)
        } else {
            game.initializeGame()
            startCountUpTimer()
        }

        gameTimerView = findViewById(R.id.timerView)
        val backButton = findViewById<Button>(R.id.backBtn)
        backButton.setOnClickListener {
            showConfirmationDialog()
        }
        val resetButton = findViewById<Button>(R.id.restartBtn)
        resetButton.setOnClickListener {
            showConfirmationDialog(quit=false)
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("elapsedSeconds", elapsedSeconds)
    }

    private fun startCountUpTimer() {
        countUpTimer = object : CountUpTimer() {
            override fun onTick(elapsedSeconds: Long) {
                this@GameActivity.elapsedSeconds = elapsedSeconds
                updateTimerUI(elapsedSeconds)
            }
        }
        countUpTimer.start()
    }

    private fun updateTimerUI(elapsedSeconds: Long) {
        gameTimerView.text = getString(R.string.timer_text, "$elapsedSeconds seconds")
    }

    private fun saveUserStatsToFirebase(score: Long): String {
        // Adds user's score to their account, and updates the leaderboard if applicable.
        // Returns the key of the user's score.
        val userScoresRef = db.child("Users").child(auth.currentUser!!.uid)
                                                .child("Scores")
                                                .child(LEVELSTOSTRING.getValue(level))
        val newScoreRef = userScoresRef.push()
        val scoreData = mapOf(
            "score" to score,
            "timestamp" to ServerValue.TIMESTAMP  // returns epoch-time
        )
        newScoreRef.setValue(scoreData)

        return newScoreRef.key!!
    }

    private fun removeGameState() {
        val userRef = db.child("Users").child(auth.currentUser!!.uid)
        userRef.child("gameState").removeValue()
            .addOnCompleteListener {
                if (!it.isSuccessful) {
                    Log.d(tag, "Error removing gameState: ${it.exception}")
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        countUpTimer.stopTimer()
    }

    override fun onGameEnd() {
        countUpTimer.stopTimer()
        val score = if (1000/elapsedSeconds <= game.tries) 0 else 1000/elapsedSeconds-game.tries
        val key = saveUserStatsToFirebase(score)
        removeGameState()

        Log.d(tag, "Sending to leaderboard... score: $score. level: $level")
        val leaderboardActivity = Intent(this, LeaderboardActivity::class.java)
        leaderboardActivity.putExtra("thisScore", score)
        leaderboardActivity.putExtra("thisLevel", level)
        leaderboardActivity.putExtra("scoreId", key)
        startActivity(leaderboardActivity)
        finish()
    }
}

abstract class CountUpTimer : Thread() {
    private var startTime : Long = 0
    private var running = false
    override fun run() {
        running = true
        startTime = System.currentTimeMillis()
        while (running) {
            val elapsedTime = System.currentTimeMillis() - startTime
            val elapsedSec = elapsedTime/1000
            onTick(elapsedSec)
            try {
                sleep(1000) // sleep for 1 second
            } catch (e: InterruptedException) {
                interrupt()
            }
        }
    }

    fun stopTimer() {
        running = false
    }

    abstract fun onTick(elapsedSeconds: Long)
}