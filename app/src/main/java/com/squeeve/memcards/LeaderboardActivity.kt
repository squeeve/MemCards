package com.squeeve.memcards

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Date
import kotlin.random.Random

class LeaderboardActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private val tag = "LeaderboardActivity"
    private lateinit var db: DatabaseReference
    private lateinit var userRef: DatabaseReference
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private fun initializeFirebase() {
        db = FirebaseDatabase.getInstance().reference
    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        Toast.makeText(this@LeaderboardActivity, "Thank you, come again!", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this@LeaderboardActivity, LoginRegister::class.java))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.drawer_view)
        navigationView.setNavigationItemSelectedListener(this)
        val actionBar = findViewById<Toolbar>(R.id.topToolbar)
        setSupportActionBar(actionBar)

        val drawerToggler = ActionBarDrawerToggle(this, drawerLayout, actionBar, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(drawerToggler)
        drawerToggler.syncState()

        initializeFirebase()

        val intent = this.intent
        val userScore = intent.getLongExtra("thisScore", -1)
        val userLevel = intent.getIntExtra("thisLevel", -1)
        val scoreId = intent.getStringExtra("scoreId")

        Log.d(tag, "@ Leaderboard now:: score: $userScore. level: $userLevel")

        if (userScore > -1 && userLevel > -1) { // triggered by a game
            val scoreRef = db.child("Users").child(auth.currentUser!!.uid)
                .child("Scores").child(LEVELSTOSTRING.getValue(userLevel))
                .child(scoreId!!)
            scoreRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val scoreData = snapshot.value as Map<String, Any>?
                    if (scoreData != null) {
                        val timestampLong: Long = scoreData["timestamp"] as Long
                        updateUserStatsView(userLevel, userScore, Date(timestampLong))
                    } else {
                        Log.e(tag, "No timestamp found for $scoreId")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(
                        tag,
                        "Error retrieving scoreId $scoreId for ${auth.currentUser}: ${error.message}"
                    )
                }
            })
        } else { // selected by user
            val userStatsView = findViewById<TextView>(R.id.userStatsText)
            userStatsView.visibility = TextView.GONE
            Toast.makeText(this, "Leaderboard list goes here", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUserStatsView(userLevel: Int, userScore: Long, timestamp: Date?) {
        val userStatsView = findViewById<TextView>(R.id.userStatsText)
        val scoreText = "${LEVELSTOSTRING.getValue(userLevel)}\nSCORE: ${userScore}"
        var tstamp: String? = null
        if (timestamp != null) {
            tstamp = "\n\n$timestamp"
        }
        userStatsView.text = "$scoreText$tstamp"
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_game -> {
                val level = LEVELS.entries.elementAt(Random.nextInt(LEVELS.size)).value
                Toast.makeText(
                    this@LeaderboardActivity,
                    "Starting game at ${LEVELSTOSTRING.getValue(level)}",
                    Toast.LENGTH_SHORT).show()
                val startGame = Intent(this, GameActivity::class.java)
                startGame.putExtra("level", level)
                startActivity(startGame)
                finish()
            }
            R.id.menu_leaderboard -> {
                val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            R.id.menu_profile -> Toast.makeText(
                this@LeaderboardActivity,
                "Go to profile",
                Toast.LENGTH_SHORT).show()
            R.id.menu_signout -> {
                logoutUser()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}