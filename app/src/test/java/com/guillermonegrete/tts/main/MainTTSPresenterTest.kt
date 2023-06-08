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

        val listener = getTTSListener()
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
    fun `Given tts unavailable, when reproduce clicked, then language unavailable`() {
        presenter.onClickReproduce("to play", "en")

        val listener = getTTSListener()
        listener.onLanguageUnavailable()

        verifyOrder {
            view.showLoadingTTS()
            view.showLanguageNotAvailable()
        }
    }

    @Test
    fun `Given tts error, when reproduce clicked, then no change`() {
        presenter.onClickReproduce("to play", "en")

        val listener = getTTSListener()
        listener.onError()

        verifyOrder { view.showLoadingTTS() }
    }

    @Test
    fun `Given null language, when reproduce clicked, then lang detected and play icon update`() {
        presenter.onClickReproduce("to play", null)

        val translationCallback = getTranslationListener()
        translationCallback.onTranslationAndLanguage(Words("to play", "en", "translation"))

        val listener = getTTSListener()
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
    fun `Given null language, when reproduce clicked and data unavailable, then no change`() {
        presenter.onClickReproduce("to play", null)

        val translationCallback = getTranslationListener()
        translationCallback.onDataNotAvailable()

        verifyOrder { view.showLoadingTTS() }
    }

    @Test
    fun `Given tts playing, when reproduce clicked, then tts stopped`() {
        presenter.onClickReproduce("to play", "en")

        val listener = getTTSListener()
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

    @Test
    fun `When full lifecycle, then tts closed`() {
        presenter.start()

        presenter.onClickReproduce("to play", "en")

        val listener = getTTSListener()
        listener.onEngineReady()
        listener.onSpeakStart()

        presenter.pause()
        presenter.stop()
        presenter.destroy()

        verifyOrder {
            tts.stop()
            tts.removeListener(any())
        }
    }

    private fun getTTSListener(): CustomTTS.Listener {
        val listenerSlot = slot<CustomTTS.Listener>()
        verify { tts.initializeTTS("en", capture(listenerSlot)) }
        return listenerSlot.captured
    }

    private fun getTranslationListener(): WordRepositorySource.GetTranslationCallback {
        val translationSlot = slot<WordRepositorySource.GetTranslationCallback>()
        verify { wordRepository.getLanguageAndTranslation("to play", capture(translationSlot)) }
        return translationSlot.captured
    }
}
