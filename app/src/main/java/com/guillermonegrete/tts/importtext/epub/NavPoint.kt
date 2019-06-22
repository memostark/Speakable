package com.guillermonegrete.tts.importtext.epub

data class NavPoint(val navLabel: String, val content: String){

    fun getContentWithoutTag(): String{
        val tagIndex = content.indexOf("#")
        return if(tagIndex > 0) content.substring(0, tagIndex) else content
    }
}