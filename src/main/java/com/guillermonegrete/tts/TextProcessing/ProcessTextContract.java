package com.guillermonegrete.tts.TextProcessing;

import com.guillermonegrete.tts.BasePresenter;
import com.guillermonegrete.tts.BaseView;
import com.guillermonegrete.tts.TextProcessing.domain.model.WiktionaryLanguage;
import com.guillermonegrete.tts.db.Words;

import java.util.List;

public interface ProcessTextContract {

    interface View extends BaseView<Presenter>{
        void setWiktionaryLayout(List<WiktionaryLanguage> items);

        void setSavedWordLayout(Words word);

        void setTranslationLayout(Words word);

        void setSentenceLayout(Words word);

        void setExternalDictionary();

        void onClickBookmark();

        void onClickReproduce();

        void onClickEdit();
    }

    interface Presenter extends BasePresenter{

        void reproduceTTS();

        void addNewWord();

        void deleteWord();

        void editWord();

        void getLayout(String text);

        void getExternalLinks();

    }
}
