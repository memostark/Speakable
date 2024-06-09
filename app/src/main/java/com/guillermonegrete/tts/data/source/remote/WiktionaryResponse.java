package com.guillermonegrete.tts.data.source.remote;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class WiktionaryResponse {

    @SerializedName("parse")
    @Expose
    private Parse parse;

    public Parse getParse() {
        return parse;
    }

    public void setParse(Parse parse) {
        this.parse = parse;
    }

    public static class Parse {

        @SerializedName("text")
        @Expose
        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
