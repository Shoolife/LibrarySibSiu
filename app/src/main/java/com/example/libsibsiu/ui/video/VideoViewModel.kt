package com.example.libsibsiu.ui.video

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.libsibsiu.models.Video
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class VideoViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _videoList = MutableLiveData<List<Video>>()
    val videoList: LiveData<List<Video>> = _videoList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadAllVideos() {
        _isLoading.value = true
        db.collection("video").get().addOnSuccessListener { snapshot ->
            val tasks = mutableListOf<Task<*>>()
            val allVideos = mutableListOf<Video>()

            for (document in snapshot.documents) {
                val videoCollectionRef = db.collection("video").document(document.id).collection("items")
                val docTasks = videoCollectionRef.get().continueWithTask { task ->
                    val innerTasks = mutableListOf<Task<*>>()
                    if (task.isSuccessful) {
                        task.result?.forEach { videoDoc ->
                            val video = videoDoc.toObject(Video::class.java)?.apply {
                                id = videoDoc.id
                                imageUrl = videoDoc.getString("imageUrl") ?: ""
                                categoryId = document.id
                            }
                            video?.let { v ->
                                innerTasks.add(fetchVideoData(db, v, allVideos))
                            }
                        }
                    } else {
                        Log.e("VideoViewModel", "Error loading video items: ${task.exception?.message}")
                    }
                    Tasks.whenAllComplete(innerTasks)
                }
                tasks.add(docTasks)
            }

            Tasks.whenAllComplete(tasks).addOnCompleteListener {
                synchronized(allVideos) {
                    _videoList.value = allVideos
                    _isLoading.value = false
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("VideoViewModel", "Error fetching videos: ", exception)
            _isLoading.value = false
        }
    }

    fun loadVideos(categoryId: String) {
        _isLoading.value = true
        db.collection("video").document(categoryId).collection("items").get()
            .addOnSuccessListener { documents ->
                val videos = documents.mapNotNull { document ->
                    document.toObject(Video::class.java)?.apply { id = document.id }
                }
                _videoList.value = videos
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                Log.e("VideoViewModel", "Error fetching videos for category: $categoryId", e)
                _isLoading.value = false
            }
    }

    fun loadAllPopularVideos() {
        _isLoading.value = true
        db.collectionGroup("items").orderBy("popularity", Query.Direction.DESCENDING).get()
            .addOnSuccessListener { documents ->
                val videos = documents.mapNotNull { document ->
                    document.toObject(Video::class.java)?.apply { id = document.id }
                }
                _videoList.value = videos
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                Log.e("VideoViewModel", "Error fetching popular videos", e)
                _isLoading.value = false
            }
    }

    fun loadLatestVideos() {
        _isLoading.value = true
        db.collectionGroup("items").orderBy("date", Query.Direction.DESCENDING).limit(10).get()
            .addOnSuccessListener { documents ->
                val videos = documents.mapNotNull { document ->
                    document.toObject(Video::class.java)?.apply { id = document.id }
                }
                _videoList.value = videos
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                Log.e("VideoViewModel", "Error fetching latest videos", e)
                _isLoading.value = false
            }
    }

    fun searchVideos(query: String) {
        _isLoading.value = true
        db.collectionGroup("items")
            .whereGreaterThanOrEqualTo("title", query)
            .whereLessThanOrEqualTo("title", "$query\uf8ff")
            .get()
            .addOnSuccessListener { documents ->
                val videos = documents.mapNotNull { document ->
                    document.toObject(Video::class.java)?.apply { id = document.id }
                }
                _videoList.value = videos
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                Log.e("VideoViewModel", "Error fetching search results", e)
                _isLoading.value = false
            }
    }

    private fun fetchVideoData(db: FirebaseFirestore, video: Video, allVideos: MutableList<Video>): Task<*> {
        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(video.imageUrl)
        val imageUrlTask = storageRef.downloadUrl
        val likeDislikeTask = loadLikeDislikeCounts(video.categoryId, video)

        return Tasks.whenAllComplete(imageUrlTask, likeDislikeTask).continueWithTask { task ->
            if (task.isSuccessful && task.result != null) {
                val downloadUrl = task.result[0].result as Uri?
                val imageUrl = downloadUrl?.toString() ?: ""
                video.imageUrl = imageUrl

                synchronized(allVideos) {
                    allVideos.add(video)
                }
                Log.d("VideoViewModel", "Video added with image URL: $imageUrl")
            } else {
                Log.e("VideoViewModel", "Failed to fetch data for video: ${video.title}, Error: ${task.exception?.message}")
            }
            Tasks.forResult(null)
        }
    }

    private fun loadLikeDislikeCounts(categoryId: String, video: Video): Task<*> {
        val likesRef = db.collection("video").document(categoryId).collection("items").document(video.id).collection("likes")
        val dislikesRef = db.collection("video").document(categoryId).collection("items").document(video.id).collection("dislikes")

        return Tasks.whenAllComplete(likesRef.get(), dislikesRef.get()).continueWithTask { task ->
            if (task.isSuccessful) {
                val likesResult = task.result?.firstOrNull()?.result as QuerySnapshot?
                val dislikesResult = task.result?.getOrNull(1)?.result as QuerySnapshot?
                video.likeCount = likesResult?.size() ?: 0
                video.dislikeCount = dislikesResult?.size() ?: 0
                video.popularity = video.likeCount - video.dislikeCount
                Log.d("VideoViewModel", "Likes and dislikes fetched for video: ${video.title}")
            } else {
                Log.e("VideoViewModel", "Error fetching likes/dislikes for video: ${video.title}", task.exception)
            }
            Tasks.forResult(video)
        }
    }
}