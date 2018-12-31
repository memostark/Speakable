package com.guillermonegrete.tts;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.guillermonegrete.tts.db.Words;
import com.guillermonegrete.tts.db.WordsDAO;
import com.guillermonegrete.tts.db.WordsDatabase;

public class SaveWordDialogFragment extends DialogFragment {
    private Context context;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View dialogue_layout = getActivity().getLayoutInflater().inflate(R.layout.new_word_dialog, null);
        final EditText wordEditText = (EditText) dialogue_layout.findViewById(R.id.new_word_edit);
        final EditText languageEditText = (EditText) dialogue_layout.findViewById(R.id.new_word_language_edit);
        final EditText translationEditText = (EditText) dialogue_layout.findViewById(R.id.new_translation_edit);

        builder.setView(dialogue_layout)
                .setTitle(getString(R.string.dialog_new_word_title))
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getActivity(), "Save word", Toast.LENGTH_SHORT).show();
                        saveWord(wordEditText.getText().toString(),
                                languageEditText.getText().toString(),
                                translationEditText.getText().toString());

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

    private void saveWord(String word, String language, String translation){
        if(TextUtils.isEmpty(word) | TextUtils.isEmpty(language) | TextUtils.isEmpty(translation)){
            return;
        }

        WordsDAO wordsDAO = WordsDatabase.getDatabase(context).wordsDAO();
        wordsDAO.insert(new Words(word, language, translation));

    }
}
