package com.example.lovecounter.base.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import android.widget.Toast
import com.example.lovecounter.navigation.NavigationHost
import com.example.lovecounter.navigation.NavigationManager

abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    val navigationManager: NavigationManager
        get() = (requireActivity() as NavigationHost).navigationManager

    private var _binding: VB? = null
    val binding: VB get() = _binding!!

    abstract fun provideViewBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    open fun initViews() {}
    open fun initData() {}
    open fun initObserver() {}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = provideViewBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initData()
        initObserver()
    }

    fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    fun showLoading() {
        // Shared loading UI belongs here when a screen needs it.
    }

    fun hideLoading() {
        // Shared loading UI belongs here when a screen needs it.
    }

    fun showInterAd(onAdClosed: () -> Unit) {
        // Can be implemented via the app's central ad utility.
        onAdClosed()
    }

    fun showRewardAd(onRewardGranted: () -> Unit) {
        // Can be implemented via the app's central ad utility.
        onRewardGranted()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
