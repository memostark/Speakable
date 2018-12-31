package com.guillermonegrete.tts;


import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.guillermonegrete.tts.SavedWords.SavedWordListAdapter;
import com.guillermonegrete.tts.SavedWords.WordsViewModel;
import com.guillermonegrete.tts.db.Words;

import java.util.List;

public class SavedWordsFragment extends Fragment {

    private SavedWordListAdapter wordListAdapter;
    private WordsViewModel wordsViewModel;
    private Context context;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        wordListAdapter = new SavedWordListAdapter(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        final View fragment_layout = inflater.inflate(R.layout.fragment_saved_words, container, false);

        RecyclerView recyclerView = (RecyclerView) fragment_layout.findViewById(R.id.recyclerview_saved_words);
        recyclerView.setAdapter(wordListAdapter);
        // recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        fragment_layout.findViewById(R.id.new_word_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSaveDialog();
            }
        });
        return fragment_layout;
    }

    private void initData(){
        wordsViewModel = ViewModelProviders.of(this).get(WordsViewModel.class);
        wordsViewModel.getWordsList().observe(this, new Observer<List<Words>>() {
            @Override
            public void onChanged(@Nullable List<Words> movies) {
                wordListAdapter.setWordsList(movies);
            }
        });
    }

    public void removeData() {
        if (wordListAdapter != null) {
            wordsViewModel.deleteAll();
        }
    }

    private void showSaveDialog() {
        DialogFragment dialogFragment;
        dialogFragment = new SaveWordDialogFragment();
        dialogFragment.show(getActivity().getSupportFragmentManager(), "New word");
    }
}
