package com.squeeve.memcards

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.actionCodeSettings


class LoginFragment : Fragment() {
    private var tag: String = "LoginFragment"
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_login, container, false)
        val emailEditText = view.findViewById<EditText>(R.id.emailText)
        val passEditText = view.findViewById<EditText>(R.id.passwordText)
        val loginBtn = view.findViewById<Button>(R.id.btnLogin)
        val emailLoginBtn = view.findViewById<Button>(R.id.btnResendEmail)
        val forgotPassBtn = view.findViewById<Button>(R.id.btnForgotPass)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        loginBtn.setOnClickListener {
            Log.d(tag, "Logging in with password.")
            loginUser(
                emailEditText.text.toString(),
                passEditText.text.toString(),
            )
        }
        emailLoginBtn.setOnClickListener {
            Log.d(tag, "Resending verification email.")
            Toast.makeText(requireContext(),
                "Resending verification email. Please check your email again soon.",
                Toast.LENGTH_SHORT).show()
            currentUser!!.sendEmailVerification()
        }
        forgotPassBtn.setOnClickListener {
            auth.sendPasswordResetEmail(emailEditText.text.toString()).addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d(tag, "Sent password reset.")
                    Toast.makeText(requireContext(),
                        "Password reset email sent. Please check your email.",
                        Toast.LENGTH_SHORT).show()
                } else {
                    Log.e(tag, "Unknown error occurred when sending password reset.")
                }
            }
        }

        return view
    }

    private fun loginUser(email: String, pass: String="", byEmail: Boolean=false) {
        if (email == "" || pass == "") {
            Log.d(tag, "registerUser: User did not fill out email field")
            Toast.makeText(
                requireContext(),
                "Please enter email and password.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (!byEmail) {
            auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(requireActivity()) {
                    if (it.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(tag, "logInWithPassword:success")
                        if (auth.currentUser!!.isEmailVerified) {
                            updateUI(auth.currentUser)
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Please verify your email address before continuing.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(tag, "logInWithPassword:failure", it.exception)
                        Toast.makeText(
                            requireContext(),
                            "Authentication failed.",
                            Toast.LENGTH_SHORT,
                        ).show()
                        updateUI(null)
                    }
                }
        } else {
            val actionCodeSettings = actionCodeSettings {
                url = "TODO"
                handleCodeInApp = true

            }
            auth.sendSignInLinkToEmail(email, actionCodeSettings)
                .addOnCompleteListener(requireActivity()) {
                    if (!it.isSuccessful) {
                        Log.e(tag, "logInWithEmail:failure", it.exception)
                        Toast.makeText(
                            requireContext(),
                            "Login failure. Check your email address and try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            val intent = Intent(requireContext(), MainActivity2::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
    }

}