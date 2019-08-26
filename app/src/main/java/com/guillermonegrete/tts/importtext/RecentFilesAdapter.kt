package com.guillermonegrete.tts.importtext

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.db.BookFile

class RecentFilesAdapter(
    private val files: List<BookFile>,
    private val viewModel: ImportTextViewModel
): RecyclerView.Adapter<RecentFilesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = LayoutInflater.from(parent.context).inflate(R.layout.recent_file_item, parent, false)
        return ViewHolder(viewModel, layout)
    }

    override fun getItemCount() = files.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file =  files[position]
        holder.bind(file)
    }

    class ViewHolder(
        private val viewModel: ImportTextViewModel,
        itemView: View
    ): RecyclerView.ViewHolder(itemView){
        private val title: TextView = itemView.findViewById(R.id.book_title)
        private val lastRead: TextView = itemView.findViewById(R.id.last_read)

        fun bind(file: BookFile){
            title.text = file.title
            lastRead.text = file.lastRead.toString()
            itemView.setOnClickListener { viewModel.openVisualizer(file) }
        }
    }
}