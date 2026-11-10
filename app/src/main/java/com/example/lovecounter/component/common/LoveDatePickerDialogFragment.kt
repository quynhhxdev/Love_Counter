package com.example.lovecounter.component.common

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.example.lovecounter.R
import com.example.lovecounter.databinding.DialogLoveDatePickerBinding
import com.example.lovecounter.utils.handle.DateUtils
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

class LoveDatePickerDialogFragment : DialogFragment() {
    private var _binding: DialogLoveDatePickerBinding? = null
    private val binding get() = _binding!!
    private lateinit var selectedDate: Calendar
    private lateinit var displayedMonth: Calendar
    private lateinit var minDate: Calendar
    private lateinit var maxDate: Calendar
    private var yearPickerDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        minDate = dateBound(ARG_MIN_DATE) {
            Calendar.getInstance().apply {
                clear()
                set(Calendar.YEAR, getMinimum(Calendar.YEAR))
                set(Calendar.MONTH, Calendar.JANUARY)
                set(Calendar.DAY_OF_MONTH, 1)
            }
        }
        maxDate = dateBound(ARG_MAX_DATE) { startOfDay(Calendar.getInstance()) }
        require(!minDate.after(maxDate)) { "minDate must not be after maxDate" }
        selectedDate = Calendar.getInstance().apply {
            timeInMillis = requireArguments().getLong(ARG_INITIAL_DATE, timeInMillis)
        }.let { DateUtils.clampDate(it, minDate, maxDate) }
        displayedMonth = (selectedDate.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogLoveDatePickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnPreviousMonth.setOnClickListener {
            val previousMonth = (displayedMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
            val lastDay = (previousMonth.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            }
            if (DateUtils.isWithinDateRange(lastDay, minDate, maxDate)) {
                displayedMonth = previousMonth
                renderCalendar()
            }
        }
        binding.btnNextMonth.setOnClickListener {
            val nextMonth = (displayedMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
            if (!nextMonth.after(maxDate)) {
                displayedMonth = nextMonth
                renderCalendar()
            }
        }
        binding.tvMonth.setOnClickListener { showYearPicker() }
        binding.btnCancelDate.setOnClickListener { dismiss() }
        binding.btnSaveDate.setOnClickListener {
            setFragmentResult(requireArguments().getString(ARG_REQUEST_KEY)!!, Bundle().apply { putLong(RESULT_DATE_MILLIS, selectedDate.timeInMillis) })
            dismiss()
        }
        renderCalendar()
    }

    override fun onStart() {
        super.onStart()
        val metrics = resources.displayMetrics
        val horizontalInset = (32 * metrics.density).toInt()
        val maxWidth = (420 * metrics.density).toInt()
        val dialogWidth = min(metrics.widthPixels - horizontalInset, maxWidth)
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun renderCalendar() {
        _binding?.tvMonth?.text = getString(
            R.string.calendar_month_year,
            DateFormatSymbols(Locale.getDefault()).months[displayedMonth.get(Calendar.MONTH)],
            displayedMonth.get(Calendar.YEAR)
        )
        val calendarGrid = _binding?.calendarGrid ?: return
        calendarGrid.removeAllViews()
        DateFormatSymbols(Locale.getDefault()).shortWeekdays
            .drop(1)
            .forEach { calendarGrid.addView(cell(it, true, false, false)) }
        val first = displayedMonth.clone() as Calendar
        first.set(Calendar.DAY_OF_MONTH, 1)
        DateUtils.monthGridDays(first).forEach { gridDay ->
            val date = gridDay.date
            val day = date.get(Calendar.DAY_OF_MONTH)
            val inRange = DateUtils.isWithinDateRange(date, minDate, maxDate)
            val selected = date.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) && date.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)
            calendarGrid.addView(cell(day.toString(), false, selected, !gridDay.isCurrentMonth || !inRange).also { view ->
                if (inRange) view.setOnClickListener { selectedDate.timeInMillis = date.timeInMillis; renderCalendar() }
            })
        }
    }

    private fun showYearPicker() {
        if (!isAdded || _binding == null || yearPickerDialog?.isShowing == true) return

        val yearPicker = NumberPicker(requireContext()).apply {
            minValue = minDate.get(Calendar.YEAR)
            maxValue = maxDate.get(Calendar.YEAR)
            value = displayedMonth.get(Calendar.YEAR).coerceIn(minValue, maxValue)
            wrapSelectorWheel = false
        }
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.select_year)
            .setView(yearPicker)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (isAdded && _binding != null) changeDisplayedYear(yearPicker.value)
            }
            .create()
        yearPickerDialog = dialog
        dialog.setOnDismissListener {
            if (yearPickerDialog === dialog) yearPickerDialog = null
        }
        dialog.show()
    }

    private fun changeDisplayedYear(year: Int) {
        val oldYear = displayedMonth.get(Calendar.YEAR)
        val month = displayedMonth.get(Calendar.MONTH)
        displayedMonth = DateUtils.dateInYear(displayedMonth, year).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }

        if (selectedDate.get(Calendar.YEAR) == oldYear && selectedDate.get(Calendar.MONTH) == month) {
            selectedDate = DateUtils.dateInYear(selectedDate, year)
                .let { DateUtils.clampDate(it, minDate, maxDate) }
        }
        renderCalendar()
    }

    private fun dateBound(argument: String, default: () -> Calendar): Calendar {
        val millis = requireArguments().getLong(argument, Long.MIN_VALUE)
        return if (millis == Long.MIN_VALUE) default() else startOfDay(
            Calendar.getInstance().apply { timeInMillis = millis }
        )
    }

    private fun startOfDay(date: Calendar): Calendar = DateUtils.startOfDay(date)

    private fun cell(label: String, header: Boolean, selected: Boolean, muted: Boolean) = TextView(requireContext()).apply {
        layoutParams = GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f)).apply { width = 0; height = 0 }
        gravity = Gravity.CENTER; text = label; textSize = if (header) 10f else 11f
        typeface = ResourcesCompat.getFont(requireContext(), if (header) R.font.plus_jakarta_sans else R.font.plus_jakarta_sans_medium)
        setTextColor(ContextCompat.getColor(requireContext(), when { selected -> R.color.white; muted -> R.color.no_select; header -> R.color.secondary_text; else -> R.color.primary_text }))
        if (selected) setBackgroundResource(R.drawable.bg_calendar_selected)
    }

    override fun onDestroyView() {
        yearPickerDialog?.setOnDismissListener(null)
        yearPickerDialog?.dismiss()
        yearPickerDialog = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val RESULT_DATE_MILLIS = "selected_date_millis"
        private const val ARG_INITIAL_DATE = "initial_date"
        private const val ARG_REQUEST_KEY = "request_key"
        private const val ARG_MIN_DATE = "min_date"
        private const val ARG_MAX_DATE = "max_date"

        fun newInstance(
            requestKey: String,
            initialDate: Long,
            minDate: Long? = null,
            maxDate: Long? = null
        ) = LoveDatePickerDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_REQUEST_KEY, requestKey)
                putLong(ARG_INITIAL_DATE, initialDate)
                minDate?.let { putLong(ARG_MIN_DATE, it) }
                maxDate?.let { putLong(ARG_MAX_DATE, it) }
            }
        }
    }
}
