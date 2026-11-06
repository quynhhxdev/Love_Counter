package com.example.lovecounter.component.lovetest.horoscopetest

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lovecounter.R
import com.example.lovecounter.component.common.LoveTestLoadingDialog
import com.example.lovecounter.component.lovetest.result.TestResultFragment
import com.example.lovecounter.data.model.ZodiacSign
import com.example.lovecounter.data.sources.CoupleProfileDataStore
import com.example.lovecounter.databinding.FragmentHoroscopeTestBinding
import com.example.lovecounter.navigation.navigationManager
import com.example.lovecounter.utils.LoveTestHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HoroscopeTestFragment : Fragment() {

    private var _binding: FragmentHoroscopeTestBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var coupleProfileDataStore: CoupleProfileDataStore

    private var userZodiac = ZodiacSign.ARIES
    private var partnerZodiac = ZodiacSign.LEO

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHoroscopeTestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                coupleProfileDataStore.horoscopeSelectionFlow
                    .catch { error ->
                        Log.e(TAG, "Failed to observe saved horoscope selection", error)
                        Toast.makeText(requireContext(), R.string.unable_to_load_horoscope, Toast.LENGTH_SHORT).show()
                    }
                    .collect { selection ->
                        userZodiac = selection.user
                        partnerZodiac = selection.partner
                        updateHoroscopeViews()
                    }
            }
        }

        binding.btnBack.setOnClickListener {
            navigationManager.goBack()
        }

        binding.layoutUserZodiac.setOnClickListener {
            showHoroscopeDialog(isUser = true)
        }

        binding.layoutPartnerZodiac.setOnClickListener {
            showHoroscopeDialog(isUser = false)
        }

        binding.btnLoveTest.setOnClickListener {
            binding.btnLoveTest.isEnabled = false
            val result = LoveTestHelper.calculateHoroscopeCompatibility(
                getString(userZodiac.displayNameRes),
                getString(partnerZodiac.displayNameRes),
                userZodiac.name,
                partnerZodiac.name
            )
            LoveTestLoadingDialog.newInstance {
                if (isAdded) {
                    navigationManager.navigate(TestResultFragment().apply { arguments = result.toBundle() })
                }
            }.show(parentFragmentManager, "LoveTestLoading")
        }
    }

    private fun showHoroscopeDialog(isUser: Boolean) {
        val sheet = HoroscopeBottomSheet.newInstance(isUser)
        sheet.onZodiacSelectedListener = { zodiac ->
            if (isUser) {
                userZodiac = zodiac
            } else {
                partnerZodiac = zodiac
            }
            updateHoroscopeViews()
        }
        sheet.show(parentFragmentManager, "HoroscopeBottomSheet")
    }

    private fun updateHoroscopeViews() {
        _binding?.let { currentBinding ->
            currentBinding.imgUserZodiac.setImageResource(userZodiac.iconRes)
            currentBinding.tvUserZodiacName.setText(userZodiac.displayNameRes)
            currentBinding.imgPartnerZodiac.setImageResource(partnerZodiac.iconRes)
            currentBinding.tvPartnerZodiacName.setText(partnerZodiac.displayNameRes)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            // Màn hình đã được hiển thị lại, kích hoạt nút
            _binding?.btnLoveTest?.isEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "HoroscopeTestFragment"
    }
}
