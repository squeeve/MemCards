package com.squeeve.memcards

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity


/* This activity actually uses the original activity_main layout file,
* as the old MainActivity.kt is now the account-verify activity, using the
* splash layout. */
class MainActivity2 : AppCompatActivity() {

    private fun gotoGame(level: Int) {
        val startGame = Intent(this, Game::class.java)
        startGame.putExtra("level", level)
        startActivity(startGame)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val easyButton = findViewById<Button>(R.id.easyButton)
        val medButton = findViewById<Button>(R.id.mediumButton)
        val hardButton = findViewById<Button>(R.id.hardButton)

        easyButton.setOnClickListener { gotoGame(EASY) }
        medButton.setOnClickListener { gotoGame(MED) }
        hardButton.setOnClickListener { gotoGame(HARD) }
    }
}