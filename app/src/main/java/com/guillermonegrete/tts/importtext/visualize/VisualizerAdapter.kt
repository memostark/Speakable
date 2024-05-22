package com.guillermonegrete.tts.importtext.visualize

import android.annotation.SuppressLint
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.RecyclerView
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.common.models.EditNote
import com.guillermonegrete.tts.common.models.NoteItem
import com.guillermonegrete.tts.common.models.Span
import com.guillermonegrete.tts.databinding.VisualizerPageItemBinding
import com.guillermonegrete.tts.databinding.VisualizerSplitPageItemBinding
import com.guillermonegrete.tts.utils.addHighlightedText
import com.guillermonegrete.tts.utils.dpToPixel
import com.guillermonegrete.tts.utils.getSelectedText
import java.text.BreakIterator
import java.util.*

class VisualizerAdapter(
    private val pages: List<PageItem>,
    private val showTextDialog: (CharSequence) -> Unit,
    private val onCreateNote: (EditNote) -> Unit,
    private val onNoteClicked: (EditNote) -> Unit = {},
    private val getPageCharPos: () -> Int = {0},
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
        val inflater = LayoutInflater.from(parent.context)
        return when(viewType){
            R.layout.visualizer_split_page_item -> SplitPageViewHolder(VisualizerSplitPageItemBinding.inflate(inflater, parent, false))
            else -> {
                if (measuringPage) ViewHolder(inflater.inflate(viewType, parent, false))
                else PageViewHolder(VisualizerPageItemBinding.inflate(inflater, parent, false))
            }
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
                when (val payload = payloads.first()) {
                    is Span -> holder.setHighlightedText(pages[position], payload.start, payload.end)
                    is Int -> {
                        if (payload == UNSELECT_SENTENCE) holder.removeHighlight()
                    }
                    else -> holder.updateLayoutParams(payload as Boolean)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        if(hasBottomSheet && !measuringPage) return R.layout.visualizer_split_page_item
        return R.layout.visualizer_page_item
    }

    @SuppressLint("ClickableViewAccessibility")
    open inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        protected val pageTextView: TextView = view.findViewById(R.id.page_text_view)

        private val actionModeCallback = PageActionModeCallback(pageTextView, showTextDialog)

        init {
            pageTextView.customSelectionActionModeCallback = actionModeCallback
            // Color taken from member variable mHighlightColor from TextView class.
            pageTextView.highlightColor = 0x6633B5E5
            pageTextView.movementMethod = LinkMovementMethod.getInstance()
            val detector = GestureDetectorCompat(itemView.context, PageGestureListener())
            pageTextView.setOnTouchListener { _, event ->
                detector.onTouchEvent(event)
            }
            pageTextView.customSelectionActionModeCallback = actionModeCallback
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

        private inner class PageGestureListener : GestureDetector.SimpleOnGestureListener() {

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val offset = pageTextView.getOffsetForPosition(e.x, e.y)

                val item = pages[adapterPosition]
                val clickedNote = item.notes.find { offset in it.span.start .. it.span.end }
                if (clickedNote != null) {
                    val span = clickedNote.span
                    val text = item.text.substring(span.start, span.end)
                    val absoluteSpan = Span(item.firstCharIndex + span.start, item.firstCharIndex + span.end)
                    onNoteClicked(EditNote(text, clickedNote.text, absoluteSpan, clickedNote.color, true, clickedNote.id))
                }

                return super.onSingleTapConfirmed(e)
            }

        }

    }

    inner class PageViewHolder(private val binding: VisualizerPageItemBinding): ViewHolder(binding.root){

        fun bind(pageItem: PageItem){
            setPageText(pageItem.text)

            pageItem.notes.forEach {
                val span = it.span
                binding.pageTextView.addHighlightedText(span.start, span.end, it.color)
            }
        }
    }

    inner class SplitPageViewHolder(private val binding: VisualizerSplitPageItemBinding): ViewHolder(binding.root){

        private var highlightSpan: BackgroundColorSpan? = null

        private val hiddenParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, pageMarginsSize, 0f)
        private val halfShownParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.5f).apply {
            setMargins(0, 0, 0, pageMarginsSize)
        }

        fun bind(pageItem: PageItem){
            updateLayoutParams(isPageSplit)

            setPageText(pageItem.text)

            pageItem.notes.forEach {
                val span = it.span
                binding.pageTextView.addHighlightedText(span.start, span.end, it.color)
            }
        }

        fun updateLayoutParams(splitPage: Boolean){
            binding.pageBottomTextView.layoutParams = if(splitPage) halfShownParams else hiddenParams
            pageTextView.setLineSpacing(if(splitPage) 0f else lineSpacingExtra, 1f)
        }

        fun setHighlightedText(item: PageItem, start: Int, end: Int){
            val text = SpannableString(pageTextView.text)
            // remove overlapping notes
            text.getSpans(start, end, BackgroundColorSpan::class.java).map { bgSpan -> text.removeSpan(bgSpan) }

            //Remove previous selection
            highlightSpan?.let { span -> text.removeSpan(span) }

            highlightSpan = BackgroundColorSpan(0x6633B5E5)
            text.setSpan(highlightSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            pageTextView.setText(text, TextView.BufferType.SPANNABLE)

            // reapply notes so they are still in front of the selection
            item.notes.forEach {
                val noteSpan = it.span
                if (start < noteSpan.end && end > noteSpan.start)
                    pageTextView.addHighlightedText(noteSpan.start, noteSpan.end, it.color)
            }
        }

        fun removeHighlight() {
            val span = highlightSpan ?: return
            val text = pageTextView.text as? Spannable ?: return
            text.removeSpan(span)
            highlightSpan = null
        }

    }

    inner class PageActionModeCallback(
        private val pageTextView: TextView,
        private val showTextDialog: (CharSequence) -> Unit
    ): ActionMode.Callback{

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when(item.itemId){
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
                    true
                }
                R.id.add_new_note_action -> {
                    val firstCharIndex = getPageCharPos()
                    val span = Span(firstCharIndex + pageTextView.selectionStart, firstCharIndex + pageTextView.selectionEnd)
                    val text = pageTextView.getSelectedText().toString()
                    onCreateNote(EditNote(text, "", span, 0, false, 0))
                    mode.finish()
                    true
                }
                else -> false
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

    fun updateNote(span: Span, position: Int, note: NoteItem) {
        val page = pages[position]
        page.notes.removeAll { note.id == it.id }
        val localSpan = Span(span.start - page.firstCharIndex, span.end - page.firstCharIndex)
        page.notes.add(note.copy(span = localSpan))
        notifyItemChanged(position)
    }

    companion object {
        const val UNSELECT_SENTENCE = 10
    }

    data class PageItem(
        val text: CharSequence,
        val notes: MutableList<NoteItem>,
        /**
         * The index of the paragraph's first char with respect to the whole text.
         */
        val firstCharIndex: Int,
    ) {
        companion object {
            val EMPTY = PageItem("", mutableListOf(), 0)
        }
    }
}
