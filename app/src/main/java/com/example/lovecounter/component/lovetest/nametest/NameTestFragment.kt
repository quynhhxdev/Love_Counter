package com.example.lovecounter.component.lovetest.nametest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.lovecounter.R
import com.example.lovecounter.component.common.LoveTestLoadingDialog
import com.example.lovecounter.component.lovetest.result.TestResultFragment
import com.example.lovecounter.databinding.FragmentNameTestBinding
import com.example.lovecounter.navigation.navigationManager
import com.example.lovecounter.utils.LoveTestHelper
import com.example.lovecounter.utils.SpManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NameTestFragment : Fragment() {

    @Inject
    lateinit var spManager: SpManager

    private var _binding: FragmentNameTestBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNameTestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etYourName.setText(spManager.getUserName())
        binding.etPartnerName.setText(spManager.getPartnerName())

        binding.btnBack.setOnClickListener {
            navigationManager.goBack()
        }

        binding.btnLoveTest.setOnClickListener {
            val yourName = binding.etYourName.text.toString().trim()
            val partnerName = binding.etPartnerName.text.toString().trim()

            if (yourName.isEmpty() || partnerName.isEmpty()) {
                Toast.makeText(requireContext(), R.string.please_enter_both_names, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnLoveTest.isEnabled = false
            val result = LoveTestHelper.calculateNameCompatibility(yourName, partnerName)
            LoveTestLoadingDialog.newInstance {
                if (isAdded) {
                    navigationManager.navigate(TestResultFragment().apply { arguments = result.toBundle() })
                }
            }.show(parentFragmentManager, "LoveTestLoading")
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
}
