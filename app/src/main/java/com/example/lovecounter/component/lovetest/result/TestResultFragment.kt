package com.example.lovecounter.component.lovetest.result

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.lovecounter.R
import com.example.lovecounter.data.model.ZodiacSign
import com.example.lovecounter.data.repositories.AvatarRepository
import com.example.lovecounter.data.repositories.ImageStorageRepository
import com.example.lovecounter.databinding.FragmentTestResultBinding
import com.example.lovecounter.navigation.navigationManager
import com.example.lovecounter.utils.handle.LoveTestResult
import com.example.lovecounter.utils.handle.LoveTestType
import com.example.lovecounter.utils.ext.loadLocalPreview
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TestResultFragment : Fragment() {

    @Inject
    lateinit var avatarRepository: AvatarRepository

    @Inject
    lateinit var imageStorageRepository: ImageStorageRepository
    private var _binding: FragmentTestResultBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTestResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Banner is owned and managed by MainActivity at the bottom of the screen
        binding.bannerContainer.visibility = View.GONE

        val testResult = LoveTestResult.fromBundle(arguments)

        val targetScore = testResult.score

        //animation test result
        testResultAnimation(targetScore)

        binding.tvAdvice.text = testResult.adviceText

        // Tải ảnh đại diện tùy chỉnh nếu người dùng đã cài đặt trong dialog_couple_profile

        loadCustomAvatars(testResult)

        binding.btnBack.setOnClickListener {
            navigationManager.goBack()
        }

        binding.btnRetry.setOnClickListener {
            navigationManager.goBack()
        }

        binding.btnShare.setOnClickListener {
            // Share action
        }
    }

    private fun testResultAnimation(targetScore: Int) {
        binding.tvScorePercentage.text = "0%"
        binding.liquidHeartResult.setProgress(0f, false)

        binding.liquidHeartResult.post {
            android.animation.ValueAnimator.ofInt(0, targetScore).apply {
                duration = 1800L
                interpolator = android.view.animation.LinearInterpolator()
                addUpdateListener { animator ->
                    val value = animator.animatedValue as Int
                    if (_binding != null) {
                        binding.tvScorePercentage.text = "$value%"
                        binding.liquidHeartResult.setProgress(value.toFloat(), false)
                    }
                }

                start()
            }
        }

    }

    private fun loadCustomAvatars(testResult: LoveTestResult) {
        viewLifecycleOwner.lifecycleScope.launch {
            //load db avt từ db
            val avatarEntity = avatarRepository.getAvatarPaths()
            val malePath = avatarEntity?.maleAvatarPath
            val femalePath = avatarEntity?.femaleAvatarPath

            //nếu có ảnh thì truyền 0, nếu không có hiển thị ảnh mặc định
            val userDefaultAvatars = if (malePath.isNullOrBlank()) R.drawable.ic_avt_boy else 0
            val partnerDefaultAvaters =
                if (femalePath.isNullOrBlank()) R.drawable.ic_avt_girl else 0

            if (testResult.testType == LoveTestType.HOROSCOPE) {
                val userZodiac = ZodiacSign.fromId(testResult.firstZodiacId)
                val partnerZodiac = ZodiacSign.fromId(testResult.secondZodiacId)

                binding.avatarUserResult.setZodiacData(
                    avatarRes = userDefaultAvatars,
                    zodiacIconRes = userZodiac?.iconRes ?: R.drawable.ic_aries,
                    zodiacName = testResult.firstSubject
                )

                binding.avatarPartnerResult.setZodiacData(
                    avatarRes = partnerDefaultAvaters,
                    zodiacIconRes = partnerZodiac?.iconRes ?: R.drawable.ic_leo,
                    zodiacName = testResult.secondSubject
                )
            } else {
                binding.avatarUserResult.setData(
                    imageRes = userDefaultAvatars,
                    name = testResult.firstSubject
                )

                binding.avatarPartnerResult.setData(
                    imageRes = partnerDefaultAvaters,
                    name = testResult.secondSubject
                )
            }

            // Tải ảnh tùy chỉnh nếu có đường dẫn
            malePath?.takeIf { it.isNotBlank() }?.let { path ->
                binding.avatarUserResult.avatarImageView.loadLocalPreview(
                    imageStorageRepository = imageStorageRepository,
                    relativePath = path,
                    scope = viewLifecycleOwner.lifecycleScope
                )
            }

            femalePath?.takeIf { it.isNotBlank() }?.let { path ->
                binding.avatarPartnerResult.avatarImageView.loadLocalPreview(
                    imageStorageRepository = imageStorageRepository,
                    relativePath = path,
                    scope = viewLifecycleOwner.lifecycleScope
                )
            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
