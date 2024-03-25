package com.squeeve.memcards

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.squeeve.memcards.Game.Companion.LEVELSTOSTRING
import java.util.Date



data class LeaderboardEntry (
    var level: String = "",
    var score: Score = Score(),
    var username: String = "",
    var userUid: String = "",
    var scoreId: String = ""
)

// ScoreNode is basically a wrapper for LeaderboardEntry, which contains more information.
// The nodes are indexed by scoreId in Firebase.
data class ScoreNode(
    val rank: Int = 0,
    val entry: LeaderboardEntry? = null
)

@Suppress("DEPRECATION")
class LeaderboardActivity2 : BaseActivity() {
    private val tag = "LeaderboardActivity2"

    private lateinit var db: DatabaseReference
    private lateinit var userRef: DatabaseReference
    private lateinit var leaderRef: DatabaseReference
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var adapter: ScoreBoxAdapter
    private lateinit var you: User
    private lateinit var mp: MediaPlayer
    private var fromGame: Boolean = false


    override fun getLayoutId(): Int {
        return if (intent.hasExtra("scoreId")) {
            fromGame = true
            Log.d(tag, "getLayoutId: activity_leaderboard")
            R.layout.activity_leaderboard
        }
        else {
            Log.d(tag, "getLayoutId: activity_general_leaderboard")
            R.layout.activity_general_leaderboard
        }
    }

    private fun initializeEverything() {
        db = FirebaseDatabase.getInstance().reference
        userRef = db.child("Users").child(auth.currentUser!!.uid)
        leaderRef = db.child("Leaderboard")
    }

    private fun happyTune() {
        mp = MediaPlayer.create(this@LeaderboardActivity2, R.raw.success_trumpets)
        mp.start()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeEverything()
        var displayLevel: Int

        // use local storage to setup the youBox
        you = User(this, auth.currentUser!!.uid)
        if (fromGame) {
            Log.d(tag, "Activated by GameActivity; setting up youBox.")

            // get extras from intent
            val scoreVal = this.intent.getIntExtra("scoreVal", -1)
            val scoreTimestamp = this.intent.getLongExtra("scoreTimestamp", -1)
            displayLevel = this.intent.getIntExtra("scoreLevel", -1)
            val scored = Score(scoreVal, scoreTimestamp, displayLevel)

            if (!listOf(scoreVal, scoreTimestamp, displayLevel).contains(-1) && displayLevel in LEVELSTOSTRING.keys) {
                youBoxUpdate(you, scored)
                startLeaderboard(displayLevel)
            } else {
                Log.e(tag, "Activated by GameActivity; something happened that shouldn't have.")
                Log.e(tag, "displayLevel: $displayLevel")
                Toast.makeText(this, "Unknown error; sorry.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity2::class.java))
                finish()
            }

        } else {
            Log.d(tag, "Activated by user choice; setting up general leaderboard.")
            // set up button listeners
            findViewById<TextView>(R.id.easyBtn).setOnClickListener {
                displayLevel = Game.EASY
                startLeaderboard(displayLevel)
            }
            findViewById<TextView>(R.id.medBtn).setOnClickListener {
                displayLevel = Game.MED
                startLeaderboard(displayLevel)
            }
            findViewById<TextView>(R.id.hardBtn).setOnClickListener {
                displayLevel = Game.HARD
                startLeaderboard(displayLevel)
            }
        }
    }

    private fun startLeaderboard(level: Int) {
        val levelRef = leaderRef.child(LEVELSTOSTRING[level]!!)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        val layoutManager = LinearLayoutManager(this)
        val adapter = ScoreBoxAdapter(this, forProfilePage=false)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        val childEventListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                val data = dataSnapshot.getValue(ScoreNode::class.java)
                data?.let {
                    Log.d(tag, "onChildAdded (rank): ${it.rank}")
                    Log.d(tag, "onChildAdded (entry): ${it.entry}")
                    adapter.addScoreNode(it)
                    if (it.entry!!.userUid == auth.currentUser!!.uid) {
                        happyTune()
                        Toast.makeText(
                            this@LeaderboardActivity2,
                            "Congratulations on your new high score! It reached #${it.rank}!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
                val newData = dataSnapshot.getValue(ScoreNode::class.java)
                newData?.let { adapter.updateScoreNode(newData) }
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                val data = dataSnapshot.getValue(ScoreNode::class.java)
                data?.let { adapter.removeScoreNode(data) }
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
                val data = dataSnapshot.getValue(ScoreNode::class.java)
                data?.let { Log.w(tag, "Received onChildMoved @ ${data.rank}. Check for consistency.") }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e(tag, "Error: ", databaseError.toException())
            }
        }
        levelRef.addChildEventListener(childEventListener)
    }

    private fun youBoxUpdate(you: User, score: Score) {
        val nameText = findViewById<TextView>(R.id.userNameText)
        nameText.text = you.username
        val statsText = findViewById<TextView>(R.id.userStatsText)
        statsText.text = getString(
            R.string.you_scored_x_at_level_y,
            score.score.toString(),
            LEVELSTOSTRING[score.level]
        )
        val timeText = findViewById<TextView>(R.id.timestampText)
        timeText.text = getString(R.string.time, Date(score.timestamp))
        val pic = you.profilePicture.ifEmpty { R.drawable.poker_face }
        Glide.with(this).load(pic).into(findViewById(R.id.profile_image))
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mp.isInitialized)
            mp.release()
    }
}