package com.example.lovecounter.component.language

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lovecounter.R
import com.example.lovecounter.base.fragment.BaseFragment
import com.example.lovecounter.component.onboarding.OnboardingFragment
import com.example.lovecounter.databinding.FragmentLanguageBinding
import com.example.lovecounter.utils.FirebaseConfigManager
import com.example.lovecounter.utils.manger.LanguageManager
import com.example.lovecounter.utils.Logger
import com.example.lovecounter.utils.ads.AdPlacement
import com.example.lovecounter.utils.ads.NativeAdsUtils
import com.example.lovecounter.utils.ext.gone
import com.example.lovecounter.utils.ext.visible
import com.google.android.gms.ads.nativead.NativeAdView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LanguageFragment : BaseFragment<FragmentLanguageBinding>() {
    @Inject lateinit var languageManager: LanguageManager
    private val viewModel: LanguageViewModel by viewModels()
    private val adapter by lazy { LanguageAdapter() }
    private val isInitialSetup get() = arguments?.getBoolean("isInitialSetup") ?: false

    override fun provideViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentLanguageBinding.inflate(
            inflater.cloneInContext(
                ContextThemeWrapper(
                    languageManager.localizedContext(inflater.context),
                    R.style.Theme_LoveCounter,
                ),
            ),
            container,
            false
        )

    override fun initViews() {
        setupHeader()
        adapter.onSelectionChanged = { selectedLanguage ->
            if (selectedLanguage == null) binding.btnSave.gone()
            else binding.btnSave.visible()
        }
        adapter.clearSelection()
        binding.rvLanguage.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLanguage.adapter = adapter
        binding.btnSave.setOnClickListener {
            val selected = adapter.getSelectedLanguage()
                ?: return@setOnClickListener showToast(getString(R.string.please_select_language))

            val currentActivity = activity ?: return@setOnClickListener
            binding.btnSave.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    languageManager.changeLanguage(selected.languageCode)
                    if (!isAdded) return@launch

                    NativeAdsUtils.destroyNative(AdPlacement.LANGUAGE_NATIVE)

                    if (isInitialSetup) {
                        navigationManager.replaceRoot(OnboardingFragment())
                    } else {
                        navigationManager.goBack()
                    }
                    currentActivity.recreate()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Logger.e("Failed to save selected language", throwable = error)
                    if (isAdded) {
                        binding.btnSave.isEnabled = true
                    }
                    showToast(getString(R.string.failed_to_save_language))
                }
            }
        }
    }

    private fun setupHeader() {
        binding.btnSave.setImageResource(R.drawable.ic_check)
        binding.btnSave.gone()
        if (isInitialSetup) {
            binding.btnBack.gone()
            ConstraintSet().also { set ->
                set.clone(binding.header)
                set.connect(R.id.tvTitle, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                set.clear(R.id.tvTitle, ConstraintSet.END)
                set.applyTo(binding.header)
            }
        } else {
            binding.btnBack.visible()
            binding.btnBack.setOnClickListener { navigationManager.goBack() }
        }
    }

    override fun initData() {
        viewModel.loadLanguages()
        if (!FirebaseConfigManager.isNativeAdsEnabled()) {
            binding.flNativeAdContainer.gone()
            return
        }
        // Show medium shimmer placeholder immediately while loading
        binding.flNativeAdContainer.visible()
        binding.flNativeAdContainer.removeAllViews()
        layoutInflater.inflate(com.ads.admob.R.layout.loading_native_ad_view_type1, binding.flNativeAdContainer, true)

        val adUnitId = AdPlacement.TEST_NATIVE_ID
        if (isInitialSetup) {
            NativeAdsUtils.preloadNative(
                requireContext().applicationContext,
                AdPlacement.ONBOARDING_NATIVE,
                adUnitId
            )
        }
        NativeAdsUtils.preloadNative(requireContext().applicationContext, AdPlacement.LANGUAGE_NATIVE, adUnitId) { success ->
            if (success && isAdded) {
                val nativeAd = NativeAdsUtils.getPreloadedNative(AdPlacement.LANGUAGE_NATIVE)
                if (nativeAd != null) {
                    binding.flNativeAdContainer.removeAllViews()
                    val adView = layoutInflater.inflate(com.ads.admob.R.layout.layout_native_ad_view_type1, binding.flNativeAdContainer, false) as? NativeAdView
                    if (adView != null) {
                        NativeAdsUtils.populateNativeAdView(nativeAd, adView)
                        binding.flNativeAdContainer.addView(adView)
                        binding.flNativeAdContainer.visible()
                    }
                }
            } else if (isAdded) {
                binding.flNativeAdContainer.visible()
            }
        }
    }

    override fun initObserver() {
        viewModel.languages.observe(viewLifecycleOwner) { languages ->
            adapter.submitList(languages)
        }
    }
}
