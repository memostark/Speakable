package com.guillermonegrete.tts.importtext.visualize.model

import android.os.Parcelable
import com.guillermonegrete.tts.common.models.Span
import kotlinx.parcelize.Parcelize

@Parcelize
data class SplitPageSpan(val topSpan: Span, val bottomSpan: Span): Parcelable
