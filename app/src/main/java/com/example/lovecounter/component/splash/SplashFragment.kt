package com.example.lovecounter.component.splash

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.lovecounter.R
import com.example.lovecounter.component.bootstrap.BootstrapFragment
import com.example.lovecounter.navigation.navigationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : Fragment(R.layout.fragment_splash) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            delay(SPLASH_DURATION_MS)
            navigationManager.replaceRoot(BootstrapFragment())
        }
    }

    private companion object {
        const val SPLASH_DURATION_MS = 2_000L
    }
}
