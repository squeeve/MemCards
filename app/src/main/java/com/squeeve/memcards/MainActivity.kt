package com.squeeve.memcards

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

import com.bumptech.glide.Glide


class MainActivity : AppCompatActivity() {
    private var tag: String = "FirebaseAuthActivity"

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.splash)

        val iView: ImageView = findViewById(R.id.splashlogo)
        Glide.with(this)
            .load(R.drawable.dragon_animoji)
            .into(iView)

        // Initialize Firebase Auth
        auth = Firebase.auth
    }

    override fun onResume() {
        super.onResume()

        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                Log.d(tag, "Splash screen timer ticking down!")
            }
            override fun onFinish() {
                val user = auth.currentUser

                if (user == null) {
                    Log.d(tag, "No active user found.")
                    startActivity(Intent(this@MainActivity, LoginRegister::class.java))
                    finish()
                } else {
                    if (user!!.isEmailVerified) {
                        Log.d(tag, "User was email-verified.")
                        startActivity(Intent(this@MainActivity, MainActivity2::class.java))
                        finish()
                    } else {
                        Log.d(tag, "User found, but session restarted; request LoginFragment again.")
                        startActivity(Intent(this@MainActivity, LoginRegister::class.java))
                        finish()
                    }
                }
            }
        }.start()
    }

}