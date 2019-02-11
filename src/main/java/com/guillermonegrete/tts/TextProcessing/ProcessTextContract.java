package com.guillermonegrete.tts.TextProcessing;

import com.guillermonegrete.tts.BasePresenter;
import com.guillermonegrete.tts.BaseView;
import com.guillermonegrete.tts.TextProcessing.domain.model.WikiItem;
import com.guillermonegrete.tts.db.ExternalLink;
import com.guillermonegrete.tts.db.Words;

import java.util.List;

public interface ProcessTextContract {

    interface View extends BaseView<Presenter>{
        void setWiktionaryLayout(List<WikiItem> items);

        void setSavedWordLayout(Words word);

        void setTranslationLayout(Words word);

        void setSentenceLayout(Words word);

        void setExternalDictionary(List<ExternalLink> links);

        void showSaveDialog(Words word);

        void showDeleteDialog(String word);

        void showWordDeleted();
    }

    interface Presenter extends BasePresenter{

        void addNewWord();


        void editWord();

        void getLayout(String text);

        void getExternalLinks(String language);

        void onClickBookmark();

        void onClickSaveWord(Words word);

        void onClickDeleteWord(String word);

        void onClickReproduce(String text);

        void onClickEdit();

    }
}
