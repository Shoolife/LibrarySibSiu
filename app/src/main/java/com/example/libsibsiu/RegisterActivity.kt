package com.example.libsibsiu

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.libsibsiu.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.UUID
import android.content.Context


class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        binding.btnBack.setOnClickListener {
            returnToLoginActivity()
        }

        binding.btnReg.setOnClickListener {
            val name = binding.textInputLayout.editText?.text.toString().trim()
            val surname = binding.textInputLayout2.editText?.text.toString().trim()
            val patronymic = binding.textInputLayout3.editText?.text.toString().trim()
            val birthDate = binding.textInputLayout4.editText?.text.toString().trim()
            val nomerBillet = binding.edNomerBilet.text.toString().trim()

            if (name.isNotEmpty() && surname.isNotEmpty() && patronymic.isNotEmpty() && birthDate.isNotEmpty() && nomerBillet.isNotEmpty()) {
                registerNewAccount(nomerBillet, birthDate, name, surname, patronymic)
            } else {
                Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
            }
        }

        // Добавление TextWatcher для автоматического форматирования даты
        binding.textInputLayout4.editText?.addTextChangedListener(object : TextWatcher {
            var ignoreChanges = false

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                if (ignoreChanges) return

                ignoreChanges = true

                // Удаляем все точки для упрощения обработки
                var text = s.toString().replace(".", "")

                // Добавляем точки обратно, если длина текста подходит под формат даты
                text = when {
                    text.length > 4 -> text.substring(0, 2) + "." + text.substring(2, 4) + "." + text.substring(4)
                    text.length > 2 -> text.substring(0, 2) + "." + text.substring(2)
                    else -> text
                }

                // Если текущий текст отличается от того, что введён пользователем, заменяем его
                if (s.toString() != text) {
                    s.replace(0, s.length, text)
                }

                // Ограничиваем ввод до 10 символов: DD.MM.YYYY
                if (s.length > 10) {
                    s.delete(10, s.length)
                }

                ignoreChanges = false
            }
        })
    }

    private fun registerNewAccount(nomerBillet: String, birthDate: String, rawName: String, rawSurname: String, rawPatronymic: String) {
        // Удаляем пробелы и делаем первую букву каждого слова заглавной
        val name = rawName.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val surname = rawSurname.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val patronymic = rawPatronymic.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        if (!isValidDate(birthDate)) {
            Toast.makeText(this, "Введена неверная дата. Пожалуйста, проверьте введенные данные.", Toast.LENGTH_LONG).show()
            return
        }

        val formattedBirthDate = birthDate.replace(".", "")

        val email = "$nomerBillet@example.com"

        auth.createUserWithEmailAndPassword(email, formattedBirthDate)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    saveUserDataToFirestore(nomerBillet, formattedBirthDate, name, surname, patronymic)
                } else {
                    handleRegistrationError(task.exception)
                }
            }
    }

    private fun isValidDate(dateStr: String): Boolean {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).apply { isLenient = false }
        return try {
            dateFormat.parse(dateStr)
            true
        } catch (e: ParseException) {
            false
        }
    }


    private fun saveUserDataToFirestore(nomerBillet: String, birthDate: String, name: String, surname: String, patronymic: String) {
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Ошибка: Пользователь не аутентифицирован", Toast.LENGTH_SHORT).show()
            return
        }

        // Убираем sessionToken из основного словаря данных пользователя
        val userData = hashMapOf(
            "name" to name,
            "surname" to surname,
            "patronymic" to patronymic,
            "birthDate" to birthDate,
            "nomerBillet" to nomerBillet
        )

        // Сохраняем основные данные пользователя
        firestore.collection("users").document(userId).set(userData)
            .addOnSuccessListener {
                // После успешного сохранения данных пользователя, добавляем сессию
                addSessionForUser(userId)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Ошибка при сохранении данных пользователя: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addSessionForUser(userId: String) {
        val newSessionToken = UUID.randomUUID().toString()
        val sessionData = hashMapOf(
            "token" to newSessionToken,
            "lastActive" to FieldValue.serverTimestamp()
        )

        firestore.collection("users").document(userId)
            .collection("sessions").document(newSessionToken).set(sessionData)
            .addOnSuccessListener {
                // Сессия добавлена успешно, сохраняем токен сессии локально
                saveSessionToken(newSessionToken)
                // Переход к HomeActivity
                navigateToHomeActivity()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка при добавлении сессии: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveSessionToken(sessionToken: String) {
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("sessionToken", sessionToken)
            apply()
        }
    }

    private fun handleRegistrationError(exception: Exception?) {
        exception?.let {
            when(it) {
                is FirebaseAuthUserCollisionException -> {
                    Toast.makeText(this, "Пользователь с таким номером читательского билета уже существует", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this, "Ошибка регистрации: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun navigateToHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    private fun returnToLoginActivity() {
        finish()
    }
}

