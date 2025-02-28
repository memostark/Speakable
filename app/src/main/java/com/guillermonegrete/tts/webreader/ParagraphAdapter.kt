package com.guillermonegrete.tts.webreader

import android.annotation.SuppressLint
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.ActionMode
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.common.models.EditNote
import com.guillermonegrete.tts.common.models.NoteItem
import com.guillermonegrete.tts.common.models.Span
import com.guillermonegrete.tts.common.models.WordUI
import com.guillermonegrete.tts.databinding.ParagraphExpandedItemBinding
import com.guillermonegrete.tts.databinding.ParagraphItemBinding
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.textprocessing.WordState
import com.guillermonegrete.tts.ui.theme.HighlightColorInt
import com.guillermonegrete.tts.utils.addHighlightedText
import com.guillermonegrete.tts.utils.findWordForRightHanded
import com.guillermonegrete.tts.utils.getBgColorSpan
import com.guillermonegrete.tts.utils.getSelectedText
import com.guillermonegrete.tts.utils.isWord
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ParagraphAdapter(
    val viewModel: WebReaderViewModel,
    val onSentenceSelected: (paragraph: Int, sentence: Int) -> Unit,
    val onParagraphSelected: (paragraph: Int?) -> Unit,
    val onTextHighlighted: () -> Unit = {},
    val onTranslateHighlightedText: (text: String, span: Span) -> Unit = { _, _ -> },
    val loadDatabaseWord: (text: CharSequence, pos: Int) -> Unit = { _, _ -> },
    val scanParagraph: (dbWords: List<Words>, text: String, position: Int) -> List<WordState> = { _, _, _ -> emptyList() },
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items = emptyList<ParagraphItem>()
    var isPageSaved: Boolean = false

    var expandedItemPos = -1
        private set

    var isLoading = false

    /**
     * Indicates whether the initial load of words from the database has been completed.
     */
    var initialWordsLoaded = false

    /**
     * Whether the current selected text (started with a long-press) is overlapping a note.
     */
    var isOverlappingNotes = false
    /**
     * Whether the current selected text (started with a long-press) is overlapping a saved word.
     */
    var isOverlappingSavedWord = false

    /**
     * Current TextView highlighted by a long-press.
     */
    private var highlightedTextView: TextView? = null

    val selectedSentence = SelectedSentence()
    /**
     * The position of the paragraph in the list that contains the selected word.
     */
    private var selectedWordPos = -1
    /**
     * Used to restore the selected word if the items are reloaded (e.g. during config change)
     */
    private var selectedWordSpan: Span? = null

    private var highlightedTextPos = -1

    private var wordInsideColor = NESTED_HIGHLIGHT_COLOR

    private val _textClicked = MutableSharedFlow<TextClick>(
        replay = 0,
        extraBufferCapacity = 1,
        BufferOverflow.DROP_OLDEST
    )
    val textClicked = _textClicked.asSharedFlow()

    private val _addNoteClicked = MutableSharedFlow<EditNote>(
        replay = 0,
        extraBufferCapacity = 1,
        BufferOverflow.DROP_OLDEST
    )
    val addNoteClicked = _addNoteClicked.asSharedFlow()

    private val _addWordClicked = MutableSharedFlow<WordState>(
        replay = 0,
        extraBufferCapacity = 1,
        BufferOverflow.DROP_OLDEST
    )
    val addWordClicked = _addWordClicked.asSharedFlow()

    val newWords = mutableSetOf<Words>()

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        var color = MaterialColors.getColor(recyclerView.context, android.R.attr.textColorHighlight, NESTED_HIGHLIGHT_COLOR)
        color = ColorUtils.setAlphaComponent(color, 255)
        wordInsideColor = ColorUtils.blendARGB(HIGHLIGHT_COLOR, color, 0.6f)
    }

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

    /**
     * This one is called when only the BackgroundSpans are modified.
     * Avoid reassigning the text, this produces a small flicker in older devices so instead modify the spans.
     */
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            if(holder is ViewHolder) {
                for (payload in payloads) {
                    when (payload) {
                        is Int -> holder.setHighlightedText(items[position], payload)
                        PAYLOAD_WORD -> holder.highlightWord(items[position])
                        PAYLOAD_WORD_SENTENCE -> holder.highlightInsideWord(items[position])
                        PAYLOAD_INITIAL_DB_WORDS ->  {
                            val spannable = holder.getSpannable()
                            if (spannable != null) holder.addSavedWords(items[position], spannable)
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) = if(expandedItemPos == position) R.layout.paragraph_expanded_item else R.layout.paragraph_item

    @SuppressLint("NotifyDataSetChanged")
    fun updateItems(items: List<ParagraphItem>) {
        this.items = items

        if (selectedSentence.paragraphIndex != -1 && selectedSentence.sentenceIndex != -1) {
            // Restore sentence
            val item = items[selectedSentence.paragraphIndex]
            item.selectedIndex = selectedSentence.sentenceIndex

            selectedWordSpan?.let {
                item.selectedWord = item.toLocal(it)
            }
        } else {
            // Restore word
            selectedWordSpan?.let {
                val pos = getCharListIndex(it.start)
                if (pos == -1) return
                val item = items[pos]
                item.selectedWord = item.toLocal(it)
                selectedWordPos = pos
            }
        }
        notifyDataSetChanged()
    }

    fun updateTranslation(translation: String){
        items.getOrNull(expandedItemPos)?.let { it.translation = translation }
    }

    fun updateExpanded(){
        notifyItemChanged(expandedItemPos)
    }

    @SuppressLint("ClickableViewAccessibility")
    inner class ViewHolder(val binding: ParagraphItemBinding): RecyclerView.ViewHolder(binding.root) {

        var firstCharIndex = 0

        /**
         * Background span of the selected sentence
         */
        private var selectionSpan: BackgroundColorSpan? = null

        /**
         * Background span of the selected word inside a sentence.
         */
        private var wordInsideSpan: BackgroundColorSpan? = null
        private val actionModeCallback = ParagraphActionModeCallback()

        init {
            // Because of a bug when having a TextView inside a CoordinatorLayout, the paragraph TextView has to have width equals to wrap_content so its text can be selectable.
            // Using match_parent the text can't be selected
            with(binding){
                val detector = GestureDetector(itemView.context, MyGestureListener())
                paragraph.setOnTouchListener { _, event ->
                    detector.onTouchEvent(event)
                }
                paragraph.customSelectionActionModeCallback = actionModeCallback
            }
        }

        private fun findSentence(offset: Int): Int {
            val item = items[adapterPosition]
            item.indexes.forEachIndexed { index, span ->
                if(offset in span.start..span.end) return index
            }
            return -1
        }

        fun bind(item: ParagraphItem) {
            val spannable = SpannableString(item.original)
            if(item.selectedIndex != -1){
                val span = item.indexes[item.selectedIndex]
                selectionSpan = spannable.addHighlightedText(span.start, span.end, HIGHLIGHT_COLOR)
                val wordSpan = item.selectedWord
                if(wordSpan != null) wordInsideSpan = spannable.addHighlightedText(wordSpan.start, wordSpan.end, wordInsideColor)
            } else {
                val span = item.selectedWord
                if(span != null) selectionSpan = spannable.addHighlightedText(span.start, span.end, HIGHLIGHT_COLOR)
            }

            item.notes.forEach {
                val span = it.span
                spannable.addHighlightedText(span.start, span.end, it.color)
            }

            if (item.scanNewWords) {
                addNewWords(item, adapterPosition)
            }

            addSavedWords(item, spannable)

            if (initialWordsLoaded && !item.databaseWordsLoaded) {
                loadDatabaseWord(item.original, adapterPosition)
            }

            binding.paragraph.setText(spannable, TextView.BufferType.SPANNABLE)
            actionModeCallback.item = item
            firstCharIndex = item.firstCharIndex
        }

        private inner class MyGestureListener : GestureDetector.SimpleOnGestureListener() {

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val offset = binding.paragraph.getOffsetForPosition(e.x, e.y)

                // First check if a note was tapped
                val item = items[adapterPosition]
                val savedWord = item.savedWords.find { it.span != null && offset in it.span.start ..it.span.end }
                val clickedNote = item.notes.find { offset in it.span.start .. it.span.end }
                if (savedWord != null && clickedNote != null) {
                    val span = savedWord.span ?: return true
                    val absoluteSpan = Span(firstCharIndex + span.start, firstCharIndex + span.end)
                    val word = savedWord.copy(span = absoluteSpan)
                    val editNote = createNote(clickedNote, item)
                    _textClicked.tryEmit(TextClick.Overlap(word, editNote))
                    return true
                } else if (savedWord != null) {
                    val span = savedWord.span ?: return true
                    val absoluteSpan = Span(firstCharIndex + span.start, firstCharIndex + span.end)
                    val word = savedWord.copy(span = absoluteSpan)
                    _textClicked.tryEmit(TextClick.SavedWord(word))
                    return true
                } else if (clickedNote != null) {
                    val editNote = createNote(clickedNote, item)
                    _textClicked.tryEmit(TextClick.Note(editNote))
                    return true
                }

                val wordSpan = binding.paragraph.findWordForRightHanded(offset)
                val clickedWord = binding.paragraph.text.substring(wordSpan.start, wordSpan.end)

                // If a highlighted sentence was tapped, notify sentence clicked to observers
                if (item.selectedIndex != -1) {
                    val span = item.indexes[item.selectedIndex]
                    if(offset in span.start..span.end) {
                        item.selectedWord = wordSpan
                        selectedSentence.wordSelected = true
                        _textClicked.tryEmit(TextClick.Sentence(clickedWord))
                        return true
                    }
                }

                if(clickedWord.isNotEmpty()) {
                    viewModel.translateWord(clickedWord, item.toAbsolute(wordSpan))
                    unselectWord()

                    // Select new word
                    item.selectedWord = wordSpan
                    selectedWordPos = adapterPosition
                    notifyItemChanged(adapterPosition, PAYLOAD_WORD)
                }
                return super.onSingleTapConfirmed(e)
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                setSentenceSelected(e)
                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                e1 ?: return false
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x
                // Detects horizontal swipes in any direction
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        onParagraphSelected(adapterPosition)
                    }
                }
                return true
            }
        }

        private fun setSentenceSelected(e: MotionEvent) {
            unselectSentence()
            unselectWord()

            val offset = binding.paragraph.getOffsetForPosition(e.x, e.y)
            val index = findSentence(offset)
            selectSentence(adapterPosition, index)
            onSentenceSelected(adapterPosition, index)
        }

        /**
         * Set the highlighted span without reassigning the text to the TextView.
         */
        fun setHighlightedText(item: ParagraphItem, sentencePos: Int) {
            val text = binding.paragraph.text as? Spannable ?: return

            if(sentencePos == -1) { // delete selected word or sentence
                selectionSpan?.let {
                    text.removeSpan(it)
                    selectionSpan = null
                    wordInsideSpan?.let { wordSpan ->
                        text.removeSpan(wordSpan)
                        wordInsideSpan = null
                    }
                }
            } else {
                val span = item.indexes[sentencePos]
                // remove overlapping notes
                text.getSpans(span.start, span.end, BackgroundColorSpan::class.java).map { bgSpan -> text.removeSpan(bgSpan) }

                // add highlight
                selectionSpan = BackgroundColorSpan(HIGHLIGHT_COLOR)
                text.setSpan(selectionSpan, span.start, span.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                val overlaps = mutableSetOf<BgColorSpan>()
                // reapply notes and words so they are still in front of the selection
                item.notes.forEach { note ->
                    val noteSpan = note.span
                    if (span.start < noteSpan.end && span.end > noteSpan.start) {
                        item.savedWords.forEach {
                            val wordSpan = it.span
                            if (wordSpan != null) {
                                val overlap = getOverlap(note, wordSpan)
                                if (overlap != null) {
                                    val overlapSpan = text.getBgColorSpan(overlap.start, overlap.end, overlap.color)
                                    if (overlapSpan != null) text.removeSpan(overlapSpan)
                                    overlaps.add(overlap)
                                }
                            }
                        }
                        text.addHighlightedText(noteSpan.start, noteSpan.end, note.color)
                    }
                }

                item.savedWords.forEach { word ->
                    val wordSpan = word.span
                    if (wordSpan != null && span.start < wordSpan.end && span.end > wordSpan.start) {
                        item.notes.forEach {
                            val overlap = getOverlap(it, wordSpan)
                            if (overlap != null) {
                                val overlapSpan = text.getBgColorSpan(overlap.start, overlap.end, overlap.color)
                                if (overlapSpan != null) text.removeSpan(overlapSpan)
                                overlaps.add(overlap)
                            }
                        }
                        text.addHighlightedText(wordSpan.start, wordSpan.end)
                    }
                }

                // Reapply overlaps
                overlaps.map { text.addHighlightedText(it.start, it.end, it.color) }
            }
        }

        private fun getOverlap(note: NoteItem, wordSpan: Span): BgColorSpan? {
            val noteSpan = note.span
            if (noteSpan.intersects(wordSpan)) {
                val start = max(wordSpan.start, noteSpan.start)
                val end = min(wordSpan.end, noteSpan.end)
                val color = ColorUtils.blendARGB(HighlightColorInt, note.color, 0.5f)
                return BgColorSpan(start, end, color)
            }
            return null
        }

        /**
         * Set the highlighted span without reassigning the text to the TextView.
         */
        fun highlightWord(item: ParagraphItem) {
            selectionSpan = BackgroundColorSpan(HIGHLIGHT_COLOR)
            val text = binding.paragraph.text as? Spannable
            val span = item.selectedWord
            if (span != null) text?.setSpan(selectionSpan, span.start, span.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        fun highlightInsideWord(item: ParagraphItem) {
            val text = binding.paragraph.text as? Spannable ?: return
            wordInsideSpan?.let { text.removeSpan(it) }

            val span = item.selectedWord
            if (span == null) {
                // no span means we only wanted to remove the highlight
                wordInsideSpan = null
                return
            }

            wordInsideSpan = BackgroundColorSpan(wordInsideColor)
            text.setSpan(wordInsideSpan, span.start, span.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        fun getSpannable() = binding.paragraph.text as? Spannable

        fun addSavedWords(item: ParagraphItem, spannable: Spannable) {
            item.savedWords.forEach {
                val span = it.span
                if (span != null) spannable.addHighlightedText(span.start, span.end)
            }

            // When a note and saved word overlap add a blend of their colors
            item.savedWords.forEach { word ->
                item.notes.forEach { note ->
                    val span = word.span
                    if (span != null && note.span.intersects(word.span)) {
                        val start = max(span.start, note.span.start)
                        val end = min(span.end, note.span.end)
                        spannable.addHighlightedText(start, end, ColorUtils.blendARGB(HighlightColorInt, note.color, 0.5f))
                    }
                }
            }
        }

        inner class ParagraphActionModeCallback: ActionMode.Callback {

            var item: ParagraphItem? = null
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                unselectSentence()
                highlightedTextView = binding.paragraph
                highlightedTextPos = adapterPosition
                onTextHighlighted()
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                if (!isPageSaved) return true

                menu ?: return false
                val localItem = item ?: return false

                menu.clear()
                menu.add(Menu.NONE, android.R.id.copy, Menu.NONE, android.R.string.copy)
                menu.add(Menu.NONE, TRANSLATE_MENU_ITEM_ID, Menu.NONE, R.string.translate_description)
                val inflater = mode?.menuInflater
                inflater?.inflate(R.menu.menu_context_web_reader, menu)

                val selStart = binding.paragraph.selectionStart
                val selEnd = binding.paragraph.selectionEnd

                // Check if selected text and note spans overlap
                for(note in localItem.notes) {
                    val span = note.span
                    isOverlappingNotes = span.start < selEnd && span.end > selStart
                    if (isOverlappingNotes) {
                        menu.findItem(R.id.add_new_note_action)?.isVisible = false
                        break
                    }
                }

                val isWord = highlightedTextView?.getSelectedText().toString().isWord()
                if (!isWord) {
                    menu.findItem(R.id.add_saved_word_action)?.isVisible = false
                    return true
                }

                // Check if selected text and note spans overlap
                for(word in localItem.savedWords) {
                    val span = word.span
                    if (span != null) {
                        isOverlappingSavedWord = span.start < selEnd && span.end > selStart
                        if (isOverlappingSavedWord) {
                            menu.findItem(R.id.add_saved_word_action)?.isVisible = false
                            break
                        }
                    }
                }

                return true
            }

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                item ?: return false
                val span = Span(firstCharIndex + binding.paragraph.selectionStart, firstCharIndex + binding.paragraph.selectionEnd)

                return when(item.itemId) {
                    R.id.add_new_note_action -> {
                        // New note so the text and color are empty and id is zero
                        val text = highlightedTextView?.getSelectedText().toString()
                        _addNoteClicked.tryEmit(EditNote(text, "", span, 0, false, 0))
                        mode?.finish()
                        true
                    }
                    R.id.add_saved_word_action -> {
                        val text = highlightedTextView?.getSelectedText().toString()
                        _addWordClicked.tryEmit(WordState(WordUI(text, "", ""), span = span))
                        mode?.finish()
                        true
                    }
                    TRANSLATE_MENU_ITEM_ID -> {
                        val text = getHighlightedText() ?: return false
                        onTranslateHighlightedText(text.toString(), span)
                        mode?.finish()
                        true
                    }
                    else -> false
                }
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                highlightedTextView = null
                highlightedTextPos = -1
                // Only reset these flags if no text was selected using the mode (if text was selected it may overlap notes)
                if (selectedWordPos == -1) {
                    isOverlappingNotes = false
                    isOverlappingSavedWord = false
                }
            }

        }
    }

    /**
     * Adds the newly inserted database words to the item if the list doesn't have them and if the paragraph contains the word.
     */
    private fun addNewWords(item: ParagraphItem, position: Int) {
        val wordsToAdd = mutableListOf<Words>()
        newWords.forEach { newWord ->
            val wordAdded = item.savedWords.any { newWord.id == it.dbId }
            if (!wordAdded) wordsToAdd.add(newWord)
        }
        if (wordsToAdd.isNotEmpty()) {
            val paragraphWords = scanParagraph(wordsToAdd, item.original.toString(), position)
            item.savedWords.addAll(paragraphWords)
        }
        item.scanNewWords = false
    }

    fun unselectSentence(){
        val previousIndex = selectedSentence.paragraphIndex
        if(previousIndex != -1) {
            val previousItem = items[previousIndex]
            previousItem.selectedIndex = -1
            previousItem.selectedWord = null
            notifyItemChanged(previousIndex, -1)
            selectedSentence.paragraphIndex = -1
            selectedSentence.sentenceIndex = -1
            selectedSentence.wordSelected = false
        }
    }

    fun unselectWord() {
        // unselect independent word
        if(selectedWordPos != -1) {
            val previousItem = items[selectedWordPos]
            previousItem.selectedWord = null
            notifyItemChanged(selectedWordPos, -1)
            selectedWordPos = -1
            selectedWordSpan = null
            isOverlappingNotes = false
            isOverlappingSavedWord = false
        }

        // unselect word that is within a sentence
        if(selectedSentence.wordSelected) {
            val index = selectedSentence.paragraphIndex
            val item = items[index]
            item.selectedWord = null
            notifyItemChanged(index, PAYLOAD_WORD_SENTENCE)
            selectedSentence.wordSelected = false
        }
    }

    fun unselectParagraph(){
        selectParagraph(-1)
    }

    fun selectWord(absSpan: Span) {
        selectedWordSpan = absSpan
        val pos = getCharListIndex(absSpan.start)
        if (pos == -1 || pos == selectedWordPos) return
        val item = items[pos]
        item.selectedWord = item.toLocal(absSpan)
        selectedWordPos = pos
        notifyItemChanged(pos, PAYLOAD_WORD)
    }

    fun selectSentence(paragraphIndex: Int, sentenceIndex: Int){
        unselectSentence()

        selectedSentence.paragraphIndex = paragraphIndex
        selectedSentence.sentenceIndex = sentenceIndex

        val item = items.getOrNull(paragraphIndex) ?: return
        item.selectedIndex = sentenceIndex

        notifyItemChanged(paragraphIndex, sentenceIndex)
    }

    fun selectParagraph(position: Int) {
        val previousExpandedPos = expandedItemPos
        expandedItemPos = position
        if (position == previousExpandedPos) return
        if (previousExpandedPos != -1) notifyItemChanged(previousExpandedPos)
        notifyItemChanged(position)
    }

    fun selectWordInSentence(paragraphIndex: Int, absSpan: Span) {
        selectedSentence.wordSelected = true
        selectedWordSpan = absSpan

        val item = items.getOrNull(paragraphIndex) ?: return
        item.selectedWord = item.toLocal(absSpan)
        updateWordInSentence()
    }

    fun nextSentence(){
        changeSentence(selectedSentence.sentenceIndex + 1)
    }

    private fun changeSentence(index: Int){
        var paragraphIndex = selectedSentence.paragraphIndex

        if(paragraphIndex != -1){
            val item = items[paragraphIndex]

            val newIndex = when {
                // Move to the next paragraph first sentence (hence the zero)
                index >= item.sentences.size -> {
                    paragraphIndex++
                    0
                }
                // Move to the previous paragraph, last sentence (if paragraph exists)
                index < 0 -> {
                    paragraphIndex--
                    if(paragraphIndex < 0) 0 else items[paragraphIndex].sentences.size - 1
                }
                else -> index
            }

            if(paragraphIndex in items.indices) {
                onSentenceSelected(paragraphIndex, newIndex)
            }
        }
    }

    fun previousSentence(){
        changeSentence(selectedSentence.sentenceIndex - 1)
    }

    fun getHighlightedText() = highlightedTextView?.getSelectedText()

    fun getHighlightedTextSpan(): Span? {
        if(highlightedTextPos != -1) {
            val item = items[highlightedTextPos]
            highlightedTextView?.let {
                return item.toAbsolute(Span(it.selectionStart, it.selectionEnd))
            }
        }
        return null
    }

    fun getSelectedWordSpan(): Span? {
        if (selectedWordPos != -1) {
            val item = items[selectedWordPos]
            val span = item.selectedWord ?: return null
            return item.toAbsolute(span)
        }

        if(selectedSentence.wordSelected) {
            val index = selectedSentence.paragraphIndex
            val item = items[index]
            val span = item.selectedWord ?: return null
            return item.toAbsolute(span)
        }
        return null
    }

    fun getSelectedSentenceSpan(): Span? {
        val sel = selectedSentence
        val item = items.getOrNull(sel.paragraphIndex)
        if (item != null) {
            val localSpan = item.indexes.getOrNull(sel.sentenceIndex)
            if (localSpan != null) return item.toAbsolute(localSpan)
        }
        return null
    }

    /**
     * Check if the given [span] is within the selected sentence.
     * If no sentence is selected false is returned.
     *
     * Assumes [span] is absolute.
     */
    fun isInsideSelectedSentence(span: Span): Boolean {
        val paragraphIndex = selectedSentence.paragraphIndex
        if(paragraphIndex != -1) {
            val item = items[paragraphIndex]
            val lSpan= span.toLocal(item)
            val index = selectedSentence.sentenceIndex
            if(index != -1) {
                val sentenceSpan = item.indexes[index]
                return lSpan.start >= sentenceSpan.start && lSpan.end <= sentenceSpan.end
            }
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    inner class ExpandedViewHolder(val binding: ParagraphExpandedItemBinding): RecyclerView.ViewHolder(binding.root){

        private val noTranslationText: CharSequence = itemView.context.getText(R.string.paragraph_not_translated)

        init {
            with(binding){

                toggleParagraph.setOnClickListener {
                    onParagraphSelected(null)
                }

                var clickedWord: String? = null

                // Handles click
                paragraph.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        val offset = paragraph.getOffsetForPosition(event.x, event.y)
                        val wordSpan = paragraph.findWordForRightHanded(offset)
                        clickedWord = paragraph.text.substring(wordSpan.start, wordSpan.end)
                    }
                    return@setOnTouchListener false
                }

                paragraph.setOnClickListener {
                    clickedWord?.let { word -> viewModel.onWordClicked(word, adapterPosition) }
                    clickedWord = null
                }

                translatedParagraph.setOnTouchListener { _, event ->
                    val duration = event.eventTime - event.downTime

                    if(event.action == MotionEvent.ACTION_UP && duration < 300){
                        val index = translatedParagraph.getOffsetForPosition(event.x, event.y)
                        val spans = viewModel.findSelectedSentence(adapterPosition, index) ?: return@setOnTouchListener false

                        paragraph.setHighlightedText(spans.topSpan.start, spans.topSpan.end)
                        translatedParagraph.setHighlightedText(spans.bottomSpan.start, spans.bottomSpan.end)
                    }
                    true
                }
            }
        }

        fun bind(item: ParagraphItem){
            binding.paragraph.text = item.original
            binding.loadingParagraph.isVisible = isLoading
            binding.translatedParagraph.text = item.translation.ifBlank { noTranslationText }
        }
    }

    private fun TextView.setHighlightedText(start: Int, end: Int): BackgroundColorSpan{
        val text = SpannableString(this.text)

        //Remove previous
        text.getSpans(0, text.length, BackgroundColorSpan::class.java).map { span -> text.removeSpan(span) }

        val selectionSpan = BackgroundColorSpan(HIGHLIGHT_COLOR)
        text.setSpan(selectionSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        this.setText(text, TextView.BufferType.SPANNABLE)
        return selectionSpan
    }

    private fun createNote(clickedNote: NoteItem, item: ParagraphItem): EditNote {
        val span = clickedNote.span
        val text = item.original.substring(span.start, span.end)
        return EditNote(text, clickedNote.text, item.toAbsolute(span), clickedNote.color, true, clickedNote.id)
    }

    fun updateNote(selection: Span, noteId: Long, result: AddNoteResult) {
        val pos = getCharListIndex(selection.start)
        if (pos == -1) return
        val paragraphItem = items[pos]
        paragraphItem.notes.removeAll { noteId == it.id }
        val span = Span(selection.start - paragraphItem.firstCharIndex, selection.end - paragraphItem.firstCharIndex)
        paragraphItem.notes.add(NoteItem(result.text, span, Color.parseColor(result.colorHex), noteId))
        notifyItemChanged(pos)
    }

    fun deleteNote(noteId: Long) {
        val pos = items.indexOfFirst {
            val note = it.notes.firstOrNull { note -> note.id == noteId }
            note != null
        }
        if (pos == -1) return
        val paragraphItem = items[pos]
        paragraphItem.notes.removeAll { noteId == it.id }
        notifyItemChanged(pos)
    }

    fun updateWordInSentence() {
        val itemIndex = selectedSentence.paragraphIndex
        notifyItemChanged(itemIndex, PAYLOAD_WORD_SENTENCE)
    }

    data class ParagraphItem(
        /**
         * The text in its original language,
         */
        val original: CharSequence,
        /**
         * An index is a [Span] that contains the start and end of each sentence on the paragraph
         */
        val indexes: List<Span>,
        val sentences: List<String>,
        val notes: MutableList<NoteItem>,
        /**
         * The index of the paragraph's first char with respect to the whole text.
         */
        val firstCharIndex: Int,
        /**
         * Index of the selected sentence, -1 means no selection.
         */
        val savedWords: MutableSet<WordState> = mutableSetOf(),
        var selectedIndex: Int = -1,
        var selectedWord: Span? = null,
        var translation: String = "",
        var databaseWordsLoaded: Boolean = false,
        var scanNewWords: Boolean = false,
    ) {
        fun toAbsolute(span: Span) : Span {
            return Span(firstCharIndex + span.start, firstCharIndex + span.end)
        }

        fun toLocal(absSpan: Span) = Span(absSpan.start - firstCharIndex, absSpan.end - firstCharIndex)
    }

    data class SelectedSentence(
        var paragraphIndex: Int = -1,
        var sentenceIndex: Int = -1,
        /**
         * Indicates whether the sentence has a selected word within.
         */
        var wordSelected: Boolean = false
    )

    private fun Span.toLocal(paragraphItem: ParagraphItem): Span {
        return Span(start - paragraphItem.firstCharIndex, end - paragraphItem.firstCharIndex)
    }

    fun selectHighlightedText() {
        val pos = highlightedTextPos
        val textView = highlightedTextView ?: return
        val item = items[pos]
        val span = Span(textView.selectionStart, textView.selectionEnd)
        item.selectedWord = span
        selectedWordPos = pos
        textView.clearFocus()
        notifyItemChanged(pos, PAYLOAD_WORD)
    }

    fun getText(pos: Int): String {
        return items[pos].original.toString()
    }

    fun getItemsText(range: IntRange): List<String> {
        return items.slice(range).map { it.original.toString() }
    }

    fun updateSavedWords(paragraphWords: List<List<WordState>>, start: Int) {
        val end = start + paragraphWords.size
        for (i in start..< end) {
            val pageItem = items[i]
            pageItem.savedWords.clear()
            pageItem.savedWords.addAll(paragraphWords[i - start])
            pageItem.databaseWordsLoaded = true
        }
        notifyItemRangeChanged(start, paragraphWords.size, PAYLOAD_INITIAL_DB_WORDS)
    }

    fun addSavedWords(paragraphWords: List<List<WordState>>, start: Int) {
        val end = start + paragraphWords.size
        for (i in start..< end) {
            val pageItem = items[i]
            pageItem.databaseWordsLoaded = true
            pageItem.scanNewWords = false
            val modified = pageItem.savedWords.addAll(paragraphWords[i - start])
            if (modified) notifyItemChanged(i)
        }
    }

    fun removeWord(wordIndexes: Set<Int>, id: Int) {
        newWords.removeAll { it.id == id }
        wordIndexes.map { pos ->
            val item = items[pos]
            val removed = item.savedWords.removeAll { it.dbId == id }
            if (removed) notifyItemChanged(pos)
        }
    }

    fun setScanNewWords() {
        items.forEach { it.scanNewWords = true }
    }

    fun removeWords(visibleItems: IntRange) {
        newWords.clear()
        visibleItems.map {
            val words = items[it].savedWords
            if (words.isNotEmpty()) {
                words.clear()
                notifyItemChanged(it)
            }
        }
        items.forEach {
            it.savedWords.clear()
            it.databaseWordsLoaded = false
        }
        initialWordsLoaded = false
    }

    private fun getCharListIndex(charPos: Int) = items.indexOfFirst { it.firstCharIndex + it.original.length > charPos }

    data class BgColorSpan(val start: Int, val end: Int, @ColorInt val color: Int)

    sealed interface TextClick {
        data class SavedWord(val word: WordState): TextClick
        data class Sentence(val word: String): TextClick
        data class Note(val item: EditNote): TextClick
        data class Overlap(val word: WordState, val note: EditNote): TextClick
    }

    companion object {
        private const val TRANSLATE_MENU_ITEM_ID = 3

        private const val SWIPE_THRESHOLD = 0.8
        private const val SWIPE_VELOCITY_THRESHOLD = 0.8

        private const val PAYLOAD_WORD = "update_word"
        private const val PAYLOAD_WORD_SENTENCE = "word_sentence"
        private const val PAYLOAD_INITIAL_DB_WORDS = "initial_db_words"

        private const val HIGHLIGHT_COLOR = 0x6633B5E5
        private const val NESTED_HIGHLIGHT_COLOR = 0xd0bcff
    }
}
