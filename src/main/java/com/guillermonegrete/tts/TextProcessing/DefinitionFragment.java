package com.guillermonegrete.tts.TextProcessing;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.guillermonegrete.tts.R;
import com.guillermonegrete.tts.WiktionaryListAdapter;
import com.guillermonegrete.tts.db.Words;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.fragment.app.Fragment;

public class DefinitionFragment extends Fragment {

    private Context mContext;

    private String wordExtra;
    private String definitionExtra;
    private String notesExtra;
    private String translationExtra;

    private static final String WORD_TEXT = "word_text";
    private static final String WORD_DEFINITION = "word_def";
    private static final String WORD_NOTES = "word_notes";
    private static final String WORD_TRANSLATION = "word_translation";

    private static WiktionaryListAdapter mAdapter;

    public static DefinitionFragment newInstance(Words foundWord, String translation, WiktionaryListAdapter adapter){
        DefinitionFragment fragment = new DefinitionFragment();

        Bundle args = new Bundle();
        if(foundWord != null){
            args.putString(WORD_TEXT, foundWord.word);
            args.putString(WORD_NOTES, foundWord.notes);
            args.putString(WORD_DEFINITION, foundWord.definition);
        }
        args.putString(WORD_TRANSLATION, translation);
        fragment.setArguments(args);

        mAdapter = adapter;

        return fragment;

    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        wordExtra = args.getString(WORD_TEXT);
        definitionExtra = args.getString(WORD_DEFINITION);
        notesExtra = args.getString(WORD_NOTES);
        translationExtra = args.getString(WORD_TRANSLATION);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View fragment_layout = inflater.inflate(R.layout.fragment_process_definition, container, false);



        if(wordExtra != null){ // Inside database
            TextView saved_definition = fragment_layout.findViewById(R.id.text_error_message);
            saved_definition.setVisibility(View.VISIBLE);
            saved_definition.setText(definitionExtra);
            TextView saved_notes = fragment_layout.findViewById(R.id.text_notes);
            saved_notes.setVisibility(View.VISIBLE);
            if (notesExtra != null) saved_notes.setText(notesExtra);
        }else if(mAdapter != null){ // Inside wiktionary
            ListView listView = fragment_layout.findViewById(R.id.listView_wiki);
            listView.setAdapter(mAdapter);
            return fragment_layout;
        } else { // Inside neither wiktionary nor database

            TextView mTextTranslation = fragment_layout.findViewById(R.id.text_error_message);
            mTextTranslation.setMovementMethod(new ScrollingMovementMethod());
            mTextTranslation.setVisibility(View.VISIBLE);
            mTextTranslation.setText(translationExtra);


        }
        return fragment_layout;
    }

}
