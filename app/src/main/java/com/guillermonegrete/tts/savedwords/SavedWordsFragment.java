package com.guillermonegrete.tts.savedwords;


import static androidx.compose.runtime.SnapshotStateKt.structuralEqualityPolicy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.compose.runtime.MutableState;
import androidx.compose.runtime.SnapshotStateKt;
import androidx.core.view.MenuProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
import android.widget.Toast;

import com.guillermonegrete.tts.R;
import com.guillermonegrete.tts.databinding.FragmentSavedWordsBinding;
import com.guillermonegrete.tts.db.Words;
import com.guillermonegrete.tts.main.MainActivity;
import com.guillermonegrete.tts.textprocessing.TextInfoDialog;
import com.guillermonegrete.tts.ui.DifferentValuesAdapter;

import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@AndroidEntryPoint
public class SavedWordsFragment extends Fragment implements SavedWordListAdapter.Listener {

    private SavedWordListAdapter wordListAdapter;
    private SavedWordsViewModel wordsViewModel;
    private SaveWordDialogViewModel saveWordViewModel;

    private FragmentSavedWordsBinding binding;

    private String language_filter;
    private String textFilter = "";

    private List<Words> words = new ArrayList<>();
    private List<CharSequence> languageIsos;
    private List<String> languageFullName;

    private List<String> allLangs;

    private final MutableState<Boolean> editDialogShown = SnapshotStateKt.mutableStateOf(false, structuralEqualityPolicy());

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

        var wordsList = binding.recyclerviewSavedWords;
        wordsList.setAdapter(wordListAdapter);
        wordsList.addItemDecoration(new DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL));
        wordsList.setLayoutManager(new LinearLayoutManager(requireContext()));

        setUpItemTouchHelper(wordsList);

        initData();
        setInsetListener();
        setupSearch(binding.searchWords);
        setEditDialog();
        createMenu();
    }

    private void setInsetListener() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerviewSavedWords, (v, windowInsets) -> {
            var insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(insets.left, v.getPaddingTop(), insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void setEditDialog() {
        saveWordViewModel.getUpdate().observe(getViewLifecycleOwner(), resultType -> editDialogShown.setValue(false));

        var deleteDialogVisible = SnapshotStateKt.mutableStateOf(false, structuralEqualityPolicy());
        var newWord = new Words("", "", "");
        SaveWordDialogViewModelKt.setContent(binding.composeRoot, newWord, false, editDialogShown, deleteDialogVisible, word ->  {
            saveWordViewModel.save(word);
            return Unit.INSTANCE;
        });
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
        saveWordViewModel = new ViewModelProvider(this).get(SaveWordDialogViewModel.class);

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

            var spinnerLang = binding.selectLanguageSpinner;
            spinnerLang.setText(ALL_OPTION, false);
            spinnerLang.setOnItemClickListener((parent, view, position, id) -> {
                language_filter = allLangs.get(position);
                filterWords();
            });
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
                    editDialogShown.setValue(true);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void setUpItemTouchHelper(RecyclerView recyclerView){
        var simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            Drawable deleteIcon;
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
                var pos = viewHolder.getAdapterPosition();
                var word = wordListAdapter.getItem(pos);
                Toast.makeText(requireContext(), "Swiped word: " + word.word, Toast.LENGTH_SHORT).show();

                wordsViewModel.delete(word);
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
        if (isListEmpty && requireActivity() instanceof MainActivity)
            ((MainActivity) requireActivity()).showBottomBar();
    }
}
