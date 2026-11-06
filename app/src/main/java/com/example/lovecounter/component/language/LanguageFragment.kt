package com.example.lovecounter.component.language

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lovecounter.R
import com.example.lovecounter.base.fragment.BaseFragment
import com.example.lovecounter.component.onboarding.OnboardingFragment
import com.example.lovecounter.databinding.FragmentLanguageBinding
import com.example.lovecounter.utils.LanguageManager
import com.example.lovecounter.utils.Logger
import com.example.lovecounter.utils.gone
import com.example.lovecounter.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LanguageFragment : BaseFragment<FragmentLanguageBinding>() {
    @Inject lateinit var languageManager: LanguageManager
    private val viewModel: LanguageViewModel by viewModels()
    private val adapter by lazy { LanguageAdapter() }
    private val isInitialSetup get() = arguments?.getBoolean("isInitialSetup") ?: false

    override fun provideViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentLanguageBinding.inflate(
            inflater.cloneInContext(
                ContextThemeWrapper(
                    languageManager.localizedContext(inflater.context),
                    R.style.Theme_LoveCounter,
                ),
            ),
            container,
            false
        )

    override fun initViews() {
        setupHeader()
        adapter.onSelectionChanged = { selectedLanguage ->
            if (selectedLanguage == null) binding.btnSave.gone()
            else binding.btnSave.visible()
        }
        adapter.clearSelection()
        binding.rvLanguage.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLanguage.adapter = adapter
        binding.btnSave.setOnClickListener {
            val selected = adapter.getSelectedLanguage()
                ?: return@setOnClickListener showToast(getString(R.string.please_select_language))

            val currentActivity = activity ?: return@setOnClickListener
            binding.btnSave.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    languageManager.changeLanguage(selected.languageCode)
                    if (!isAdded) return@launch

                    if (isInitialSetup) {
                        navigationManager.replaceRoot(OnboardingFragment())
                    } else {
                        navigationManager.goBack()
                    }
                    currentActivity.recreate()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Logger.e("Failed to save selected language", throwable = error)
                    if (isAdded) {
                        binding.btnSave.isEnabled = true
                    }
                    showToast(getString(R.string.failed_to_save_language))
                }
            }
        }
    }

    private fun setupHeader() {
        binding.btnSave.setImageResource(R.drawable.ic_check)
        binding.btnSave.gone()
        if (isInitialSetup) {
            binding.btnBack.gone()
            ConstraintSet().also { set ->
                set.clone(binding.header)
                set.connect(R.id.tvTitle, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                set.clear(R.id.tvTitle, ConstraintSet.END)
                set.applyTo(binding.header)
            }
        } else {
            binding.btnBack.visible()
            binding.btnBack.setOnClickListener { navigationManager.goBack() }
        }
    }

    override fun initData() = viewModel.loadLanguages()
    override fun initObserver() {
        viewModel.languages.observe(viewLifecycleOwner) { languages ->
            adapter.submitList(languages)
        }
    }
}
