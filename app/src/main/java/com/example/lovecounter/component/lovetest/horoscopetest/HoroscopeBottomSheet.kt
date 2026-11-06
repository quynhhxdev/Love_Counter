package com.example.lovecounter.component.lovetest.horoscopetest

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lovecounter.R
import com.example.lovecounter.data.model.ZodiacSign
import com.example.lovecounter.data.sources.CoupleProfileDataStore
import com.example.lovecounter.databinding.DialogChooseHoroscopeBinding
import com.example.lovecounter.databinding.ItemHoroscopeOptionBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HoroscopeBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogChooseHoroscopeBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var coupleProfileDataStore: CoupleProfileDataStore

    var onZodiacSelectedListener: ((ZodiacSign) -> Unit)? = null

    private val isUserSelection: Boolean
        get() = requireArguments().getBoolean(ARG_IS_USER_SELECTION)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogChooseHoroscopeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvHoroscopes.layoutManager = GridLayoutManager(requireContext(), 2)

        binding.rvHoroscopes.visibility = View.INVISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val selection = coupleProfileDataStore.horoscopeSelectionFlow.first()
                val initialSign = selection.run { if (isUserSelection) user else partner }

                val adapter =
                    HoroscopeAdapter(::onHoroscopeSelected, initialSign) // Truyền initialSign vào
                binding.rvHoroscopes.adapter = adapter
                binding.rvHoroscopes.visibility = View.VISIBLE
            } catch (error: Exception) {
                Log.e(TAG, "Failed to restore horoscope selection", error)
                Toast.makeText(
                    requireContext(),
                    R.string.unable_to_load_horoscope,
                    Toast.LENGTH_SHORT
                ).show()
                binding.rvHoroscopes.visibility = View.VISIBLE
            }
        }
    }

    private fun onHoroscopeSelected(zodiacSign: ZodiacSign, previous: ZodiacSign) {
        val adapter = (_binding?.rvHoroscopes?.adapter as? HoroscopeAdapter) ?: return
        adapter.setInteractionEnabled(enabled = false)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                coupleProfileDataStore.saveHoroscopeSelection(isUserSelection, zodiacSign)
                onZodiacSelectedListener?.invoke(zodiacSign)
                dismiss()
            } catch (error: Exception) {
                Log.e(TAG, "Failed to save horoscope selection", error)
                adapter.setSelected(previous)
                adapter.setInteractionEnabled(enabled = true)
                Toast.makeText(
                    requireContext(),
                    R.string.unable_to_save_horoscope,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class HoroscopeAdapter(
        private val onClick: (ZodiacSign, ZodiacSign) -> Unit,
        initialSign: ZodiacSign // Thêm tham số
    ) : RecyclerView.Adapter<HoroscopeAdapter.HoroscopeViewHolder>() {

        private val items = ZodiacSign.entries
        private var selectedSign = initialSign
        private var interactionEnabled = true

        fun setSelected(sign: ZodiacSign) {
            if (selectedSign == sign) return
            val previousPosition = items.indexOf(selectedSign)
            val selectedPosition = items.indexOf(sign)
            selectedSign = sign
            if (previousPosition >= 0) notifyItemChanged(previousPosition)
            if (selectedPosition >= 0) notifyItemChanged(selectedPosition)
        }

        fun setInteractionEnabled(enabled: Boolean) {
            interactionEnabled = enabled
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HoroscopeViewHolder {
            val binding = ItemHoroscopeOptionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return HoroscopeViewHolder(binding)
        }

        override fun onBindViewHolder(holder: HoroscopeViewHolder, position: Int) {
            val sign = items[position]
            val isSelected = sign == selectedSign

            holder.binding.imgZodiacIcon.setImageResource(sign.iconRes)
            holder.binding.root.setBackgroundResource(
                if (isSelected) R.drawable.bg_select_horoscope
                else R.drawable.bg_item_horoscope
            )
            holder.binding.root.setOnClickListener {
                if (!interactionEnabled) return@setOnClickListener
                val previous = selectedSign
                setSelected(sign)
                onClick(sign, previous)
            }
        }

        override fun getItemCount(): Int = items.size

        class HoroscopeViewHolder(val binding: ItemHoroscopeOptionBinding) :
            RecyclerView.ViewHolder(binding.root)
    }

    companion object {
        private const val ARG_IS_USER_SELECTION = "is_user_selection"
        private const val TAG = "HoroscopeBottomSheet"

        fun newInstance(isUserSelection: Boolean) = HoroscopeBottomSheet().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_IS_USER_SELECTION, isUserSelection)
            }
        }
    }
}
