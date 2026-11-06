package com.example.lovecounter

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.lovecounter.component.splash.SplashFragment
import com.example.lovecounter.data.sources.CoupleProfileDataStore
import com.example.lovecounter.databinding.ActivityMainHostBinding
import com.example.lovecounter.navigation.NavigationHost
import com.example.lovecounter.navigation.NavigationManager
import com.example.lovecounter.navigation.RootTab
import com.example.lovecounter.utils.LocaleHelper
import com.example.lovecounter.utils.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking

/** The application's only Activity. Navigation is managed centrally by NavigationManager. */
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavigationHost {

    private lateinit var binding: ActivityMainHostBinding

    override lateinit var navigationManager: NavigationManager
        private set

    override fun attachBaseContext(newBase: Context) {
        val languageCode = try {
            runBlocking { CoupleProfileDataStore.readLanguageCode(newBase) }
        } catch (error: Exception) {
            Logger.e("Failed to load saved language", throwable = error)
            "en"
        }
        super.attachBaseContext(LocaleHelper.setLocale(newBase, languageCode))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainHostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navigationManager = NavigationManager(supportFragmentManager, binding.fragmentContainer.id)
        setupBottomNavigation()
        setupBackNavigation()

        if (savedInstanceState == null) {
            navigationManager.replaceRoot(SplashFragment())
        } else {
            navigationManager.onRestoreState()
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!navigationManager.goBack()) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupBottomNavigation() {
        navigationManager.addOnDestinationChangedListener { destination ->
            updateBottomNavigation(destination)
        }

        binding.bottomNavContainer.tabHome.setOnClickListener {
            navigationManager.navigateToRoot(RootTab.HOME)
        }
        binding.bottomNavContainer.tabLoveTest.setOnClickListener {
            navigationManager.navigateToRoot(RootTab.LOVE_TEST)
        }
        binding.bottomNavContainer.tabMemory.setOnClickListener {
            navigationManager.navigateToRoot(RootTab.MEMORY)
        }
    }

    private fun updateBottomNavigation(destination: Fragment?) {
        val tab = RootTab.fromFragment(destination)
        if (tab != null) {
            binding.bottomNavContainer.root.visibility = View.VISIBLE
            highlightTab(tab)
        } else {
            binding.bottomNavContainer.root.visibility = View.GONE
        }
    }

    private fun highlightTab(tab: RootTab) {
        val pink = ContextCompat.getColor(this, R.color.primary_pink)
        val inactive = ContextCompat.getColor(this, R.color.secondary_text)

        with(binding.bottomNavContainer) {
            listOf(imgTabHome, imgTabLoveTest, imgTabMemory).forEach { it.setColorFilter(inactive) }
            listOf(tvTabHome, tvTabLoveTest, tvTabMemory).forEach { it.setTextColor(inactive) }

            when (tab) {
                RootTab.HOME -> {
                    imgTabHome.setColorFilter(pink)
                    tvTabHome.setTextColor(pink)
                }
                RootTab.LOVE_TEST -> {
                    imgTabLoveTest.setColorFilter(pink)
                    tvTabLoveTest.setTextColor(pink)
                }
                RootTab.MEMORY -> {
                    imgTabMemory.setColorFilter(pink)
                    tvTabMemory.setTextColor(pink)
                }
            }
        }
    }
}