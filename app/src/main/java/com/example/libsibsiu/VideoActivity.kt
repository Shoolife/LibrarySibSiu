package com.example.libsibsiu

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.libsibsiu.adapters.video.VerticalAdapter
import com.example.libsibsiu.databinding.ActivityVideoBinding
import com.example.libsibsiu.models.Video
import com.example.libsibsiu.models.VideoCategory
import com.example.libsibsiu.ui.video.VideoListActivity
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.android.gms.tasks.Task
import android.net.Uri
import com.example.libsibsiu.ui.video.VideoCategoryActivity
import com.example.libsibsiu.ui.video.VideoDetailActivity
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

class VideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoBinding
    private lateinit var verticalAdapter: VerticalAdapter
    private val videoCategories = mutableListOf<List<VideoCategory>>()
    private val popularVideos = mutableListOf<Video>()
    private val latestVideos = mutableListOf<Video>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        setupVerticalRecyclerView()
        fetchDataFromFirestore()
        fetchPopularVideos()
        fetchLatestVideos()

        binding.backArrow.setOnClickListener {
            finish()
        }
    }

    private fun setupVerticalRecyclerView() {
        verticalAdapter = VerticalAdapter(
            collections = videoCategories,
            popularVideos = popularVideos,
            latestVideos = latestVideos,
            onCategoryClick = { categoryId, categoryName ->
                val intent = Intent(this, VideoListActivity::class.java).apply {
                    putExtra("category_id", categoryId)
                    putExtra("category_name", categoryName)
                }
                startActivity(intent)
            },
            onAllCategoriesClick = {
                val intent = Intent(this, VideoCategoryActivity::class.java)
                startActivity(intent)
            },
            onVideoClick = { video ->
                val intent = Intent(this, VideoDetailActivity::class.java).apply {
                    putExtra("VIDEO_ID", video.id)
                    putExtra("CATEGORY_ID", video.categoryId)  // Используем идентификатор категории из видео
                }
                startActivity(intent)
            },
            onAllPopularClick = {
                val intent = Intent(this, VideoListActivity::class.java).apply {
                    putExtra("all_popular", true)
                }
                startActivity(intent)
            },
            onAllLatestClick = {
                val intent = Intent(this, VideoListActivity::class.java).apply {
                    putExtra("load_latest", true)
                }
                startActivity(intent)
            }
        )
        binding.categoryRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.categoryRecyclerView.adapter = verticalAdapter
        Log.d("VideoActivity", "Vertical RecyclerView setup completed.")
    }

    private fun fetchDataFromFirestore() {
        val db = FirebaseFirestore.getInstance()
        db.collection("video")
            .get()
            .addOnSuccessListener { snapshot ->
                val categories = mutableListOf<VideoCategory>()
                val fetches = mutableListOf<Task<Uri>>()

                for (document in snapshot.documents.take(5)) {  // Ограничиваем количество категорий до 5
                    val title = document.getString("title") ?: "No Title"
                    val imagesPath = document.getString("images") ?: ""
                    val id = document.id

                    if (imagesPath.isNotEmpty()) {
                        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imagesPath)
                        val downloadTask = storageRef.downloadUrl
                        downloadTask.addOnSuccessListener { uri ->
                            val category = VideoCategory(title, uri.toString(), id)
                            categories.add(category)
                            Log.d("VideoActivity", "Category added: $title, Image URL: ${uri.toString()}")
                        }.addOnFailureListener { e ->
                            Log.e("VideoActivity", "Failed to load image for category $title: ", e)
                            val category = VideoCategory(title, "", id)
                            categories.add(category)
                        }
                        fetches.add(downloadTask)
                    } else {
                        val category = VideoCategory(title, "", id)
                        categories.add(category)
                    }
                }

                Tasks.whenAllComplete(fetches).addOnCompleteListener {
                    if (categories.isNotEmpty()) {
                        videoCategories.add(categories)
                        verticalAdapter.notifyDataSetChanged()
                        Log.d("VideoActivity", "All categories loaded including images: ${categories.size}")
                    } else {
                        Log.d("VideoActivity", "No categories fetched.")
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("VideoActivity", "Error getting video categories: ", exception)
            }
    }

    private fun fetchPopularVideos() {
        val db = FirebaseFirestore.getInstance()
        db.collection("video")
            .get()
            .addOnSuccessListener { snapshot ->
                val tasks = mutableListOf<Task<*>>()
                for (document in snapshot.documents) {
                    val videoCollectionRef = db.collection("video").document(document.id).collection("items")
                    tasks.add(videoCollectionRef.get().addOnSuccessListener { videoSnapshot ->
                        for (videoDoc in videoSnapshot.documents.take(5)) {  // Ограничиваем количество видео до 5
                            val video = videoDoc.toObject(Video::class.java)?.apply {
                                id = videoDoc.id
                                imageUrl = videoDoc.getString("imageUrl") ?: ""
                                categoryId = document.id  // Сохраняем идентификатор категории
                            }
                            if (video != null) {
                                val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(video.imageUrl)
                                storageRef.downloadUrl.addOnSuccessListener { uri ->
                                    video.imageUrl = uri.toString()
                                    Log.d("VideoActivity", "Popular video image URL: ${video.imageUrl}")
                                    fetchLikesAndDislikes(
                                        db.collection("video").document(video.categoryId).collection("items").document(video.id).collection("likes"),
                                        db.collection("video").document(video.categoryId).collection("items").document(video.id).collection("dislikes"),
                                        video,
                                        isPopular = true
                                    )
                                }.addOnFailureListener { e ->
                                    Log.e("VideoActivity", "Failed to load image for video ${video.title}: ", e)
                                    fetchLikesAndDislikes(
                                        db.collection("video").document(video.categoryId).collection("items").document(video.id).collection("likes"),
                                        db.collection("video").document(video.categoryId).collection("items").document(video.id).collection("dislikes"),
                                        video,
                                        isPopular = true
                                    )
                                }
                            }
                        }
                    })
                }
                Tasks.whenAllComplete(tasks).addOnCompleteListener {
                    val limitedPopularVideos = popularVideos.sortedByDescending { it.popularity }.take(5)
                    popularVideos.clear()
                    popularVideos.addAll(limitedPopularVideos)
                    verticalAdapter.updatePopularVideos(popularVideos)
                    Log.d("VideoActivity", "All popular videos fetched and sorted.")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("VideoActivity", "Error fetching popular videos: ", exception)
            }
    }

    private fun fetchLatestVideos() {
        val db = FirebaseFirestore.getInstance()
        db.collection("video")
            .get()
            .addOnSuccessListener { snapshot ->
                val tasks = mutableListOf<Task<*>>()
                for (document in snapshot.documents) {
                    val videoCollectionRef = db.collection("video").document(document.id).collection("items").orderBy("date", Query.Direction.DESCENDING).limit(10)
                    tasks.add(videoCollectionRef.get().addOnSuccessListener { videoSnapshot ->
                        for (videoDoc in videoSnapshot.documents) {
                            val video = videoDoc.toObject(Video::class.java)?.apply {
                                id = videoDoc.id
                                imageUrl = videoDoc.getString("imageUrl") ?: ""
                                categoryId = document.id  // Сохраняем идентификатор категории
                                date = videoDoc.getString("date") ?: ""
                            }
                            if (video != null) {
                                val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(video.imageUrl)
                                storageRef.downloadUrl.addOnSuccessListener { uri ->
                                    video.imageUrl = uri.toString()
                                    Log.d("VideoActivity", "Latest video image URL: ${video.imageUrl}")
                                    fetchLikesAndDislikes(
                                        db.collection("video").document(video.categoryId).collection("items").document(video.id).collection("likes"),
                                        db.collection("video").document(video.categoryId).collection("items").document(video.id).collection("dislikes"),
                                        video,
                                        isPopular = false
                                    )
                                }.addOnFailureListener { e ->
                                    Log.e("VideoActivity", "Failed to load image for video ${video.title}: ", e)
                                    fetchLikesAndDislikes(
                                        db.collection("video").document(video.categoryId).collection("items").document(video.id).collection("likes"),
                                        db.collection("video").document(video.categoryId).collection("items").document(video.id).collection("dislikes"),
                                        video,
                                        isPopular = false
                                    )
                                }
                            }
                        }
                    })
                }
                Tasks.whenAllComplete(tasks).addOnCompleteListener {
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    val limitedLatestVideos = latestVideos.sortedByDescending { dateFormat.parse(it.date) }.take(5)
                    latestVideos.clear()
                    latestVideos.addAll(limitedLatestVideos)
                    verticalAdapter.updateLatestVideos(latestVideos)
                    Log.d("VideoActivity", "All latest videos fetched and sorted.")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("VideoActivity", "Error fetching latest videos: ", exception)
            }
    }

    private fun fetchLikesAndDislikes(
        likesRef: CollectionReference,
        dislikesRef: CollectionReference,
        video: Video,
        isPopular: Boolean
    ) {
        likesRef.get().addOnSuccessListener { likes ->
            val likeCount = if (likes.isEmpty) 0 else likes.size()
            dislikesRef.get().addOnSuccessListener { dislikes ->
                val dislikeCount = if (dislikes.isEmpty) 0 else dislikes.size()
                video.likeCount = likeCount
                video.dislikeCount = dislikeCount
                video.popularity = likeCount - dislikeCount
                if (isPopular) {
                    if (!popularVideos.contains(video)) {
                        popularVideos.add(video)
                    }
                    popularVideos.sortByDescending { it.popularity }
                    verticalAdapter.updatePopularVideos(popularVideos)
                    Log.d("VideoActivity", "Updated popular videos: ${popularVideos.size}")
                } else {
                    if (!latestVideos.contains(video)) {
                        latestVideos.add(video)
                    }
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    latestVideos.sortByDescending { dateFormat.parse(it.date) }
                    verticalAdapter.updateLatestVideos(latestVideos)
                    Log.d("VideoActivity", "Updated latest videos: ${latestVideos.size}")
                }
            }.addOnFailureListener { e ->
                Log.e("VideoActivity", "Error getting dislikes: ", e)
                video.dislikeCount = 0
                video.popularity = video.likeCount - video.dislikeCount
                if (isPopular) {
                    if (!popularVideos.contains(video)) {
                        popularVideos.add(video)
                    }
                    popularVideos.sortByDescending { it.popularity }
                    verticalAdapter.updatePopularVideos(popularVideos)
                } else {
                    if (!latestVideos.contains(video)) {
                        latestVideos.add(video)
                    }
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    latestVideos.sortByDescending { dateFormat.parse(it.date) }
                    verticalAdapter.updateLatestVideos(latestVideos)
                }
            }
        }.addOnFailureListener { e ->
            Log.e("VideoActivity", "Error getting likes: ", e)
            video.likeCount = 0
            dislikesRef.get().addOnSuccessListener { dislikes ->
                val dislikeCount = if (dislikes.isEmpty) 0 else dislikes.size()
                video.dislikeCount = dislikeCount
                video.popularity = video.likeCount - dislikeCount
                if (isPopular) {
                    if (!popularVideos.contains(video)) {
                        popularVideos.add(video)
                    }
                    popularVideos.sortByDescending { it.popularity }
                    verticalAdapter.updatePopularVideos(popularVideos)
                    Log.d("VideoActivity", "Updated popular videos: ${popularVideos.size}")
                } else {
                    if (!latestVideos.contains(video)) {
                        latestVideos.add(video)
                    }
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    latestVideos.sortByDescending { dateFormat.parse(it.date) }
                    verticalAdapter.updateLatestVideos(latestVideos)
                    Log.d("VideoActivity", "Updated latest videos: ${latestVideos.size}")
                }
            }.addOnFailureListener { e ->
                Log.e("VideoActivity", "Error getting dislikes: ", e)
                video.dislikeCount = 0
                video.popularity = video.likeCount - video.dislikeCount
                if (isPopular) {
                    if (!popularVideos.contains(video)) {
                        popularVideos.add(video)
                    }
                    popularVideos.sortByDescending { it.popularity }
                    verticalAdapter.updatePopularVideos(popularVideos)
                } else {
                    if (!latestVideos.contains(video)) {
                        latestVideos.add(video)
                    }
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    latestVideos.sortByDescending { dateFormat.parse(it.date) }
                    verticalAdapter.updateLatestVideos(latestVideos)
                }
            }
        }
    }
}