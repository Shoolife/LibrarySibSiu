package com.example.libsibsiu.ui.billet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BilletViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Читательский билет"
    }
    val text: LiveData<String> = _text

    private val _userData = MutableLiveData<String>()
    val userData: LiveData<String> = _userData

    private val _readerTicketNumber = MutableLiveData<String>()
    val readerTicketNumber: LiveData<String> = _readerTicketNumber

    init {
        loadUserData()
    }

    private fun loadUserData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .get().addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val name = documentSnapshot.getString("name") ?: "Недоступно"
                    val surname = documentSnapshot.getString("surname") ?: "Недоступно"
                    val patronymic = documentSnapshot.getString("patronymic") ?: "Недоступно"
                    _userData.value = "$surname $name $patronymic"
                    // Получаем номер билета и сохраняем его
                    _readerTicketNumber.value = documentSnapshot.getString("nomerBillet") ?: ""
                }
            }
            .addOnFailureListener { e ->
                _userData.value = "Ошибка загрузки данных пользователя: ${e.message}"
            }
    }
}