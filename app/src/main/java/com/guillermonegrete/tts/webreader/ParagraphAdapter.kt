package com.guillermonegrete.tts.webreader

import android.annotation.SuppressLint
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.guillermonegrete.tts.R
import com.guillermonegrete.tts.common.models.Span
import com.guillermonegrete.tts.databinding.ParagraphExpandedItemBinding
import com.guillermonegrete.tts.databinding.ParagraphItemBinding
import com.guillermonegrete.tts.utils.findWordForRightHanded
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ParagraphAdapter(
    val items: List<ParagraphItem>,
    val viewModel: WebReaderViewModel,
    val onSentenceSelected: () -> Unit,
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var expandedItemPos = -1
        private set

    var isLoading = false

    val selectedSentence = SelectedSentence()
    var selectedWordPos = -1

    private val _sentenceClicked = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1,
        BufferOverflow.DROP_OLDEST
    )
    val sentenceClicked = _sentenceClicked.asSharedFlow()

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

    fun updateTranslation(translation: String){
        items[expandedItemPos].translation = translation
    }

    fun updateExpanded(){
        notifyItemChanged(expandedItemPos)
    }

    @SuppressLint("ClickableViewAccessibility")
    inner class ViewHolder(val binding: ParagraphItemBinding): RecyclerView.ViewHolder(binding.root){

        init {
            // Because of a bug when having a TextView inside a CoordinatorLayout, the paragraph TextView has to have width equals to wrap_content so its text can be selectable.
            // Using match_parent the text can't be selected
            with(binding){
                val detector = GestureDetectorCompat(itemView.context, MyGestureListener())
                paragraph.setOnTouchListener { _, event ->
                    detector.onTouchEvent(event)
                }
            }
        }

        private fun findSentence(offset: Int): Int {
            val item = items[adapterPosition]
            item.indexes.forEachIndexed { index, span ->
                if(offset in span.start..span.end) return index
            }
            return -1
        }

        fun bind(item: ParagraphItem){
            binding.paragraph.text = item.original
            if(item.selectedIndex != -1){
                val span = item.indexes[item.selectedIndex]
                binding.paragraph.setHighlightedText(span.start, span.end)
                val wordSpan = item.selectedWord
                if(wordSpan != null) binding.paragraph.addHighlightedText(wordSpan.start, wordSpan.end)
            } else {
                val span = item.selectedWord
                if(span != null) binding.paragraph.setHighlightedText(span.start, span.end)
            }
        }

        private inner class MyGestureListener : GestureDetector.SimpleOnGestureListener() {

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val offset = binding.paragraph.getOffsetForPosition(e.x, e.y)
                val wordSpan = binding.paragraph.findWordForRightHanded(offset)
                val clickedWord = binding.paragraph.text.substring(wordSpan.start, wordSpan.end)

                // If a highlighted sentence was tapped just unselect it and don't select a word
                val item = items[adapterPosition]
                if(item.selectedIndex != -1) {
                    val span = item.indexes[item.selectedIndex]
                    if(offset in span.start..span.end) {
                        item.selectedWord = wordSpan
                        _sentenceClicked.tryEmit(clickedWord)
                        return super.onSingleTapConfirmed(e)
                    }
                }

                if(clickedWord.isNotEmpty()) {
                    viewModel.translateText(clickedWord)
                    unselectSentence()
                    unselectWord()

                    // Select new word
                    item.selectedWord = wordSpan
                    selectedWordPos = adapterPosition
                    notifyItemChanged(adapterPosition)
                }
                return super.onSingleTapConfirmed(e)
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                setSentenceSelected(e)
                return super.onDoubleTap(e)
            }
        }

        private fun setSentenceSelected(e: MotionEvent) {
            unselectSentence()
            removeExpanded()

            val offset = binding.paragraph.getOffsetForPosition(e.x, e.y)
            val index = findSentence(offset)
            selectSentence(adapterPosition, index)
            onSentenceSelected()
        }

        fun setExpanded(){
            val previousExpandedPos = expandedItemPos
            val isExpanded = adapterPosition == expandedItemPos
            expandedItemPos = if(isExpanded) -1 else adapterPosition
            notifyItemChanged(previousExpandedPos)
            notifyItemChanged(adapterPosition)
        }
    }

    fun unselectSentence(){
        val previousIndex = selectedSentence.paragraphIndex
        if(previousIndex != -1) {
            val previousItem = items[previousIndex]
            previousItem.selectedIndex = -1
            previousItem.selectedWord = null
            notifyItemChanged(previousIndex)
            selectedSentence.paragraphIndex = -1
            selectedSentence.sentenceIndex = -1
        }
    }

    private fun selectSentence(paragraphIndex: Int, sentenceIndex: Int){
        val item = items[paragraphIndex]
        item.selectedIndex = sentenceIndex

        notifyItemChanged(paragraphIndex)

        selectedSentence.paragraphIndex = paragraphIndex
        selectedSentence.sentenceIndex = sentenceIndex
    }

    fun unselectWord() {
        if(selectedWordPos != -1) {
            val previousItem = items[selectedWordPos]
            previousItem.selectedWord = null
            notifyItemChanged(selectedWordPos)
            selectedWordPos = -1
        }
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
                unselectSentence()
                selectSentence(paragraphIndex, newIndex)
            }
        }
    }

    fun previousSentence(){
        changeSentence(selectedSentence.sentenceIndex - 1)
    }

    @SuppressLint("ClickableViewAccessibility")
    inner class ExpandedViewHolder(val binding: ParagraphExpandedItemBinding): RecyclerView.ViewHolder(binding.root){

        private val noTranslationText: CharSequence = itemView.context.getText(R.string.paragraph_not_translated)

        init {
            with(binding){

                toggleParagraph.setOnClickListener {
                    removeExpanded()
                }

                var clickedWord: String? = null

                // Handles click
                paragraph.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        val offset = paragraph.getOffsetForPosition(event.x, event.y)
                        val possibleWord = findWordForRightHanded(paragraph.text.toString(), offset)
                        clickedWord = possibleWord.ifBlank { null }
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

        private fun findWordForRightHanded(
            str: String,
            offset: Int
        ): String { // when you touch ' ', this method returns left word.
            var newOffset = offset
            if (str.length == newOffset) {
                newOffset-- // without this code, you will get exception when touching end of the text
            }
            if (str[newOffset] == ' ') {
                newOffset--
            }
            var startIndex = newOffset
            var endIndex = newOffset
            try {
                while (Character.isLetterOrDigit(str[startIndex])) {
                    startIndex--
                }
            } catch (e: StringIndexOutOfBoundsException) {
                startIndex = 0
            }
            try {
                while (Character.isLetterOrDigit(str[endIndex])) {
                    endIndex++
                }
            } catch (e: StringIndexOutOfBoundsException) {
                endIndex = str.length
            }

            return str.substring(startIndex, endIndex)
        }

    }

    private fun removeExpanded(){
        val previousExpandedPos = expandedItemPos
        expandedItemPos = -1
        notifyItemChanged(previousExpandedPos)
    }

    private fun TextView.setHighlightedText(start: Int, end: Int){
        val text = SpannableString(this.text)

        //Remove previous
        text.getSpans(0, text.length, BackgroundColorSpan::class.java).map { span -> text.removeSpan(span) }

        text.setSpan(BackgroundColorSpan(0x6633B5E5), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        this.setText(text, TextView.BufferType.SPANNABLE)
    }

    private fun TextView.addHighlightedText(start: Int, end: Int){
        val text = SpannableString(this.text)

        val color = Color.argb(128, 255, 0, 0)
        text.setSpan(BackgroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        this.setText(text, TextView.BufferType.SPANNABLE)
    }

    data class ParagraphItem(
        val original: CharSequence,
        val indexes: List<Span>,
        val sentences: List<String>,
        var selectedIndex: Int = -1,
        var selectedWord: Span? = null,
        var translation: String = "",
    )

    data class SelectedSentence(var paragraphIndex: Int =-1, var sentenceIndex: Int = -1)
}
