package com.guillermonegrete.tts.savedwords;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.guillermonegrete.tts.R;
import com.guillermonegrete.tts.databinding.NewWordDialogBinding;
import com.guillermonegrete.tts.db.Words;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SaveWordDialogFragment extends DialogFragment {

    private NewWordDialogBinding binding;

    private SaveWordDialogViewModel saveWordViewModel;

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
        saveWordViewModel = new ViewModelProvider(this).get(SaveWordDialogViewModel.class);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        binding = NewWordDialogBinding.inflate(requireActivity().getLayoutInflater());
        final EditText wordEditText = binding.newWordEdit;
        final EditText languageEditText = binding.newWordLanguageEdit;
        final EditText translationEditText = binding.newTranslationEdit;
        final EditText notesEditText = binding.newNotesEdit;

        if(wordItem != null) {
            wordEditText.setText(wordItem.word);
            wordEditText.setSelection(wordItem.word.length());
            languageEditText.setText(wordItem.lang);
            translationEditText.setText(wordItem.definition);
            notesEditText.setText(wordItem.notes);
        }


        builder.setView(binding.getRoot())
                .setTitle(getString(R.string.dialog_new_word_title))
                .setPositiveButton(R.string.save, (dialog, which) -> {})
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        return builder.create();
    }

    @Override
    public void onResume() {
        super.onResume();

        // We need to setup the positive button listener like this to avoid the dialog automatically closing when clicked
        // The dialog sometimes closed before the async database operations finished.
        // More info about avoiding closing the dialog here: https://stackoverflow.com/a/15619098/10244759
        final AlertDialog dialog = (AlertDialog) getDialog();
        if(dialog != null){
            final EditText wordEditText = binding.newWordEdit;
            final EditText languageEditText = binding.newWordLanguageEdit;
            final EditText translationEditText = binding.newTranslationEdit;
            final EditText notesEditText = binding.newNotesEdit;

            Button positiveButton = (Button) dialog.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> saveWord(wordEditText.getText().toString(),
                    languageEditText.getText().toString(),
                    translationEditText.getText().toString(),
                    notesEditText.getText().toString())
            );
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // Setting up here instead of onViewCreated() because that method is not called for DialogFragments
        saveWordViewModel.getUpdate().observe(this, resultType -> {
            if(resultType instanceof ResultType.Update){
                Toast.makeText(getActivity(), "Word updated", Toast.LENGTH_SHORT).show();
            } else if(resultType instanceof ResultType.Insert){
                ResultType.Insert result = (ResultType.Insert) resultType;
                Toast.makeText(getActivity(), "New word saved", Toast.LENGTH_SHORT).show();

                Fragment fragment = getParentFragment();
                if(fragment instanceof Callback)
                    ((Callback) fragment).onWordSaved(result.getWord());
            }
            if(getDialog() != null) getDialog().dismiss();
        });

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void saveWord(String word, String language, String translation, String notes){

        Words word_entry = new Words(word, language, translation);
        if(!TextUtils.isEmpty(notes)) word_entry.notes = notes;

        if(TAG_DIALOG_UPDATE_WORD.equals(getTag())){
            saveWordViewModel.update(word_entry);
        }else{
            saveWordViewModel.save(word_entry);
        }
    }

    public interface Callback{
        void onWordSaved(Words word);
    }
}
