package com.example.lovecounter.component.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.example.lovecounter.base.fragment.BaseFragment
import com.example.lovecounter.component.permission.PermissionFragment
import com.example.lovecounter.databinding.FragmentOnboardingBinding
import com.example.lovecounter.utils.FirebaseConfigManager
import com.example.lovecounter.utils.manger.SpManager
import com.example.lovecounter.utils.ads.AdPlacement
import com.example.lovecounter.utils.ads.NativeAdsUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OnboardingFragment : BaseFragment<FragmentOnboardingBinding>() {
    @Inject lateinit var spManager: SpManager
    private val viewModel: OnBoardingViewModel by viewModels()
    private val adapter by lazy { OnBoardingAdapter() }

    override fun provideViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentOnboardingBinding.inflate(inflater, container, false)

    override fun initViews() {
        binding.viewPager.adapter = adapter
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = binding.indicatorView.setCurrentPage(position)
        })
        binding.btnNext.setOnClickListener {
            if (binding.viewPager.currentItem < adapter.itemCount - 1) {
                binding.viewPager.currentItem++
            } else {
                spManager.setOnBoardingCompleted(true)
                navigationManager.replaceRoot(PermissionFragment())
            }
        }
    }

    override fun initData() {
        viewModel.onBoardingList.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.indicatorView.setup(list.size)
        }
        viewModel.loadOnBoarding()

        if (!FirebaseConfigManager.isNativeAdsEnabled()) {
            return
        }

        val adUnitId = AdPlacement.TEST_NATIVE_ID
        NativeAdsUtils.preloadNative(
            requireContext().applicationContext,
            AdPlacement.PERMISSION_NATIVE,
            adUnitId
        )
        NativeAdsUtils.preloadNative(requireContext().applicationContext, AdPlacement.ONBOARDING_NATIVE, adUnitId) { success ->
            if (success && isAdded) {
                val nativeAd = NativeAdsUtils.getPreloadedNative(AdPlacement.ONBOARDING_NATIVE)
                adapter.setNativeAd(nativeAd)
            }
        }
    }
}
