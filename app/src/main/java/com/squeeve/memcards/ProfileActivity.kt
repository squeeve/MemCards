@file:Suppress("CanBeVal")

package com.squeeve.memcards

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.squeeve.memcards.Game.Companion.LEVELSTOSTRING

class ProfileActivity : BaseActivity() {

    override fun getLayoutId(): Int {
        return R.layout.activity_profile
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = User(this, currentUser!!.uid)

        val imageView = findViewById<ImageView>(R.id.profile_image)
        if (user.profilePicture.isBlank()) {
            Glide.with(this)
                .load(R.drawable.poker_face)
                .circleCrop()
                .into(imageView)
        } else {
            Glide.with(this)
                .load(user.profilePicture)
                .circleCrop()
                .into(imageView)
        }
        val name = findViewById<TextView>(R.id.username)
        name.text = user.username

        // Set up recyclerView to see user's scores
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)

        // Android Studio keeps saying this is never modified but IT CLEARLY IS, like, in the next op.
        // DO NOT CHANGE TO VAL. Eff. You, Android Studio.
        var scoreSheet = mutableListOf<LeaderboardEntry>()
        for (score in user.scoreHistory) {
            scoreSheet.add(LeaderboardEntry(
                level = LEVELSTOSTRING[score.level]!!,
                score = score,
                username = user.username,
                userUid = user.uid ?: ""
            ))
        }
        Log.d("ProfileActivity", "scoreSheet len: ${scoreSheet.size}")
        Log.d("ProfileActivity", "user.scoreHistory len: ${user.scoreHistory.size}")
        val adapter = ScoreBoxAdapter(this, entries=scoreSheet, forProfilePage=true)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }
}