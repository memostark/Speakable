package com.guillermonegrete.tts.savedwords;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import android.app.SearchManager;
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

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.guillermonegrete.tts.R;
import com.guillermonegrete.tts.databinding.FragmentSavedWordsBinding;
import com.guillermonegrete.tts.db.Words;
import com.guillermonegrete.tts.textprocessing.TextInfoDialog;
import com.guillermonegrete.tts.ui.DifferentValuesAdapter;

import dagger.hilt.android.AndroidEntryPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@AndroidEntryPoint
public class SavedWordsFragment extends Fragment implements AdapterView.OnItemSelectedListener, SavedWordListAdapter.Listener {

    private SavedWordListAdapter wordListAdapter;
    private SavedWordsViewModel wordsViewModel;

    private FragmentSavedWordsBinding binding;

    private String language_filter;
    private String textFilter = "";

    private List<Words> words = new ArrayList<>();
    private List<CharSequence> languageIsos;
    private List<String> languageFullName;

    private List<String> allLangs;

    private static final String ALL_OPTION = "All";

    public SavedWordsFragment(){
        super(R.layout.fragment_saved_words);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        wordListAdapter = new SavedWordListAdapter(this);

        languageIsos = Arrays.asList(getResources().getTextArray(R.array.googleTranslateLanguagesValue));
        languageFullName = Arrays.asList(getResources().getStringArray(R.array.googleTranslateLanguagesArray));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentSavedWordsBinding.bind(view);

        RecyclerView wordsList = binding.recyclerviewSavedWords;
        wordsList.setAdapter(wordListAdapter);
        wordsList.addItemDecoration(new DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL));
        wordsList.setLayoutManager(new LinearLayoutManager(requireContext()));

        setUpItemTouchHelper(wordsList);

        initData();
        setupSearch(binding.searchWords);
        createMenu();
    }

    private void setupSearch(SearchView searchWords) {
        var searchManager = (SearchManager) requireContext().getSystemService(Context.SEARCH_SERVICE);
        searchWords.setSearchableInfo(searchManager.getSearchableInfo(requireActivity().getComponentName()));
        searchWords.setMaxWidth(Integer.MAX_VALUE);

        searchWords.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                textFilter = newText;
                filterWords();
                return true;
            }
        });
    }

    @Override
    public void onDestroyView() {
        binding.recyclerviewSavedWords.setAdapter(null);
        binding = null;
        super.onDestroyView();
    }

    private void initData(){
        wordsViewModel = new ViewModelProvider(this).get(SavedWordsViewModel.class);

        wordsViewModel.getLanguagesList().observe(getViewLifecycleOwner(), languages -> {

            var spinnerItems = new ArrayList<String>();
            for(String lang: languages){
                int index = languageIsos.indexOf(lang);
                var newLang = index != -1 ? String.format("%s (%s)", languageFullName.get(index), lang) : lang;
                spinnerItems.add(newLang);
            }

            spinnerItems.add(0, ALL_OPTION);
            allLangs = new ArrayList<>();
            allLangs.addAll(languages);
            allLangs.add(0, ALL_OPTION);
            var adapter = new DifferentValuesAdapter(requireContext(), android.R.layout.simple_spinner_item, allLangs, spinnerItems);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            Spinner spinnerLang = binding.selectLanguageSpinner;
            spinnerLang.setOnItemSelectedListener(SavedWordsFragment.this);
            spinnerLang.setAdapter(adapter);
        });

        wordsViewModel.getWordsList().observe(getViewLifecycleOwner(), wordsList -> {
            words = wordsList;
            // TODO filtering should be done in view model
            filterWords();

        });
    }

    private void createMenu(){
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_saved_words_frag, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if(menuItem.getItemId() == R.id.add_word_menu_item){
                    showSaveDialog();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void showSaveDialog() {
        DialogFragment dialogFragment;
        dialogFragment = SaveWordDialogFragment.newInstance(null);
        dialogFragment.show(requireActivity().getSupportFragmentManager(), "New word");
    }

    private void setUpItemTouchHelper(RecyclerView recyclerView){
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
        mItemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
        language_filter = allLangs.get(pos);
        filterWords();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    // TODO filter in worker thread
    private void filterWords(){
        List<Words> newWords;
        if(language_filter == null || language_filter.equals(ALL_OPTION)) {
            newWords = words;
        } else {
            List<Words> filteredWords = new ArrayList<>();
            for (Words word : words) {
                if (language_filter.equals(word.lang.toLowerCase())) {
                    filteredWords.add(word);
                }
            }
            newWords = filteredWords;
        }

        wordListAdapter.setWordsList(newWords);
        wordListAdapter.getFilter().filter(textFilter);
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
                word,
                true
        );
        dialog.show(getChildFragmentManager(), "Text_info");
    }

    @Override
    public void onLongClick() {
        ((AppCompatActivity) requireActivity()).startSupportActionMode(wordListAdapter.actionModeCallback);
    }

    @Override
    public void onFiltered(boolean isListEmpty) {
        binding.noLinksMessage.setVisibility(isListEmpty ? View.VISIBLE : View.GONE);
    }
}
