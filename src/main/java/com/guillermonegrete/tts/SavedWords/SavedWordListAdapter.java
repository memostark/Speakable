package com.guillermonegrete.tts.SavedWords;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.guillermonegrete.tts.R;
import com.guillermonegrete.tts.db.Words;

import java.util.List;

public class SavedWordListAdapter extends RecyclerView.Adapter<SavedWordListAdapter.WordsViewHolder> {

    private LayoutInflater layoutInflater;
    private List<Words> wordsList;
    private Context context;

    public SavedWordListAdapter(Context context){
        this.layoutInflater = LayoutInflater.from(context);
        this.context = context;
    }

    public void setWordsList(List<Words> wordsList){
        this.wordsList = wordsList;
        notifyDataSetChanged();
    }

    @Override
    public WordsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View itemView = layoutInflater.inflate(R.layout.saved_word_item, parent, false);
        return new WordsViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(WordsViewHolder holder, int position) {
        if (wordsList == null) {
            return;
        }

        final Words word = wordsList.get(position);
        if (word != null) {
            holder.wordText.setText(word.word);
            holder.languageText.setText(word.lang);
            holder.translationText.setText(word.definition);
            if (word.notes != null) {
                holder.notesText.setVisibility(View.VISIBLE);
                holder.notesText.setText(word.notes);
            }
        }

    }

    @Override
    public int getItemCount() {
        if (wordsList == null) {
            return 0;
        } else {
            return wordsList.size();
        }
    }

    static class WordsViewHolder extends RecyclerView.ViewHolder{
        private TextView wordText;
        private TextView languageText;
        private TextView translationText;
        private TextView notesText;

        public WordsViewHolder(View itemView){
            super(itemView);

            wordText = (TextView) itemView.findViewById(R.id.saved_word_text);
            languageText = (TextView) itemView.findViewById(R.id.saved_word_lang_text);
            translationText = (TextView) itemView.findViewById(R.id.saved_word_translation_text);
            notesText = (TextView) itemView.findViewById(R.id.saved_word_notes_text);
        }
    }
}
