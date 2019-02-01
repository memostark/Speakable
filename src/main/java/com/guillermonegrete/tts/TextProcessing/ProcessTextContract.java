package com.guillermonegrete.tts.TextProcessing;

import com.guillermonegrete.tts.BasePresenter;
import com.guillermonegrete.tts.BaseView;

public interface ProcessTextContract {

    interface View extends BaseView<Presenter>{
        void setWindow();

        void setExternalDictionary();

        void setTranslation();

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
