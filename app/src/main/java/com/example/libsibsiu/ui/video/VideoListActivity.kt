package com.example.libsibsiu.ui.video

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.libsibsiu.adapters.video.VideoListAdapter
import com.example.libsibsiu.databinding.ActivityVideoListBinding
import com.example.libsibsiu.models.Video
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.QuerySnapshot
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class VideoListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVideoListBinding
    private lateinit var videoAdapter: VideoListAdapter
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechRecognizerIntent: Intent
    private var categoryId: String? = null
    private var loadAllPopular: Boolean = false
    private var loadLatest: Boolean = false

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d("VideoListActivity", "Returning from VideoDetailActivity with RESULT_OK")
            if (loadAllPopular) {
                loadAllPopularVideos()
            } else if (loadLatest) {
                loadLatestVideos()
            } else {
                categoryId?.let { loadVideos(it) }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("VideoListActivity", "onCreate called")
        binding = ActivityVideoListBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        categoryId = intent.getStringExtra("category_id")
        val categoryName = intent.getStringExtra("category_name") ?: "Videos"
        loadAllPopular = intent.getBooleanExtra("all_popular", false)
        loadLatest = intent.getBooleanExtra("load_latest", false)

        binding.tvVideoList.text = when {
            loadAllPopular -> "Популярное"
            loadLatest -> "Последние видео"
            else -> categoryName
        }

        Log.d("VideoListActivity", "Category ID: $categoryId, Load all popular: $loadAllPopular, Load latest: $loadLatest")

        setupRecyclerView()
        setupSpeechRecognizer()
        setupListeners()

        when {
            loadAllPopular -> loadAllPopularVideos()
            loadLatest -> loadLatestVideos()
            else -> categoryId?.let { loadVideos(it) }
        }

        binding.backArrow.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        videoAdapter = VideoListAdapter(emptyList())
        videoAdapter.setOnItemClickListener { videoId ->
            val selectedVideo = videoAdapter.videos.firstOrNull { it.id == videoId }
            val intent = Intent(this, VideoDetailActivity::class.java).apply {
                putExtra("VIDEO_ID", selectedVideo?.id)
                putExtra("CATEGORY_ID", selectedVideo?.categoryId)
            }
            startForResult.launch(intent)
        }

        binding.rvVideoList.apply {
            layoutManager = LinearLayoutManager(this@VideoListActivity)
            adapter = videoAdapter
        }
        Log.d("VideoListActivity", "RecyclerView setup completed")
    }

    private fun setupListeners() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrEmpty()) {
                    performSearch(s.toString())
                } else {
                    when {
                        loadAllPopular -> loadAllPopularVideos()
                        loadLatest -> loadLatestVideos()
                        else -> categoryId?.let { loadVideos(it) }
                    }
                }
            }
        })

        binding.microphoneIcon.setOnClickListener {
            startVoiceInput()
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
                    val query = it.lowercase(Locale.getDefault())
                    binding.searchEditText.setText(query)
                    performSearch(query)
                }
            }

            override fun onError(error: Int) { showError("Ошибка ввода голоса") }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun startVoiceInput() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            speechRecognizer.startListening(speechRecognizerIntent)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
    }

    private fun performSearch(query: String) {
        Log.d("VideoListActivity", "Searching for videos with query: $query")
        showLoadingIndicator()
        val db = FirebaseFirestore.getInstance()
        db.collectionGroup("items")
            .whereArrayContains("searchKeywords", query.toLowerCase(Locale.getDefault()))
            .get()
            .addOnSuccessListener { documents ->
                val videos = documents.mapNotNull { document ->
                    document.toObject(Video::class.java)?.apply { id = document.id }
                }
                videoAdapter.updateData(videos)
                hideLoadingIndicator()
            }
            .addOnFailureListener { e ->
                Log.e("VideoListActivity", "Error searching videos", e)
                hideLoadingIndicator()
            }
    }

    private fun loadVideos(categoryId: String) {
        Log.d("VideoListActivity", "Loading videos for category ID: $categoryId")
        showLoadingIndicator()
        val db = FirebaseFirestore.getInstance()
        db.collection("video")
            .document(categoryId)
            .collection("items")
            .get()
            .addOnSuccessListener { documents ->
                Log.d("VideoListActivity", "Successfully fetched videos for category: $categoryId")
                val videos = mutableListOf<Video>()
                val tasks = mutableListOf<Task<*>>()
                for (document in documents) {
                    val id = document.id
                    val title = document.getString("title") ?: "No title"
                    val date = document.getString("date") ?: "No date"
                    val imageUrl = document.getString("imageUrl") ?: ""
                    val video = Video(
                        title = title,
                        date = date,
                        imageUrl = imageUrl,
                        id = id,
                        categoryId = categoryId,
                        searchKeywords = title.split(" ").map { it.toLowerCase(Locale.getDefault()) } // Создаем ключевые слова для поиска
                    )
                    Log.d("VideoListActivity", "Fetched video: $title")

                    if (imageUrl.isNotEmpty()) {
                        val imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
                        val task = imageRef.downloadUrl.continueWithTask { uriTask ->
                            if (uriTask.isSuccessful) {
                                val updatedVideo = video.copy(imageUrl = uriTask.result.toString())
                                loadLikeDislikeCounts(categoryId, updatedVideo) {
                                    videos.add(it)
                                    Log.d("VideoListActivity", "Loaded video: ${it.title}, loaded count: ${videos.size}/${documents.size()}")
                                    if (videos.size == documents.size()) {
                                        videoAdapter.updateData(videos)
                                        Log.d("VideoListActivity", "Updated video list adapter with ${videos.size} videos")
                                    }
                                }
                            } else {
                                loadLikeDislikeCounts(categoryId, video) {
                                    videos.add(it)
                                    Log.d("VideoListActivity", "Loaded video: ${it.title}, loaded count: ${videos.size}/${documents.size()}")
                                    if (videos.size == documents.size()) {
                                        videoAdapter.updateData(videos)
                                        Log.d("VideoListActivity", "Updated video list adapter with ${videos.size} videos")
                                    }
                                }
                            }
                            Tasks.forResult<Void>(null)
                        }
                        tasks.add(task)
                    } else {
                        loadLikeDislikeCounts(categoryId, video) {
                            videos.add(it)
                            Log.d("VideoListActivity", "Loaded video: ${it.title}, loaded count: ${videos.size}/${documents.size()}")
                            if (videos.size == documents.size()) {
                                videoAdapter.updateData(videos)
                                Log.d("VideoListActivity", "Updated video list adapter with ${videos.size} videos")
                            }
                        }
                    }
                }
                Tasks.whenAllComplete(tasks).addOnCompleteListener {
                    Log.d("VideoListActivity", "All tasks complete")
                    videoAdapter.updateData(videos)
                    Log.d("VideoListActivity", "Updated video list adapter with ${videos.size} videos")
                    hideLoadingIndicator()
                }
            }
            .addOnFailureListener { e ->
                Log.e("VideoListActivity", "Error loading videos", e)
                hideLoadingIndicator()
            }
    }

    private fun loadAllPopularVideos() {
        Log.d("VideoListActivity", "Loading all popular videos")
        showLoadingIndicator()
        val db = FirebaseFirestore.getInstance()
        db.collection("video").get().addOnSuccessListener { snapshot ->
            Log.d("VideoListActivity", "Successfully fetched all popular videos")
            val allVideos = mutableListOf<Video>()
            val allTasks = mutableListOf<Task<*>>()  // Список всех задач

            for (document in snapshot.documents) {
                val videoCollectionRef = db.collection("video").document(document.id).collection("items")
                val docTasks = videoCollectionRef.get().continueWithTask { task ->
                    val innerTasks = mutableListOf<Task<*>>()
                    if (task.isSuccessful) {
                        task.result?.forEach { videoDoc ->
                            val video = videoDoc.toObject(Video::class.java)?.apply {
                                id = videoDoc.id
                                imageUrl = videoDoc.getString("imageUrl") ?: ""
                                categoryId = document.id  // Сохраняем идентификатор категории
                            }
                            video?.let { v ->
                                innerTasks.add(fetchVideoData(db, v, allVideos))
                            }
                        }
                    } else {
                        Log.e("VideoListActivity", "Error loading video items: ${task.exception?.message}")
                    }
                    Tasks.whenAllComplete(innerTasks)  // Дожидаемся завершения обработки всех видео в документе
                }
                allTasks.add(docTasks)  // Добавляем задачи по документам в общий список задач
            }

            // Дожидаемся завершения всех задач по всем документам
            Tasks.whenAllComplete(allTasks).addOnCompleteListener {
                synchronized(allVideos) {
                    val sortedVideos = allVideos.sortedByDescending { it.popularity }
                    videoAdapter.updateData(sortedVideos)
                    Log.d("VideoListActivity", "Updated video list adapter with ${sortedVideos.size} popular videos after all tasks completed")
                    hideLoadingIndicator()
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("VideoListActivity", "Error fetching popular videos: ", exception)
            hideLoadingIndicator()
        }
    }

    private fun loadLatestVideos() {
        Log.d("VideoListActivity", "Loading latest videos")
        showLoadingIndicator()
        val db = FirebaseFirestore.getInstance()
        db.collection("video")
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d("VideoListActivity", "Successfully fetched all latest videos")
                val allVideos = mutableListOf<Video>()
                val allTasks = mutableListOf<Task<*>>()  // Список всех задач

                for (document in snapshot.documents) {
                    val videoCollectionRef = db.collection("video").document(document.id).collection("items").orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(10)
                    val docTasks = videoCollectionRef.get().continueWithTask { task ->
                        val innerTasks = mutableListOf<Task<*>>()
                        if (task.isSuccessful) {
                            task.result?.forEach { videoDoc ->
                                val video = videoDoc.toObject(Video::class.java)?.apply {
                                    id = videoDoc.id
                                    imageUrl = videoDoc.getString("imageUrl") ?: ""
                                    categoryId = document.id  // Сохраняем идентификатор категории
                                    date = videoDoc.getString("date") ?: ""
                                }
                                video?.let { v ->
                                    innerTasks.add(fetchVideoData(db, v, allVideos))
                                }
                            }
                        } else {
                            Log.e("VideoListActivity", "Error loading video items: ${task.exception?.message}")
                        }
                        Tasks.whenAllComplete(innerTasks)  // Дожидаемся завершения обработки всех видео в документе
                    }
                    allTasks.add(docTasks)  // Добавляем задачи по документам в общий список задач
                }

                // Дожидаемся завершения всех задач по всем документам
                Tasks.whenAllComplete(allTasks).addOnCompleteListener {
                    synchronized(allVideos) {
                        val sortedVideos = allVideos.sortedByDescending { parseDate(it.date) }
                        videoAdapter.updateData(sortedVideos)
                        Log.d("VideoListActivity", "Updated video list adapter with ${sortedVideos.size} latest videos after all tasks completed")
                        hideLoadingIndicator()
                    }
                }
            }.addOnFailureListener { exception ->
                Log.e("VideoListActivity", "Error fetching latest videos: ", exception)
                hideLoadingIndicator()
            }
    }

    private fun fetchVideoData(db: FirebaseFirestore, video: Video, allVideos: MutableList<Video>): Task<*> {
        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(video.imageUrl)
        val imageUrlTask = storageRef.downloadUrl
        val likeDislikeTask = fetchLikesAndDislikesForAllPopular(db, video)

        return Tasks.whenAllComplete(imageUrlTask, likeDislikeTask).continueWithTask { task ->
            if (task.isSuccessful && task.result != null) {
                // Извлекаем результаты из списка задач
                val downloadUrl = task.result[0].result as Uri?  // Предполагаем, что первая задача - это imageUrlTask
                val imageUrl = downloadUrl?.toString() ?: ""
                video.imageUrl = imageUrl

                synchronized(allVideos) {
                    allVideos.add(video)
                }
                Log.d("VideoListActivity", "Video added with image URL: $imageUrl")
            } else {
                Log.e("VideoListActivity", "Failed to fetch data for video: ${video.title}, Error: ${task.exception?.message}")
            }
            Tasks.forResult(null)
        }
    }

    private fun fetchLikesAndDislikesForAllPopular(db: FirebaseFirestore, video: Video): Task<*> {
        val likesRef = db.collection("video").document(video.categoryId).
                            collection("items").document(video.id).collection("likes")
        val dislikesRef = db.collection("video").document(video.categoryId).
                            collection("items").document(video.id).collection("dislikes")

        return Tasks.whenAllComplete(likesRef.get(), dislikesRef.get()).continueWithTask { task ->
            if (task.isSuccessful) {
                val likesResult = task.result?.firstOrNull()?.result as QuerySnapshot?
                val dislikesResult = task.result?.getOrNull(1)?.result as QuerySnapshot?
                video.likeCount = likesResult?.size() ?: 0
                video.dislikeCount = dislikesResult?.size() ?: 0
                video.popularity = video.likeCount - video.dislikeCount
                Log.d("VideoListActivity", "Likes and dislikes fetched for video: ${video.title}")
            } else {
                Log.e("VideoListActivity", "Error fetching likes/dislikes for video: ${video.title}", task.exception)
            }
            Tasks.forResult(video)
        }
    }

    private fun loadLikeDislikeCounts(categoryId: String, video: Video, callback: (Video) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val likesRef = db.collection("video").document(categoryId).collection("items").document(video.id).collection("likes")
        val dislikesRef = db.collection("video").document(categoryId).collection("items").document(video.id).collection("dislikes")

        likesRef.get().addOnSuccessListener { likesSnapshot ->
            val likeCount = likesSnapshot.size()
            dislikesRef.get().addOnSuccessListener { dislikesSnapshot ->
                val dislikeCount = dislikesSnapshot.size()
                val updatedVideo = video.copy(likeCount = likeCount, dislikeCount = dislikeCount)
                callback(updatedVideo)
                Log.d("VideoListActivity", "Fetched like and dislike counts for video: ${video.title}")
            }.addOnFailureListener { e ->
                Log.e("VideoListActivity", "Error fetching dislikes for video: ${video.id}", e)
                callback(video)
            }
        }.addOnFailureListener { e ->
            Log.e("VideoListActivity", "Error fetching likes for video: ${video.id}", e)
            callback(video)
        }
    }

    private fun parseDate(dateString: String): Long {
        val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return try {
            format.parse(dateString)?.time ?: 0
        } catch (e: ParseException) {
            0
        }
    }

    private fun showLoadingIndicator() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideLoadingIndicator() {
        binding.progressBar.visibility = View.GONE
    }
}