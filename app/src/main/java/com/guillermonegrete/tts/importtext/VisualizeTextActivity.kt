package com.guillermonegrete.tts.importtext

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.guillermonegrete.tts.R

class VisualizeTextActivity: AppCompatActivity() {

    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visualize_text)

        val text = intent?.extras?.getString(IMPORTED_TEXT) ?: "No text"

        viewPager = findViewById(R.id.text_reader_viewpager)
        viewPager.adapter = VisualizerAdapter(listOf(text, "Second page", "Third page"))

    }

    companion object{
        const val IMPORTED_TEXT = "imported_text"
    }
}