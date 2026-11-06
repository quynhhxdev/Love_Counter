package com.example.lovecounter.component.lovetest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.lovecounter.component.lovetest.horoscopetest.HoroscopeTestFragment
import com.example.lovecounter.component.lovetest.nametest.NameTestFragment
import com.example.lovecounter.databinding.FragmentLoveTestBinding
import com.example.lovecounter.navigation.navigationManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoveTestFragment : Fragment() {

    private var _binding: FragmentLoveTestBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoveTestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardNameTest.setOnClickListener {
            navigationManager.navigate(NameTestFragment())
        }

        binding.cardHoroscopeTest.setOnClickListener {
            navigationManager.navigate(HoroscopeTestFragment())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
