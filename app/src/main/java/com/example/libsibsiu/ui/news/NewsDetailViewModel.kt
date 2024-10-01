package com.example.libsibsiu.ui.news

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.libsibsiu.models.News

class NewsDetailViewModel : ViewModel() {
    private val repository = NewsRepository()

    private val _newsDetails = MutableLiveData<News?>()
    val newsDetails: LiveData<News?> = _newsDetails

    private val _likeCount = MutableLiveData<Int>()
    val likeCount: LiveData<Int> = _likeCount

    private val _dislikeCount = MutableLiveData<Int>()
    val dislikeCount: LiveData<Int> = _dislikeCount

    private val _userLiked = MutableLiveData<Boolean>()
    val userLiked: LiveData<Boolean> = _userLiked

    private val _userDisliked = MutableLiveData<Boolean>()
    val userDisliked: LiveData<Boolean> = _userDisliked

    private val _errorMessages = MutableLiveData<String?>()
    val errorMessages: LiveData<String?> = _errorMessages

    fun loadNewsDetails(newsId: String) {
        repository.getNewsDetails(newsId) { news, errorMessage ->
            if (errorMessage != null) {
                _errorMessages.postValue(errorMessage)
            } else {
                _newsDetails.postValue(news)
            }
        }
    }

    fun getLikeDislikeCounts(newsId: String) {
        repository.getLikeDislikeCounts(newsId) { likes, dislikes ->
            _likeCount.postValue(likes)
            _dislikeCount.postValue(dislikes)
        }
    }

    fun getUserLikeDislikeStatus(newsId: String) {
        repository.getUserLikeDislikeStatus(newsId) { liked, disliked ->
            _userLiked.postValue(liked)
            _userDisliked.postValue(disliked)
        }
    }

    fun toggleLikeDislike(newsId: String, isLike: Boolean) {
        repository.toggleLikeDislike(newsId, isLike) { success, errorMessage ->
            if (success) {
                getLikeDislikeCounts(newsId)
                getUserLikeDislikeStatus(newsId)
            } else {
                _errorMessages.postValue(errorMessage)
            }
        }
    }

    fun clearErrors() {
        _errorMessages.postValue(null)
    }
}