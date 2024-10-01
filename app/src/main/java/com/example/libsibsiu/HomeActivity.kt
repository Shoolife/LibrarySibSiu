package com.example.libsibsiu

import android.content.Intent
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.libsibsiu.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth
import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)

        checkSessionToken { isValidSession ->
            if (isValidSession) {
                setContentView(binding.root)
                setupNavigation()
            } else {
                navigateToLoginActivity()
            }
        }
    }

    private fun setupNavigation() {
        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)
    }

    private fun checkSessionToken(completion: (Boolean) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            completion(false)
            return
        }

        val userId = currentUser.uid
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val localToken = prefs.getString("sessionToken", null) ?: kotlin.run {
            completion(false)
            return
        }

        firestore.collection("users").document(userId)
            .collection("sessions").document(localToken).get()
            .addOnSuccessListener { document ->
                completion(document.exists())
            }
            .addOnFailureListener {
                completion(false)
            }
    }

    private fun navigateToLoginActivity() {
        val startLoginActivity = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(startLoginActivity)
        finish()
    }
}
