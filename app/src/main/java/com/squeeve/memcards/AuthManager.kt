package com.squeeve.memcards

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class AuthManager(private val context: Context) {
    fun startLoginActivity(msg: String = "Please re-authenticate.") {
        Log.d("AuthMan", "Login was requested again.")
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        context.startActivity(Intent(context, LoginRegister::class.java))
    }

    fun logout() {
        FirebaseAuth.getInstance().signOut()
        Toast.makeText(context, "Thank you, come again!", Toast.LENGTH_SHORT).show()
        context.startActivity(Intent(context, LoginRegister::class.java))
    }
}