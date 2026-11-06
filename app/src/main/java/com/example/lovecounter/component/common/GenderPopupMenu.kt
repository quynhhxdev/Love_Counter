package com.example.lovecounter.component.common

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.lovecounter.R
import com.example.lovecounter.databinding.PopupGenderMenuBinding

object GenderPopupMenu {
    fun show(
        context: Context,
        anchorView: View,
        layoutInflater: LayoutInflater,
        currentGender: String,
        onGenderSelected: (String) -> Unit
    ) {
        val popupBinding = PopupGenderMenuBinding.inflate(layoutInflater)
        val popupWindow = PopupWindow(
            popupBinding.root,
            anchorView.width,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.elevation = 8f
        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.bg_input_box))

        val pinkColor = ContextCompat.getColor(context, R.color.primary_pink)
        val darkColor = ContextCompat.getColor(context, R.color.primary_text)

        fun highlightOption(tv: TextView, optionName: String) {
            if (currentGender.equals(optionName, ignoreCase = true)) {
                tv.setTextColor(pinkColor)
            } else {
                tv.setTextColor(darkColor)
            }
        }

        popupBinding.tvMale.setText(R.string.male)
        popupBinding.tvFemale.setText(R.string.female)
        popupBinding.tvOther.setText(R.string.other)
        highlightOption(popupBinding.tvMale, "MALE")
        highlightOption(popupBinding.tvFemale, "FEMALE")
        highlightOption(popupBinding.tvOther, "OTHER")

        fun selectGender(selected: String) {
            onGenderSelected(selected)
            popupWindow.dismiss()
        }

        popupBinding.tvMale.setOnClickListener { selectGender("MALE") }
        popupBinding.tvFemale.setOnClickListener { selectGender("FEMALE") }
        popupBinding.tvOther.setOnClickListener { selectGender("OTHER") }

        popupWindow.showAsDropDown(anchorView, 0, 4)
    }
}
