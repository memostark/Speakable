package com.guillermonegrete.tts.importtext

import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.db.BookFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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
        private val coverImage: ImageView = itemView.findViewById(R.id.cover_image)

        private val title: TextView = itemView.findViewById(R.id.book_title)
        private val lastRead: TextView = itemView.findViewById(R.id.last_read)

        private val progress: ProgressBar = itemView.findViewById(R.id.file_progressBar)
        private val optionsBtn: ImageButton = itemView.findViewById(R.id.menu_button)

        init{
            optionsBtn.setOnClickListener {
                viewModel.openItemMenu(adapterPosition)
            }
        }

        fun bind(file: BookFile){
            title.text = file.title
            progress.progress = file.percentageDone

            val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            lastRead.text = dateFormatter.format(file.lastRead.time)

            itemView.setOnClickListener { viewModel.openVisualizer(file) }

            val imageDir = File(viewModel.filesPath, file.folderPath)
            val imageFile = File(imageDir, "cover_thumbnail.png")

            Glide.with(itemView.context)
                .load(imageFile)
                .error(R.drawable.ic_broken_image_black_24dp)
                .into(coverImage)
        }
    }
}