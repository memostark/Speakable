package com.guillermonegrete.tts;

public interface BaseView<T extends BasePresenter> {

    void setPresenter(T presenter);

}
