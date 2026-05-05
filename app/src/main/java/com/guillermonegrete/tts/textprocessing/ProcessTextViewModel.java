package com.guillermonegrete.tts.textprocessing;


import static com.guillermonegrete.tts.textprocessing.TextInfoScreenKt.NOT_SAVED_ID;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.guillermonegrete.tts.common.models.Span;
import com.guillermonegrete.tts.common.models.UIKt;
import com.guillermonegrete.tts.common.models.WordUI;
import com.guillermonegrete.tts.customtts.CustomTTS;
import com.guillermonegrete.tts.customtts.interactors.PlayTTS;
import com.guillermonegrete.tts.MainThread;
import com.guillermonegrete.tts.data.DialogStateList;
import com.guillermonegrete.tts.data.Translation;
import com.guillermonegrete.tts.data.source.WordRepositorySource;
import com.guillermonegrete.tts.db.ExternalLink;
import com.guillermonegrete.tts.importtext.visualize.model.SplitPageSpan;
import com.guillermonegrete.tts.main.SettingsFragment;
import com.guillermonegrete.tts.textprocessing.domain.interactors.DeleteWord;
import com.guillermonegrete.tts.textprocessing.domain.interactors.GetDictionaryEntry;
import com.guillermonegrete.tts.textprocessing.domain.interactors.GetDictionaryEntryInteractor;
import com.guillermonegrete.tts.textprocessing.domain.interactors.GetExternalLink;
import com.guillermonegrete.tts.textprocessing.domain.interactors.GetLayout;
import com.guillermonegrete.tts.textprocessing.domain.interactors.GetLayoutInteractor;
import com.guillermonegrete.tts.textprocessing.domain.model.GetLayoutResult;
import com.guillermonegrete.tts.textprocessing.domain.model.StatusTTS;
import com.guillermonegrete.tts.textprocessing.domain.model.WikiItem;
import com.guillermonegrete.tts.data.source.DictionaryRepository;
import com.guillermonegrete.tts.db.Words;

import com.guillermonegrete.tts.main.domain.interactors.GetLangAndTranslation;
import com.guillermonegrete.tts.utils.EspressoIdlingResource;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlin.Unit;

@HiltViewModel
public class ProcessTextViewModel extends ViewModel implements ProcessTextContract.Presenter{

    private ProcessTextContract.View mView;
    private final ExecutorService executorService;
    private final MainThread mMainThread;
    private final WordRepositorySource mRepository;
    private final DictionaryRepository dictionaryRepository;
    private final SharedPreferences sharedPreferences;
    private final CustomTTS customTTS;
    private final GetLangAndTranslation getTranslationInteractor;
    private final GetExternalLink getExternalLink;

    private final MutableLiveData<DialogStateList<ExternalLink>> wordLinks;
    private final MutableLiveData<Integer> selectedLink;

    private Words foundWord;
    @Nullable
    private Translation currentTranslation;

    private boolean isPlaying;
    private boolean isAvailable;
    private boolean hasTranslation;

    private boolean viewIsActive = false;

    private final MutableLiveData<GetLayoutResult> layoutResult = new MutableLiveData<>();
    private final MutableLiveData<StatusTTS> ttsStatus = new MutableLiveData<>();
    private final MutableLiveData<SentenceDialogUIState> sentenceState;
    private final MutableLiveData<SentenceEditingUIState> editDialogs;

    @Inject
    ProcessTextViewModel(
            ExecutorService executor,
            MainThread mainThread,
            WordRepositorySource repository,
            DictionaryRepository dictRepository,
            SharedPreferences sharedPreferences,
            CustomTTS customTTS,
            GetLangAndTranslation getTranslationInteractor,
            GetExternalLink getExternalLink,
            SavedStateHandle savedStateHandle){
        executorService = executor;
        mMainThread = mainThread;
        mRepository = repository;
        dictionaryRepository = dictRepository;
        this.sharedPreferences = sharedPreferences;
        this.customTTS = customTTS;
        this.getTranslationInteractor = getTranslationInteractor;
        this.getExternalLink = getExternalLink;

        wordLinks = savedStateHandle.getLiveData("wordLinks");
        selectedLink = savedStateHandle.getLiveData("selectedLink");
        sentenceState = savedStateHandle.getLiveData("sentenceState");
        editDialogs = savedStateHandle.getLiveData("editDialogs");

        isPlaying = false;
        isAvailable = true;

        hasTranslation = false;
    }

    @Override
    public void start(Words word) {
        getDictionaryEntry(word, true);
    }

    @Override
    public void startWithService(String selectedText, String languageFrom, String languageTo) {
        mView.startService();
        getLayout(selectedText, languageFrom, languageTo);
    }

    @Override
    public void getLayout(String text, String languageFrom, String languageTo) {
        hasTranslation = true;
        viewIsActive = true;

        EspressoIdlingResource.increment();

        var interactor = new GetLayout(executorService, mMainThread, new GetLayoutInteractor.Callback() {
            @Override
            public void onLayoutDetermined(Words word, ProcessTextLayoutType layoutType) {
                foundWord = word;
                checkTTSInitialization();
                getExternalLinks(word.lang);

                layoutResult.setValue(new GetLayoutResult.WordSuccess(layoutType, word));

                EspressoIdlingResource.decrement();
            }

            @Override
            public void onSentenceLayout(Translation translation) {
                currentTranslation = translation;
                foundWord = new Words(text, translation.getSrc(), translation.getTranslatedText());
                checkTTSInitialization();

                layoutResult.setValue(new GetLayoutResult.Sentence(translation));

                EspressoIdlingResource.decrement();
            }

            @Override
            public void onDictionaryLayoutDetermined(Words word, List<WikiItem> items) {
                foundWord = word;
                checkTTSInitialization();
                getExternalLinks(word.lang);
                layoutResult.setValue(new GetLayoutResult.DictionarySuccess(word, items));

                EspressoIdlingResource.decrement();
            }

            @Override
            public void onTranslationError(Exception error) {
                hasTranslation = false;
                layoutResult.setValue(new GetLayoutResult.Error(error));

                EspressoIdlingResource.decrement();
            }
        }, mRepository, dictionaryRepository, getTranslationInteractor, text, languageFrom, languageTo);

        interactor.execute();

    }

    @Override
    public LiveData<Words> wordStream(String text, String languageFrom) {
        return mRepository.getLocalWord(text, languageFrom);
    }

    public void setSelectedWord(String word, String languageFrom, String languageTo, Span span) {

        executorService.execute(() ->
            mRepository.getWordLanguageInfo(word, languageFrom, languageTo, new WordRepositorySource.GetWordRepositoryCallback() {
                @Override
                public void onLocalWordLoaded(Words word) {
                    var wordState = new WordState(UIKt.toUI(word), word.id, span);
                    sentenceState.postValue(new SentenceDialogUIState.Builder(getSentenceState()).selectedWord(wordState).build());
                }

                @Override
                public void onLocalWordNotAvailable() {}

                @Override
                public void onRemoteWordLoaded(Words word) {
                    var wordState = new WordState(UIKt.toUI(word), NOT_SAVED_ID, span);
                    sentenceState.postValue(new SentenceDialogUIState.Builder(getSentenceState()).selectedWord(wordState).build());
                }

                @Override
                public void onDataNotAvailable(Words emptyWord) {
                    var newState = new SentenceDialogUIState.Builder(getSentenceState()).selectedWord(null).hasError(emptyWord.toString()).build();
                    sentenceState.postValue(newState);
                }
            })
        );
    }

    /**
     * This method is used to get the dictionary data when the the translation of the word is already known.
     * @param word Contains the information of the word (language, translation, etc)
     * @param isSaved Whether the word passed is saved in the database
     */
    @Override
    public void getDictionaryEntry(final Words word, boolean isSaved) {
        foundWord = word;
        hasTranslation = true;
        viewIsActive = true;

        EspressoIdlingResource.increment();

        checkTTSInitialization();

        var interactor = new GetDictionaryEntry(executorService, mMainThread, dictionaryRepository, word.word, new GetDictionaryEntryInteractor.Callback(){

            @Override
            public void onEntryNotAvailable() {
                getExternalLinks(word.lang);
                var layoutType= isSaved ? ProcessTextLayoutType.SAVED_WORD: ProcessTextLayoutType.WORD_TRANSLATION;
                layoutResult.setValue(new GetLayoutResult.WordSuccess(layoutType, word));
                if(!isAvailable) mView.showLanguageNotAvailable();

                EspressoIdlingResource.decrement();
            }

            @Override
            public void onDictionaryLayoutDetermined(@NotNull List<WikiItem> items) {
                getExternalLinks(word.lang);
                layoutResult.setValue(new GetLayoutResult.DictionarySuccess(word, items));
                if(!isAvailable) mView.showLanguageNotAvailable();

                EspressoIdlingResource.decrement();
            }


        });
        interactor.execute();
    }

    private void getExternalLinks(String language) {
        getExternalLink.invoke(language, links -> {
                    mView.setExternalDictionary(links);
                    if(!hasTranslation) mView.setTranslationErrorMessage();
        });
    }

    public void getExternalLinks(Words word) {
        executorService.execute(() -> {
            var links = getExternalLink.invoke(word.lang);
            for(var link: links) {
                link.link = link.link.replace("{q}", word.word);
            }
            // If out of index, default to the first item
            var selected = selectedLink.getValue();
            if(selected != null && selected >= links.size()) selectedLink.postValue(0);
            wordLinks.postValue(new DialogStateList.Success<>(links));
        });
    }

    /** @noinspection unchecked*/
    public void hideWordLinks() {
        wordLinks.setValue(DialogStateList.Empty.INSTANCE);
    }

    public void setWordLink(int position) {
        selectedLink.setValue(position);
    }

    @Override
    public void onClickDeleteWord(String word) {
        var interactor = new DeleteWord(executorService, mMainThread, mRepository, word);
        interactor.execute();
        var state = getSentenceState();
        var wordState = state.getSelectedWord();
        if (wordState != null) {
            var previous = wordState.getWord();
            var newWord = new WordUI(previous.getWord(), previous.getLang(), previous.getDefinition(), null);
            var newState = new WordState(newWord, NOT_SAVED_ID, wordState.getSpan());
            updateSelectedWord(newState);
        }
        stopEditing();
        mView.showWordDeleted();
    }

    @Override
    public void onClickReproduce(String text) {
        if(isPlaying){
            customTTS.stop();
            isPlaying = false;
            mView.showPlayIcon();
        }else if(isAvailable){
            mView.showLoadingTTS();
            var interactor = new PlayTTS(executorService, mMainThread, customTTS, ttsListener, text);
            interactor.execute();
        }
    }

    @Override
    public void onLanguageSpinnerChange(String languageFrom, final String languageTo) {
        EspressoIdlingResource.increment();
        hasTranslation = true;
        var detectedLang = languageFrom.equals("auto");

        getTranslationInteractor.invoke(foundWord.word, languageFrom, languageTo,
                translation -> {
                    currentTranslation = translation;
                    customTTS.initializeTTS(translation.getSrc(), ttsListener);
                    mView.updateTranslation(translation);
                    if (detectedLang) getExternalLink.invoke(translation.getSrc(), links -> mView.updateExternalLinks(links));
                    EspressoIdlingResource.decrement();
                    return Unit.INSTANCE;
                },
                exception -> {
                    hasTranslation = false;
                    mView.setTranslationErrorMessage();
                    EspressoIdlingResource.decrement();
                    return Unit.INSTANCE;
                });

        if (!detectedLang) getExternalLink.invoke(languageFrom, links -> mView.updateExternalLinks(links));
    }

    @Override
    public void setView(ProcessTextContract.View view) {
        mView = view;
    }

    @Override
    public void start() {
        viewIsActive = true;
    }

    @Override
    public void pause() {
        onViewInactive();
    }

    @Override
    public void stop() {
        onViewInactive();
    }

    @Override
    public void destroy() {
        onViewInactive();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        customTTS.removeListener(ttsListener);
        executorService.shutdown();
        executorService.shutdownNow();
    }

    private void onViewInactive(){
        viewIsActive = false;

        if(isPlaying) {
            customTTS.stop();
            isPlaying = false;
        }
    }

    private void checkTTSInitialization(){
        isAvailable = true;
        String lang = foundWord.lang;
        EspressoIdlingResource.increment();
        customTTS.initializeTTS(lang, ttsListener);
    }

    private Boolean getAutoTTSPreference(){
        return sharedPreferences.getBoolean(SettingsFragment.PREF_AUTO_TEST_SWITCH, true);
    }

    private final CustomTTS.Listener ttsListener = new CustomTTS.Listener() {

        @Override
        public void onEngineReady() {
            isAvailable = true;
            ttsStatus.postValue(StatusTTS.LanguageReady.INSTANCE);
            var autoPlay = getAutoTTSPreference();

            if(autoPlay && viewIsActive) {
                onClickReproduce(foundWord.word);
            }
            else {
                mView.showPlayIcon();
                EspressoIdlingResource.decrement();
            }
        }

        @Override
        public void onLanguageUnavailable() {
            isPlaying = false;
            isAvailable = false;
            mMainThread.post(() -> {
                ttsStatus.setValue(StatusTTS.Unavailable.INSTANCE);
                mView.showLanguageNotAvailable();
                EspressoIdlingResource.decrement();
            });
        }

        @Override
        public void onSpeakStart() {
            isPlaying = true;

            mMainThread.post(() -> mView.showStopIcon());
        }

        @Override
        public void onSpeakDone() {
            isPlaying = false;
            mMainThread.post(() -> {
                mView.showPlayIcon();
                EspressoIdlingResource.decrement();
            });
        }

        @Override
        public void onError() {
            isPlaying = false;
            mMainThread.post(() -> {
                mView.showErrorPlayingAudio();
                EspressoIdlingResource.decrement();
            });
        }
    };

    public LiveData<GetLayoutResult> getLayoutResult() {
        return layoutResult;
    }

    public LiveData<StatusTTS> getStatusTTS() {
        return ttsStatus;
    }

    public @NonNull LiveData<DialogStateList<ExternalLink>> getWordLinks() {
        return wordLinks;
    }

    public @NonNull LiveData<Integer> getSelectedLink() {
        return selectedLink;
    }

    public LiveData<SentenceDialogUIState> getSentenceUIState() {
        return sentenceState;
    }

    public @NonNull MutableLiveData<SentenceEditingUIState> getEditDialogs() {
        return editDialogs;
    }

    public void findSelectedSentence(int charIndex) {
        var translation = currentTranslation;
        if (translation == null) return;
        var state = getSentenceState();

        int start = 0;
        int originStart = 0;

        for(var sentence: translation.getSentences()){
            var end = start + sentence.getTrans().length();
            var originalEnd = originStart + sentence.getOrig().length();
            if(charIndex < end) {
                // indicate UI to highlight this sentence
                var spans = new SplitPageSpan(new Span(originStart, originalEnd), new Span(start, end));
                var newSpans = Objects.equals(state.getHighlights(), spans) ? null : spans;
                sentenceState.setValue(new SentenceDialogUIState.Builder(state).highlights(newSpans).build());
                return;
            }
            start = end;
            originStart = originalEnd;
        }

    }

    public void findWord(int offset, Span newSpan, String languageFrom, String languageTo) {
        var state = getSentenceState();

        // If true the selected word was tapped, unselect
        var word = state.getSelectedWord();
        if (word != null) {
            var span = word.getSpan();
            if (span != null && span.inside(offset)) {
                sentenceState.setValue(new SentenceDialogUIState.Builder(state).selectedWord(null).build());
                return;
            }
        }

        if (currentTranslation == null) return;
        var text = currentTranslation.getOriginalText();

        var newWord = text.substring(newSpan.getStart(), newSpan.getEnd());
        setSelectedWord(newWord, languageFrom, languageTo, newSpan);
    }

    public void startEditing() {
        editDialogs.setValue(new SentenceEditingUIState(true));
    }

    public void stopEditing() {
        editDialogs.setValue(new SentenceEditingUIState(false));
    }

    public void  setDeleteSate(Boolean isShown) {
        editDialogs.setValue(new SentenceEditingUIState(true, isShown));
    }

    public void updateSelectedWord(WordState state) {
        sentenceState.setValue(new SentenceDialogUIState.Builder(getSentenceState()).selectedWord(state).build());
    }

    public void errorShown() {
        sentenceState.postValue(new SentenceDialogUIState.Builder(getSentenceState()).hasError(null).build());
    }

    private @NonNull SentenceDialogUIState getSentenceState() {
        var state = sentenceState.getValue();
        if (state == null) state = new SentenceDialogUIState();
        return state;
    }
}
