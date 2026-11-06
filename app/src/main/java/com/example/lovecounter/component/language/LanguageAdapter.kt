package com.example.lovecounter.component.language

import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.lovecounter.R
import com.example.lovecounter.base.adapter.BaseAdapter
import com.example.lovecounter.data.model.Language
import com.example.lovecounter.databinding.ItemLanguageBinding

class LanguageAdapter : BaseAdapter<Language, ItemLanguageBinding>() {

    private var selectedLanguageCode: String? = null
    var onSelectionChanged: ((Language?) -> Unit)? = null

    init {
        onItemClickListener = { language, _ -> selectLanguage(language) }
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup): ItemLanguageBinding {
        return ItemLanguageBinding.inflate(inflater, parent, false)
    }

    override fun bind(binding: ItemLanguageBinding, item: Language, position: Int) {
        binding.tvLanguageName.text = item.languageName
        binding.imgFlag.setImageResource(item.flagRes)

        val isSelected = item.languageCode == selectedLanguageCode
        if (isSelected) {
            binding.itemContainer.setBackgroundResource(R.drawable.bg_select_language)
        } else {
            binding.itemContainer.setBackgroundResource(R.drawable.bg_item_language)
        }
    }

    override fun submitList(newList: List<Language>) {
        val previousSelection = selectedLanguageCode
        super.submitList(newList)
        selectedLanguageCode = previousSelection?.takeIf { code ->
            mItems.any { it.languageCode == code }
        }
        onSelectionChanged?.invoke(getSelectedLanguage())
    }

    fun getSelectedLanguage(): Language? {
        return mItems.firstOrNull { it.languageCode == selectedLanguageCode }
    }

    fun clearSelection() {
        val oldPosition = mItems.indexOfFirst { it.languageCode == selectedLanguageCode }
        selectedLanguageCode = null
        if (oldPosition >= 0) notifyItemChanged(oldPosition)
        onSelectionChanged?.invoke(null)
    }

    private fun selectLanguage(language: Language) {
        val oldPosition = mItems.indexOfFirst { it.languageCode == selectedLanguageCode }
        val newPosition = mItems.indexOfFirst { it.languageCode == language.languageCode }
        if (newPosition < 0 || selectedLanguageCode == language.languageCode) return

        selectedLanguageCode = language.languageCode
        if (oldPosition >= 0) notifyItemChanged(oldPosition)
        notifyItemChanged(newPosition)
        onSelectionChanged?.invoke(language)
    }
}