package com.guillermonegrete.tts.SavedWords;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.guillermonegrete.tts.R;
import com.guillermonegrete.tts.db.Words;

import java.util.ArrayList;
import java.util.List;

public class SavedWordListAdapter extends RecyclerView.Adapter<SavedWordListAdapter.WordsViewHolder> {

    private LayoutInflater layoutInflater;
    private List<Words> wordsList;
    private Context context;

    private boolean multiSelect = false;
    private ArrayList<Integer> selectedItems = new ArrayList<>();

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

        holder.update(position);

    }

    @Override
    public int getItemCount() {
        if (wordsList == null) {
            return 0;
        } else {
            return wordsList.size();
        }
    }

    private ActionMode.Callback actionModeCallback =  new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            multiSelect = true;
            menu.add("Delete");
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
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

    class WordsViewHolder extends RecyclerView.ViewHolder{
        private TextView wordText;
        private TextView languageText;
        private TextView translationText;
        private TextView notesText;
        private LinearLayout container;

        public WordsViewHolder(View itemView){
            super(itemView);

            wordText = itemView.findViewById(R.id.saved_word_text);
            languageText = itemView.findViewById(R.id.saved_word_lang_text);
            translationText = itemView.findViewById(R.id.saved_word_translation_text);
            notesText = itemView.findViewById(R.id.saved_word_notes_text);
            container = itemView.findViewById(R.id.saved_word_item_container);
        }

        void update(final Integer position){
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    ((AppCompatActivity)view.getContext()).startSupportActionMode(actionModeCallback);
                    selectItem(position);
                    return true;
                }
            });

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    selectItem(position);
                }
            });
        }

        void selectItem(Integer item){
            if (multiSelect) {
                if (selectedItems.contains(item)) {
                    // int item_index = selectedItems.indexOf(item);
                    selectedItems.remove(item);
                    container.setBackgroundColor(Color.WHITE);
                } else {
                    selectedItems.add(item);
                    container.setBackgroundColor(Color.LTGRAY);
                }
            }
        }
    }
}
