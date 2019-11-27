//
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license.
//
// Microsoft Cognitive Services (formerly Project Oxford): https://www.microsoft.com/cognitive-services
//
// Microsoft Cognitive Services (formerly Project Oxford) GitHub:
// https://github.com/Microsoft/Cognitive-Speech-TTS
//
// Copyright (c) Microsoft Corporation
// All rights reserved.
//
// MIT License:
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
package com.guillermonegrete.speech.tts;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;

public class Synthesizer {

    private Voice m_serviceVoice;
    private Voice m_localVoice;

    public String m_audioOutputFormat = AudioOutputFormat.Raw16Khz16BitMonoPcm;

    private AudioTrack audioTrack;

    private byte[] localAudioBytes;
    private String cacheText;

    private Callback callback;

    private void playSound(final byte[] sound) {
        if (sound == null || sound.length == 0) {
            callback.onError();
            return;
        }

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final int SAMPLE_RATE = 16000;
                final int audioLength = sound.length;

                audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, audioLength, AudioTrack.MODE_STREAM);

                // 16 bit has to be divided by 2. More info: https://stackoverflow.com/questions/7642704/audiotrack-how-to-detect-end-of-sound
                audioTrack.setNotificationMarkerPosition(audioLength / 2);
                audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
                    @Override
                    public void onMarkerReached(AudioTrack audioTrack) {
                        callback.onStop();
                        audioTrack.stop();
                        audioTrack.release();
                    }

                    @Override
                    public void onPeriodicNotification(AudioTrack audioTrack) {}
                });

                if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                    audioTrack.play();
                    callback.onStart();
                    audioTrack.write(sound, 0, audioLength);
                }
            }
        });
    }

    //stop playing audio data
    // if use STREAM mode, will wait for the end of the last write buffer data will stop.
    // if you stop immediately, call the pause() method and then call the flush() method to discard the data that has not yet been played
    public void stopSound() {
        try {
            if (audioTrack != null && audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                audioTrack.pause();
                audioTrack.flush();
                callback.onStop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public enum ServiceStrategy {
        AlwaysService//, WiFiOnly, WiFi3G4GOnly, NoService
    }

    public Synthesizer(String apiKey, Callback callback) {
        m_serviceVoice = new Voice("en-US");
        m_localVoice = null;
        m_eServiceStrategy = ServiceStrategy.AlwaysService;
        m_ttsServiceClient = new TtsServiceClient(apiKey);
        this.callback = callback;
    }

    public void SetVoice(Voice serviceVoice, Voice localVoice) {
        m_serviceVoice = serviceVoice;
        m_localVoice = localVoice;
    }

    public void SetServiceStrategy(ServiceStrategy eServiceStrategy) {
        m_eServiceStrategy = eServiceStrategy;
    }

    private byte[] getAudioBytes(String text) {
        String ssml = "<speak version='1.0' xml:lang='" + m_serviceVoice.lang + "'><voice xml:lang='" + m_serviceVoice.lang + "' xml:gender='" + m_serviceVoice.gender + "'";
        if (m_eServiceStrategy == ServiceStrategy.AlwaysService) {
            if (m_serviceVoice.voiceName.length() > 0) {
                ssml += " name='" + m_serviceVoice.voiceName + "'>";
            } else {
                ssml += ">";
            }
            ssml +=  text + "</voice></speak>";
        }
        return SpeakSSML(ssml);
    }

    public void SpeakToAudio(String text) {
        playSound(getAudioBytes(text));
    }

    public void SpeakSSMLToAudio(String ssml) {
        playSound(SpeakSSML(ssml));
    }

    private byte[] SpeakSSML(String ssml) {
        byte[] result = null;
        /*
         * check current network environment
         * to do...
         */
        if (m_eServiceStrategy == ServiceStrategy.AlwaysService) {
            result = m_ttsServiceClient.SpeakSSML(ssml);
            if (result == null || result.length == 0) {
                return null;
            }

        }
        return result;
    }

    public void getAudio(String text){
        localAudioBytes = getAudioBytes(text);
        cacheText = text;
    }

    public void speakLocalAudio(){
        playSound(localAudioBytes);
    }

    public byte[] getLocalAudioBytes(String text) {
        if(text.equals(cacheText)) return localAudioBytes;
        else return null;
    }

    private TtsServiceClient m_ttsServiceClient;
    private ServiceStrategy m_eServiceStrategy;

    public interface Callback{
        void onStart();

        void onStop();

        void onError();
    }
}
