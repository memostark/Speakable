package com.guillermonegrete.tts.importtext.visualize

import android.annotation.SuppressLint
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.util.TypedValue
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.core.view.updatePadding
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.common.models.EditNote
import com.guillermonegrete.tts.common.models.NoteItem
import com.guillermonegrete.tts.common.models.Span
import com.guillermonegrete.tts.databinding.VisualizerPageItemBinding
import com.guillermonegrete.tts.databinding.VisualizerSplitPageItemBinding
import com.guillermonegrete.tts.textprocessing.WordState
import com.guillermonegrete.tts.ui.theme.HighlightColorInt
import com.guillermonegrete.tts.ui.theme.NestedHighlightColor
import com.guillermonegrete.tts.ui.theme.TextHighlightColor
import com.guillermonegrete.tts.ui.theme.YellowNoteHighlightInt
import com.guillermonegrete.tts.utils.addHighlightedText
import com.guillermonegrete.tts.utils.findWordForRightHanded
import com.guillermonegrete.tts.utils.getSelectedText
import kotlin.math.max
import kotlin.math.min

class VisualizerAdapter(
    private var pages: List<PageItem>,
    private val onCreateNote: (EditNote) -> Unit,
    private val onTextClick: (TextClick) -> Unit = {},
    var measuringPage: Boolean = false
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var hasBottomSheet = false
    var isPageSplit = false

    private var selectedTextAbsSpan: Span? = null
    private var selectedText: SelectedText? = null
    private var selectionSpan: BackgroundColorSpan? = null

    private var pageMarginsSize = 0
    private var lineSpacingExtra = 0f
    private var largeText = 0

    var horizontalPadding: Int? = null
    var verticalPadding: Int? = null

    private val textHighlightColor = TextHighlightColor.toArgb()
    private var wordInsideColor = NestedHighlightColor.toArgb()

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        with(recyclerView.context){
            lineSpacingExtra = resources.getDimension(R.dimen.visualize_page_text_line_spacing_extra)
            pageMarginsSize = resources.getDimensionPixelSize(R.dimen.visualize_sheet_bar_height)
            largeText = resources.getDimensionPixelSize(R.dimen.text_size_large)
            var color = MaterialColors.getColor(this, android.R.attr.textColorHighlight, wordInsideColor)
            color = ColorUtils.setAlphaComponent(color, 255)
            wordInsideColor = ColorUtils.blendARGB(textHighlightColor, color, 0.6f)
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
                    is Boolean -> holder.updateLayoutParams(payload)
                    Payload.Text -> holder.setHighlightedText()
                }
            } else if (holder is PageViewHolder) {
                for (payload in payloads) {
                    if (payload is Payload) {
                        when(payload) {
                            Payload.Text -> holder.setHighlightedText()
                        }
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        if(hasBottomSheet && !measuringPage) return R.layout.visualizer_split_page_item
        return R.layout.visualizer_page_item
    }

    fun updateItems(pages: List<PageItem>) {
        this.pages = pages

        selectedTextAbsSpan?.let {
            val pos = getCharListIndex(it.start)
            if (pos != -1) {
                val item = pages[pos]
                selectedText = SelectedText(pos, item.toLocal(it))
            }
        }
    }

    fun getPageText(position: Int) = pages.getOrNull(position)?.text

    @SuppressLint("ClickableViewAccessibility")
    open inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        protected val pageTextView: TextView = view.findViewById(R.id.page_text_view)

        private val actionModeCallback = PageActionModeCallback(pageTextView) { text, span ->
            onTextClick(TextClick.Word(text, span))
        }

        init {
            // Color taken from member variable mHighlightColor from TextView class.
            pageTextView.highlightColor = 0x6633B5E5
            updateCardPadding()
            val detector = GestureDetector(itemView.context, PageGestureListener())
            pageTextView.setOnTouchListener { _, event ->
                detector.onTouchEvent(event)
            }
            pageTextView.customSelectionActionModeCallback = actionModeCallback
        }

        open fun bind(pageItem: PageItem) {
            actionModeCallback.pageItem = pageItem
        }

        protected fun addHighlightItems(pageItem: PageItem, text: Spannable) {
            pageItem.notes.forEach {
                val span = it.span
                val start = span.start.coerceAtLeast(0)
                val end = span.end.coerceAtMost(text.length)
                text.addHighlightedText(start, end, it.color)
            }

            pageItem.savedWords.forEach {
                val span = it.span
                if (span != null) text.addHighlightedText(span.start, span.end)
            }

            // When a note and saved word overlap add a blend of their colors
            pageItem.notes.forEach { note ->
                pageItem.savedWords.forEach {
                    val span = it.span
                    if (span != null && note.span.intersects(it.span)) {
                        val start = max(span.start, note.span.start)
                        val end = min(span.end, note.span.end)
                        text.addHighlightedText(start, end, ColorUtils.blendARGB(HighlightColorInt, note.color, 0.5f))
                    }
                }
            }
        }

        open fun setHighlightedText() {
            val text = pageTextView.text as? Spannable ?: return
            selectionSpan?.let { text.removeSpan(it) }

            val sel = selectedText
            if (sel != null && sel.index == adapterPosition) {
                selectionSpan = BackgroundColorSpan(textHighlightColor)
                val start = sel.span.start.coerceAtLeast(0)
                val end = sel.span.end.coerceAtMost(text.length)
                text.setSpan(selectionSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else {
                selectionSpan = null
            }
        }

        private fun updateCardPadding() {
            if (horizontalPadding != null || verticalPadding != null) {
                val newHorizontal = horizontalPadding ?: pageTextView.paddingLeft
                val newVertical = verticalPadding ?: pageTextView.paddingTop
                pageTextView.updatePadding(left = newHorizontal, right = newHorizontal, top = newVertical, bottom = newVertical)
            }
        }

        private inner class PageGestureListener : GestureDetector.SimpleOnGestureListener() {

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val offset = pageTextView.getOffsetForPosition(e.x, e.y)

                val index = adapterPosition
                val item = pages[index]
                val savedWord = item.savedWords.find { it.span != null && offset in it.span.start ..it.span.end }
                val clickedNote = item.notes.find { offset in it.span.start .. it.span.end }
                if (savedWord != null && clickedNote != null) {
                    val word = savedWord.word.word
                    val note = EditNote("", clickedNote.text, item.toAbsolute(clickedNote.span), clickedNote.color, true, clickedNote.id)
                    onTextClick(TextClick.Overlap(note, word))
                    return true
                } else if (savedWord != null) {
                    onTextClick(TextClick.SavedWord(savedWord))
                    return true
                } else if (clickedNote != null) {
                    val note = EditNote("", clickedNote.text, item.toAbsolute(clickedNote.span), clickedNote.color, true, clickedNote.id)
                    onTextClick(TextClick.Note(note))
                    return true
                }

                val wordSpan = pageTextView.findWordForRightHanded(offset)
                val clickedWord = pageTextView.text.substring(wordSpan.start, wordSpan.end)

                if (clickedWord.isNotEmpty()) {
                    selectedText = SelectedText(index, wordSpan)
                    onTextClick(TextClick.Word(clickedWord, item.toAbsolute(wordSpan)))
                    notifyItemChanged(index, Payload.Text)
                    return true
                }

                return super.onSingleTapConfirmed(e)
            }

        }

        inner class PageActionModeCallback(
            private val pageTextView: TextView,
            private val showTextDialog: (text: String, span: Span) -> Unit
        ): ActionMode.Callback {

            var pageItem: PageItem? = null

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                val localItem = pageItem ?: return true
                return when(item.itemId){
                    R.id.show_process_text_activity -> {
                        if (pageTextView.isFocused) {
                            val selStart = pageTextView.selectionStart
                            val selEnd = pageTextView.selectionEnd

                            // We need to make sure start and end are within the text length
                            val min = 0.coerceAtLeast(selStart.coerceAtMost(selEnd))
                            val max = 0.coerceAtLeast(selStart.coerceAtLeast(selEnd))

                            val selectedText = pageTextView.text.subSequence(min, max).toString()
                            showTextDialog(selectedText, localItem.toAbsolute(Span(min, max)))
                        }

                        mode.finish()
                        true
                    }
                    R.id.add_new_note_action -> {
                        val span = Span(pageTextView.selectionStart, pageTextView.selectionEnd)
                        val text = pageTextView.getSelectedText().toString()
                        onCreateNote(EditNote(text, "", localItem.toAbsolute(span), YellowNoteHighlightInt, false, 0))
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

                if (notesOverlap()) {
                    val item = menu.findItem(R.id.add_new_note_action)
                    item.isVisible = false
                }
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode?) {}

            private fun notesOverlap(): Boolean {
                val localItem = pageItem ?: return true
                val selStart = pageTextView.selectionStart
                val selEnd = pageTextView.selectionEnd
                localItem.notes.forEach {
                    val span = it.span
                    val isOverlappingNotes = span.start < selEnd && span.end > selStart
                    if (isOverlappingNotes) return true
                }

                return false
            }
        }

    }

    inner class PageViewHolder(val binding: VisualizerPageItemBinding): ViewHolder(binding.root){

        override fun bind(pageItem: PageItem){
            super.bind(pageItem)
            val spannable = SpannableString(pageItem.text)
            addHighlightItems(pageItem, spannable)
            pageTextView.setText(spannable, TextView.BufferType.SPANNABLE)
            setHighlightedText()
        }
    }

    inner class SplitPageViewHolder(private val binding: VisualizerSplitPageItemBinding): ViewHolder(binding.root){

        private var sentenceHighlight: SentenceHighlight? = null

        private val hiddenParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, pageMarginsSize, 0f)
        private val halfShownParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.5f).apply {
            setMargins(0, 0, 0, pageMarginsSize)
        }

        override fun bind(pageItem: PageItem){
            super.bind(pageItem)
            updateLayoutParams(isPageSplit)

            val spannable = SpannableString(pageItem.text)
            sentenceHighlight?.let {
                spannable.setSpan(it.span, it.pos.start, it.pos.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            addHighlightItems(pageItem, spannable)
            pageTextView.setText(spannable, TextView.BufferType.SPANNABLE)
            setHighlightedText()
        }

        fun updateLayoutParams(splitPage: Boolean){
            binding.pageBottomTextView.layoutParams = if(splitPage) halfShownParams else hiddenParams
            if (splitPage) {
                pageTextView.setLineSpacing(0f, 1f)
                TextViewCompat.setAutoSizeTextTypeWithDefaults(pageTextView, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM)
            } else {
                pageTextView.setLineSpacing(lineSpacingExtra, 1f)
                TextViewCompat.setAutoSizeTextTypeWithDefaults(pageTextView, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE)
                pageTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, largeText.toFloat())
            }
        }

        fun setHighlightedText(item: PageItem, start: Int, end: Int){
            val text = SpannableString(pageTextView.text)
            // remove overlapping notes
            text.getSpans(start, end, BackgroundColorSpan::class.java).map { bgSpan -> text.removeSpan(bgSpan) }

            //Remove previous selection
            sentenceHighlight?.let { text.removeSpan(it.span) }

            val highlightSpan = BackgroundColorSpan(0x6633B5E5)
            sentenceHighlight = SentenceHighlight(highlightSpan, Span(start, end))
            text.setSpan(highlightSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            // reapply notes so they are still in front of the selection
            item.notes.forEach {
                val noteSpan = it.span
                if (start < noteSpan.end && end > noteSpan.start)
                    text.addHighlightedText(noteSpan.start, noteSpan.end, it.color)
            }
            item.savedWords.forEach {
                val noteSpan = it.span
                if (noteSpan != null && start < noteSpan.end && end > noteSpan.start)
                    text.addHighlightedText(noteSpan.start, noteSpan.end)
            }

            pageTextView.setText(text, TextView.BufferType.SPANNABLE)
        }

        override fun setHighlightedText() {
            val text = pageTextView.text as? Spannable
            selectionSpan?.let { text?.removeSpan(it) }

            val sel = selectedText
            if (sel != null && sel.index == adapterPosition) {
                val sentenceSpan = sentenceHighlight?.pos
                val color = if (sentenceSpan != null && sentenceSpan.intersects(sel.span)) wordInsideColor else textHighlightColor
                selectionSpan = BackgroundColorSpan(color)
                text?.setSpan(selectionSpan, sel.span.start, sel.span.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else {
                selectionSpan = null
            }
        }

        fun removeHighlight() {
            val highlight = sentenceHighlight ?: return
            val text = pageTextView.text as? Spannable ?: return
            text.removeSpan(highlight.span)
            sentenceHighlight = null
        }

    }

    fun updateNote(note: NoteItem) {
        val positions = getCharListIndex(note.span)
        positions.forEach { pos ->
            val page = pages[pos]
            page.notes.removeAll { note.id == it.id }
            val localSpan = Span(note.span.start - page.firstCharIndex, note.span.end - page.firstCharIndex)
            page.notes.add(note.copy(span = localSpan))
            notifyItemChanged(pos)
        }
    }

    fun deleteNote(noteId: Long) {
        val pos = pages.indexOfFirst {
            val note = it.notes.firstOrNull { note -> note.id == noteId }
            note != null
        }
        if (pos == -1) return
        val pageItem = pages[pos]
        pageItem.notes.removeAll { noteId == it.id }
        notifyItemChanged(pos)
    }

    fun updateSavedWords(words: List<WordState>, position: Int) {
        val pageItem = pages[position]
        pageItem.savedWords.clear()
        pageItem.savedWords.addAll(words)
        notifyItemChanged(position)
    }

    fun selectText(span: Span){
        selectedTextAbsSpan = span
        val index = getCharListIndex(span.start)
        if (index == -1) return

        val item = pages[index]
        val newSel = SelectedText(index, item.toLocal(span))
        if (selectedText == newSel) return

        selectedText = newSel
        notifyItemChanged(index, Payload.Text)
    }

    fun unselectText() {
        selectedText?.let {
            val index = it.index
            selectedText = null
            notifyItemChanged(index, Payload.Text)
        }
    }

    fun getCharListIndex(charPos: Int) = pages.indexOfFirst { it.firstCharIndex + it.text.length > charPos }

    /**
     * Returns the indices of all the pages that contain part of the given [span] (the span can be a note for example).
     */
    private fun getCharListIndex(span: Span): MutableList<Int> {
        val indexes = mutableListOf<Int>()
        for ((i, page) in pages.withIndex()) {
            if (page.firstCharIndex > span.end) break
            val pageSpan = Span(page.firstCharIndex, page.firstCharIndex + page.text.length)
            if (span.intersects(pageSpan)) indexes.add(i)
        }
        return indexes
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
        val savedWords: MutableList<WordState> = mutableListOf(),
    ) {
        companion object {
            val EMPTY = PageItem("", mutableListOf(), 0)
        }

        fun toAbsolute(span: Span) : Span {
            return Span(firstCharIndex + span.start, firstCharIndex + span.end)
        }

        fun toLocal(absSpan: Span) = Span(absSpan.start - firstCharIndex, absSpan.end - firstCharIndex)
    }

    data class SelectedText(
        val index: Int,
        val span: Span,
    )

    data class SentenceHighlight(val span: BackgroundColorSpan, val pos: Span)

    sealed class TextClick {
        data class Note(val note: EditNote): TextClick()
        data class SavedWord(val state: WordState): TextClick()
        data class Overlap(val note: EditNote, val word: String): TextClick()
        data class Word(val word: String, val span: Span): TextClick()
    }

    sealed interface Payload {
        data object Text: Payload
    }
}
