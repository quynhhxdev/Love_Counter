package com.example.lovecounter.component.setting

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.lovecounter.R
import com.example.lovecounter.base.fragment.BaseFragment
import com.example.lovecounter.component.language.LanguageFragment
import com.example.lovecounter.databinding.FragmentSettingsBinding

class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {
    override fun provideViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentSettingsBinding.inflate(inflater, container, false)

    override fun initViews() {
        // Banner is owned and managed by MainActivity at the bottom of the screen
        binding.bannerContainer.visibility = View.GONE

        binding.btnBack.setOnClickListener { navigationManager.goBack() }
        binding.btnLanguage.setOnClickListener { navigationManager.navigate(LanguageFragment()) }
        binding.btnRate.setOnClickListener { showToast(getString(R.string.rate_app_clicked)) }
        binding.btnShare.setOnClickListener { showToast(getString(R.string.share_app_clicked)) }
        binding.btnFeedback.setOnClickListener { showToast(getString(R.string.feedback_clicked)) }
        binding.btnPrivacy.setOnClickListener { showToast(getString(R.string.privacy_policy_clicked)) }
    }
}
