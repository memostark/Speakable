package com.guillermonegrete.tts.TextProcessing;


import com.guillermonegrete.tts.AbstractPresenter;
import com.guillermonegrete.tts.Executor;
import com.guillermonegrete.tts.MainThread;
import com.guillermonegrete.tts.TextProcessing.domain.interactors.GetLayout;
import com.guillermonegrete.tts.TextProcessing.domain.interactors.GetLayoutInteractor;
import com.guillermonegrete.tts.data.source.WordRepository;

public class ProcessTextPresenter extends AbstractPresenter implements ProcessTextContract.Presenter{

    private ProcessTextContract.View mView;
    private WordRepository mRepository;

    public ProcessTextPresenter(Executor executor, MainThread mainThread, ProcessTextContract.View view, WordRepository repository){
        super(executor, mainThread);
        mView = view;
        mRepository = repository;

        mView.setPresenter(this);
    }

    @Override
    public void reproduceTTS() {

    }

    @Override
    public void addNewWord() {

    }

    @Override
    public void deleteWord() {

    }

    @Override
    public void editWord() {

    }

    @Override
    public void getLayout(String text) {
        GetLayout interactor = new GetLayout(mExecutor, mMainThread, new GetLayoutInteractor.Callback() {
            @Override
            public void onLayoutDetermined() {
                System.out.print("Got layout");
            }
        }, mRepository, text);

        interactor.run();

    }

    @Override
    public void getExternalLinks() {

    }

    @Override
    public void start() {

    }
}
