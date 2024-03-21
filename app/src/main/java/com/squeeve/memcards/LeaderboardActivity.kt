package com.squeeve.memcards

import android.content.Intent
import android.media.MediaPlayer
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private lateinit var leaderboardManager: LeaderboardManager
    private lateinit var db: DatabaseReference
    private lateinit var userRef: DatabaseReference
    private lateinit var leaderRef: DatabaseReference
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var levelStr: String
    private var username: String = ""
    private lateinit var adapter: ScoreBoxAdapter

    private fun initializeFirebase() {
        db = FirebaseDatabase.getInstance().reference
        userRef = db.child("Users").child(auth.currentUser!!.uid)
        leaderRef = db.child("Leaderboard")
        userRef.child("username").get().addOnSuccessListener { dataSnapshot ->
            username = dataSnapshot.getValue(String::class.java) ?: ""
            Log.d(tag, "Got username ${dataSnapshot.getValue()}")
        }.addOnFailureListener {exception ->
            Log.e("LeaderActivity::InitFB", "Error getting username: $exception")
        }
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

        // --- Drawer/Nav toolbar activity, which could be refactored if there's time
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.drawer_view)
        navigationView.setNavigationItemSelectedListener(this)
        val actionBar = findViewById<Toolbar>(R.id.topToolbar)
        setSupportActionBar(actionBar)

        val drawerToggler = ActionBarDrawerToggle(this, drawerLayout, actionBar, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(drawerToggler)
        drawerToggler.syncState()
        // --- Drawer/Nav toolbar activity, which could be refactored if there's time

        // --- Actual Activity code
        initializeFirebase()

        val intent = this.intent
        val userScore = intent.getLongExtra("thisScore", -1)
        val userLevel = intent.getIntExtra("thisLevel", -1)
        val scoreId = intent.getStringExtra("scoreId")
        if (userLevel > -1) {
            levelStr = LEVELSTOSTRING.getValue(userLevel)
        } else { // TODO: Figure out a way to print out everything.
            levelStr = "EASY"
        }
        leaderboardManager = LeaderboardManager(this, userLevel)

        Log.d(tag, "@ Leaderboard now:: score: $userScore. level: $userLevel")

        if (userScore > -1 && userLevel > -1) { // triggered by a game
            val scoreRef = userRef.child("Scores")
                .child(levelStr)
                .child(scoreId!!)
            scoreRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d(tag, "Made it into scoreRef's onDataChange")
                    val scoreData = snapshot.value as Map<String, Any>?
                    if (scoreData != null) {
                        val timestampLong: Long = scoreData["timestamp"] as Long
                        updateUserStatsView(userScore, Date(timestampLong))
                        val userEntry = LeaderboardEntry(
                            level = userLevel,
                            score = userScore,
                            timeStamp = Date(timestampLong),
                            username = username,
                            userUid = auth.uid.toString(),
                            scoreId = scoreId
                        )
                        val madeIt = leaderboardManager.addUserScoreIfEligible(userEntry)
                        if (madeIt) {
                            Log.d(tag, "Made it to leaderboard! Should hear sounds...")
                            val mediaPlayer = MediaPlayer.create(this@LeaderboardActivity, R.raw.success_trumpets)
                            mediaPlayer.start()
                            mediaPlayer.setOnCompletionListener { mediaPlayer.release() }
                            Toast.makeText(
                                this@LeaderboardActivity,
                                "Congratulations! Your score made it on the leaderboard!",
                                Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e(tag, "No timestamp found for $scoreId")
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e(
                        tag,
                        "Error getting scoreId $scoreId for ${auth.currentUser}: ${error.message}"
                    )
                }
            })
        } else { // if started by user nav, hide score view.
            val userStatsView = findViewById<TextView>(R.id.userStatsText)
            userStatsView.visibility = TextView.GONE
        }

        // Create the leaderboard (independent from showing user score)
        leaderboardManager.getLeaderboardEntries { entries ->
            val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
            recyclerView.layoutManager = LinearLayoutManager(this)
            adapter = ScoreBoxAdapter(entries)
            recyclerView.adapter = adapter
        }
    }

    private fun updateUserStatsView(userScore: Long, timestamp: Date?) {
        val userStatsView = findViewById<TextView>(R.id.userStatsText)
        val scoreText = "$levelStr\nSCORE: $userScore"
        var tStamp: String? = null
        if (timestamp != null) {
            tStamp = "\n\n$timestamp"
        }
        userStatsView.text = "$scoreText$tStamp"
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