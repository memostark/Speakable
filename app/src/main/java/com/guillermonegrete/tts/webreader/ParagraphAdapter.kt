package com.guillermonegrete.tts.webreader

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.databinding.ParagraphItemBinding

class ParagraphAdapter(val items: List<String>): RecyclerView.Adapter<ParagraphAdapter.ViewHolder>() {

    private var expandedItemPos = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ParagraphItemBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(val binding: ParagraphItemBinding): RecyclerView.ViewHolder(binding.root){

        init {
            with(binding){

                toggleParagraph.setOnClickListener {
                    val previousExpandedPos = expandedItemPos
                    val isExpanded = adapterPosition == expandedItemPos
                    expandedItemPos = if(isExpanded) -1 else adapterPosition
                    notifyItemChanged(previousExpandedPos)
                    notifyItemChanged(adapterPosition)
                }

                translate.setOnClickListener {
                    translatedParagraph.text = "Text is now translated"
                }
            }
        }

        fun bind(item: String){
            binding.paragraph.text = item

            val isExpanded = adapterPosition == expandedItemPos
            binding.translate.isInvisible = !isExpanded
            binding.translatedParagraph.isVisible = isExpanded
            binding.toggleParagraph.setIconResource(if(isExpanded) R.drawable.ic_baseline_arrow_drop_down_24 else R.drawable.ic_baseline_arrow_right_24)
        }

    }
}