package com.guillermonegrete.tts.services

import android.content.SharedPreferences
import android.graphics.Rect
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.guillermonegrete.tts.MainThread
import com.guillermonegrete.tts.customtts.CustomTTS
import com.guillermonegrete.tts.data.LoadResult
import com.guillermonegrete.tts.db.Words
import com.guillermonegrete.tts.imageprocessing.ScreenImageCaptor
import com.guillermonegrete.tts.imageprocessing.domain.interactors.DetectTextFromScreen
import com.guillermonegrete.tts.main.SettingsFragment
import com.guillermonegrete.tts.main.domain.interactors.GetLangAndTranslation

class ScreenTextViewModel(
    private val languagesISO: Array<String>,
    private val mainThread: MainThread,
    private val tts: CustomTTS,
    private val sharedPreferences: SharedPreferences,
    private val detectTextInteractor: DetectTextFromScreen,
    private val getTranslationInteractor: GetLangAndTranslation
): ViewModel() {


    private val _langDetected = MutableLiveData<String>()
    val langDetected: LiveData<String> = _langDetected

    private val _langToPreference = MutableLiveData<Int>()
    val langToPreference: LiveData<Int> = _langToPreference

    private val _detectTextError = MutableLiveData<Boolean>()
    val detectTextError: LiveData<Boolean> = _detectTextError

    private val _ttsLoading = MutableLiveData<Boolean>()
    val ttsLoading: LiveData<Boolean> = _ttsLoading

    private val _isPlaying = MutableLiveData<Boolean>()
    val isPlaying: LiveData<Boolean> = _isPlaying

    private val _onError = MutableLiveData<String>()
    val onError: LiveData<String> = _onError

    private val _textTranslated = MutableLiveData<LoadResult<Words>>()
    val textTranslated: LiveData<LoadResult<Words>> = _textTranslated

    private var isPlayingValue = false
    private var detectedText = ""


    fun onPlayClick(imageCaptor: ScreenImageCaptor, rect: Rect){
        if(isPlayingValue){
            tts.stop()
            isPlayingValue = false
            setStopState()
        }else{
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
                        setStopState()
                        _detectTextError.value = true
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
                    isPlayingValue = false
                    setStopState()
                    _textTranslated.value = LoadResult.Error(error)
                }
            }
        )
    }

    fun detectLanguageAndTranslate(text: String){
        val langPrefIndex = getLanguageToPreference()
        _langToPreference.value = langPrefIndex

        getTranslationInteractor.invoke(
            text,
            object: GetLangAndTranslation.Callback {
                override fun onTranslationAndLanguage(word: Words) {
                    _textTranslated.value = LoadResult.Success(word)
                }

                override fun onDataNotAvailable() {
                    _textTranslated.value = LoadResult.Error(RuntimeException())
                }
            },
            "auto",
            languagesISO[langPrefIndex]
        )

    }

    private fun setStopState() {
        _ttsLoading.value = false
    }

    private fun setLoadingTts() {
        _ttsLoading.value = true
    }

    private fun setPlayingState() {
        _isPlaying.value = true
    }

    private fun setErrorState(msg: String){
        setStopState()
        _onError.value = msg
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
            setStopState()
            // set language unavailable state
        }

        override fun onSpeakStart() {
            mainThread.post {
                isPlayingValue = true
                setPlayingState()
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
                setErrorState("TTS error: Failed to play")
            }
        }
    }
}