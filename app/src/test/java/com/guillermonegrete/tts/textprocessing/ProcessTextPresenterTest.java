package com.guillermonegrete.tts.textprocessing;

import android.content.SharedPreferences;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.guillermonegrete.tts.LiveDataTestUtilKt;
import com.guillermonegrete.tts.customtts.CustomTTS;
import com.guillermonegrete.tts.TestThreadExecutor;
import com.guillermonegrete.tts.data.source.ExternalLinksDataSource;
import com.guillermonegrete.tts.db.ExternalLink;
import com.guillermonegrete.tts.main.SettingsFragment;
import com.guillermonegrete.tts.main.domain.interactors.GetLangAndTranslation;
import com.guillermonegrete.tts.textprocessing.domain.model.GetLayoutResult;
import com.guillermonegrete.tts.textprocessing.domain.model.WikiItem;
import com.guillermonegrete.tts.data.source.DictionaryDataSource;
import com.guillermonegrete.tts.data.source.DictionaryRepository;
import com.guillermonegrete.tts.data.source.WordRepository;
import com.guillermonegrete.tts.data.source.WordRepositorySource;
import com.guillermonegrete.tts.data.source.local.DatabaseExternalLinksSource;
import com.guillermonegrete.tts.db.Words;
import com.guillermonegrete.tts.textprocessing.domain.model.WiktionaryItem;
import com.guillermonegrete.tts.threading.TestMainThread;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ProcessTextPresenterTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private ProcessTextContract.View view;
    @Mock private WordRepository wordRepository;
    @Mock private DictionaryRepository dictionaryRepository;
    @Mock private DatabaseExternalLinksSource linksRepository;
    @Mock private SharedPreferences sharedPreferences;
    @Mock private CustomTTS customTTS;
    @Mock private GetLangAndTranslation getTranslationInteractor;

    @Captor
    private ArgumentCaptor<WordRepositorySource.GetWordRepositoryCallback> getWordCallbackCaptor;

    @Captor
    private ArgumentCaptor<WordRepositorySource.GetTranslationCallback> getTranslationCallbackCaptor;

    @Captor
    private ArgumentCaptor<DictionaryDataSource.GetDefinitionCallback> getDefinitionCallbackCaptor;

    @Captor
    private ArgumentCaptor<ExternalLinksDataSource.Callback> getLinksCaptor;

    @Captor
    private ArgumentCaptor<CustomTTS.Listener> ttsListenerCaptor;

    @Captor
    private ArgumentCaptor<GetLangAndTranslation.Callback> getLangTransCallbackCaptor;

    private ProcessTextPresenter presenter;

    private final List<WikiItem> defaultDictionaryItems = Arrays.asList(
            new WiktionaryItem("First", "First header"),
            new WiktionaryItem("Second", "Second header")
    );

    private final List<ExternalLink> defaultLinksItems = Collections.singletonList(
            new ExternalLink("Dummy Site", "dummy-link", "en")
    );

    private static final String languageFrom = "auto";
    private static final String languageTo = "en";

    @Before
    public void setupPresenter(){
        MockitoAnnotations.openMocks(this);
        presenter = givenPresenter();
    }

    private ProcessTextPresenter givenPresenter(){
        var mainThread = new TestMainThread();
        var executor = new TestThreadExecutor();
        var presenter = new ProcessTextPresenter(executor, mainThread, wordRepository, dictionaryRepository, linksRepository, sharedPreferences, customTTS, getTranslationInteractor);
        presenter.setView(view);
        return presenter;
    }

    // region start presenter tests

    @Test
    public void when_start_with_word_then_dictionary_load_success(){
        var word = new Words("comida", "es", "food");
        presenter.start(word);

        loadDictionaryDefinition("comida");

        var expected = new GetLayoutResult.DictionarySuccess.DictionarySuccess(word, defaultDictionaryItems);
        assertEquals(expected, LiveDataTestUtilKt.getOrAwaitValue(presenter.getLayoutResult()));
    }

    @Test
    public void given_no_definition_when_start_with_word_then_word_success(){
        var word = new Words("comida", languageFrom, languageTo);
        presenter.start(word);

        verify(dictionaryRepository).getDefinition(eq("comida"), getDefinitionCallbackCaptor.capture());
        getDefinitionCallbackCaptor.getValue().onDataNotAvailable();

        var expected = new GetLayoutResult.DictionarySuccess.WordSuccess(ProcessTextLayoutType.SAVED_WORD, word);
        assertEquals(expected, LiveDataTestUtilKt.getOrAwaitValue(presenter.getLayoutResult()));
    }

    @Test
    public void given_word_input_when_start_with_service_then_get_layout_success(){
        presenter.startWithService("comida", languageFrom, languageTo);

        var word = loadLocalWord("comida");

        var expected = new GetLayoutResult.WordSuccess(ProcessTextLayoutType.SAVED_WORD, word);
        assertEquals(LiveDataTestUtilKt.getOrAwaitValue(presenter.getLayoutResult()), expected);
    }

    // endregion

    // region get layout tests

    @Test
    public void setSavedWordLayout(){
        var test_text = "Prueba";

        presenter.getLayout(test_text, languageFrom, languageTo);

        var word = loadLocalWord(test_text);

        var expected = new GetLayoutResult.WordSuccess(ProcessTextLayoutType.SAVED_WORD, word);
        assertEquals(LiveDataTestUtilKt.getOrAwaitValue(presenter.getLayoutResult()), expected);
    }

    @Test
    public void setSentenceLayout(){
        var test_text = "Prueba oracion";
        presenter.getLayout(test_text, languageFrom, languageTo);

        var return_word = new Words(test_text, languageFrom, "Sentence test");
        loadTextTranslation(return_word);

        GetLayoutResult expected = new GetLayoutResult.WordSuccess(ProcessTextLayoutType.SENTENCE_TRANSLATION, return_word);
        assertEquals(LiveDataTestUtilKt.getOrAwaitValue(presenter.getLayoutResult()), expected);
    }

    @Test
    public void setTranslationLayout(){
        var test_text = "Prueba";
        presenter.getLayout(test_text, languageFrom, languageTo);

        var return_word = new Words(test_text,"ES", "Test");
        loadRemoteWord(return_word);

        verify(dictionaryRepository).getDefinition(eq(test_text), getDefinitionCallbackCaptor.capture());
        getDefinitionCallbackCaptor.getValue().onDataNotAvailable();

        var expected = new GetLayoutResult.WordSuccess(ProcessTextLayoutType.WORD_TRANSLATION, return_word);
        assertEquals(LiveDataTestUtilKt.getOrAwaitValue(presenter.getLayoutResult()), expected);

    }

    @Test
    public void setExternalDictionaryLayout(){
        var test_text = "Prueba";
        presenter.getLayout(test_text, languageFrom, languageTo);

        var return_word = new Words(test_text,"ES", "Sentence test");
        loadRemoteWord(return_word);

        loadDictionaryDefinition(test_text);

        var expected = new GetLayoutResult.DictionarySuccess(return_word, defaultDictionaryItems);
        assertEquals(expected, LiveDataTestUtilKt.getOrAwaitValue(presenter.getLayoutResult()));
    }

    @Test
    public void show_error_with_word_translation_and_dictionary_available(){
        var inputText = "test_input";
        var languageFrom = "ES";

        presenter.getLayout(inputText, languageFrom, languageTo);

        // Indicate that no local word nor remote word available
        verify(wordRepository).getWordLanguageInfo(eq(inputText), eq(languageFrom), eq(languageTo), getWordCallbackCaptor.capture());
        getWordCallbackCaptor.getValue().onLocalWordNotAvailable();
        Words emptyWord = new Words(inputText, "un", "un");
        getWordCallbackCaptor.getValue().onDataNotAvailable(emptyWord);

        // Return dictionary definitions
        loadDictionaryDefinition(inputText);

        // Two values are set, first the dictionary result and then the error when translating
        // Because LiveData is a data holder, not a stream, only the last value is available
        GetLayoutResult result = LiveDataTestUtilKt.getOrAwaitValue(presenter.getLayoutResult());
        assertTrue(result instanceof GetLayoutResult.Error);
        assertEquals("Error", ((GetLayoutResult.Error) result).getException().getMessage());

        // Return external links
        verify(linksRepository).getLanguageLinks(eq("un"), getLinksCaptor.capture());
        getLinksCaptor.getValue().onLinksRetrieved(defaultLinksItems);

        verify(view).setTranslationErrorMessage();
    }

    // endregion

    // region play audio tests

    @Test
    public void showLanguageNotAvailable(){
        String inputText = "desconocido";

        // Gets saved word layout
        presenter.getLayout(inputText, languageFrom, languageTo);
        loadLocalWord(inputText);

        // Reproduce tts
        presenter.onClickReproduce(inputText);
        verify(customTTS).speak(eq(inputText), ttsListenerCaptor.capture());
        ttsListenerCaptor.getValue().onLanguageUnavailable();

        verify(view).showLanguageNotAvailable();
    }

    @Test
    public void new_language_available_and_previous_not_available(){
        var inputText = "desconocido";
        var unknownLanguageFrom = languageFrom;

        // Gets saved word layout
        presenter.getLayout(inputText, unknownLanguageFrom, languageTo);
        loadLocalWord(inputText);

        // Tts returns language not available
        presenter.onClickReproduce(inputText);
        verify(customTTS).speak(eq(inputText), ttsListenerCaptor.capture());
        ttsListenerCaptor.getValue().onLanguageUnavailable();
        verify(view).showLanguageNotAvailable();

        inputText = "correct";

        when(customTTS.getLanguage()).thenReturn(unknownLanguageFrom);

        // Gets saved word layout
        presenter.getLayout(inputText, languageFrom, languageTo);
        loadLocalWord(inputText);

        // Reproduce tts
        presenter.onClickReproduce(inputText);
        verify(customTTS).speak(eq(inputText), any());

    }

    @Test
    public void given_audio_playing_when_play_again_then_audio_stop() {
        when(sharedPreferences.getBoolean(SettingsFragment.PREF_AUTO_TEST_SWITCH, true)).thenReturn(true);

        var textInput = "first";
        presenter.getLayout(textInput, languageFrom, languageTo);
        loadLocalWord(textInput);

        verify(customTTS).initializeTTS(eq("ES"), ttsListenerCaptor.capture());
        var ttsListener = ttsListenerCaptor.getValue();
        ttsListener.onEngineReady();
        ttsListener.onSpeakStart();


        presenter.onClickReproduce(textInput);
        verify(customTTS).stop();
        verify(view).showPlayIcon();
    }

    @Test
    public void given_audio_error_when_play_then_audio_stop() {
        var textInput = "first";
        presenter.getLayout(textInput, languageFrom, languageTo);
        loadLocalWord(textInput);

        presenter.onClickReproduce(textInput);
        verify(customTTS).speak(eq(textInput), ttsListenerCaptor.capture());
        var ttsListener = ttsListenerCaptor.getValue();
        ttsListener.onError();

        verify(view).showErrorPlayingAudio();
    }

    // endregion

    @SuppressWarnings("deprecation")
    @Test
    public void when_language_change_then_translation_updated() {
        var textInput = "first";
        presenter.getLayout(textInput, languageFrom, languageTo);
        loadLocalWord(textInput);

        presenter.onLanguageSpinnerChange("de", languageTo);

        var word = new Words("first", "de", "new");
        verify(getTranslationInteractor).invoke(eq(textInput), getLangTransCallbackCaptor.capture(), eq("de"), eq(languageTo));
        getLangTransCallbackCaptor.getValue().onTranslationAndLanguage(word);

        verify(view).updateTranslation(word);

        // verify external links
        verify(linksRepository).getLanguageLinks(eq("de"), getLinksCaptor.capture());
        getLinksCaptor.getValue().onLinksRetrieved(defaultLinksItems);
        verify(view).updateExternalLinks(defaultLinksItems);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void given_no_lang_data_when_language_change_then_error() {
        var textInput = "first";
        presenter.getLayout(textInput, languageFrom, languageTo);
        loadLocalWord(textInput);

        presenter.onLanguageSpinnerChange("de", languageTo);

        verify(getTranslationInteractor).invoke(eq(textInput), getLangTransCallbackCaptor.capture(), eq("de"), eq(languageTo));
        getLangTransCallbackCaptor.getValue().onDataNotAvailable();

        verify(view).setTranslationErrorMessage();
    }

    @Test
    public void when_click_delete_then_view_updated() {
        presenter.onClickDeleteWord("ejemplo");
        verify(view).showWordDeleted();
    }

    @Test
    public void when_listen_to_word_stream_then_word_emitted() {
        var word = new Words("ejemplo", "es", "example");
        when(wordRepository.getLocalWord("ejemplo", "es")).thenReturn(new MutableLiveData<>(word));
        var result = LiveDataTestUtilKt.getOrAwaitValue(presenter.wordStream("ejemplo", "es"));
        assertEquals(word, result);
    }

    @Test
    public void when_full_lifecycle_then_tts_removed() {
        presenter.start();

        presenter.onClickReproduce("to play");

        verify(customTTS).speak(eq("to play"), ttsListenerCaptor.capture());
        var ttsListener = ttsListenerCaptor.getValue();
        ttsListener.onEngineReady();
        ttsListener.onSpeakStart();

        presenter.pause();
        presenter.stop();
        presenter.destroy();

        ttsListener.onSpeakDone();

        verify(customTTS).stop();
        verify(customTTS).removeListener(any());
    }

    private Words loadLocalWord(String wordText) {
        verify(wordRepository).getWordLanguageInfo(eq(wordText), eq(languageFrom), eq(languageTo), getWordCallbackCaptor.capture());
        var returnWord = new Words(wordText,"ES", "Test");
        getWordCallbackCaptor.getValue().onLocalWordLoaded(returnWord);
        return returnWord;
    }

    private void loadRemoteWord(Words word) {
        verify(wordRepository).getWordLanguageInfo(eq(word.word), eq(languageFrom), eq(languageTo), getWordCallbackCaptor.capture());
        getWordCallbackCaptor.getValue().onLocalWordNotAvailable();

        getWordCallbackCaptor.getValue().onRemoteWordLoaded(word);
    }

    private void loadTextTranslation(Words word) {
        verify(wordRepository).getLanguageAndTranslation(eq(word.word), eq(word.lang), eq(languageTo), getTranslationCallbackCaptor.capture());
        getTranslationCallbackCaptor.getValue().onTranslationAndLanguage(word);
    }

    private void loadDictionaryDefinition(String text) {
        verify(dictionaryRepository).getDefinition(eq(text), getDefinitionCallbackCaptor.capture());
        getDefinitionCallbackCaptor.getValue().onDefinitionLoaded(defaultDictionaryItems);
    }

}
