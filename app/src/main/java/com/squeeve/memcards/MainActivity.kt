package com.squeeve.memcards

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class MainActivity : AppCompatActivity() {
    private var TAG: String = "FirebaseAuthActivity"

    private lateinit var auth: FirebaseAuth
    private var user: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.splash)

        // Initialize Firebase Auth
        auth = Firebase.auth
        user = Firebase.auth.currentUser
    }

    override fun onResume() {
        super.onResume()

        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                Log.d(TAG, "Splash screen timer ticking down!")
            }
            override fun onFinish() {
                Log.d(TAG, "With @ sign: " + this@MainActivity)
                Log.d(TAG, "Without @ sign: " + this)
                if (user == null) {
                    Toast.makeText(
                        this@MainActivity,
                        "No user found",
                        Toast.LENGTH_SHORT
                    ).show()
                    //startActivity(Intent(this@MainActivity, LoginSignup::class.java))
                    //finish()
                } else {
                    if (user!!.isEmailVerified) {
                        Toast.makeText(
                            this@MainActivity,
                            "User already signed in",
                            Toast.LENGTH_SHORT
                        ).show()
                        startActivity(Intent(this@MainActivity, MainActivity2::class.java))
                        finish()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Please verify your email and login.",
                            Toast.LENGTH_SHORT
                        ).show()
                        //startActivity(Intent(this@MainActivity, LoginSignup::class.java))
                        //finish()
                    }
                }
            }
        }.start()
    }

}