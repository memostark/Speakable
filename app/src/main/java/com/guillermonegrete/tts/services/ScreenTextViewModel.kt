package com.guillermonegrete.tts.services

import android.content.SharedPreferences
import android.graphics.Rect
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.guillermonegrete.tts.MainThread
import com.guillermonegrete.tts.customtts.CustomTTS
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

    private val _langToPreference = MutableLiveData<String>()
    val langToPreference: LiveData<String> = _langToPreference

    private val _detectTextError = MutableLiveData<Boolean>()
    val detectTextError: LiveData<Boolean> = _detectTextError

    private val _ttsLoading = MutableLiveData<Boolean>()
    val ttsLoading: LiveData<Boolean> = _ttsLoading

    private val _isPlaying = MutableLiveData<Boolean>()
    val isPlaying: LiveData<Boolean> = _isPlaying

    private val _textTranslated = MutableLiveData<Words>()
    val textTranslated: LiveData<Words> = _textTranslated

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

                    override fun onError(message: String) {
                        isPlayingValue = false
                        setStopState()
                        _detectTextError.value = true
                    }
                }
            )
        }
    }

    fun onTranslateClick(imageCaptor: ScreenImageCaptor, rect: Rect){
        detectTextInteractor(
            imageCaptor,
            rect,
            object: DetectTextFromScreen.Callback{
                override fun onTextDetected(text: String, language: String) {
                    detectLanguageAndTranslate(text)
                }

                override fun onError(message: String) {
                    isPlayingValue = false
                    setStopState()
                    _detectTextError.value = true
                }
            }
        )
    }

    fun detectLanguageAndTranslate(text: String){
        val languagePreference = getLanguageToPreference()
        _langToPreference.value = languagePreference

        getTranslationInteractor.invoke(
            text,
            "auto",
            languagePreference,
            object: GetLangAndTranslation.Callback {
                override fun onTranslationAndLanguage(word: Words) {
                    _textTranslated.value = word
                }

                override fun onDataNotAvailable() {
                    println("Error translating")
                }
            }
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

    private fun getLanguageToPreference(): String {
        val englishIndex = 15
        /**
         * Reference to a fragment is bad, either wrap around repo/data source
         * or move this code somewhere outside the view model.
         */
        val languagePreferenceIndex: Int = sharedPreferences.getInt(SettingsFragment.PREF_LANGUAGE_TO, englishIndex)
        return languagesISO[languagePreferenceIndex]
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
    }
}