package com.guillermonegrete.tts.importtext

import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.guillermonegrete.tts.R
import java.text.BreakIterator
import java.util.*

class VisualizerAdapter(private val pages: List<String>): RecyclerView.Adapter<VisualizerAdapter.PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val layout = LayoutInflater.from(parent.context).inflate(R.layout.visualizer_page_item, parent, false)
        return PageViewHolder(layout)
    }

    override fun getItemCount() = pages.size

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(pages[position])
    }

    class PageViewHolder(view: View): RecyclerView.ViewHolder(view){
        private val pageTextView: TextView = view.findViewById(R.id.page_text_view)

        fun bind(text: String){

            pageTextView.movementMethod = LinkMovementMethod.getInstance()
            pageTextView.setText(SpannableString(text), TextView.BufferType.SPANNABLE)
            setSpannables(pageTextView, text)

            println("Calling on bind")
        }

        // Based on: https://stackoverflow.com/questions/8612652/select-a-word-on-a-tap-in-textview-edittext
        private fun setSpannables(view: TextView, text: String){
            val spans = view.text as SpannableString
            val iterator = BreakIterator.getWordInstance(Locale.US)
            iterator.setText(text)
            var start = iterator.first()
            var end = iterator.next()
            while (end != BreakIterator.DONE) {
                val possibleWord = text.substring(start, end)
                if (Character.isLetterOrDigit(possibleWord.first())) {
                    val clickSpan = ImportedClickableSpan(possibleWord)
                    spans.setSpan(
                        clickSpan, start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                start = end
                end = iterator.next()
            }
        }
    }
}