package com.example.simsekolah.ui.dashboard

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.simsekolah.R
import com.example.simsekolah.adapter.CalendarAdapter
import com.example.simsekolah.adapter.EventAdapter
import com.example.simsekolah.data.model.EventModel
import com.example.simsekolah.databinding.DialogAddEventBinding
import com.example.simsekolah.receiver.AlarmReceiver
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.graphics.toColorInt

class EventFragment : Fragment() {

    private lateinit var rvCalendar: RecyclerView
    private lateinit var rvEvent: RecyclerView
    private lateinit var tvMonth: TextView
    private lateinit var btnPrevMonth: ImageView
    private lateinit var btnNextMonth: ImageView

    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var eventAdapter: EventAdapter
    private val eventList = mutableListOf<EventModel>()

    private var calendar = Calendar.getInstance()
    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_event, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvCalendar = view.findViewById(R.id.rvCalendar)
        rvEvent = view.findViewById(R.id.rvEvent)
        tvMonth = view.findViewById(R.id.tvMonth)
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth)
        btnNextMonth = view.findViewById(R.id.btnNextMonth)

        updateCalendarUI()
        setupEventList()

        btnPrevMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendarUI()
        }

        btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendarUI()
        }

        checkAlarmPermission()
    }

    private fun checkAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Permission Required")
                    .setMessage("This app needs permission to set exact alarms for your events. Please enable it in settings.")
                    .setPositiveButton("Settings") { _, _ ->
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun updateCalendarUI() {
        tvMonth.text = monthFormat.format(calendar.time)

        val dates = mutableListOf<Int>()
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in 1..maxDay) {
            dates.add(i)
        }

        calendarAdapter = CalendarAdapter(dates) { selectedDate ->
            showAddEventDialog(selectedDate)
        }

        rvCalendar.layoutManager = GridLayoutManager(requireContext(), 7)
        rvCalendar.adapter = calendarAdapter
    }

    private fun setupEventList() {
        eventAdapter = EventAdapter(eventList)
        rvEvent.layoutManager = LinearLayoutManager(requireContext())
        rvEvent.adapter = eventAdapter
    }

    // ... bagian kode lainnya tetap sama ...

    private fun showAddEventDialog(date: Int) {
        val dialogBinding = DialogAddEventBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Buat EditText waktu tidak bisa diketik manual agar user wajib pakai Picker
        dialogBinding.etEventTime.isFocusable = false
        dialogBinding.etEventTime.isClickable = true

        val selectedMonthName = monthFormat.format(calendar.time).split(" ")[0]
        val selectedYear = calendar.get(Calendar.YEAR)
        dialogBinding.tvSelectedDate.text = "$selectedMonthName $date, $selectedYear"

        dialogBinding.etEventTime.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(requireContext(), { _, hour, minute ->
                val time = String.format("%02d:%02d", hour, minute)
                dialogBinding.etEventTime.setText(time)
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSave.setOnClickListener {
            val title = dialogBinding.etEventTitle.text.toString().trim()
            val timeStr = dialogBinding.etEventTime.text.toString().trim()
            val location = dialogBinding.etEventLocation.text.toString().trim()

            // Validasi input
            if (title.isEmpty()) {
                dialogBinding.etEventTitle.error = "Title required"
                return@setOnClickListener
            }
            if (timeStr.isEmpty() || !timeStr.contains(":")) {
                dialogBinding.etEventTime.error = "Valid time required"
                Toast.makeText(requireContext(), "Please select a valid time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedColor = when (dialogBinding.rgColor.checkedRadioButtonId) {
                R.id.rbRed -> "#E03131".toColorInt()
                R.id.rbGreen -> "#2F9E44".toColorInt()
                R.id.rbOrange -> "#F08C00".toColorInt()
                else -> "#1971C2".toColorInt()
            }

            val newEvent = EventModel(
                day = date.toString(),
                month = selectedMonthName,
                title = title,
                time = timeStr,
                location = location,
                color = selectedColor
            )

            // Panggil setAlarm dengan aman
            setAlarm(date, timeStr, title, location)

            eventList.add(0, newEvent)
            eventAdapter.notifyItemInserted(0)
            rvEvent.scrollToPosition(0)

            Toast.makeText(requireContext(), "Event and Alarm set for $timeStr", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setAlarm(day: Int, timeStr: String, title: String, location: String) {
        try {
            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Membersihkan karakter non-angka (seperti AM/PM) jika ada
            val timeParts = timeStr.split(":")
            if (timeParts.size < 2) return

            val hour = timeParts[0].filter { it.isDigit() }.toIntOrNull() ?: 0
            val minute = timeParts[1].filter { it.isDigit() }.toIntOrNull() ?: 0

            val intent = Intent(requireContext(), AlarmReceiver::class.java).apply {
                putExtra(AlarmReceiver.EXTRA_TITLE, "Event Started: $title")
                putExtra(AlarmReceiver.EXTRA_MESSAGE, "Location: $location at $timeStr")
                val uniqueId = (day.toString() + timeStr + calendar.get(Calendar.MONTH)).hashCode()
                putExtra(AlarmReceiver.EXTRA_ID, uniqueId)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                (day.toString() + timeStr + calendar.get(Calendar.MONTH)).hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val alarmCalendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, calendar.get(Calendar.YEAR))
                set(Calendar.MONTH, calendar.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (alarmCalendar.timeInMillis <= System.currentTimeMillis()) {
                Toast.makeText(requireContext(), "Warning: Time has already passed!", Toast.LENGTH_SHORT).show()
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmCalendar.timeInMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmCalendar.timeInMillis, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to set alarm: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}