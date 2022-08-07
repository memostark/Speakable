package com.guillermonegrete.tts.savedwords;

import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.guillermonegrete.tts.R;
import com.guillermonegrete.tts.db.Words;
import com.guillermonegrete.tts.utils.ColorUtilsKt;

import java.util.ArrayList;
import java.util.List;

public class SavedWordListAdapter extends RecyclerView.Adapter<SavedWordListAdapter.WordsViewHolder> {

    private List<Words> wordsList = new ArrayList<>();

    private boolean multiSelect = false;
    private final ArrayList<Words> selectedItems = new ArrayList<>();

    private final Listener listener;

    SavedWordListAdapter(Listener listener){
        this.listener = listener;
    }

    void setWordsList(@NonNull List<Words> wordsList){
        this.wordsList = wordsList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WordsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.saved_word_item, parent, false);
        return new WordsViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull WordsViewHolder holder, int position) {
        if (wordsList == null) return;

        final Words word = wordsList.get(position);
        if (word != null) holder.setWord(word);

        holder.update();

    }

    @Override
    public int getItemCount() {
        return wordsList.size();
    }

    // Taken from: https://blog.teamtreehouse.com/contextual-action-bars-removing-items-recyclerview
    private final ActionMode.Callback actionModeCallback =  new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            multiSelect = true;
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.action_mode_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            if (menuItem.getItemId() == R.id.action_menu_delete) {
                listener.onDeleteWords(selectedItems);
            }
            actionMode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            multiSelect = false;
            selectedItems.clear();
            notifyDataSetChanged();
        }
    };

    // TODO Implement View Binding
    class WordsViewHolder extends RecyclerView.ViewHolder{
        private final TextView wordText;
        private final TextView languageText;
        private final TextView translationText;
        private final TextView notesText;
        private final ConstraintLayout container;
        private Words word;
        private int color;

        WordsViewHolder(View itemView){
            super(itemView);

            wordText = itemView.findViewById(R.id.saved_word_text);
            languageText = itemView.findViewById(R.id.saved_word_lang_text);
            translationText = itemView.findViewById(R.id.saved_word_translation_text);
            notesText = itemView.findViewById(R.id.saved_word_notes_text);
            container = itemView.findViewById(R.id.saved_word_item_container);

            itemView.setOnLongClickListener(view -> {
                ((AppCompatActivity) view.getContext()).startSupportActionMode(actionModeCallback);
                selectItem(word);
                return true;
            });

            itemView.setOnClickListener(view -> selectItem(word));
            color = itemView.getResources().getColor(R.color.list_bg_variant);
        }

        void update(){
            if (selectedItems.contains(word)) {
                container.setBackgroundColor(Color.LTGRAY);
            } else {
                int defaultBGColor = ColorUtilsKt.getThemeColor(itemView.getContext(), R.attr.colorSurface);
                container.setBackgroundColor(getAdapterPosition() % 2 == 1 ? defaultBGColor : color);
            }
        }

        void setWord(Words word){
            this.word = word;
            wordText.setText(word.word);
            languageText.setText(word.lang);
            translationText.setText(word.definition);
            if (word.notes != null) {
                notesText.setVisibility(View.VISIBLE);
                notesText.setText(itemView.getContext().getString(R.string.notes_item_label, word.notes));
            }else {
                notesText.setVisibility(View.GONE);
            }
        }

        void selectItem(Words item){
            if (multiSelect) {
                if (selectedItems.contains(item)) {
                    selectedItems.remove(item);
                    int defaultBGColor = ColorUtilsKt.getThemeColor(itemView.getContext(), R.attr.colorSurface);
                    container.setBackgroundColor(defaultBGColor);
                } else {
                    selectedItems.add(item);
                    container.setBackgroundColor(Color.LTGRAY);
                }
            } else {
                listener.showTextInfoDialog(wordText.getText().toString(), word);
            }
        }
    }

    interface Listener{
        void onDeleteWords(List<Words> words);

        void showTextInfoDialog(String text, Words word);
    }
}
