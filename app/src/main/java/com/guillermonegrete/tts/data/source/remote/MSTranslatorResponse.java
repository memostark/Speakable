package com.guillermonegrete.tts.data.source.remote;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MSTranslatorResponse {

    @SerializedName("detectedLanguage")
    @Expose
    private DetectedLanguage detectedLanguage;
    @SerializedName("translations")
    @Expose
    private List<Translation> translations = null;

    public DetectedLanguage getDetectedLanguage() {
        return detectedLanguage;
    }

    public void setDetectedLanguage(DetectedLanguage detectedLanguage) {
        this.detectedLanguage = detectedLanguage;
    }

    public List<Translation> getTranslations() {
        return translations;
    }

    public void setTranslations(List<Translation> translations) {
        this.translations = translations;
    }


    public class DetectedLanguage {

        @SerializedName("language")
        @Expose
        private String language;
        @SerializedName("score")
        @Expose
        private Integer score;

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }

    }

    public class Translation {

        @SerializedName("text")
        @Expose
        private String text;
        @SerializedName("to")
        @Expose
        private String to;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }

    }
}
