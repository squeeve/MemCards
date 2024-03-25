package com.squeeve.memcards

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ScoreBoxHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
    val profileImageView: de.hdodenhof.circleimageview.CircleImageView = itemView.findViewById(R.id.profileImage)
    val usernameView: TextView = itemView.findViewById(R.id.usernameText)
    val scoreView: TextView = itemView.findViewById(R.id.scoreText)
    val dateView: TextView = itemView.findViewById(R.id.timeStamp)
}