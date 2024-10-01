package com.example.libsibsiu

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.libsibsiu.databinding.ActivitySettingsBinding
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import com.example.libsibsiu.ui.account.AccountViewModel
import com.google.firebase.storage.FirebaseStorage


class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var accountViewModel: AccountViewModel
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        accountViewModel = ViewModelProvider(this).get(AccountViewModel::class.java)

        setupListeners()

        // Получение информации о пакете, включая номер версии сборки
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionName = packageInfo.versionName
        val buildDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(packageInfo.lastUpdateTime)
        binding.assembly.text = getString(R.string.version_format, versionName, buildDate)
    }

    private fun setupListeners() {
        binding.backArrow.setOnClickListener {
            finish()
        }

        binding.tvLogOutProfileDevice.setOnClickListener {
            showConfirmationDialog("выйти из профиля на этом устройстве")
        }

        binding.tvSignOutAllDevices.setOnClickListener {
            showConfirmationDialog("выйти на всех устройствах, кроме этого")
        }

        binding.tvDeleteProfile.setOnClickListener {
            showConfirmationDialog("удалить профиль")
        }
    }

    private fun showConfirmationDialog(action: String) {
        AlertDialog.Builder(this, R.style.RoundedAlertDialog)
            .setTitle("Подтверждение")
            .setMessage("Вы уверены, что хотите $action?")
            .setPositiveButton("Да") { _, _ ->
                when (action) {
                    "выйти из профиля на этом устройстве" -> signOutCurrentSession()
                    "выйти на всех устройствах, кроме этого" -> signOutFromAllDevicesExceptCurrent()
                    "удалить профиль" -> deleteUserProfile()
                }
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    private fun signOutCurrentSession() {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val currentSessionToken = prefs.getString("sessionToken", null)
        auth.currentUser?.uid?.let { userId ->
            currentSessionToken?.let { token ->
                firestore.collection("users").document(userId).collection("sessions").document(token)
                    .delete()
                    .addOnSuccessListener {
                        prefs.edit().remove("sessionToken").apply()
                        auth.signOut()
                        navigateToLoginActivity()
                    }.addOnFailureListener { e ->
                        Toast.makeText(this, "Ошибка выхода: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun signOutFromAllDevicesExceptCurrent() {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val currentSessionToken = prefs.getString("sessionToken", "")
        auth.currentUser?.uid?.let { userId ->
            firestore.collection("users").document(userId).collection("sessions").get()
                .addOnSuccessListener { snapshot ->
                    for (session in snapshot.documents) {
                        if (session.id != currentSessionToken) {
                            session.reference.delete()
                        }
                    }
                    Toast.makeText(this, "Вы вышли на всех устройствах, кроме этого", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Ошибка: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun deleteUserProfile() {
        // Получаем текущего пользователя
        val user = auth.currentUser ?: return

        // Удаление изображения профиля из Firebase Storage, если оно есть
        val profileImageRef = FirebaseStorage.getInstance().reference.child("profileImages/${user.uid}")
        profileImageRef.delete()

        // Удаление данных пользователя из Firestore
        val userDocRef = firestore.collection("users").document(user.uid)

        // Удаление подколлекций пользователя, если они есть
        // Замените "subcollectionName" на имя вашей подколлекции
        userDocRef.collection("sessions").get()
            .addOnSuccessListener { snapshot ->
                for (document in snapshot) {
                    userDocRef.collection("sessions").document(document.id).delete()
                }
                // После удаления подколлекций удалите сам документ пользователя
                userDocRef.delete().addOnSuccessListener {
                    // Удаление пользователя из Firebase Authentication
                    user.delete().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            navigateToLoginActivity()
                            Toast.makeText(this, "Аккаунт успешно удален.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this,
                                "Ошибка удаления аккаунта: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
    }

    private fun navigateToLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}