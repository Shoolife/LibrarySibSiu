package com.example.libsibsiu.ui.room

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.libsibsiu.adapters.room.RoomHistoryAdapter
import com.example.libsibsiu.databinding.ActivityRoomBookingHistoryBinding
import com.example.libsibsiu.models.RoomHistory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class RoomBookingHistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRoomBookingHistoryBinding
    private lateinit var historyAdapter: RoomHistoryAdapter
    private val historyList = mutableListOf<RoomHistory>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoomBookingHistoryBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        setupRecyclerView()
        loadRoomBookingHistory()

        binding.backArrow.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = RoomHistoryAdapter(historyList) { history ->
            deleteBooking(history)
        }
        binding.rvRoom.apply {
            layoutManager = LinearLayoutManager(this@RoomBookingHistoryActivity)
            adapter = historyAdapter
        }
    }

    private fun loadRoomBookingHistory() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        showLoadingIndicator()

        db.collection("room").get()
            .addOnSuccessListener { roomDocuments ->
                for (roomDocument in roomDocuments) {
                    val roomId = roomDocument.id
                    val roomTitle = roomDocument.getString("title") ?: "Без названия"
                    db.collection("room").document(roomId).collection("bookings").get()
                        .addOnSuccessListener { bookingDocuments ->
                            for (bookingDocument in bookingDocuments) {
                                val date = bookingDocument.id
                                val times = bookingDocument.get("times") as Map<String, String>
                                for ((timeSlot, bookedUserId) in times) {
                                    if (bookedUserId == userId) {
                                        val history = RoomHistory(roomId, roomTitle, date, timeSlot)
                                        historyList.add(history)
                                    }
                                }
                            }
                            // Сортировка списка по дате и времени
                            historyList.sortBy {
                                SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).parse("${it.date} ${it.time.split(" - ")[0]}")
                            }
                            historyAdapter.updateData(historyList)
                            hideLoadingIndicator()
                        }
                        .addOnFailureListener { e ->
                            Log.e("RoomBookingHistoryActivity", "Error loading bookings", e)
                            hideLoadingIndicator()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("RoomBookingHistoryActivity", "Error loading rooms", e)
                hideLoadingIndicator()
            }
    }

    private fun deleteBooking(history: RoomHistory) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val roomDocRef = db.collection("room").document(history.roomId)
            .collection("bookings").document(history.date)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(roomDocRef)
            val times = snapshot.get("times") as MutableMap<String, String>
            times.remove(history.time)

            transaction.update(roomDocRef, "times", times)
        }.addOnSuccessListener {
            historyList.remove(history)
            historyAdapter.updateData(historyList)
            Toast.makeText(this, "Бронирование удалено успешно", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Log.e("RoomBookingHistoryActivity", "Ошибка при удалении бронирования", e)
        }
    }

    private fun showLoadingIndicator() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideLoadingIndicator() {
        binding.progressBar.visibility = View.GONE
    }
}