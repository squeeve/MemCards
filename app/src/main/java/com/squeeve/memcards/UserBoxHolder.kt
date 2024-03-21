package com.squeeve.memcards

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserBoxHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
    val usernameView: TextView = itemView.findViewById(R.id.username)
    val scoreView: TextView = itemView.findViewById(R.id.score)
    val dateView: TextView = itemView.findViewById(R.id.timeStamp)
}