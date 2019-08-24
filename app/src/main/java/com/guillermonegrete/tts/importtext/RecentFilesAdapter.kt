package com.guillermonegrete.tts.importtext

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.db.BookFile

class RecentFilesAdapter(private val files: List<BookFile>): RecyclerView.Adapter<RecentFilesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = LayoutInflater.from(parent.context).inflate(R.layout.recent_file_item, parent, false)
        return ViewHolder(layout)
    }

    override fun getItemCount() = files.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file =  files[position]
        holder.title.text = file.title
        holder.lastRead.text = file.lastRead.toString()
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val title: TextView = itemView.findViewById(R.id.book_title)
        val lastRead: TextView = itemView.findViewById(R.id.last_read)
    }
}