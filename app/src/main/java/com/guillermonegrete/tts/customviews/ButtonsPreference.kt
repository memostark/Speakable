package com.guillermonegrete.tts.customviews

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.guillermonegrete.tts.R

class ButtonsPreference: Preference {

    private var preferenceValue = false
    private lateinit var largeBtn: View
    private lateinit var smallBtn: View

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, android.R.attr.preferenceStyle)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle){
        layoutResource = R.layout.preference_window_size
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        if(holder != null) {
            holder.itemView.isClickable = false
            largeBtn = holder.findViewById(R.id.large_window_pref_btn)
            smallBtn = holder.findViewById(R.id.small_window_pref_btn)

            largeBtn.apply {
                isClickable = true
                setOnClickListener {
                    preferenceValue = true
                    persistBoolean(true)
                    updateSelectedButton()
                }
            }
            smallBtn.apply {
                isClickable = true
                setOnClickListener {
                    preferenceValue = false
                    persistBoolean(false)
                    updateSelectedButton()
                }
            }

            updateSelectedButton()
        }
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        val defaultReturnValue = if(defaultValue == null) DEFAULT_VALUE else defaultValue as Boolean
        preferenceValue =  getPersistedBoolean(defaultReturnValue)
    }

    override fun onGetDefaultValue(a: TypedArray?, index: Int): Boolean? {
        return a?.getBoolean(index, DEFAULT_VALUE)
    }

    private fun updateSelectedButton(){
        largeBtn.isSelected = preferenceValue == true
        smallBtn.isSelected = preferenceValue == false
    }

    companion object{
        const val DEFAULT_VALUE = true
    }

}