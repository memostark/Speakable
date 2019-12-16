package com.guillermonegrete.tts.ui

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.ArrayRes
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes

/**
 * Custom adapter for android spinner widget that shows different string values for closed view and drop down list view.
 */
class DifferentValuesAdapter(
    context: Context,
    @LayoutRes private val resource: Int,
    @IdRes private val fieldId: Int = 0,
    private val closedViewItems: Array<String>,
    listViewItems: Array<String>
): ArrayAdapter<String>(context, resource, listViewItems) {
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    /**
     * Similar code to parent class getView method but sets text to a value from member array closedViewItems.
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val view = convertView ?: inflater.inflate(resource, parent, false)
        val text: TextView

        try {
            if (fieldId == 0) {
             //  If no custom field is assigned, assume the whole resource is a TextView
                text = view as TextView
            } else {
                //  Otherwise, find the TextView field within the layout
                text = view.findViewById(fieldId)

                if (text == null) {
                    throw RuntimeException(
                        "Failed to find view with ID "
                        + context.resources.getResourceName(fieldId)
                        + " in item layout"
                    )
                }
            }
        } catch (e:ClassCastException) {
            Log.e("ArrayAdapter", "You must supply a resource ID for a TextView")
            throw IllegalStateException(
                "ArrayAdapter requires the resource ID to be a TextView", e)
        }

        val item = closedViewItems[position]
        text.text = item
        return view
    }

    companion object{
        @JvmStatic fun createFromResource(
            context: Context,
            @ArrayRes closedViewArrayResId: Int,
            @ArrayRes listViewArrayResId: Int,
            @LayoutRes textViewResId: Int
        ): DifferentValuesAdapter{
            val closedViewArray = context.resources.getTextArray(closedViewArrayResId).map { it.toString() }.toTypedArray()
            val listViewArray = context.resources.getTextArray(listViewArrayResId).map { it.toString() }.toTypedArray()
            return DifferentValuesAdapter(context, textViewResId, 0, closedViewArray, listViewArray)
        }
    }
}