package com.example.memcards

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
//import android.widget.Toast
//import androidx.databinding.DataBindingUtil


class MainActivity : AppCompatActivity() {

    private val easy = 2
    private val med = 4
    private val hard = 6

    private fun gotoGame(level: Int) {
        /*
        Toast.makeText(applicationContext,
            "You've picked $level! Cool beans...",
            Toast.LENGTH_SHORT).show() */
        val startGame = Intent(this, Game::class.java)
        startGame.putExtra("level", level)
        startActivity(startGame)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //val binding: ActivityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setContentView(R.layout.activity_main)

        val easyButton = findViewById<Button>(R.id.easyButton)
        val medButton = findViewById<Button>(R.id.mediumButton)
        val hardButton = findViewById<Button>(R.id.hardButton)

        easyButton.setOnClickListener {
            val grid = easy
            gotoGame(grid)
        }
        medButton.setOnClickListener {
            val grid = med
            gotoGame(grid)
        }
        hardButton.setOnClickListener {
            val grid = hard
            gotoGame(grid)
        }


    }

}