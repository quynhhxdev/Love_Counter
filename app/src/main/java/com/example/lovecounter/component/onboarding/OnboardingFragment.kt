package com.example.lovecounter.component.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.example.lovecounter.base.fragment.BaseFragment
import com.example.lovecounter.component.permission.PermissionFragment
import com.example.lovecounter.databinding.FragmentOnboardingBinding
import com.example.lovecounter.utils.SpManager
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
    }
}
