package com.guillermonegrete.tts.savedwords;


import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.guillermonegrete.tts.R;
import com.guillermonegrete.tts.db.Words;
import com.guillermonegrete.tts.db.WordsDAO;
import com.guillermonegrete.tts.db.WordsDatabase;

import java.util.ArrayList;
import java.util.List;

public class SavedWordsFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    private SavedWordListAdapter wordListAdapter;
    private WordsViewModel wordsViewModel;
    private RecyclerView mRecyclerView;
    private Spinner spinnerLang;
    private Context context;

    private String language_filter;

    private List<Words> words;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        wordListAdapter = new SavedWordListAdapter(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        final View fragment_layout = inflater.inflate(R.layout.fragment_saved_words, container, false);

        mRecyclerView = fragment_layout.findViewById(R.id.recyclerview_saved_words);
        mRecyclerView.setAdapter(wordListAdapter);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));

        setUpItemTouchHelper();

        spinnerLang = fragment_layout.findViewById(R.id.select_language_spinner);

        fragment_layout.findViewById(R.id.new_word_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSaveDialog();
            }
        });
        initData();
        return fragment_layout;
    }

    private void initData(){
        wordsViewModel = ViewModelProviders.of(this).get(WordsViewModel.class);
        wordsViewModel.getLanguagesList().observe(getViewLifecycleOwner(), new Observer<List<String>>() {
            @Override
            public void onChanged(List<String> languages) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, languages);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerLang.setOnItemSelectedListener(SavedWordsFragment.this);
                spinnerLang.setAdapter(adapter);
            }
        });

        wordsViewModel.getWordsList().observe(this, new Observer<List<Words>>() {
            @Override
            public void onChanged(@Nullable List<Words> wordsList) {
                words = wordsList;
                filterWords();

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
        dialogFragment = SaveWordDialogFragment.newInstance(null);
        dialogFragment.show(getActivity().getSupportFragmentManager(), "New word");
    }

    private void setUpItemTouchHelper(){
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            Drawable deleteIcon ;
            Drawable backgroundColor;

            boolean initiated;
            private int intrinsicWidth;
            private int intrinsicHeight;

            private void init() {
                backgroundColor = new ColorDrawable(Color.RED);
                deleteIcon  = ContextCompat.getDrawable(context, R.drawable.ic_delete_black_24dp);
                deleteIcon .setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                // xMarkMargin = (int) MainActivity.this.getResources().getDimension(R.dimen.ic_clear_margin);
                intrinsicWidth = deleteIcon.getIntrinsicWidth();
                intrinsicHeight = deleteIcon.getIntrinsicHeight();
                initiated = true;
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                TextView saved_word_text = viewHolder.itemView.findViewById(R.id.saved_word_text);
                String word_text = saved_word_text.getText().toString();
                Toast.makeText(context, "Swiped word: " + word_text, Toast.LENGTH_SHORT).show();

                WordsDAO wordsDAO = WordsDatabase.getDatabase(context).wordsDAO();
                wordsDAO.deleteWord(word_text);

            }

            @Override
            public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (!initiated) {
                    init();
                }

                View itemView = viewHolder.itemView;
                int itemHeight = itemView.getBottom() - itemView.getTop();

                backgroundColor.setBounds(
                        itemView.getRight() + (int)dX,
                        itemView.getTop(),
                        itemView.getRight(),
                        itemView.getBottom()
                );
                backgroundColor.draw(canvas);

                // Calculate position of delete icon
                int iconTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
                int iconMargin = (itemHeight - intrinsicHeight) / 2;
                int iconLeft = itemView.getRight() - iconMargin - intrinsicWidth;
                int iconRight = itemView.getRight() - iconMargin;
                int iconBottom = iconTop + intrinsicHeight;

                // Draw the delete icon
                deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                deleteIcon.draw(canvas);

                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
        language_filter = (String) adapterView.getItemAtPosition(pos);
        filterWords();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    // TODO filter in worker thread
    private void filterWords(){
        if(language_filter == null || language_filter.equals("All")) {
            wordListAdapter.setWordsList(words);

        }else {

            List<Words> filtered_word = new ArrayList<>();
            for (Words word : words) {
                if (language_filter.equals(word.lang.toLowerCase())) {
                    filtered_word.add(word);
                }
            }

            wordListAdapter.setWordsList(filtered_word);
        }
    }
}
