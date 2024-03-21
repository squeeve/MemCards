package com.squeeve.memcards

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class ScoreBoxAdapter(private val dataList: List<LeaderboardEntry>) : RecyclerView.Adapter<UserBoxHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserBoxHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.user_scorebox_layout, parent, false)
        return UserBoxHolder(view)
    }

    override fun onBindViewHolder(holder: UserBoxHolder, position: Int) {
        val data = dataList[position]
        holder.usernameView.text = data.username
        holder.scoreView.text = data.score.toString()
        holder.dateView.text = data.timeStamp?.toString()
    }

    override fun getItemCount(): Int {
        return dataList.size
    }
}