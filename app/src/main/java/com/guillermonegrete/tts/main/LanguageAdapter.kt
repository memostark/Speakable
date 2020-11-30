package com.guillermonegrete.tts.main

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.guillermonegrete.tts.databinding.DialogItemBinding

/**
 * [RecyclerView.Adapter] that can display a [String].
 */
class LanguageAdapter(
    private val values: List<String>
) : RecyclerView.Adapter<LanguageAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DialogItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(private val binding: DialogItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: String){
            binding.text1.text = item
        }
    }
}