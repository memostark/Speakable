package com.guillermonegrete.tts.importtext.visualize

import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.common.models.Span
import com.guillermonegrete.tts.utils.dpToPixel
import java.text.BreakIterator
import java.util.*

class VisualizerAdapter(
    private val pages: List<CharSequence>,
    private val showTextDialog: (CharSequence) -> Unit,
    private val measuringPage: Boolean = false
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var hasBottomSheet = false

    var isPageSplit = false

    private var pageMarginsSize = 0
    private var lineSpacingExtra = 0f

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        with(recyclerView.context){
            lineSpacingExtra = resources.getDimension(R.dimen.visualize_page_text_line_spacing_extra)
            pageMarginsSize = dpToPixel(40)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layout = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return when(viewType){
            R.layout.visualizer_split_page_item -> SplitPageViewHolder(layout)
            else -> if(measuringPage) ViewHolder(layout) else PageViewHolder(layout)
        }
    }

    override fun getItemCount() = pages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder){
            is PageViewHolder -> holder.bind(pages[position])
            is SplitPageViewHolder -> holder.bind(pages[position])
        }
    }

    /**
     * Used for updating [ViewHolder] while providing additional data with [payloads].
     */
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if(payloads.isEmpty()){
            onBindViewHolder(holder, position)
        }else{
            if(holder is SplitPageViewHolder) {
                val payload = payloads.first()
                if(payload is Span) {
                    holder.setHighlightedText(payload.start, payload.end)
                } else {
                    holder.updateLayoutParams(payloads.first() as Boolean)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        if(hasBottomSheet && !measuringPage) return R.layout.visualizer_split_page_item
        return R.layout.visualizer_page_item
    }

    open inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        protected val pageTextView: TextView = view.findViewById(R.id.page_text_view)

        private val actionModeCallback = PageActionModeCallback(pageTextView, showTextDialog)

        init {
            pageTextView.customSelectionActionModeCallback = actionModeCallback
            // Color taken from member variable mHighlightColor from TextView class.
            pageTextView.highlightColor = 0x6633B5E5
            pageTextView.movementMethod = LinkMovementMethod.getInstance()
        }

        // Based on: https://stackoverflow.com/questions/8612652/select-a-word-on-a-tap-in-textview-edittext
        private fun setSpannables(view: TextView){
            val spans = view.text as SpannableString
//            BreakIterator.
            val iterator = BreakIterator.getWordInstance(Locale.US)
            iterator.setText(spans.toString())
            var start = iterator.first()
            var end = iterator.next()
            while (end != BreakIterator.DONE) {
                val possibleWord = spans.substring(start, end)
                if (Character.isLetterOrDigit(possibleWord.first())) {

                    val clickSpan = object: ImportedClickableSpan() {
                        override fun onClick(widget: View) {
                            super.onClick(widget)
                            showTextDialog(possibleWord)
                        }
                    }

                    spans.setSpan(
                        clickSpan, start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                start = end
                end = iterator.next()
            }
        }

        protected fun setPageText(text: CharSequence){
            pageTextView.movementMethod = LinkMovementMethod.getInstance()

            pageTextView.setText(text, TextView.BufferType.SPANNABLE)
            setSpannables(pageTextView)
        }

    }

    inner class PageViewHolder(view: View): ViewHolder(view){

        fun bind(text: CharSequence){
            setPageText(text)
        }
    }

    inner class SplitPageViewHolder(view: View): ViewHolder(view){
        private val bottomText: View = view.findViewById(R.id.page_bottom_text_view)

        private val hiddenParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, pageMarginsSize, 0f)
        private val halfShownParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.5f).apply {
            setMargins(0, 0, 0, pageMarginsSize)
        }

        fun bind(text: CharSequence){
            setPageText(text)

            updateLayoutParams(isPageSplit)
        }

        fun updateLayoutParams(splitPage: Boolean){
            bottomText.layoutParams = if(splitPage) halfShownParams else hiddenParams
            pageTextView.setLineSpacing(if(splitPage) 0f else lineSpacingExtra, 1f)
        }

        fun setHighlightedText(start: Int, end: Int){
            val text = SpannableString(pageTextView.text)

            //Remove previous
            text.getSpans(0, text.length, BackgroundColorSpan::class.java).map { span -> text.removeSpan(span) }

            text.setSpan(BackgroundColorSpan(0x6633B5E5), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            pageTextView.setText(text, TextView.BufferType.SPANNABLE)
        }

    }

    class PageActionModeCallback(
        private val pageTextView: TextView,
        private val showTextDialog: (CharSequence) -> Unit
    ): ActionMode.Callback{

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
                else -> return false
            }
        }

        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?) = true

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            menu.clear()
            val inflater: MenuInflater = mode.menuInflater
            menu.add(Menu.NONE, android.R.id.copy, Menu.NONE, android.R.string.copy)
            inflater.inflate(R.menu.menu_context_text_visualizer, menu)
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {}
    }
}