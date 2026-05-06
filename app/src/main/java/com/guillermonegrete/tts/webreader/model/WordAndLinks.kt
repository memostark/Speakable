package com.guillermonegrete.tts.webreader.model

import android.os.Parcelable
import com.guillermonegrete.tts.db.ExternalLink
import kotlinx.parcelize.Parcelize

@Parcelize
data class WordAndLinks(val word: String, val links: List<ExternalLink>): Parcelable
