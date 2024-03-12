package com.squeeve.memcards

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginRegister : AppCompatActivity() {

    private lateinit var auth : FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_register)

        auth = FirebaseAuth.getInstance()

        val fragmentManager = supportFragmentManager
        val loginFragment = LoginFragment()
        val registerFragment = RegisterFragment()

        val currentUser = auth.currentUser
        val initialFrag = if (currentUser == null) registerFragment else loginFragment

        // Display login fragment by default
        fragmentManager.beginTransaction().apply {
            replace(R.id.userform, initialFrag)
            commit()
        }

        // Button click listeners
        findViewById<Button>(R.id.btnLoginForm).setOnClickListener {
            fragmentManager.beginTransaction().apply {
                replace(R.id.userform, loginFragment)
                commit()
            }
        }
        findViewById<Button>(R.id.btnRegisterForm).setOnClickListener {
            fragmentManager.beginTransaction().apply {
                replace(R.id.userform, registerFragment)
                commit()
            }
        }
    }
}