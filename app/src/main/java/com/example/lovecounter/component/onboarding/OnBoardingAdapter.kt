package com.example.lovecounter.component.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.lovecounter.data.model.OnBoarding
import com.example.lovecounter.databinding.ItemOnboardingBinding
import com.example.lovecounter.utils.FirebaseConfigManager
import com.example.lovecounter.utils.ads.NativeAdsUtils
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

class OnBoardingAdapter : RecyclerView.Adapter<OnBoardingAdapter.OnBoardingViewHolder>() {

    private val items = mutableListOf<OnBoarding>()
    private var nativeAd: NativeAd? = null

    fun submitList(list: List<OnBoarding>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun setNativeAd(ad: NativeAd?) {
        nativeAd = ad
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnBoardingViewHolder {
        val binding = ItemOnboardingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OnBoardingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OnBoardingViewHolder, position: Int) {
        holder.bind(items[position], nativeAd)
    }

    class OnBoardingViewHolder(private val binding: ItemOnboardingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: OnBoarding, nativeAd: NativeAd?) {
            binding.imgOnBoarding.setImageResource(item.imageRes)
            binding.tvTitle.setText(item.titleRes)
            binding.tvSubtitle.setText(item.subtitleRes)

            if (!FirebaseConfigManager.isNativeAdsEnabled()) {
                binding.flNativeAdContainer.visibility = View.GONE
                return
            }

            binding.flNativeAdContainer.visibility = View.VISIBLE
            binding.flNativeAdContainer.removeAllViews()
            val context = binding.root.context
            if (nativeAd != null) {
                val adView = LayoutInflater.from(context).inflate(com.ads.admob.R.layout.layout_native_ad_view_type1, binding.flNativeAdContainer, false) as? NativeAdView
                if (adView != null) {
                    NativeAdsUtils.populateNativeAdView(nativeAd, adView)
                    binding.flNativeAdContainer.addView(adView)
                }
            } else {
                LayoutInflater.from(context).inflate(com.ads.admob.R.layout.loading_native_ad_view_type1, binding.flNativeAdContainer, true)
            }
        }
    }
}
