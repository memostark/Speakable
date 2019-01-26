package com.guillermonegrete.tts.TextProcessing;

import com.guillermonegrete.tts.BasePresenter;
import com.guillermonegrete.tts.BaseView;

public interface ProcessTextContract {

    interface View extends BaseView<Presenter>{
        void setWindow();

        void setExternalDictionaryData();

    }

    interface Presenter extends BasePresenter{

        void reproduceTTS();

        void getLayout(String text);

        void getExternalLinks();

    }
}
