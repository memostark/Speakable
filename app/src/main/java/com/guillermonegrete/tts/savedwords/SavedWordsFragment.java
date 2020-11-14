package com.guillermonegrete.tts.savedwords;


import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
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
import com.guillermonegrete.tts.textprocessing.TextInfoDialog;

import dagger.hilt.android.AndroidEntryPoint;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@AndroidEntryPoint
public class SavedWordsFragment extends Fragment implements AdapterView.OnItemSelectedListener, SavedWordListAdapter.Listener {

    private SavedWordListAdapter wordListAdapter;
    @Inject ViewModelProvider.Factory viewModelFactory;
    private SavedWordsViewModel wordsViewModel;
    private RecyclerView mRecyclerView;
    private Spinner spinnerLang;

    private String language_filter;

    private List<Words> words = new ArrayList<>();

    private static final String ALL_OPTION = "All";

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        wordListAdapter = new SavedWordListAdapter(context, this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        final View fragment_layout = inflater.inflate(R.layout.fragment_saved_words, container, false);

        mRecyclerView = fragment_layout.findViewById(R.id.recyclerview_saved_words);
        mRecyclerView.setAdapter(wordListAdapter);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        setUpItemTouchHelper();

        spinnerLang = fragment_layout.findViewById(R.id.select_language_spinner);

        fragment_layout.findViewById(R.id.new_word_button).setOnClickListener(view -> showSaveDialog());
        initData();
        return fragment_layout;
    }

    private void initData(){
        wordsViewModel = new ViewModelProvider(this, viewModelFactory).get(SavedWordsViewModel.class);

        wordsViewModel.getLanguagesList().observe(getViewLifecycleOwner(), languages -> {
            ArrayList<String> spinnerItems = new ArrayList<>(languages);
            spinnerItems.add(0, ALL_OPTION);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, spinnerItems);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerLang.setOnItemSelectedListener(SavedWordsFragment.this);
            spinnerLang.setAdapter(adapter);
        });

        wordsViewModel.getWordsList().observe(getViewLifecycleOwner(), wordsList -> {
            words = wordsList;
            // TODO filtering should be done in view model
            filterWords();

        });
    }

    private void showSaveDialog() {
        DialogFragment dialogFragment;
        dialogFragment = SaveWordDialogFragment.newInstance(null);
        dialogFragment.show(requireActivity().getSupportFragmentManager(), "New word");
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
                deleteIcon  = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete_black_24dp);
                if (deleteIcon != null) {
                    deleteIcon .setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                    // xMarkMargin = (int) MainActivity.this.getResources().getDimension(R.dimen.ic_clear_margin);
                    intrinsicWidth = deleteIcon.getIntrinsicWidth();
                    intrinsicHeight = deleteIcon.getIntrinsicHeight();
                    initiated = true;
                }
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                TextView saved_word_text = viewHolder.itemView.findViewById(R.id.saved_word_text);
                String word_text = saved_word_text.getText().toString();
                Toast.makeText(requireContext(), "Swiped word: " + word_text, Toast.LENGTH_SHORT).show();

                wordsViewModel.delete(word_text);
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
        if(language_filter == null || language_filter.equals(ALL_OPTION)) {
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

    @Override
    public void onDeleteWords(List<Words> words) {
        wordsViewModel.delete(words.toArray(new Words[0]));
    }

    @Override
    public void showTextInfoDialog(String text, Words word) {
        TextInfoDialog dialog = TextInfoDialog.newInstance(
                text,
                TextInfoDialog.NO_SERVICE,
                word
        );
        dialog.show(getChildFragmentManager(), "Text_info");
    }
}
