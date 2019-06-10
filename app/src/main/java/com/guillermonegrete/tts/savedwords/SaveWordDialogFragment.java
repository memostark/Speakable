package com.guillermonegrete.tts.savedwords;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.guillermonegrete.tts.R;
import com.guillermonegrete.tts.db.Words;
import com.guillermonegrete.tts.db.WordsDAO;
import com.guillermonegrete.tts.db.WordsDatabase;

public class SaveWordDialogFragment extends DialogFragment {
    private Context context;

    private Words wordItem;
    private static final String EXTRA_WORD_OBJECT = "wordObject";


    public static final String TAG_DIALOG_UPDATE_WORD = "dialog_update_word";


    public static SaveWordDialogFragment newInstance(Words word){
        SaveWordDialogFragment fragment = new SaveWordDialogFragment();

        Bundle args = new Bundle();
        args.putParcelable(EXTRA_WORD_OBJECT, word);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            wordItem = args.getParcelable(EXTRA_WORD_OBJECT);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View dialogue_layout = getActivity().getLayoutInflater().inflate(R.layout.new_word_dialog, null);
        final EditText wordEditText = dialogue_layout.findViewById(R.id.new_word_edit);
        final EditText languageEditText = dialogue_layout.findViewById(R.id.new_word_language_edit);
        final EditText translationEditText = dialogue_layout.findViewById(R.id.new_translation_edit);
        final EditText notesEditText = dialogue_layout.findViewById(R.id.new_notes_edit);

        if(wordItem != null) {
            wordEditText.setText(wordItem.word);
            wordEditText.setSelection(wordItem.word.length());
            languageEditText.setText(wordItem.lang);
            translationEditText.setText(wordItem.definition);
            notesEditText.setText(wordItem.notes);
        }


        builder.setView(dialogue_layout)
                .setTitle(getString(R.string.dialog_new_word_title))
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveWord(wordEditText.getText().toString(),
                                languageEditText.getText().toString(),
                                translationEditText.getText().toString(),
                                notesEditText.getText().toString());

                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        return builder.create();
    }

    private void saveWord(String word, String language, String translation, String notes){
        if(TextUtils.isEmpty(word) | TextUtils.isEmpty(language) | TextUtils.isEmpty(translation)){
            return;
        }

        Words word_entry = new Words(word, language, translation);
        if(!TextUtils.isEmpty(notes)) word_entry.notes = notes;

        // TODO remove database logic from here
        WordsDAO wordsDAO = WordsDatabase.getDatabase(context).wordsDAO();

        if(TAG_DIALOG_UPDATE_WORD.equals(getTag())){

            Toast.makeText(getActivity(), "Word updated", Toast.LENGTH_SHORT).show();
            // TODO allow users to update other fields
            Words wordToUpdate = wordsDAO.findWord(wordItem.word);
            if (wordToUpdate != null)  {
                if (wordToUpdate.notes == null){
                    wordToUpdate.notes = notes;
                    wordsDAO.update(wordToUpdate);
                } else if (!wordToUpdate.notes.equals(notes)) {
                    wordToUpdate.notes = notes;
                    wordsDAO.update(wordToUpdate);
                }
            }
        }else{
            wordsDAO.insert(word_entry);
            Toast.makeText(getActivity(), "New word saved", Toast.LENGTH_SHORT).show();

            Activity activity = getActivity();
            if(activity instanceof Callback)
                ((Callback)activity).onWordSaved(word_entry);
        }

    }

    public interface Callback{
        void onWordSaved(Words word);
    }
}
