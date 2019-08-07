package com.guillermonegrete.tts

interface BasePresenter<T> {

    fun start()

    fun pause()

    fun stop()

    fun destroy()

    fun setView(view: T)
}
