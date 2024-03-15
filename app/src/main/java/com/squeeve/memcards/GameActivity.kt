package com.squeeve.memcards

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView



/* class EndScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_endscreen)

        // We're changing EndScreen to be a leaderboard page w/ a possible fragment.
    }
} */

class GameActivity : AppCompatActivity() {
    private val tag: String = "GameActivity"
    private lateinit var game: Game
    private lateinit var countUpTimer: CountUpTimer
    private var elapsedSeconds: Long = 0
    private lateinit var gameTimerView: TextView

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
        val gridSize: Int = intent.getIntExtra("level", 2)
        val gridLayout = findViewById<GridLayout>(R.id.gameLayout)
        game = Game(this, gridSize, gridLayout)
        game.initializeGame()

        gameTimerView = findViewById<TextView>(R.id.timerView)
        val backButton = findViewById<Button>(R.id.backBtn)
        backButton.setOnClickListener {
            showConfirmationDialog()
        }
        val resetButton = findViewById<Button>(R.id.restartBtn)
        resetButton.setOnClickListener {
            showConfirmationDialog(quit=false)
        }
        startCountUpTimer()
    }

    private fun startCountUpTimer() {
        countUpTimer = object : CountUpTimer() {
            override fun onTick(elapsedSeconds: Long) {
                this@GameActivity.elapsedSeconds = elapsedSeconds
                updateTimerUI(elapsedSeconds)
                //game.updateTimer(elapsedSeconds)
            }
        }
        countUpTimer.start()
    }

    private fun updateTimerUI(elapsedSeconds: Long) {
        gameTimerView.text = getString(R.string.timer_text, "${elapsedSeconds} seconds")
    }

    override fun onDestroy() {
        super.onDestroy()
        countUpTimer.stopTimer()
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