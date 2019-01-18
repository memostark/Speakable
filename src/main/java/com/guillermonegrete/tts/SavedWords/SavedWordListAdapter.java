package com.guillermonegrete.tts.SavedWords;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.guillermonegrete.tts.ProcessTextActivity;
import com.guillermonegrete.tts.R;
import com.guillermonegrete.tts.SaveWordDialogFragment;
import com.guillermonegrete.tts.ScreenTextService;
import com.guillermonegrete.tts.db.Words;
import com.guillermonegrete.tts.db.WordsDAO;
import com.guillermonegrete.tts.db.WordsDatabase;

import java.util.ArrayList;
import java.util.List;

import static com.guillermonegrete.tts.SaveWordDialogFragment.TAG_DIALOG_UPDATE_WORD;

public class SavedWordListAdapter extends RecyclerView.Adapter<SavedWordListAdapter.WordsViewHolder> {

    private LayoutInflater layoutInflater;
    private List<Words> wordsList;
    private Context context;

    private boolean multiSelect = false;
    private ArrayList<String> selectedItems = new ArrayList<>();

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
            }else {
                holder.notesText.setVisibility(View.GONE);
            }

        }

        holder.update();

    }

    @Override
    public int getItemCount() {
        if (wordsList == null) {
            return 0;
        } else {
            return wordsList.size();
        }
    }

    // Taken from: https://blog.teamtreehouse.com/contextual-action-bars-removing-items-recyclerview
    private ActionMode.Callback actionModeCallback =  new ActionMode.Callback() {
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
            switch (menuItem.getItemId()){
                case (R.id.action_menu_delete):
                    String to_remove="";
                    WordsDAO wordsDAO = WordsDatabase.getDatabase(context).wordsDAO();
                    //StringBuilder stringBuilder =
                    for (String intItem : selectedItems) {
                        wordsDAO.deleteWord(intItem);
                        to_remove += ", " + intItem;
                    }
                    Toast.makeText(context, "To delete: " + to_remove, Toast.LENGTH_SHORT).show();
                    break;
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

        void update(){
            if (selectedItems.contains(wordText.getText().toString())) {
                container.setBackgroundColor(Color.LTGRAY);
            } else {
                container.setBackgroundColor(Color.WHITE);
            }

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    ((AppCompatActivity)view.getContext()).startSupportActionMode(actionModeCallback);
                    selectItem(wordText.getText().toString());
                    return true;
                }
            });

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    selectItem(wordText.getText().toString());
                }
            });
        }

        void selectItem(String item){
            if (multiSelect) {
                if (selectedItems.contains(item)) {
                    selectedItems.remove(item);
                    container.setBackgroundColor(Color.WHITE);
                } else {
                    selectedItems.add(item);
                    container.setBackgroundColor(Color.LTGRAY);
                }
            } else {
                Toast.makeText(context, "Show more definitions", Toast.LENGTH_SHORT).show();
                Intent wiktionaryIntent = new Intent(context, ProcessTextActivity.class);
                wiktionaryIntent.putExtra("android.intent.extra.PROCESS_TEXT", wordText.getText().toString());
                wiktionaryIntent.setAction("WITH_FLAG");
                context.startActivity(wiktionaryIntent);
                /*DialogFragment dialogFragment = SaveWordDialogFragment.newInstance(
                        wordText.getText().toString(),
                        languageText.getText().toString(),
                        translationText.getText().toString());
                dialogFragment.show(((AppCompatActivity) context).getSupportFragmentManager(), TAG_DIALOG_UPDATE_WORD);*/
            }
        }
    }
}
