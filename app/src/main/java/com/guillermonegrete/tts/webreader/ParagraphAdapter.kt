package com.guillermonegrete.tts.webreader

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.databinding.ParagraphExpandedItemBinding
import com.guillermonegrete.tts.databinding.ParagraphItemBinding
import com.guillermonegrete.tts.db.Words

class ParagraphAdapter(
    val items: List<Words>,
    val viewModel: WebReaderViewModel
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var expandedItemPos = -1

    var isLoading = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if(viewType == R.layout.paragraph_expanded_item) ExpandedViewHolder(ParagraphExpandedItemBinding.inflate(inflater, parent, false))
            else ViewHolder(ParagraphItemBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder){
            is ViewHolder -> holder.bind(items[position])
            is ExpandedViewHolder -> holder.bind(items[position])
        }
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) = if(expandedItemPos == position) R.layout.paragraph_expanded_item else R.layout.paragraph_item

    fun updateExpanded(){
        notifyItemChanged(expandedItemPos)
    }

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
            }
        }

        fun bind(item: Words){
            binding.paragraph.text = item.word
        }

    }

    inner class ExpandedViewHolder(val binding: ParagraphExpandedItemBinding): RecyclerView.ViewHolder(binding.root){

        val noTranslationText: CharSequence = itemView.context.getText(R.string.paragraph_not_translated)

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
                    viewModel.translateParagraph(adapterPosition)
                }
            }
        }

        fun bind(item: Words){
            binding.paragraph.text = item.word
            binding.loadingParagraph.isVisible = isLoading
            binding.translate.isVisible = !isLoading
            binding.translatedParagraph.text = if(item.definition.isNotBlank()) item.definition else noTranslationText
        }

    }
}