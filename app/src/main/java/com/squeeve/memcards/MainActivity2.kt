package com.squeeve.memcards

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.random.Random


/* This activity actually uses the original activity_main layout file,
* as the old MainActivity.kt is now the account-verify activity, using the
* splash layout. */
class MainActivity2 : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val tag: String = "MainActivity2"
    private lateinit var auth: FirebaseAuth
    private lateinit var dbReference: DatabaseReference

    private fun gotoGame(level: Int) {
        val startGame = Intent(this, GameActivity::class.java)
        startGame.putExtra("level", level)
        startActivity(startGame)
    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this@MainActivity2, LoginRegister::class.java))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        dbReference = FirebaseDatabase.getInstance().reference
        val currentUser = auth.currentUser!!

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.drawer_view)
        navigationView.setNavigationItemSelectedListener(this)
        val actionBar = findViewById<Toolbar>(R.id.topToolbar)
        setSupportActionBar(actionBar)

        val drawerToggler = ActionBarDrawerToggle(this, drawerLayout, actionBar, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(drawerToggler)
        drawerToggler.syncState()

        // change username in header
        Log.d(tag, "NavigationView's header count: ${navigationView.headerCount}")
        val headerView = navigationView.getHeaderView(0)
        val nameTextView = headerView.findViewById<TextView>(R.id.profile_name)
        currentUser.let { user ->
            val userId = currentUser.uid
            val userRef = dbReference.child("Users").child(userId)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnap: DataSnapshot) {
                    if (dataSnap.exists()) {
                        val username = dataSnap.child("username").getValue(String::class.java)
                        Log.d(tag, "Username for ${user.email}: ${username}")
                        nameTextView.text = username
                    } else {
                        Log.e(tag, "User ID $userId doesn't match any db entries.")
                        Toast.makeText(
                            this@MainActivity2,
                            "Please register to play!",
                            Toast.LENGTH_SHORT).show()
                        logoutUser()
                    }
                }
                override fun onCancelled(dbError: DatabaseError) {
                    Log.e(tag, "userRef:: Database error: ${dbError}")
                    Toast.makeText(this@MainActivity2,
                        "Couldn't access your profile. Please register.",
                        Toast.LENGTH_SHORT).show()
                }
            })
        }

        val easyButton = findViewById<Button>(R.id.easyButton)
        val medButton = findViewById<Button>(R.id.mediumButton)
        val hardButton = findViewById<Button>(R.id.hardButton)

        easyButton.setOnClickListener { gotoGame(EASY) }
        medButton.setOnClickListener { gotoGame(MED) }
        hardButton.setOnClickListener { gotoGame(HARD) }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_game -> {
                val level = LEVELS.entries.elementAt(Random.nextInt(LEVELS.size))
                Toast.makeText(
                    this@MainActivity2,
                    "Starting game at ${level.key}",
                    Toast.LENGTH_SHORT).show()
                gotoGame(level.value)
            }
            R.id.menu_leaderboard -> Toast.makeText(
                            this@MainActivity2,
                            "Go to leaderboard",
                            Toast.LENGTH_SHORT).show()
            R.id.menu_profile -> Toast.makeText(
                            this@MainActivity2,
                            "Go to profile",
                            Toast.LENGTH_SHORT).show()
            R.id.menu_signout -> {
                logoutUser()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}