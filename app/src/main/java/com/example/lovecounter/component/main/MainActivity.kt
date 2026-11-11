package com.example.lovecounter.component.main

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.constraintlayout.widget.ConstraintLayout
import com.ads.admob.helper.banner.BannerAdHelper
import com.ads.admob.helper.banner.params.AdBannerState
import com.ads.admob.listener.BannerAdCallBack
import com.example.lovecounter.R
import com.example.lovecounter.component.home.HomeFragment
import com.example.lovecounter.component.home.StartDateFragment
import com.example.lovecounter.component.lovetest.LoveTestFragment
import com.example.lovecounter.component.lovetest.horoscopetest.HoroscopeTestFragment
import com.example.lovecounter.component.lovetest.nametest.NameTestFragment
import com.example.lovecounter.component.lovetest.result.TestResultFragment
import com.example.lovecounter.component.memory.AddMemoryFragment
import com.example.lovecounter.component.memory.MemoryFragment
import com.example.lovecounter.component.setting.SettingsFragment
import com.example.lovecounter.component.splash.SplashFragment
import com.example.lovecounter.data.sources.CoupleProfileDataStore
import com.example.lovecounter.databinding.ActivityMainHostBinding
import com.example.lovecounter.navigation.NavigationHost
import com.example.lovecounter.navigation.NavigationManager
import com.example.lovecounter.navigation.RootTab
import com.example.lovecounter.utils.LocaleHelper
import com.example.lovecounter.utils.Logger
import com.example.lovecounter.utils.ads.AdPlacement
import com.example.lovecounter.utils.ads.BannerAdsUtils
import com.example.lovecounter.utils.ads.InterAdsUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking

/** The application's only Activity. Navigation is managed centrally by NavigationManager. */
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavigationHost {

    private lateinit var binding: ActivityMainHostBinding
    private var mainBannerHelper: BannerAdHelper? = null
    private var hasRequestedMainBanner = false
    private var isBannerLoaded = false
    private var isRootTabVisible = false
    private var systemBarsBottomInset = 0

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
        setupWindowInsets()
        setupBottomNavigation()
        setupBackNavigation()

        InterAdsUtils.loadInterAd(this, AdPlacement.INTER_MAIN_ID)

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
            InterAdsUtils.showInterAd(this) {
                navigationManager.navigateToRoot(RootTab.MEMORY)
            }
        }
    }

    private fun updateBottomNavigation(destination: Fragment?) {
        val tab = RootTab.fromFragment(destination)
        isRootTabVisible = tab != null
        if (isRootTabVisible) {
            binding.bottomNavContainer.root.visibility = View.VISIBLE
            tab?.let { highlightTab(it) }
        } else {
            binding.bottomNavContainer.root.visibility = View.GONE
        }

        val shouldShowBanner = shouldDisplayBanner(destination)
        if (shouldShowBanner) {
            ensureMainBannerLoaded()
            val isBannerLoading = mainBannerHelper?.state?.value == AdBannerState.LOADING
            setMainBannerVisible(isBannerLoaded || isBannerLoading)
        } else {
            setMainBannerVisible(false)
        }
        applyBottomBarInsets()
    }

    private fun shouldDisplayBanner(destination: Fragment?): Boolean {
        return when (destination) {
            is HomeFragment,
            is LoveTestFragment,
            is MemoryFragment,
            is StartDateFragment,
            is HoroscopeTestFragment,
            is NameTestFragment,
            is AddMemoryFragment,
            is TestResultFragment,
            is SettingsFragment -> true
            else -> false
        }
    }

    /** A single banner is owned by the Activity, so it persists across all banner-enabled screens. */
    private fun ensureMainBannerLoaded() {
        if (mainBannerHelper == null) {
            hasRequestedMainBanner = true
            mainBannerHelper = BannerAdsUtils.loadBanner(
                context = this,
                container = binding.bannerContainer,
                adId = AdPlacement.BANNER_MAIN_ID,
                callback = object : BannerAdCallBack {
                    override fun onAdLoaded() {
                        isBannerLoaded = true
                        val currentDest = navigationManager.getCurrentFragment()
                        setMainBannerVisible(shouldDisplayBanner(currentDest))
                    }

                    override fun onAdFailedToLoad(error: String) {
                        isBannerLoaded = false
                        setMainBannerVisible(false)
                    }

                    override fun onAdClicked() = Unit
                    override fun onAdImpression() = Unit
                }
            )
        } else if (!isBannerLoaded && mainBannerHelper?.state?.value == AdBannerState.FAIL) {
            mainBannerHelper?.reloadBanner()
        }
    }

    private fun setMainBannerVisible(visible: Boolean) {
        binding.bannerContainer.visibility = if (visible) View.VISIBLE else View.GONE
        applyBottomBarInsets()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            systemBarsBottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            applyBottomBarInsets()
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun applyBottomBarInsets() {
        val bannerVisible = binding.bannerContainer.visibility == View.VISIBLE
        binding.bannerContainer.updatePadding(bottom = if (bannerVisible) systemBarsBottomInset else 0)
        binding.bottomNavContainer.root.updateLayoutParams<ConstraintLayout.LayoutParams> {
            bottomMargin = if (bannerVisible) 0 else systemBarsBottomInset
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
