package com.example.libsibsiu

import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.CalendarView
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.libsibsiu.adapters.room.RoomAdapter
import com.example.libsibsiu.databinding.ActivityRoomBinding
import com.example.libsibsiu.models.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RoomActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRoomBinding
    private lateinit var roomAdapter: RoomAdapter
    private val roomList = mutableListOf<Room>()
    private lateinit var selectedDate: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoomBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        setupRecyclerView()
        loadRoomData()

        binding.backArrow.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        roomAdapter = RoomAdapter(roomList) { roomId ->
            showCalendarDialog(roomId)
        }
        binding.RoomRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@RoomActivity)
            adapter = roomAdapter
        }
    }

    private fun loadRoomData() {
        showLoadingIndicator()
        val db = FirebaseFirestore.getInstance()
        db.collection("room")
            .get()
            .addOnSuccessListener { documents ->
                roomList.clear()
                for (document in documents) {
                    val room = document.toObject(Room::class.java).apply { id = document.id }
                    roomList.add(room)
                }
                roomAdapter.updateData(roomList)
                hideLoadingIndicator()
            }
            .addOnFailureListener { e ->
                Log.e("RoomActivity", "Error loading rooms", e)
                hideLoadingIndicator()
            }
    }

    private fun showLoadingIndicator() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideLoadingIndicator() {
        binding.progressBar.visibility = View.GONE
    }

    private fun showCalendarDialog(roomId: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_calendar, null)
        val calendarView = dialogView.findViewById<CalendarView>(R.id.calendarView)

        selectedDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(calendarView.date))
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }
            selectedDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(calendar.time)
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this, R.style.RoundedAlertDialog)
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                showTimePickerDialog(roomId, selectedDate)
            }
            .setNegativeButton("Отмена", null)
            .create()

        dialog.show()
    }

    private fun showTimePickerDialog(roomId: String, date: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_time_picker, null)
        val dateEditText = dialogView.findViewById<TextView>(R.id.selectedDate)
        dateEditText.text = date

        val checkBoxList = listOf(
            dialogView.findViewById<CheckBox>(R.id.checkBox1),
            dialogView.findViewById<CheckBox>(R.id.checkBox2),
            dialogView.findViewById<CheckBox>(R.id.checkBox3),
            dialogView.findViewById<CheckBox>(R.id.checkBox4),
            dialogView.findViewById<CheckBox>(R.id.checkBox5),
            dialogView.findViewById<CheckBox>(R.id.checkBox6),
            dialogView.findViewById<CheckBox>(R.id.checkBox7),
            dialogView.findViewById<CheckBox>(R.id.checkBox8),
            dialogView.findViewById<CheckBox>(R.id.checkBox9)
        )

        getBookedTimesForDate(roomId, date) { bookedTimes ->
            for (checkBox in checkBoxList) {
                val timeSlot = checkBox.text.toString()
                checkBox.setButtonTintList(ContextCompat.getColorStateList(this, R.color.textColor))
                if (bookedTimes.contains(timeSlot)) {
                    checkBox.isEnabled = false
                    checkBox.paintFlags = checkBox.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    checkBox.setButtonDrawable(R.drawable.crossed)
                } else {
                    checkBox.isEnabled = true
                    checkBox.paintFlags = checkBox.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    checkBox.setButtonDrawable(R.drawable.custom_checkbox)
                }
                checkBox.setTextColor(ContextCompat.getColor(this, R.color.textColor))
            }

            val dialog = androidx.appcompat.app.AlertDialog.Builder(this, R.style.RoundedAlertDialog)
                .setView(dialogView)
                .setPositiveButton("OK") { _, _ ->
                    val selectedTimes = checkBoxList.filter { it.isChecked && it.isEnabled }.map { it.text.toString() }
                    bookTimes(roomId, date, selectedTimes)
                }
                .setNegativeButton("Отмена", null)
                .create()

            dialog.show()
        }
    }

    private fun getBookedTimesForDate(roomId: String, date: String, callback: (List<String>) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("room").document(roomId).collection("bookings").document(date)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val bookedTimes = document.data?.get("times") as? Map<String, String>
                    callback(bookedTimes?.keys?.toList() ?: emptyList())
                } else {
                    callback(emptyList())
                }
            }
            .addOnFailureListener { e ->
                Log.e("RoomActivity", "Error fetching bookings", e)
                callback(emptyList())
            }
    }

    private fun bookTimes(roomId: String, date: String, times: List<String>) {
        val db = FirebaseFirestore.getInstance()
        val bookingRef = db.collection("room").document(roomId).collection("bookings").document(date)
        bookingRef.get().addOnSuccessListener { document ->
            val currentBookings = document.data?.get("times") as? Map<String, String> ?: emptyMap()
            val newBookings = currentBookings.toMutableMap()
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown_user"

            for (time in times) {
                newBookings[time] = userId
            }

            bookingRef.set(mapOf("times" to newBookings))
                .addOnSuccessListener {
                    Toast.makeText(this, "Бронирование выполнено: $times на $date", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("RoomActivity", "Error booking times", e)
                    Toast.makeText(this, "Ошибка бронирования", Toast.LENGTH_SHORT).show()
                }
        }
    }
}