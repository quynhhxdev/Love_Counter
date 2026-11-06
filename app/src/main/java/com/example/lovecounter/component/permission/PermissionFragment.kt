package com.example.lovecounter.component.permission

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.lovecounter.base.fragment.BaseFragment
import com.example.lovecounter.databinding.FragmentPermissionBinding
import com.example.lovecounter.navigation.RootTab
import com.example.lovecounter.utils.PermissionUtils
import com.example.lovecounter.utils.SpManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PermissionFragment : BaseFragment<FragmentPermissionBinding>() {
    @Inject lateinit var spManager: SpManager
    private val targetPermission get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.POST_NOTIFICATIONS else Manifest.permission.READ_EXTERNAL_STORAGE

    override fun provideViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentPermissionBinding.inflate(inflater, container, false)

    override fun initViews() {
        updatePermissionSwitchState()
        binding.switchPermission.setOnCheckedChangeListener { _, checked ->
            val granted = PermissionUtils.isPermissionGranted(requireContext(), targetPermission)
            if (checked && !granted) PermissionUtils.requestPermissions(requireActivity(), listOf(targetPermission)) {
                binding.switchPermission.isChecked = it
            } else if (!checked && granted) openAppSettings()
        }
        binding.btnContinue.setOnClickListener {
            spManager.setPermissionFlowCompleted(true)
            navigationManager.navigateToRoot(RootTab.HOME)
        }
    }

    override fun onResume() { super.onResume(); updatePermissionSwitchState() }
    private fun updatePermissionSwitchState() { binding.switchPermission.isChecked = PermissionUtils.isPermissionGranted(requireContext(), targetPermission) }
    private fun openAppSettings() = startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", requireContext().packageName, null)
    })
}
