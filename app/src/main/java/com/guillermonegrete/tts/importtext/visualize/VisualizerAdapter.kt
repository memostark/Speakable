package com.guillermonegrete.tts.importtext.visualize

import android.content.Intent
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.textprocessing.ProcessTextActivity
import java.text.BreakIterator
import java.util.*

class VisualizerAdapter(private val pages: List<CharSequence>): RecyclerView.Adapter<VisualizerAdapter.PageViewHolder>() {

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

        private val actionModeCallback = object : ActionMode.Callback{
            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                when(item.itemId){
                    R.id.show_process_text_activity -> {
                        if (pageTextView.isFocused) {
                            val selStart = pageTextView.selectionStart
                            val selEnd = pageTextView.selectionEnd

                            // We need to make sure start and end are within the text length
                            val min = 0.coerceAtLeast(selStart.coerceAtMost(selEnd))
                            val max = 0.coerceAtLeast(selStart.coerceAtLeast(selEnd))

                            val selectedText = pageTextView.text.subSequence(min, max)
                            showTextDialog(selectedText)
                        }

                        mode.finish()
                        return true
                    }
                }
                return false
            }

            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                menu.clear()
                val inflater: MenuInflater = mode.menuInflater
                menu.add(Menu.NONE, android.R.id.copy, Menu.NONE, "Copy")
                inflater.inflate(R.menu.menu_context_text_visualizer, menu)
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode?) {}
        }

        init {
            pageTextView.customSelectionActionModeCallback = actionModeCallback
            // Color taken from member variable mHighlightColor from TextView class.
            pageTextView.highlightColor = 0x6633B5E5
            pageTextView.movementMethod = LinkMovementMethod.getInstance()
        }

        fun bind(text: CharSequence){
            pageTextView.movementMethod = LinkMovementMethod.getInstance()

            pageTextView.setText(text, TextView.BufferType.SPANNABLE)
            setSpannables(pageTextView)
        }

        // Based on: https://stackoverflow.com/questions/8612652/select-a-word-on-a-tap-in-textview-edittext
        private fun setSpannables(view: TextView){
            val spans = view.text as SpannableString
            val iterator = BreakIterator.getWordInstance(Locale.US)
            iterator.setText(spans.toString())
            var start = iterator.first()
            var end = iterator.next()
            while (end != BreakIterator.DONE) {
                val possibleWord = spans.substring(start, end)
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

        private fun showTextDialog(text: CharSequence){
            val wiktionaryIntent = Intent(itemView.context, ProcessTextActivity::class.java)
            wiktionaryIntent.action = ProcessTextActivity.NO_SERVICE
            wiktionaryIntent.putExtra("android.intent.extra.PROCESS_TEXT", text)
            itemView.context.startActivity(wiktionaryIntent)
        }
    }
}