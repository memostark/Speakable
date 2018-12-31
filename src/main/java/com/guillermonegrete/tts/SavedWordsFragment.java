package com.guillermonegrete.tts;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SavedWordsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        final View fragment_layout = inflater.inflate(R.layout.fragment_saved_words, container, false);

        fragment_layout.findViewById(R.id.new_word_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSaveDialog();
            }
        });
        return fragment_layout;
    }

    private void showSaveDialog() {
        DialogFragment dialogFragment;
        dialogFragment = new SaveWordDialogFragment();
        dialogFragment.show(getActivity().getSupportFragmentManager(), "New word");
    }
}
