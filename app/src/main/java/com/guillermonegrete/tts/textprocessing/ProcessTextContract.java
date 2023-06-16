package com.guillermonegrete.tts.textprocessing;

import androidx.lifecycle.LiveData;

import com.guillermonegrete.tts.BasePresenter;
import com.guillermonegrete.tts.BaseView;
import com.guillermonegrete.tts.textprocessing.domain.model.WikiItem;
import com.guillermonegrete.tts.db.ExternalLink;
import com.guillermonegrete.tts.db.Words;

import java.util.List;

public interface ProcessTextContract {

    interface View extends BaseView<Presenter>{

        void setSavedWordLayout(Words word);

        void setDictWithSaveWordLayout(Words word, List<WikiItem> items);

        void setExternalDictionary(List<ExternalLink> links);

        void setTranslationErrorMessage();

        void showSaveDialog(Words word);

        void showDeleteDialog(String word);

        void showWordDeleted();

        void startService();

        void showLanguageNotAvailable();

        void showLoadingTTS();

        void showPlayIcon();

        void showStopIcon();

        void showTranslationError(String error);

        void showErrorPlayingAudio();

        void updateTranslation(Words words);

        void updateExternalLinks(List<ExternalLink> links);
    }

    interface Presenter extends BasePresenter<View>{

        void start(Words word);

        void startWithService(String selectedText, String languageFrom, String languageTo);

        void getLayout(String text, String languageFrom, String languageTo);

        LiveData<Words> wordStream(String text, String languageFrom);

        void getDictionaryEntry(Words word, boolean isSaved);

        void onClickDeleteWord(String word);

        void onClickReproduce(String text);

        void onLanguageSpinnerChange(String languageFrom, String languageTo);

    }
}
