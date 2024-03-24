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
//import com.google.firebase.database.ServerValue
//import java.util.Date


class GameActivity : AppCompatActivity(), Game.OnGameEndListener {
    private val tag: String = "GameActivity"
    private var level: Int = 0
    private lateinit var levelStr: String
    private lateinit var game: Game
    private lateinit var gameTimerView: TextView
    private lateinit var countUpTimer: CountUpTimer
    private lateinit var db: DatabaseReference
    private lateinit var userRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var authMan: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        auth = FirebaseAuth.getInstance()
        authMan = AuthManager(this)
        if (auth.currentUser == null) {
            authMan.startLoginActivity()
            finish()
        }

        val intent = this.intent
        level = intent.getIntExtra("level", 2)
        levelStr = LEVELSTOSTRING.getValue(level)
        val gridLayout = findViewById<GridLayout>(R.id.gameLayout)
        gameTimerView = findViewById(R.id.timerView)

        game = Game(this, level, gridLayout)
        game.onGameEndListener = this
        db = FirebaseDatabase.getInstance().reference
        userRef = db.child("Users").child(auth.currentUser!!.uid)

        if (savedInstanceState != null) {
            startCountUpTimer(savedInstanceState.getLong("elapsedSeconds"))
            game.resetGame(reoriented=true)
        } else {
            startCountUpTimer(0)
            game.initializeGame(shuffle=true)
        }

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
        outState.putLong("elapsedSeconds", countUpTimer.getElapsedSeconds())
    }

    private fun startCountUpTimer(offset: Long = 0) {
        countUpTimer = object : CountUpTimer(this) {
            override fun onTick(elapsedSeconds: Long) {
                updateTimerUI(elapsedSeconds)
            }
        }
        countUpTimer.setInitialOffset(offset)
        countUpTimer.start()
    }

    internal fun updateTimerUI(elapsedSeconds: Long) {
        gameTimerView.text = getString(R.string.timer_text, "$elapsedSeconds seconds")
    }

    private fun saveUserStatsToFirebase(score: Score): String {
        // Adds user's score to their account, and updates the leaderboard if applicable.
        // Returns the key of the user's score.
        val userScoresRef = db.child("Users").child(auth.currentUser!!.uid)
                                                .child("Scores")
                                                .child(levelStr)
        val newScoreRef = userScoresRef.push()
        val scoreData = mapOf(
            "score" to score.score as Int,
            "timestamp" to score.timestamp as Long
        )
        newScoreRef.setValue(scoreData)
        /*
        val userEntry = LeaderboardEntry(
            level = levelStr,
            score = score,
            timeStamp = Date(scoreData.getValue("timestamp")),
            //username = ,
            userUid = auth.currentUser!!.uid.toString(),
            scoreId = newScoreRef.key!!.toString()
        ) */

        return newScoreRef.key!!
    }

    override fun onDestroy() {
        super.onDestroy()
        countUpTimer.stopTimer()
    }

    override fun onGameEnd() {
        Log.d(tag, "onGameEnd: elapsedSeconds = ${countUpTimer.getElapsedSeconds()}")
        countUpTimer.stopTimer()
        var score = (1000*level/countUpTimer.getElapsedSeconds()).toInt()
        score = if (score > game.tries) { score - game.tries } else { 0 }
        try {
            val user = User(this, auth.currentUser!!.uid)
            Log.d(tag, "User object created: ${user.username}. Scores: ${user.scoreHistory}")
            val scorePoint = Score(score, System.currentTimeMillis() / 1000, level)
            val scoreList = user.getScoresForLevel(level).toMutableList()
            scoreList.add(scorePoint)
            user.scoreHistory = scoreList
            user.writeUserPrefs()

            val key = saveUserStatsToFirebase(scorePoint)

            Log.d(tag, "Sending to leaderboard... score: $score. level: $level")
            val leaderboardActivity = Intent(this, LeaderboardActivity::class.java)
            leaderboardActivity.putExtra("thisScore", scorePoint)
            leaderboardActivity.putExtra("thisLevel", level)
            leaderboardActivity.putExtra("scoreId", key)
            startActivity(leaderboardActivity)
            finish()
        } catch (e: Exception) {
            Log.e(tag, "Error creating user object: $e")
            authMan.startLoginActivity()
            finish()
        }
    }

    private fun showConfirmationDialog(quit: Boolean = true) {
        Log.d(tag, "Entered showConfirmationDialog")
        val builder = AlertDialog.Builder(this)
        if (quit) {     // Quit button
            builder.setTitle("Are you sure?")
            builder.setMessage("Do you really want to quit?")

            builder.setPositiveButton("Yes") { _, _ ->
                val restart = Intent(this, MainActivity2::class.java)
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
                countUpTimer.stopTimer()
                startCountUpTimer(0)
                game.resetGame(reoriented=false)
            }
            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
        }
        val dialog = builder.create()
        dialog.show()
    }
}

abstract class CountUpTimer(private val activity: GameActivity) : Thread() {
    private var startTime : Long = 0
    private var running = false
    private var elapsedSeconds : Long = 0
    override fun run() {
        running = true
        startTime = System.currentTimeMillis()
        while (running) {
            val elapsedTime = System.currentTimeMillis() - startTime
            elapsedSeconds = elapsedTime/1000
            updateUI(elapsedSeconds)
            try {
                sleep(1000) // sleep for 1 second
            } catch (e: InterruptedException) {
                interrupt()
            }
        }
    }

    fun getElapsedSeconds(): Long {
        return elapsedSeconds
    }

    private fun updateUI(elapsedSeconds: Long) {
        activity.runOnUiThread {
            activity.updateTimerUI(elapsedSeconds)
        }
    }

    fun stopTimer() {
        Log.d("GameActivity", "stopTimer called.")
        running = false
    }

    abstract fun onTick(elapsedSeconds: Long)

    fun setInitialOffset(offset: Long) {
        elapsedSeconds = offset
    }
}