package com.example.lovecounter.component.common

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import com.example.lovecounter.R

class PrimaryButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        val customTypeface = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_bold)
        setTypeface(customTypeface)
        setTextColor(ResourcesCompat.getColor(resources, R.color.primary_text, null))
        textSize = 14f
    }
}