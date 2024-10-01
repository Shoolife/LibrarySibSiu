package com.example.libsibsiu.ui.video

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.libsibsiu.models.Video

class VideoDetailViewModel : ViewModel() {
    private val repository = VideoRepository()

    private val _videoDetails = MutableLiveData<Video?>()
    val videoDetails: LiveData<Video?> = _videoDetails

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

    fun loadVideoDetails(categoryId: String, videoId: String) {
        repository.getVideoDetails(categoryId, videoId) { video, errorMessage ->
            if (errorMessage != null) {
                _errorMessages.postValue(errorMessage)
            } else {
                _videoDetails.postValue(video)
            }
        }
    }

    fun getLikeDislikeCounts(categoryId: String, videoId: String) {
        repository.getLikeDislikeCounts(categoryId, videoId) { likes, dislikes ->
            _likeCount.postValue(likes)
            _dislikeCount.postValue(dislikes)
        }
    }

    fun getUserLikeDislikeStatus(categoryId: String, videoId: String) {
        repository.getUserLikeDislikeStatus(categoryId, videoId) { liked, disliked ->
            _userLiked.postValue(liked)
            _userDisliked.postValue(disliked)
        }
    }

    fun toggleLikeDislike(categoryId: String, videoId: String, isLike: Boolean) {
        repository.toggleLikeDislike(categoryId, videoId, isLike) { success, errorMessage ->
            if (success) {
                getLikeDislikeCounts(categoryId, videoId) // Update counts after toggling
                getUserLikeDislikeStatus(categoryId, videoId) // Update like/dislike status
            } else {
                _errorMessages.postValue(errorMessage)
            }
        }
    }

    // Optional: Clear error messages
    fun clearErrors() {
        _errorMessages.postValue(null)
    }
}