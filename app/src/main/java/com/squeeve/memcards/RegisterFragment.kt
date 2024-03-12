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
import com.google.firebase.database.FirebaseDatabase



class RegisterFragment : Fragment() {

    private var tag: String = "RegisterFragment"
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_register, container, false)
        val registerBtn = view.findViewById<Button>(R.id.btnRegister)
        val emailEditText = view.findViewById<EditText>(R.id.emailText)
        val userEditText = view.findViewById<EditText>(R.id.nameText)
        val passwordEditText = view.findViewById<EditText>(R.id.passwordText)

        auth = FirebaseAuth.getInstance()

        registerBtn.setOnClickListener {
            registerUser(
                userEditText.text.toString(),
                emailEditText.text.toString(),
                passwordEditText.text.toString()
            )
        }
        return view
    }

    private fun saveUserToDB(username: String, email: String) {
        val db: FirebaseDatabase = FirebaseDatabase.getInstance()
        val usersRef = db.getReference("users")
        currentUser.let { user ->
            val newUser = User(
                username = username,
                email = email,
            )
            usersRef.child(user.uid).setValue(newUser)
        }
    }

    private fun registerUser(username: String, email: String, pass: String) {
        if (email == "" || pass == "" || username == "") {
            Log.d(tag, "registerUser: User did not fill out entire form")
            Toast.makeText(
                requireContext(),
                "All fields are required",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Log.d(tag, "createUserWithEmail:success")
                    Toast.makeText(
                        requireContext(),
                        "Registration successful!",
                        Toast.LENGTH_SHORT).show()
                    currentUser = auth.currentUser!!
                    currentUser.sendEmailVerification().addOnCompleteListener(requireActivity()) {
                        if (it.isSuccessful) {
                            Toast.makeText(
                                requireContext(),
                                "Verification email sent to ${currentUser.email}",
                                Toast.LENGTH_SHORT
                            ).show()
                            this.saveUserToDB(username, email)
                            if (currentUser.isEmailVerified) {
                                val intent = Intent(requireContext(), MainActivity2::class.java)
                                startActivity(intent)
                                requireActivity().finish()
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Please verify your email before continuing.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Log.e(tag, "sendEmailVerification", it.exception)
                            Toast.makeText(
                                requireContext(),
                                "Failed to send verification email.",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                } else {
                    Log.w(tag, "createUserWithEmail:failed", task.exception)
                    Toast.makeText(
                        requireContext(),
                        "Registration failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}