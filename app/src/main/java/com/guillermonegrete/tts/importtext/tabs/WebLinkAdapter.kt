package com.guillermonegrete.tts.importtext.tabs

import android.text.format.DateUtils
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.navigation.findNavController
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.databinding.FragmentWebLinksBinding
import com.guillermonegrete.tts.db.WebLink
import com.guillermonegrete.tts.importtext.ImportTextFragmentDirections
import java.util.*

/**
 * [RecyclerView.Adapter] that can display a [WebLink].
 */
class WebLinkAdapter(
    private val values: List<WebLink>,
    private val onDeleteCallback: (WebLink) -> Unit
) : RecyclerView.Adapter<WebLinkAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(
            FragmentWebLinksBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(values[position])
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(private val binding: FragmentWebLinksBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val contentView: TextView = binding.content

        fun bind(link: WebLink){
            contentView.text = link.url
            contentView.setOnClickListener {
                val action = ImportTextFragmentDirections.toWebReaderFragment(link.url)
                itemView.findNavController().navigate(action)
            }

            contentView.setOnLongClickListener {
                AlertDialog.Builder(itemView.context).setTitle(R.string.delete_item)
                    .setPositiveButton(android.R.string.ok) { _, _ -> onDeleteCallback(link) }
                    .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
                    .show()
                true
            }

            val formattedDate = DateUtils.getRelativeTimeSpanString(link.lastRead.timeInMillis, Calendar.getInstance().timeInMillis, DateUtils.MINUTE_IN_MILLIS)
            binding.lastOpened.text = formattedDate
        }

        override fun toString(): String {
            return super.toString() + " '" + contentView.text + "'"
        }
    }

}