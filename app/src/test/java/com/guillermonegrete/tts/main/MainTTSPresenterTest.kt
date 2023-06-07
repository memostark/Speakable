package com.guillermonegrete.tts.main

import com.guillermonegrete.tts.TestThreadExecutor
import com.guillermonegrete.tts.customtts.CustomTTS
import com.guillermonegrete.tts.data.source.WordRepository
import com.guillermonegrete.tts.data.source.WordRepositorySource
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.threading.TestMainThread
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Before
import org.junit.Test

class MainTTSPresenterTest {

    private lateinit var presenter: MainTTSPresenter

    @MockK
    private lateinit var wordRepository: WordRepository
    @MockK(relaxed = true)
    private lateinit var view: MainTTSContract.View
    @MockK(relaxed = true)
    private lateinit var tts: CustomTTS


    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        presenter = MainTTSPresenter(TestThreadExecutor(), TestMainThread(), wordRepository, tts)
        presenter.setView(view)
    }

    @Test
    fun `Given idle, when reproduce clicked, then play icon update`() {
        presenter.onClickReproduce("to play", "en")

        val listenerSlot = slot<CustomTTS.Listener>()
        verify { tts.initializeTTS("en", capture(listenerSlot)) }
        val listener = listenerSlot.captured
        listener.onEngineReady()
        listener.onSpeakStart()
        listener.onSpeakDone()

        verifyOrder {
            view.showLoadingTTS()
            view.showStopIcon()
            view.showPlayIcon()
        }
    }

    @Test
    fun `Given no language, when reproduce clicked, then lang detected and play icon update`() {
        presenter.onClickReproduce("to play", null)

        val translationSlot = slot<WordRepositorySource.GetTranslationCallback>()
        verify { wordRepository.getLanguageAndTranslation("to play", capture(translationSlot)) }
        translationSlot.captured.onTranslationAndLanguage(Words("to play", "en", "translation"))

        val listenerSlot = slot<CustomTTS.Listener>()
        verify { tts.initializeTTS("en", capture(listenerSlot)) }
        val listener = listenerSlot.captured
        listener.onEngineReady()
        listener.onSpeakStart()
        listener.onSpeakDone()

        verifyOrder {
            view.showLoadingTTS()
            view.showDetectedLanguage("en")
            view.showStopIcon()
            view.showPlayIcon()
        }
    }

    @Test
    fun `Given tts playing, when reproduce clicked, then tts stopped`() {
        presenter.onClickReproduce("to play", "en")

        val listenerSlot = slot<CustomTTS.Listener>()
        verify { tts.initializeTTS("en", capture(listenerSlot)) }
        val listener = listenerSlot.captured
        listener.onEngineReady()
        listener.onSpeakStart()

        presenter.onClickReproduce("to play", "en")

        verifyOrder {
            view.showLoadingTTS()
            view.showStopIcon()
            tts.stop()
            view.showPlayIcon()
        }
    }
}
