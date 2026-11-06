package com.example.lovecounter.component.common

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.example.lovecounter.databinding.ViewProfileAvatarBinding

class ProfileAvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewProfileAvatarBinding =
        ViewProfileAvatarBinding.inflate(LayoutInflater.from(context), this, true)

    val avatarImageView: android.widget.ImageView
        get() = binding.imgAvatar

    fun setData(imageRes: Int, name: String, genderRes: Int? = null, age: String? = null) {
        binding.imgAvatar.setImageResource(imageRes)
        binding.tvName.text = name
        binding.tvName.visibility = VISIBLE

        if (genderRes != null || age != null) {
            binding.llGenderAge.visibility = VISIBLE
            if (genderRes != null) {
                binding.imgGender.visibility = VISIBLE
                binding.imgGender.setImageResource(genderRes)
            } else {
                binding.imgGender.visibility = GONE
            }
            if (age != null) {
                binding.tvAge.visibility = VISIBLE
                binding.tvAge.text = age
            } else {
                binding.tvAge.visibility = GONE
            }
        } else {
            binding.llGenderAge.visibility = GONE
        }
    }

    fun setZodiacData(avatarRes: Int, zodiacIconRes: Int, zodiacName: String) {
        binding.imgAvatar.setImageResource(avatarRes)
        binding.tvName.visibility = GONE
        binding.llGenderAge.visibility = VISIBLE
        binding.imgGender.visibility = VISIBLE
        binding.imgGender.setImageResource(zodiacIconRes)
        binding.tvAge.visibility = VISIBLE
        binding.tvAge.text = zodiacName
    }

    fun setAvatarBitmap(bitmap: Bitmap) {
        binding.imgAvatar.setImageBitmap(bitmap)
    }
}
