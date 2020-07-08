package com.guillermonegrete.tts.importtext.epub

class TableOfContents(points: List<NavPoint> = listOf()) {

    private val _navPoints = points.toMutableList()
    val navPoints: List<NavPoint>
        get() = _navPoints

    fun add(navPoint: NavPoint){
        _navPoints.add(navPoint)
    }
}