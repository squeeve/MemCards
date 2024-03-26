package com.squeeve.memcards

import com.squeeve.memcards.Game.Companion.EASY
import com.squeeve.memcards.Game.Companion.MED
import com.squeeve.memcards.Game.Companion.HARD

import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.google.firebase.database.FirebaseDatabase


class MainActivity2 : BaseActivity() {
    private val tag: String = "MainActivity2"

    override fun getLayoutId(): Int {
        return R.layout.activity_main
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val easyButton = findViewById<Button>(R.id.easyButton)
        val medButton = findViewById<Button>(R.id.mediumButton)
        val hardButton = findViewById<Button>(R.id.hardButton)

        easyButton.setOnClickListener { gotoGame(EASY) }
        medButton.setOnClickListener { gotoGame(MED) }
        hardButton.setOnClickListener { gotoGame(HARD) }

        Log.d(tag, "onCreate: MainActivity2 complete.")
    }

}
