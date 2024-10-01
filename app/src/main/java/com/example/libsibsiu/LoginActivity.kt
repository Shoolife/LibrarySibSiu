package com.example.libsibsiu

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.libsibsiu.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val readerTicketNumber = binding.edNomerBilet.text.toString().trim()
            val password = binding.edPassword.text.toString().trim() // Предполагаем, что пароль - это дата рождения

            if (readerTicketNumber.isNotEmpty() && password.isNotEmpty()) {
                signIn(readerTicketNumber, password)
            } else {
                Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnReg.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun signIn(readerTicketNumber: String, password: String) {
        val email = "$readerTicketNumber@example.com"
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Вход выполнен успешно, добавляем сессию пользователя
                    val newSessionToken = UUID.randomUUID().toString()
                    addSessionForUser(auth.currentUser!!.uid, newSessionToken)
                } else {
                    // Обработка ошибок входа
                    Toast.makeText(this, "Введен неверный логин или пароль. Пожалуйста, попробуйте еще раз.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun addSessionForUser(userId: String, sessionToken: String) {
        val sessionData = hashMapOf(
            "token" to sessionToken,
            "lastActive" to FieldValue.serverTimestamp()
        )

        FirebaseFirestore.getInstance().collection("users").document(userId)
            .collection("sessions").document(sessionToken).set(sessionData)
            .addOnSuccessListener {
                // Сессия добавлена успешно, сохраняем токен сессии локально и переходим в HomeActivity
                saveSessionToken(sessionToken)
                navigateToHomeActivity()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this,
                    "Ошибка при добавлении сессии: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveSessionToken(sessionToken: String) {
        getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit()
            .putString("sessionToken", sessionToken)
            .apply()
    }

    private fun navigateToHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }
}