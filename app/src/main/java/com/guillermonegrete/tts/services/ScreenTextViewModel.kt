package com.guillermonegrete.tts.services

import android.content.SharedPreferences
import android.graphics.Rect
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guillermonegrete.tts.MainThread
import com.guillermonegrete.tts.customtts.CustomTTS
import com.guillermonegrete.tts.data.LoadResult
import com.guillermonegrete.tts.data.PlayAudioState
import com.guillermonegrete.tts.data.Result
import com.guillermonegrete.tts.data.Translation
import com.guillermonegrete.tts.imageprocessing.ScreenImageCaptor
import com.guillermonegrete.tts.imageprocessing.domain.interactors.DetectTextFromScreen
import com.guillermonegrete.tts.main.SettingsFragment
import com.guillermonegrete.tts.main.domain.interactors.GetLangAndTranslation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScreenTextViewModel @JvmOverloads constructor(
    private val languagesISO: Array<String>,
    private val mainThread: MainThread,
    private val tts: CustomTTS,
    private val sharedPreferences: SharedPreferences,
    private val detectTextInteractor: DetectTextFromScreen,
    private val getTranslationInteractor: GetLangAndTranslation,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
): ViewModel() {


    private val _langDetected = MutableLiveData<String>()
    val langDetected: LiveData<String> = _langDetected

    private val _langToPreference = MutableLiveData<Int>()
    val langToPreference: LiveData<Int> = _langToPreference

    private val _playingAudio = MutableLiveData<PlayAudioState>()
    val playingAudio: LiveData<PlayAudioState> = _playingAudio

    private val _textTranslated = MutableLiveData<LoadResult<Translation>>()
    val textTranslated: LiveData<LoadResult<Translation>> = _textTranslated

    private var isPlayingValue = false
    private var detectedText = ""


    fun onPlayClick(imageCaptor: ScreenImageCaptor, rect: Rect){
        if (isPlayingValue) {
            tts.stop()
            isPlayingValue = false
            setStopState()
        } else {
            setLoadingTts()

            detectTextInteractor(
                imageCaptor,
                rect,
                object : DetectTextFromScreen.Callback {
                    override fun onTextDetected(text: String, language: String) {
                        _langDetected.value = language
                        detectedText = text
                        tts.initializeTTS(language, ttsListener)
                    }

                    override fun onError(error: Exception) {
                        isPlayingValue = false
                        _playingAudio.value = PlayAudioState.Error(error)
                    }
                }
            )
        }
    }

    fun onTranslateClick(imageCaptor: ScreenImageCaptor, rect: Rect){
        _textTranslated.value = LoadResult.Loading
        detectTextInteractor(
            imageCaptor,
            rect,
            object: DetectTextFromScreen.Callback{
                override fun onTextDetected(text: String, language: String) {
                    detectLanguageAndTranslate(text)
                }

                override fun onError(error: Exception) {
                    _textTranslated.value = LoadResult.Error(error)
                }
            }
        )
    }

    fun detectLanguageAndTranslate(text: String){
        val langPrefIndex = getLanguageToPreference()
        _langToPreference.value = langPrefIndex

        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                getTranslationInteractor(text, "auto", languagesISO[langPrefIndex])
            }

            when(result){
                is Result.Success ->  _textTranslated.value = LoadResult.Success(result.data)
                is Result.Error -> _textTranslated.value = LoadResult.Error(RuntimeException())
            }
        }
    }

    private fun setStopState() {
        _playingAudio.value = PlayAudioState.Stopped
    }

    private fun setLoadingTts() {
        _playingAudio.value = PlayAudioState.Loading
    }

    private fun setErrorState(msg: String) {
        _playingAudio.value = PlayAudioState.Error(RuntimeException(msg))
    }

    private fun getLanguageToPreference(): Int {
        val englishIndex = 15
        /**
         * Reference to a fragment is bad, either wrap around repo/data source
         * or move this code somewhere outside the view model.
         */
        return sharedPreferences.getInt(SettingsFragment.PREF_LANGUAGE_TO, englishIndex)
    }

    val ttsListener =  object: CustomTTS.Listener{

        override fun onEngineReady() {
            tts.speak(detectedText, this)
        }

        override fun onLanguageUnavailable() {
            isPlayingValue = false
            setErrorState(LANG_UNAVAILABLE_ERROR)
        }

        override fun onSpeakStart() {
            mainThread.post {
                isPlayingValue = true
                _playingAudio.value = PlayAudioState.Playing
            }
        }

        override fun onSpeakDone() {

            mainThread.post {
                isPlayingValue = false
                setStopState()
            }
        }

        override fun onError() {
            mainThread.post {
                isPlayingValue = false
                setErrorState(GENERIC_TTS_ERROR)
            }
        }
    }

    companion object {
        const val LANG_UNAVAILABLE_ERROR = "TTS error: Language Unknown/Unavailable"
        const val GENERIC_TTS_ERROR = "TTS error: Failed to play"
    }
}
