package com.example.lovecounter.component.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lovecounter.R
import com.example.lovecounter.base.fragment.BaseFragment
import com.example.lovecounter.component.common.ProfileAvatarView
import com.example.lovecounter.component.main.MainViewModel
import com.example.lovecounter.component.setting.SettingsFragment
import com.example.lovecounter.data.model.CoupleProfile
import com.example.lovecounter.data.repositories.AvatarRepository
import com.example.lovecounter.data.repositories.BackgroundRepository
import com.example.lovecounter.data.repositories.ImageStorageRepository
import com.example.lovecounter.data.sources.CoupleProfileDataStore
import com.example.lovecounter.databinding.FragmentHomeBinding
import com.example.lovecounter.utils.Constant
import com.example.lovecounter.utils.manger.SpManager
import com.example.lovecounter.utils.ads.AdPlacement
import com.example.lovecounter.utils.ads.InterAdsUtils
import com.example.lovecounter.utils.ext.loadLocalPreview
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>() {
    @Inject lateinit var spManager: SpManager
    @Inject lateinit var coupleProfileDataStore: CoupleProfileDataStore
    @Inject lateinit var avatarRepository: AvatarRepository
    @Inject lateinit var backgroundRepository: BackgroundRepository
    @Inject lateinit var imageStorageRepository: ImageStorageRepository
    private val viewModel: MainViewModel by viewModels()
    private var dateReceiverRegistered = false

    private val dateChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateLoveCounter()
        }
    }

    override fun provideViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentHomeBinding.inflate(inflater, container, false)

    override fun initViews() {
        binding.btnSetting.setOnClickListener { navigationManager.navigate(SettingsFragment()) }
        binding.btnGallery.setOnClickListener { openBackgroundDialog() }

        // Root-tab banner is owned by MainActivity so it persists between tabs.
        binding.bannerContainer.visibility = View.GONE

        val adUnitId = AdPlacement.INTER_MAIN_ID
        InterAdsUtils.loadInterAd(requireActivity(), adUnitId)

        binding.llLoveCounter.setOnClickListener {
            InterAdsUtils.showInterAd(requireActivity()) {
                navigationManager.navigate(StartDateFragment())
            }
        }
        binding.avatarUser.setOnClickListener { openProfileDialog(true) }
        binding.avatarPartner.setOnClickListener { openProfileDialog(false) }
        binding.liquidHeartHome.setProgress(85f, animate = false)
        loadProfilesFromDataStore()
        restoreSelectedBackground()
        updateLoveCounter()
    }

    override fun onResume() {
        super.onResume()
        updateLoveCounter()
        loadProfilesFromDataStore()
        restoreSelectedBackground()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(
            requireContext(),
            dateChangeReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        dateReceiverRegistered = true
        updateLoveCounter()
    }

    override fun onStop() {
        if (dateReceiverRegistered) {
            requireContext().unregisterReceiver(dateChangeReceiver)
            dateReceiverRegistered = false
        }
        super.onStop()
    }

    override fun initData() = viewModel.loadData()

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                spManager.startDateFlow.collect { startTimestamp ->
                    updateLoveCounter(startTimestamp)
                }
            }
        }
    }

    private fun loadProfilesFromDataStore() = viewLifecycleOwner.lifecycleScope.launch {
        val profile = coupleProfileDataStore.coupleProfileFlow.first()
        val avatars = avatarRepository.getAvatarPaths()
        updateHomeProfiles(profile, avatars?.maleAvatarPath, avatars?.femaleAvatarPath)
    }

    private fun updateHomeProfiles(
        profile: CoupleProfile,
        maleAvatarPath: String?,
        femaleAvatarPath: String?
    ) {
        val male = profile.maleProfile
        val female = profile.femaleProfile
        spManager.putString(Constant.KEY_SP_USER_NAME, male.name.ifEmpty { "Sasuke" })
        spManager.putString(Constant.KEY_SP_PARTNER_NAME, female.name.ifEmpty { "Naruto" })
        binding.avatarUser.setData(R.drawable.ic_avt_boy, male.name.ifEmpty { "Sasuke" }, when (male.gender.lowercase()) {
            "female" -> R.drawable.ic_women; "other" -> R.drawable.ic_gender_other; else -> R.drawable.ic_men
        }, male.calculateAge())
        binding.avatarPartner.setData(R.drawable.ic_avt_girl, female.name.ifEmpty { "Naruto" }, when (female.gender.lowercase()) {
            "male" -> R.drawable.ic_men; "other" -> R.drawable.ic_gender_other; else -> R.drawable.ic_women
        }, female.calculateAge())
        loadCustomAvatar(binding.avatarUser, maleAvatarPath)
        loadCustomAvatar(binding.avatarPartner, femaleAvatarPath)
    }

    private fun loadCustomAvatar(avatarView: ProfileAvatarView, path: String?) {
        avatarView.avatarImageView.loadLocalPreview(
            imageStorageRepository = imageStorageRepository,
            relativePath = path,
            scope = viewLifecycleOwner.lifecycleScope
        )
    }

    private fun openProfileDialog(isMale: Boolean) {
        CoupleProfileBottomSheetFragment.newInstance(isMale).also { dialog ->
            dialog.onProfileSavedListener = { loadProfilesFromDataStore() }
            dialog.show(parentFragmentManager, "CoupleProfileDialog")
        }
    }

    private fun openBackgroundDialog() {
        ChooseBackgroundBottomSheet().also { sheet ->
            sheet.onBackgroundSelectedListener = { result ->
                applyBackground(result)
            }
            sheet.show(parentFragmentManager, "ChooseBackgroundBottomSheet")
        }
    }

    private fun restoreSelectedBackground() = viewLifecycleOwner.lifecycleScope.launch {
        val selected = try {
            backgroundRepository.getSelectedBackground()
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            Log.e(TAG, "Failed to restore selected background", exception)
            binding.imgHero.tag = null
            binding.imgHero.setImageResource(R.drawable.bg_top_home)
            return@launch
        }
        binding.imgHero.loadLocalPreview(
            imageStorageRepository = imageStorageRepository,
            relativePath = selected?.localPath,
            scope = viewLifecycleOwner.lifecycleScope,
            placeholderRes = R.drawable.bg_top_home
        )
    }

    private fun applyBackground(background: Any) {
        when (background) {
            is Int -> {
                binding.imgHero.tag = null
                binding.imgHero.setImageResource(background)
            }
            is String -> {
                binding.imgHero.loadLocalPreview(
                    imageStorageRepository = imageStorageRepository,
                    relativePath = background,
                    scope = viewLifecycleOwner.lifecycleScope,
                    placeholderRes = R.drawable.bg_top_home
                )
            }
        }
    }

    private fun updateLoveCounter(startDateTimestamp: Long = spManager.getStartDate()) {
        binding.tvDaysCount.text = spManager.calculateDays(startDateTimestamp).toString()
    }

    private companion object {
        const val TAG = "HomeFragment"
    }
}
