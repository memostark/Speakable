package com.guillermonegrete.tts.importtext.tabs

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
import com.guillermonegrete.tts.databinding.FragmentWebLinksBinding
import com.guillermonegrete.tts.db.WebLink
import com.guillermonegrete.tts.importtext.ImportTextFragmentDirections

/**
 * [RecyclerView.Adapter] that can display a [WebLink].
 */
class WebLinkAdapter(
    private val values: List<WebLink>
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

    class ViewHolder(binding: FragmentWebLinksBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val contentView: TextView = binding.content

        fun bind(link: WebLink){
            contentView.text = link.url
            contentView.setOnClickListener {
                val action = ImportTextFragmentDirections.toWebReaderFragment(link.url)
                itemView.findNavController().navigate(action)
            }
        }

        override fun toString(): String {
            return super.toString() + " '" + contentView.text + "'"
        }
    }

}