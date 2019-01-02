package com.guillermonegrete.tts;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.guillermonegrete.tts.db.Words;
import com.guillermonegrete.tts.db.WordsDAO;
import com.guillermonegrete.tts.db.WordsDatabase;

public class SaveWordDialogFragment extends DialogFragment {
    private Context context;
    private String wordExtra;
    private String languageExtra;
    private String translationExtra;

    private static final String EXTRA_WORD = "word";
    private static final String EXTRA_LANGUAGE= "language";
    private static final String EXTRA_TRANSLATION = "translation";

    public static SaveWordDialogFragment newInstance(String word, String language, String translation){
        SaveWordDialogFragment fragment = new SaveWordDialogFragment();

        Bundle args = new Bundle();
        args.putString(EXTRA_WORD, word);
        args.putString(EXTRA_LANGUAGE, language);
        args.putString(EXTRA_TRANSLATION, translation);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        wordExtra = args.getString(EXTRA_WORD);
        languageExtra = args.getString(EXTRA_LANGUAGE);
        translationExtra = args.getString(EXTRA_TRANSLATION);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View dialogue_layout = getActivity().getLayoutInflater().inflate(R.layout.new_word_dialog, null);
        final EditText wordEditText = (EditText) dialogue_layout.findViewById(R.id.new_word_edit);
        final EditText languageEditText = (EditText) dialogue_layout.findViewById(R.id.new_word_language_edit);
        final EditText translationEditText = (EditText) dialogue_layout.findViewById(R.id.new_translation_edit);
        final EditText notesEditText = (EditText) dialogue_layout.findViewById(R.id.new_notes_edit);

        if(wordExtra != null){
            wordEditText.setText(wordExtra);
            wordEditText.setSelection(wordExtra.length());
        }

        if(languageExtra != null){
            languageEditText.setText(languageExtra);
            languageEditText.setSelection(languageExtra.length());
        }
        if(translationExtra != null){
            translationEditText.setText(translationExtra);
            translationEditText.setSelection(translationExtra.length());
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

        WordsDAO wordsDAO = WordsDatabase.getDatabase(context).wordsDAO();
        wordsDAO.insert(word_entry);
        Toast.makeText(getActivity(), "Word saved", Toast.LENGTH_SHORT).show();
        ImageButton saveIcon = (ImageButton) getActivity().findViewById(R.id.save_icon);
        saveIcon.setImageResource(R.drawable.ic_bookmark_black_24dp);

    }
}
