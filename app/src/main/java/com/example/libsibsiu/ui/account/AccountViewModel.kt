package com.example.libsibsiu.ui.account

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AccountViewModel : ViewModel() {
    private val _text = MutableLiveData<String>().apply {
        value = "Личный кабинет"
    }
    val text: LiveData<String> = _text

    private val _userInfo = MutableLiveData<String>()
    val userInfo: LiveData<String> = _userInfo

    private val _userPhotoUrl = MutableLiveData<String?>()
    val userPhotoUrl: LiveData<String?> = _userPhotoUrl

    private val _favoritesCount = MutableLiveData<String>()
    val favoritesCount: LiveData<String> = _favoritesCount

    private val _bookStatusCount = MutableLiveData<String>()
    val bookStatusCount: LiveData<String> = _bookStatusCount

    private val _activeBookingCount = MutableLiveData<String>()
    val activeBookingCount: LiveData<String> = _activeBookingCount

    init {
        loadUserInfo()
        loadFavoritesCount()
        loadBookStatusCounts()
        loadActiveBookingCount()
    }

    private fun loadActiveBookingCount() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("room").get()
            .addOnSuccessListener { roomDocuments ->
                var activeCount = 0
                for (roomDocument in roomDocuments) {
                    val roomId = roomDocument.id
                    db.collection("room").document(roomId).collection("bookings").get()
                        .addOnSuccessListener { bookingDocuments ->
                            for (bookingDocument in bookingDocuments) {
                                val date = bookingDocument.id
                                val times = bookingDocument.get("times") as Map<String, String>
                                for ((timeSlot, bookedUserId) in times) {
                                    if (bookedUserId == userId) {
                                        val dateTimeStr = "$date ${timeSlot.split(" - ")[0]}"
                                        val dateTime = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).parse(dateTimeStr)
                                        if (dateTime != null && dateTime.after(Date())) {
                                            activeCount++
                                        }
                                    }
                                }
                            }
                            _activeBookingCount.value = "$activeCount активных"
                        }
                }
            }
    }

    private fun loadBookStatusCounts() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = FirebaseFirestore.getInstance().collection("users").document(userId)

        // Определение ссылок на коллекции
        val pendingRef = userRef.collection("pending_books")
        val issuedRef = userRef.collection("issued_books")
        val overdueRef = userRef.collection("overdue_books")

        // Создание агрегатной функции для обновления счетчика
        val updateTotalCount = {
            val pendingTask = pendingRef.get()
            val issuedTask = issuedRef.get()
            val overdueTask = overdueRef.get()

            // Ожидание завершения всех задач
            Tasks.whenAllComplete(pendingTask, issuedTask, overdueTask).addOnCompleteListener {
                if (it.isSuccessful) {
                    val totalCount = listOf(pendingTask, issuedTask, overdueTask).sumOf { task ->
                        (task.result?.documents?.size ?: 0)
                    }
                    _bookStatusCount.value = "$totalCount активных"
                } else {
                    _bookStatusCount.value = "Ошибка загрузки статусов"
                }
            }
        }

        // Подписка на изменения в каждой коллекции
        pendingRef.addSnapshotListener { _, _ -> updateTotalCount() }
        issuedRef.addSnapshotListener { _, _ -> updateTotalCount() }
        overdueRef.addSnapshotListener { _, _ -> updateTotalCount() }
    }

    private fun loadFavoritesCount() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val favoritesRef = FirebaseFirestore.getInstance().collection("users").
                            document(userId).collection("favorites")

        favoritesRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                _favoritesCount.value = "Ошибка: ${e.message}"
                return@addSnapshotListener
            }
            val count = snapshot?.size() ?: 0
            _favoritesCount.value = "$count активных"
        }
    }

    private fun loadUserInfo() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val name = documentSnapshot.getString("name") ?: "N/A"
                    val surname = documentSnapshot.getString("surname") ?: "N/A"
                    val patronymic = documentSnapshot.getString("patronymic") ?: "N/A"
                    _userInfo.value = "$surname $name $patronymic"
                    _userPhotoUrl.value = documentSnapshot.getString("image")
                } else {
                    _userInfo.value = "Данные пользователя не найдены."
                }
            }
            .addOnFailureListener { exception ->
                _userInfo.value = "Ошибка при загрузке данных: ${exception.message}"
            }
    }

    fun uploadImageToStorage(imageUri: Uri) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val storageRef = FirebaseStorage.getInstance().reference.child("profileImages/$userId")

        storageRef.putFile(imageUri)
            .addOnSuccessListener { it.metadata?.reference?.downloadUrl?.addOnSuccessListener { uri ->
                _userPhotoUrl.value = uri.toString()
                updateUserInfoImage(uri.toString())
            }}
            .addOnFailureListener { e ->
                _userInfo.value = "Ошибка при загрузке изображения: ${e.message}"
            }
    }

    private fun updateUserInfoImage(imageUrl: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .update("image", imageUrl)
            .addOnFailureListener { e ->
                _userInfo.value = "Ошибка обновления URL изображения: ${e.message}"
            }
    }
}