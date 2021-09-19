package com.guillermonegrete.tts.importtext.visualize.model

data class Span(val start: Int, val end: Int)

data class SplitPageSpan(val topSpan: Span, val bottomSpan: Span)