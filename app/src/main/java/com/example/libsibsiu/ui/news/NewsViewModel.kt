package com.example.libsibsiu.ui.news

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.libsibsiu.models.News
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.Locale

class NewsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _newsList = MutableLiveData<List<News>>()
    val newsList: LiveData<List<News>> = _newsList

    private val _tags = MutableLiveData<List<String>>()
    val tags: LiveData<List<String>> = _tags

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadAllNews() {
        _isLoading.value = true
        db.collection("news").get().addOnSuccessListener { documents ->
            val tasks = mutableListOf<Task<*>>()
            val loadedNews = mutableListOf<News>()

            for (document in documents) {
                val news = document.toObject(News::class.java)?.apply { id = document.id }
                if (news != null) {
                    val imageTask = FirebaseStorage.getInstance().getReferenceFromUrl(news.imageUrl)
                        .downloadUrl
                        .continueWithTask { uriTask ->
                            if (uriTask.isSuccessful) {
                                news.imageUrl = uriTask.result.toString()
                            }
                            Tasks.forResult(news)
                        }

                    val likeDislikeTask = loadLikeDislikeCounts(news.id, news)

                    tasks.add(Tasks.whenAll(imageTask, likeDislikeTask))
                    loadedNews.add(news)
                }
            }

            Tasks.whenAllComplete(tasks).addOnCompleteListener {
                _newsList.value = loadedNews
                _isLoading.value = false
            }
        }
    }

    fun loadAllTags() {
        db.collection("news").get().addOnSuccessListener { documents ->
            val allTags = mutableSetOf<String>()
            for (document in documents) {
                val news = document.toObject(News::class.java)
                allTags.addAll(news.tags)
            }
            _tags.value = allTags.toList()
        }
    }

    fun filterNewsByTags(tags: List<String>) {
        if (tags.isEmpty()) {
            loadAllNews()
            return
        }

        _isLoading.value = true
        db.collection("news")
            .whereArrayContainsAny("tags", tags)
            .get()
            .addOnSuccessListener { result ->
                val tasks = mutableListOf<Task<*>>()
                val loadedNews = mutableListOf<News>()

                for (document in result) {
                    val news = document.toObject(News::class.java)?.apply { id = document.id }
                    if (news != null) {
                        val imageTask = FirebaseStorage.getInstance().getReferenceFromUrl(news.imageUrl)
                            .downloadUrl
                            .continueWithTask { uriTask ->
                                if (uriTask.isSuccessful) {
                                    news.imageUrl = uriTask.result.toString()
                                }
                                Tasks.forResult(news)
                            }

                        val likeDislikeTask = loadLikeDislikeCounts(news.id, news)

                        tasks.add(Tasks.whenAll(imageTask, likeDislikeTask))
                        loadedNews.add(news)
                    }
                }

                Tasks.whenAllComplete(tasks).addOnCompleteListener {
                    _newsList.value = loadedNews
                    _isLoading.value = false
                }
            }
    }

    private fun loadLikeDislikeCounts(newsId: String, news: News): Task<News> {
        val likesRef = db.collection("news").document(newsId).collection("likes")
        val dislikesRef = db.collection("news").document(newsId).collection("dislikes")

        return likesRef.get().continueWithTask { task ->
            val likeCount = task.result?.size() ?: 0
            news.likeCount = likeCount
            dislikesRef.get()
        }.continueWith { task ->
            val dislikeCount = task.result?.size() ?: 0
            news.dislikeCount = dislikeCount
            news // Возвращаем обновленный объект News
        }
    }

    fun updateNewsItem(newsId: String) {
        _isLoading.value = true
        db.collection("news").document(newsId).get().addOnSuccessListener { document ->
            val news = document.toObject(News::class.java)?.apply { id = document.id }
            if (news != null) {
                val imageTask = FirebaseStorage.getInstance().getReferenceFromUrl(news.imageUrl)
                    .downloadUrl
                    .continueWithTask { uriTask ->
                        if (uriTask.isSuccessful) {
                            news.imageUrl = uriTask.result.toString()
                        }
                        Tasks.forResult(news)
                    }

                val likeDislikeTask = loadLikeDislikeCounts(news.id, news)

                Tasks.whenAll(imageTask, likeDislikeTask).addOnCompleteListener {
                    _newsList.value = _newsList.value?.map {
                        if (it.id == newsId) news else it
                    }
                    _isLoading.value = false
                }
            }
        }
    }

    fun searchNews(query: String) {
        val searchQuery = query.lowercase(Locale.getDefault()).split(" ").filter { it.isNotBlank() }

        if (searchQuery.isEmpty()) {
            _newsList.value = emptyList() // Очищаем список новостей
            return
        }

        _isLoading.value = true
        db.collection("news").whereArrayContainsAny("searchKeywords", searchQuery).get()
            .addOnSuccessListener { result ->
                val tasks = mutableListOf<Task<*>>()
                val loadedNews = mutableListOf<News>()

                for (document in result) {
                    val news = document.toObject(News::class.java)?.apply { id = document.id }
                    if (news != null) {
                        val imageTask = FirebaseStorage.getInstance().getReferenceFromUrl(news.imageUrl)
                            .downloadUrl
                            .continueWithTask { uriTask ->
                                if (uriTask.isSuccessful) {
                                    news.imageUrl = uriTask.result.toString()
                                }
                                Tasks.forResult(news)
                            }

                        val likeDislikeTask = loadLikeDislikeCounts(news.id, news)

                        tasks.add(Tasks.whenAll(imageTask, likeDislikeTask))
                        loadedNews.add(news)
                    }
                }

                Tasks.whenAllComplete(tasks).addOnCompleteListener {
                    _newsList.value = loadedNews
                    _isLoading.value = false
                }
            }
    }
}