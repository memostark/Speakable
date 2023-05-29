package com.guillermonegrete.tts.importtext

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.databinding.RecentFileItemBinding
import com.guillermonegrete.tts.db.BookFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RecentFilesAdapter(
    val filesFolder: File,
    val onClick: (BookFile) -> Unit,
    val onMenuButtonClick: (Int) -> Unit,
): ListAdapter<BookFile, RecentFilesAdapter.ViewHolder>(BookFileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = LayoutInflater.from(parent.context).inflate(R.layout.recent_file_item, parent, false)
        return ViewHolder(layout)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file =  getItem(position)
        holder.bind(file)
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){

        private val binding = RecentFileItemBinding.bind(itemView)

        init{
            binding.menuButton.setOnClickListener {
                onMenuButtonClick(adapterPosition)
            }
        }

        fun bind(file: BookFile){
            itemView.setOnClickListener { onClick(file) }

            with(binding){
                bookTitle.text = file.title
                fileProgressBar.progress = file.percentageDone

                val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                lastRead.text = dateFormatter.format(file.lastRead.time)

                val imageDir = File(filesFolder, file.folderPath)
                val imageFile = File(imageDir, "cover_thumbnail.png")

                Glide.with(itemView.context)
                    .load(imageFile)
                    .error(R.drawable.ic_broken_image_black_24dp)
                    .into(coverImage)
            }
        }
    }
}

class BookFileDiffCallback: DiffUtil.ItemCallback<BookFile>(){

    override fun areItemsTheSame(oldItem: BookFile, newItem: BookFile): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: BookFile, newItem: BookFile): Boolean {
        return oldItem == newItem
    }
}