package com.example.libsibsiu.ui.video

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.libsibsiu.adapters.video.VideoCategoryAdapter
import com.example.libsibsiu.databinding.ActivityVideoCategoryBinding
import com.example.libsibsiu.models.VideoCategory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks

class VideoCategoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVideoCategoryBinding
    private lateinit var adapter: VideoCategoryAdapter // Добавим поле адаптера для удобства обновления

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoCategoryBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        setupRecyclerView()
        fetchDataFromFirestore()

        binding.backArrow.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        // Используем GridLayoutManager для отображения элементов в 2 столбца
        binding.rvVideoCategory.layoutManager = GridLayoutManager(this, 2)
        // Создаем адаптер с пустым списком категорий
        adapter = VideoCategoryAdapter(emptyList()) // Удалите контекст из конструктора
        binding.rvVideoCategory.adapter = adapter
    }

    private fun fetchDataFromFirestore() {
        showLoadingIndicator()
        val db = FirebaseFirestore.getInstance()
        db.collection("video").get().addOnSuccessListener { result ->
            val categories = mutableListOf<VideoCategory>()
            val tasks = mutableListOf<Task<*>>()

            for (document in result) {
                val id = document.id  // Получаем ID документа
                val title = document.getString("title") ?: "No Title"
                val storagePath = document.getString("images") ?: ""

                if (storagePath.isNotEmpty()) {
                    val imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(storagePath)
                    val downloadTask = imageRef.downloadUrl.addOnSuccessListener { uri ->
                        categories.add(VideoCategory(title, uri.toString(), id))
                        // Обновляем адаптер после завершения загрузки всех изображений
                        if (categories.size == result.size()) {
                            adapter.updateData(categories)
                            hideLoadingIndicator()
                        }
                    }.addOnFailureListener { e ->
                        Log.e("VideoCategoryActivity", "Failed to fetch download URL for image: $storagePath", e)
                    }
                    tasks.add(downloadTask)
                } else {
                    categories.add(VideoCategory(title, "", id))
                }
            }

            Tasks.whenAllComplete(tasks).addOnCompleteListener {
                if (categories.size == result.size()) {
                    adapter.updateData(categories)
                    hideLoadingIndicator()
                }
            }
        }.addOnFailureListener { e ->
            Log.e("VideoCategoryActivity", "Error loading video categories", e)
            hideLoadingIndicator()
        }
    }

    private fun showLoadingIndicator() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideLoadingIndicator() {
        binding.progressBar.visibility = View.GONE
    }
}