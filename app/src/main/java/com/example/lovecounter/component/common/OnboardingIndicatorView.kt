package com.example.lovecounter.component.common

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.example.lovecounter.R
import com.example.lovecounter.utils.ext.dpToPx

class OnboardingIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var pageCount: Int = 0
    private var currentPage: Int = 0

    init {
        orientation = HORIZONTAL
    }

    fun setup(count: Int) {
        this.pageCount = count
        removeAllViews()

        for (i in 0 until count) {
            val dot = View(context)
            val params = LayoutParams(
                if (i == currentPage) 20.dpToPx(context) else 6.dpToPx(context),
                6.dpToPx(context)
            )
            params.setMargins(3.dpToPx(context), 0, 3.dpToPx(context), 0)
            dot.layoutParams = params
            dot.setBackgroundResource(
                if (i == currentPage) R.drawable.bg_indicator_active else R.drawable.bg_indicator_inactive
            )
            addView(dot)
        }
    }

    fun setCurrentPage(position: Int) {
        if (position < 0 || position >= pageCount) return
        this.currentPage = position

        for (i in 0 until childCount) {
            val dot = getChildAt(i)
            val isSelected = i == position
            val params = dot.layoutParams as LayoutParams
            params.width = if (isSelected) 20.dpToPx(context) else 6.dpToPx(context)
            dot.layoutParams = params
            dot.setBackgroundResource(
                if (isSelected) R.drawable.bg_indicator_active else R.drawable.bg_indicator_inactive
            )
        }
    }
}