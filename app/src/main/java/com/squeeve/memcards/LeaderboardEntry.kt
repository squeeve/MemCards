package com.squeeve.memcards

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Date

data class LeaderboardEntry (
    var level: Int = 0,
    var score: Long =  0,
    var timeStamp: Date? = null,
    var username: String = "",
    var userUid: String = "",
    var scoreId: String = ""
)

class LeaderboardManager(private val context: Context, private val level: Int) {
    private val tag: String = "LeaderboardManager"
    private var levelString: String = ""
    private val db: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val leaderRef: DatabaseReference = db.child("Leaderboard")
    private lateinit var levelRef: DatabaseReference

    fun getLeaderboardEntries(callback: (List<LeaderboardEntry>) -> Unit) {
        levelString = if (level > -1) {
            LEVELSTOSTRING.getValue(level)
        } else { // TODO: Show everything.
            "EASY"
        }
        levelRef = leaderRef.child(levelString)
        levelRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() && snapshot.childrenCount > 0) {
                    val entries = mutableListOf<LeaderboardEntry>()
                    for (entrySnapshot in snapshot.children) {
                        val entry = entrySnapshot.getValue(LeaderboardEntry::class.java)
                        entry?.let { entries.add(it) }
                    }
                    val e = entries.last()
                    Log.d(tag, "Example last entry: SCORE: ${e.score}. LEVEL: ${e.level}")
                    callback(entries)
                } else {
                    levelRef.setValue(emptyList<LeaderboardEntry>())
                    callback(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (error.code == DatabaseError.DATA_STALE) { // apparently node doesn't exist
                    Log.d(tag, "Instantiating the level $levelString in leaderboardRef.")
                    levelRef = leaderRef.child(levelString)
                    levelRef.setValue(emptyList<LeaderboardEntry>())
                } else {
                    Log.e(tag,
                        "Error getting leaderboard for $levelString: ${error.message}"
                    )
                    Toast.makeText(context,
                        "Unknown error getting leaderboard. ¯\\_(ツ)_/¯",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    // Function to check if user's score should be added
    private fun isUserScoreEligible(userEntry: LeaderboardEntry, leaderboardEntries: List<LeaderboardEntry>?): Boolean {
        // Logic to determine if user's score should be added
        // For example, if user's score is greater than the lowest score in leaderboard
        if (leaderboardEntries!!.size < 10) {
            return true
        }
        return leaderboardEntries.any { (it.score < userEntry.score) ||
                            ((it.score == userEntry.score) && (it.timeStamp?.compareTo(userEntry.timeStamp) ?: 1) > 0) }
                    // RE: Elvis operator: if it.timeStamp is null, return 1; else, check that the comparison's ret value > 0.
    }

    // Returns true if the user achieved leaderboard. Otherwise, pulls the leaderboard
    fun addUserScoreIfEligible(userEntry: LeaderboardEntry): Boolean {
        var shouldAdd = false
        getLeaderboardEntries { leaderboardEntries ->
            shouldAdd = isUserScoreEligible(userEntry, leaderboardEntries)
            if (shouldAdd) {
                var updatedList = (leaderboardEntries + userEntry)
                            .sortedByDescending { it.score }
                if (updatedList.size > 10) {
                    updatedList = updatedList.dropLast(updatedList.size - 10)
                }
                val updatedBoardMap = updatedList.map { lbe ->
                    mapOf(
                        "level" to lbe.level,
                        "score" to lbe.score,
                        "timeStamp" to lbe.timeStamp,
                        "username" to lbe.username,
                        "userUid" to lbe.userUid,
                        "scoreId" to lbe.scoreId
                    )
                }
                levelRef.setValue(updatedBoardMap)
            }
        }
        return shouldAdd
    }
}