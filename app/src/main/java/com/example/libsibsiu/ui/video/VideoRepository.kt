package com.example.libsibsiu.ui.video

import com.example.libsibsiu.models.Video
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class VideoRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getVideoDetails(categoryId: String, videoId: String, callback: (Video?, String?) -> Unit) {
        val docRef = db.collection("video").document(categoryId).collection("items").document(videoId)
        docRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val video = document.toObject(Video::class.java)
                callback(video, null)
            } else {
                callback(null, "Video document does not exist")
            }
        }.addOnFailureListener { exception ->
            callback(null, exception.message)
        }
    }

    fun getLikeDislikeCounts(categoryId: String, videoId: String, callback: (Int, Int) -> Unit) {
        val likesRef = db.collection("video").document(categoryId).collection("items").document(videoId).collection("likes")
        val dislikesRef = db.collection("video").document(categoryId).collection("items").document(videoId).collection("dislikes")

        likesRef.get().addOnSuccessListener { likesSnapshot ->
            val likesCount = likesSnapshot.size()
            dislikesRef.get().addOnSuccessListener { dislikesSnapshot ->
                val dislikesCount = dislikesSnapshot.size()
                callback(likesCount, dislikesCount)
            }
        }
    }

    fun getUserLikeDislikeStatus(categoryId: String, videoId: String, callback: (Boolean, Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return callback(false, false)

        val likesRef = db.collection("video").document(categoryId).collection("items").document(videoId).collection("likes").document(userId)
        val dislikesRef = db.collection("video").document(categoryId).collection("items").document(videoId).collection("dislikes").document(userId)

        likesRef.get().addOnSuccessListener { likeDoc ->
            dislikesRef.get().addOnSuccessListener { dislikeDoc ->
                callback(likeDoc.exists(), dislikeDoc.exists())
            }
        }
    }

    fun toggleLikeDislike(categoryId: String, videoId: String, isLike: Boolean, callback: (Boolean, String?) -> Unit) {
        val userId = auth.currentUser?.uid ?: return callback(false, "User not logged in")

        val targetCollection = if (isLike) "likes" else "dislikes"
        val oppositeCollection = if (isLike) "dislikes" else "likes"

        val targetRef = db.collection("video").document(categoryId).collection("items").document(videoId).collection(targetCollection).document(userId)
        val oppositeRef = db.collection("video").document(categoryId).collection("items").document(videoId).collection(oppositeCollection).document(userId)

        db.runTransaction { transaction ->
            val targetDocument = transaction.get(targetRef)
            if (targetDocument.exists()) {
                transaction.delete(targetRef)
            } else {
                transaction.set(targetRef, hashMapOf("timestamp" to System.currentTimeMillis()))
                transaction.delete(oppositeRef)
            }
        }.addOnSuccessListener {
            callback(true, null)
        }.addOnFailureListener { exception ->
            callback(false, exception.message)
        }
    }
}