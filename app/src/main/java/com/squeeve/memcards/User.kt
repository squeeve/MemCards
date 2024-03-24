package com.squeeve.memcards

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat.getString
import java.io.Serializable


data class Score(val score: Int, val timestamp: Long, val level: Int) : Serializable, Comparable<Score> {
    override fun compareTo(other: Score): Int {
        // sort by score, then by timestamp
        // return negative if this < other, 0 if equal, positive if this > other
        return if (this.score - other.score != 0) {
            this.score - other.score
        } else {
            (this.timestamp - other.timestamp).toInt()
        }
    }
}

class User(
    private val context: Context,
    var username: String = "",
    var email: String = "",
    var profilePicture: String = "",
    var scoreHistory: MutableList<Score> = mutableListOf(),
    private var uid: String? = null
) {
    private lateinit var userPrefFile: String
    private lateinit var gameStateFile: String

    init {
        if (this.scoreHistory.isEmpty()) {
            this.scoreHistory = mutableListOf()
        }
        if (uid != null) {
            userPrefFile = getString(context, R.string.app_domain) + ".$uid"
            gameStateFile = userPrefFile + "gameState.json"
        }
    }

    fun getScoresForLevel(level: Int): List<Score> {
        return this.scoreHistory.filter { it.level == level }
    }

    constructor(context: Context, uid: String): this(context, "","","", mutableListOf(),uid) {
        userPrefFile = getString(context, R.string.app_domain) + ".$uid"
        gameStateFile = getString(context, R.string.app_domain) + ".$uid.gameState.json"
        val fh = FileHelper(context)
        if (!fh.getInternalFile(userPrefFile).exists()) {
            throw Exception("User pref file not found")
        }
        val currUser = fh.readFromFile<Map<String,Any>?>(userPrefFile)
        if (currUser != null) {
            for ((key, value) in currUser) {
                when (key) {
                    "username" -> username = value.toString()
                    "email" -> email = value.toString()
                    "profilePicture" -> profilePicture = value.toString()
                    "scoreHistory" -> {
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
                            scoreHistory = mutableListOf()
                        }
                    }
                }
            }
        } else {
            throw Exception("User pref file was unuseable.")
        }
    }

    fun toMap(): Map<String, Any?> {
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
    }

}