package com.guillermonegrete.tts.webreader.model

import com.guillermonegrete.tts.db.ExternalLink

data class WordAndLinks(val word: String, val links: List<ExternalLink>)
