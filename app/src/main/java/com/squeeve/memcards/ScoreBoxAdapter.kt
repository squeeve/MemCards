package com.squeeve.memcards

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.squeeve.memcards.Game.Companion.LEVELSTOSTRING
import java.util.Date

class ScoreBoxAdapter(
    private val context: Context,
    private var entries: MutableList<LeaderboardEntry> = mutableListOf(),
    private val forProfilePage: Boolean=false
) : RecyclerView.Adapter<ScoreBoxHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreBoxHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.user_scorebox_layout, parent, false)
        return ScoreBoxHolder(view)
    }

    override fun onBindViewHolder(holder: ScoreBoxHolder, position: Int) {
        val data = entries[position]
        val storageRef = FirebaseStorage.getInstance().reference
        val profileRef = storageRef.child("images/${data.userUid}")

        if (forProfilePage) {
            holder.usernameView.isVisible = false
            holder.profileImageView.isVisible = false
        } else {
            holder.usernameView.text = data.username
            profileRef.downloadUrl.addOnSuccessListener { url ->
                Glide.with(holder.profileImageView.context).load(url.toString())
                    .into(holder.profileImageView)
            }.addOnFailureListener {
                val defaultImg = ContextCompat.getDrawable(context, R.drawable.poker_face)
                Glide.with(holder.profileImageView.context).load(defaultImg)
                    .into(holder.profileImageView)
            }
        }
        holder.scoreView.text = context.getString(R.string.you_scored_x_at_level_y,
                    data.score.score.toString(),
                    LEVELSTOSTRING[data.score.level])

        holder.dateView.text = Date(data.score.timestamp).toString()
    }

    override fun getItemCount(): Int {
        return entries.size
    }

    fun addScoreNode(scoreNode: ScoreNode): Boolean {
        // adds a score if applicable, and calls notifyItemInserted or notifyItemRemoved as
        // necessary. Returns true if the score was added, false otherwise.
        if (scoreNode?.entry == null) {
            Log.d("ScoreAdapt", "addScoreNode: Entry or scoreNode was null")
            return false
        }

        // if empty list
        if (entries.isEmpty()) {
            entries.add(scoreNode.entry)
            notifyItemInserted(0)
            return false
        }

        // if not empty
        var incrementRanksAfter = -1
        for (i in 0 until entries.size) {
            if (scoreNode.entry.score > entries[i].score) {
                entries.add(i, scoreNode.entry)
                notifyItemInserted(i)
                return true
            }
        }
        if (entries.size > 10) {
            entries.removeAt(entries.size-1)
            notifyItemRemoved(entries.size-1)
        }
        return false
    }

    fun updateScoreNode(scoreNode: ScoreNode) {
        if (scoreNode?.entry == null) {
            Log.d("ScoreAdapt", "updateScoreNode: Entry or scoreNode was null")
            return
        }
        entries.set(scoreNode.rank, scoreNode.entry)
        Log.d("ScoreAdapt", "updateScoreNode: updated @ ${scoreNode.rank}")
        notifyItemChanged(scoreNode.rank)
    }

    fun removeScoreNode(scoreNode: ScoreNode) {
        if (scoreNode?.entry == null) {
            Log.d("ScoreAdapt", "removeScoreNode: Entry or scoreNode was null")
            return
        }
        entries.removeAt(scoreNode.rank)
        Log.d("ScoreAdapt", "removeScoreNode: removed @ ${scoreNode.rank}")
        notifyItemRemoved(scoreNode.rank)
    }
}