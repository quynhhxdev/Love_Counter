package com.example.lovecounter.component.common

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.lovecounter.databinding.DialogLoveTestLoadingBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoveTestLoadingDialog : DialogFragment() {

    private var _binding: DialogLoveTestLoadingBinding? = null
    private val binding get() = _binding!!

    private var progressAnimator: ValueAnimator? = null
    var onLoadingComplete: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogLoveTestLoadingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false

        binding.liquidHeartLoading.setProgress(0f, animate = false)
        startLoadingAnimation()
    }

    private fun startLoadingAnimation() {
        // Phase 1: 0% -> 100% over 3000ms
        progressAnimator = ValueAnimator.ofFloat(0f, 100f).apply {
            duration = 3000L
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                if (_binding != null) {
                    binding.liquidHeartLoading.setProgress(progress, animate = false)
                }
            }
            start()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            delay(3000L)
            if (_binding == null) return@launch
            // Phase 2: 100% -> 0% over 3500ms
            progressAnimator?.cancel()
            progressAnimator = ValueAnimator.ofFloat(100f, 0f).apply {
                duration = 3000L
                interpolator = LinearInterpolator()
                addUpdateListener { animator ->
                    val progress = animator.animatedValue as Float
                    if (_binding != null) {
                        binding.liquidHeartLoading.setProgress(progress, animate = false)
                    }
                }
                start()
            }

            delay(3000L)
            if (_binding == null) return@launch
            onLoadingComplete?.invoke()
            dismissAllowingStateLoss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    override fun onDestroyView() {
        progressAnimator?.cancel()
        progressAnimator = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(onComplete: () -> Unit): LoveTestLoadingDialog {
            return LoveTestLoadingDialog().apply {
                onLoadingComplete = onComplete
            }
        }
    }
}
