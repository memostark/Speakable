package com.guillermonegrete.tts.services

import android.content.SharedPreferences
import android.graphics.Rect
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.guillermonegrete.tts.MainCoroutineRule
import com.guillermonegrete.tts.TestThreadExecutor
import com.guillermonegrete.tts.customtts.CustomTTS
import com.guillermonegrete.tts.data.LoadResult
import com.guillermonegrete.tts.data.PlayAudioState
import com.guillermonegrete.tts.data.Segment
import com.guillermonegrete.tts.data.Translation
import com.guillermonegrete.tts.data.source.FakeWordRepository
import com.guillermonegrete.tts.getOrAwaitValue
import com.guillermonegrete.tts.imageprocessing.ImageProcessingSource
import com.guillermonegrete.tts.imageprocessing.ScreenImageCaptor
import com.guillermonegrete.tts.imageprocessing.domain.interactors.DetectTextFromScreen
import com.guillermonegrete.tts.main.SettingsFragment
import com.guillermonegrete.tts.main.domain.interactors.GetLangAndTranslation
import com.guillermonegrete.tts.threading.TestMainThread
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.lang.RuntimeException

@ExperimentalCoroutinesApi
class ScreenTextViewModelTest {

    private lateinit var viewModel: ScreenTextViewModel

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule(UnconfinedTestDispatcher())

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @MockK(relaxed = true)
    private lateinit var imageProcessor: ImageProcessingSource
    @MockK(relaxed = true)
    private lateinit var tts: CustomTTS
    @MockK
    private lateinit var preferences: SharedPreferences
    @MockK(relaxed = true)
    private lateinit var imageCaptor: ScreenImageCaptor
    @MockK
    private lateinit var rect: Rect

    private lateinit var wordRepository: FakeWordRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        val mainThread = TestMainThread()
        val executor = TestThreadExecutor()
        val detectTextInteractor = DetectTextFromScreen(TestThreadExecutor(), TestMainThread(), imageProcessor)
        wordRepository = FakeWordRepository()
        viewModel = ScreenTextViewModel(
            arrayOf("de", "es"),
            TestMainThread(),
            tts,
            preferences,
            detectTextInteractor,
            GetLangAndTranslation(executor, mainThread, wordRepository),
            UnconfinedTestDispatcher()
        )
    }

    @Test
    fun `When play clicked, then language detected and playing state changes`() {
        viewModel.onPlayClick(imageCaptor, rect)

        getDetectedText()

        assertEquals("en", viewModel.langDetected.getOrAwaitValue())

        val ttsListener = getTTSListener()
        ttsListener.onEngineReady()
        ttsListener.onSpeakStart()
        assertEquals(PlayAudioState.Playing, viewModel.playingAudio.getOrAwaitValue())
        ttsListener.onSpeakDone()
        assertEquals(PlayAudioState.Stopped, viewModel.playingAudio.getOrAwaitValue())
    }

    @Test
    fun `Given detection error, when play clicked, then error state`() {
        viewModel.onPlayClick(imageCaptor, rect)

        val error = IOException("Error reading bitmap")
        setDetectedTextError(error)

        assertEquals(PlayAudioState.Error(error), viewModel.playingAudio.getOrAwaitValue())
    }

    @Test
    fun `Given tts playing, when reproduce clicked, then tts stopped`() {
        viewModel.onPlayClick(imageCaptor, rect)

        getDetectedText()

        assertEquals("en", viewModel.langDetected.getOrAwaitValue())

        val ttsListener = getTTSListener()
        ttsListener.onEngineReady()
        ttsListener.onSpeakStart()
        assertEquals(PlayAudioState.Playing, viewModel.playingAudio.getOrAwaitValue())

        viewModel.onPlayClick(imageCaptor, rect)
        assertEquals(PlayAudioState.Stopped, viewModel.playingAudio.getOrAwaitValue())
    }

    @Test
    fun `Given language unavailable, When play clicked, then error`() {
        viewModel.onPlayClick(imageCaptor, rect)

        getDetectedText()

        val ttsListener = getTTSListener()
        ttsListener.onLanguageUnavailable()

        val error = viewModel.playingAudio.getOrAwaitValue() as PlayAudioState.Error
        assertEquals("TTS error: Language Unavailable", error.exception.message)
    }

    @Test
    fun `Given tts error, when play clicked, then error`() {
        viewModel.onPlayClick(imageCaptor, rect)

        getDetectedText()

        val ttsListener = getTTSListener()
        ttsListener.onError()

        val error = viewModel.playingAudio.getOrAwaitValue() as PlayAudioState.Error
        assertEquals("TTS error: Failed to play", error.exception.message)
    }

    @Test
    fun `When translate click, then load success`() {
        val translation = Translation(listOf(Segment("translated text", "Detected text")), "ES")
        wordRepository.addTranslation(translation)

        viewModel.onTranslateClick(imageCaptor, rect)

        assertEquals(LoadResult.Loading, viewModel.textTranslated.getOrAwaitValue())

        every { preferences.getInt(SettingsFragment.PREF_LANGUAGE_TO, 15) } returns 1

        getDetectedText()

        assertEquals(1, viewModel.langToPreference.getOrAwaitValue())
        assertEquals(LoadResult.Success(translation), viewModel.textTranslated.getOrAwaitValue())
    }

    @Test
    fun `Given no translations, when translate click, then error`() {
        viewModel.onTranslateClick(imageCaptor, rect)

        assertEquals(LoadResult.Loading, viewModel.textTranslated.getOrAwaitValue())

        every { preferences.getInt(SettingsFragment.PREF_LANGUAGE_TO, 15) } returns 1

        getDetectedText()

        assertEquals(1, viewModel.langToPreference.getOrAwaitValue())
        val error = viewModel.textTranslated.getOrAwaitValue() as LoadResult.Error
        assertTrue(error.exception is RuntimeException)
    }

    @Test
    fun `Given detection error, when translate click, then error`() {
        val translation = Translation(listOf(Segment("translated text", "Detected text")), "ES")
        wordRepository.addTranslation(translation)

        viewModel.onTranslateClick(imageCaptor, rect)

        assertEquals(LoadResult.Loading, viewModel.textTranslated.getOrAwaitValue())

        val error = IOException("Error reading bitmap")
        setDetectedTextError(error)

        assertEquals(LoadResult.Error<Translation>(error), viewModel.textTranslated.getOrAwaitValue())
    }

    private fun getDetectedText() {
        val slot = slot<ScreenImageCaptor.Callback>()
        verify { imageCaptor.getImage(rect, capture(slot)) }

        slot.captured.onImageCaptured(mockk())

        val imageProcessingSlot = slot<ImageProcessingSource.Callback>()
        verify { imageProcessor.detectText(any(), capture(imageProcessingSlot)) }

        imageProcessingSlot.captured.onTextDetected("Detected text", "en")
    }

    private fun setDetectedTextError(error: Exception) {
        val slot = slot<ScreenImageCaptor.Callback>()
        verify { imageCaptor.getImage(rect, capture(slot)) }

        slot.captured.onImageCaptured(mockk())

        val imageProcessingSlot = slot<ImageProcessingSource.Callback>()
        verify { imageProcessor.detectText(any(), capture(imageProcessingSlot)) }

        imageProcessingSlot.captured.onFailure(error)
    }

    private fun getTTSListener(): CustomTTS.Listener {
        val listenerSlot = slot<CustomTTS.Listener>()
        verify { tts.initializeTTS("en", capture(listenerSlot)) }
        return listenerSlot.captured
    }
}