package com.squeeve.memcards

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat.getString
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.io.Serializable


data class Score(
    val score: Int = -1,
    val timestamp: Long = System.currentTimeMillis(),
    val level: Int = 0
) : Serializable, Comparable<Score> {

    override fun compareTo(other: Score): Int {
        // sort by score, then by timestamp
        // return negative if this < other, 0 if equal, positive if this > other
        return if (this.score - other.score != 0) {
            this.score - other.score
        } else {
            // if this is the newer score, it should compare smaller than the other
            (this.timestamp - other.timestamp).toInt() * -1
        }
    }

    fun toJson(): String {
        return """{
            "score": $score,
            "timestamp": $timestamp,
            "level": $level
        }""".trimIndent()
    }
}

class User(
    private val context: Context,
    var username: String = "",
    var email: String = "",
    var profilePicture: String = "",
    var scoreHistory: MutableList<Score> = mutableListOf(),
    var uid: String? = null
) {
    private lateinit var userPrefFile: String
    private lateinit var gameStateFile: String
    private var userRef: DatabaseReference?

    init {
        if (this.scoreHistory.isEmpty()) {
            this.scoreHistory = mutableListOf()
        }
        if (uid != null) {
            userPrefFile = getString(context, R.string.app_domain) + ".$uid"
            gameStateFile = userPrefFile + "gameState.json"
        }
        userRef = if (uid != null) {
            FirebaseDatabase.getInstance().reference.child("Users").child(uid!!)
        } else { null }
    }

    fun getScoresForLevel(level: Int): List<Score> {
        return this.scoreHistory.filter { it.level == level }
    }

    override fun toString(): String {
        return """{
             \"username\": \"$username\",
             \"email\": \"$email\",
             \"profilePicture\": \"$profilePicture\",
             \"scoreHistory\": ${scoreHistory.map { it.toJson() }} 
        }""".trimIndent()
    }

    constructor(context: Context, uid: String): this(context, "","","", mutableListOf(),uid) {
        userPrefFile = getString(context, R.string.app_domain) + ".$uid"
        gameStateFile = getString(context, R.string.app_domain) + ".$uid.gameState.json"
        val fh = FileHelper(context)
        if (!fh.getInternalFile(userPrefFile).exists()) {
            // TODO: This actually happens when a user starts using a new device.
            // It's possible to use the UID to rebuild the local file.
            throw Exception("User pref file does not exist.")
        }
        val currUser = fh.readFromFile<Map<String,Any>?>(userPrefFile)
        if (currUser != null) {
            for ((key, value) in currUser) {
                when (key) {
                    "username" -> username = value.toString()
                    "email" -> email = value.toString()
                    "profilePicture" -> profilePicture = value.toString()
                    "scoreHistory" -> {
                        @Suppress("UNCHECKED_CAST")
                        val scoreSheet = value as? List<Map<String,Any?>>
                        Log.d("userHist", "Read scoreHistory: $scoreSheet")
                        if (scoreSheet != null) {
                            scoreHistory = scoreSheet.map {
                                Score(
                                    (it["score"] as Double).toInt(),
                                    (it["timestamp"] as Double).toLong(),
                                    (it["level"] as Double).toInt()
                                )
                            }.toMutableList()
                            Log.d("userHist", "Saved scoreHistory: $scoreHistory")
                        } else {
                            Log.d("userHist", "scoreSheet was null")
                            scoreHistory = mutableListOf()
                        }
                    }
                }
            }
        } else {
            throw Exception("User pref file was unusable.")
        }
        userRef = try {
            FirebaseDatabase.getInstance().reference.child("Users").child(uid)
        } catch (e: Exception) {
            Log.e("User", "Failed to get userRef: $e")
            null
        }
    }

    private fun toMap(): Map<String, Any?> {
        val r= mapOf(
            "username" to this.username,
            "email" to this.email,
            "profilePicture" to this.profilePicture,
            "scoreHistory" to this.scoreHistory.map { score ->
                mapOf(
                    "score" to score.score,
                    "timestamp" to score.timestamp,
                    "level" to score.level
                )
            }
        )
        Log.d("userHist", "Written scoreHistory: ${this.scoreHistory}")
        return r
    }

    fun writeUserPrefs(overwrite: Boolean = true) {
        val fh = FileHelper(context)
        fh.saveToFile(this.toMap(), userPrefFile, overwrite)
        // write/update Firebase
        if (userRef != null) {
            userRef!!.updateChildren(this.toMap())
        }
    }

}