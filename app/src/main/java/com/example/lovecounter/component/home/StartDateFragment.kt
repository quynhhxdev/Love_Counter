package com.example.lovecounter.component.home

import android.widget.Toast
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.example.lovecounter.R
import com.example.lovecounter.base.fragment.BaseFragment
import com.example.lovecounter.component.common.LoveDatePickerDialogFragment
import com.example.lovecounter.databinding.FragmentStartDateBinding
import com.example.lovecounter.utils.handle.DateUtils
import com.example.lovecounter.utils.manger.SpManager
import com.example.lovecounter.utils.ads.AdPlacement
import com.example.lovecounter.utils.ads.InterAdsUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import java.util.Calendar
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class StartDateFragment : BaseFragment<FragmentStartDateBinding>() {
    @Inject lateinit var spManager: SpManager
    private var selectedDate = Calendar.getInstance()

    override fun provideViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentStartDateBinding.inflate(inflater, container, false)

    override fun initViews() {
        // Banner is owned and managed by MainActivity at the bottom of the screen
        binding.bannerContainer.visibility = View.GONE

        val saved = spManager.getStartDate()
        if (saved > 0) selectedDate.timeInMillis = saved else selectedDate.set(2023, Calendar.SEPTEMBER, 11)
        updateDate()

        val adUnitId = AdPlacement.INTER_MAIN_ID
        InterAdsUtils.loadInterAd(requireActivity(), adUnitId)

        parentFragmentManager.setFragmentResultListener(DATE_RESULT, viewLifecycleOwner) { _, result ->
            selectedDate.timeInMillis = result.getLong(LoveDatePickerDialogFragment.RESULT_DATE_MILLIS)
            updateDate()
        }
        binding.btnBack.setOnClickListener { navigationManager.goBack() }
        binding.cardDateOfLove.setOnClickListener {
            LoveDatePickerDialogFragment.newInstance(DATE_RESULT, selectedDate.timeInMillis)
                .show(parentFragmentManager, "LoveDatePicker")
        }
        binding.btnDone.setOnClickListener {
            InterAdsUtils.showInterAd(requireActivity()) {
                val timestamp = selectedDate.timeInMillis
                binding.btnDone.isEnabled = false
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        if (spManager.saveStartDate(timestamp)) {
                            navigationManager.goBack()
                        } else {
                            Toast.makeText(requireContext(), R.string.failed_to_save_start_date, Toast.LENGTH_SHORT).show()
                        }
                    } catch (exception: Exception) {
                        if (exception is CancellationException) throw exception
                        if (isAdded) {
                            Toast.makeText(requireContext(), R.string.failed_to_save_start_date, Toast.LENGTH_SHORT).show()
                        }
                    } finally {
                        if (isAdded) binding.btnDone.isEnabled = true
                    }
                }
            }
        }
    }

    private fun updateDate() { binding.tvSelectedDate.text = DateUtils.formatDate(selectedDate.timeInMillis) }
    companion object { private const val DATE_RESULT = "start_date_picker_result" }
}
