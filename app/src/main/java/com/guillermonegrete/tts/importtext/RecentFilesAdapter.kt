package com.guillermonegrete.tts.importtext

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.databinding.RecentFileItemBinding
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
        private val binding = RecentFileItemBinding.bind(itemView)

        init{
            binding.menuButton.setOnClickListener {
                viewModel.openItemMenu(adapterPosition)
            }
        }

        fun bind(file: BookFile){
            itemView.setOnClickListener { viewModel.openVisualizer(file) }

            with(binding){
                bookTitle.text = file.title
                fileProgressBar.progress = file.percentageDone

                val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                lastRead.text = dateFormatter.format(file.lastRead.time)

                val imageDir = File(viewModel.filesPath, file.folderPath)
                val imageFile = File(imageDir, "cover_thumbnail.png")

                Glide.with(itemView.context)
                    .load(imageFile)
                    .error(R.drawable.ic_broken_image_black_24dp)
                    .into(coverImage)
            }
        }
    }
}