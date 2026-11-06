package com.example.lovecounter.component.bootstrap

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.example.lovecounter.R
import com.example.lovecounter.component.language.LanguageFragment
import com.example.lovecounter.component.onboarding.OnboardingFragment
import com.example.lovecounter.component.permission.PermissionFragment
import com.example.lovecounter.navigation.RootTab
import com.example.lovecounter.navigation.navigationManager
import com.example.lovecounter.utils.SpManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Decides the first destination without creating an additional splash Activity. */
@AndroidEntryPoint
class BootstrapFragment : Fragment(R.layout.fragment_bootstrap) {
    @Inject lateinit var spManager: SpManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        when {
            !spManager.isLanguageChosen() -> navigationManager.replaceRoot(
                LanguageFragment().apply {
                    arguments = bundleOf("isInitialSetup" to true)
                }
            )
            !spManager.isOnBoardingCompleted() -> navigationManager.replaceRoot(OnboardingFragment())
            !spManager.isPermissionFlowCompleted() -> navigationManager.replaceRoot(PermissionFragment())
            else -> navigationManager.navigateToRoot(RootTab.HOME)
        }
    }
}
