package com.example.libsibsiu.ui.news

import com.example.libsibsiu.models.News
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class NewsRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getNewsDetails(newsId: String, callback: (News?, String?) -> Unit) {
        db.collection("news").document(newsId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val news = document.toObject(News::class.java)
                    news?.id = document.id  // Ensure the ID is set
                    if (news?.imageUrl?.isNotEmpty() == true) {
                        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(news.imageUrl)
                        storageRef.downloadUrl.addOnSuccessListener { uri ->
                            news.imageUrl = uri.toString()
                            callback(news, null)
                        }.addOnFailureListener { exception ->
                            callback(null, "Error getting image URL: ${exception.message}")
                        }
                    } else {
                        callback(news, null)
                    }
                } else {
                    callback(null, "Document not found")
                }
            }
            .addOnFailureListener { exception ->
                callback(null, "Error getting document: ${exception.message}")
            }
    }

    fun getLikeDislikeCounts(newsId: String, callback: (Int, Int) -> Unit) {
        val likesRef = db.collection("news").document(newsId).collection("likes")
        val dislikesRef = db.collection("news").document(newsId).collection("dislikes")

        likesRef.get().addOnSuccessListener { likesSnapshot ->
            val likeCount = likesSnapshot.size()
            dislikesRef.get().addOnSuccessListener { dislikesSnapshot ->
                val dislikeCount = dislikesSnapshot.size()
                callback(likeCount, dislikeCount)
            }
        }
    }

    fun getUserLikeDislikeStatus(newsId: String, callback: (Boolean, Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return callback(false, false)

        val likesRef = db.collection("news").document(newsId).collection("likes").document(userId)
        val dislikesRef = db.collection("news").document(newsId).collection("dislikes").document(userId)

        likesRef.get().addOnSuccessListener { likeDoc ->
            dislikesRef.get().addOnSuccessListener { dislikeDoc ->
                callback(likeDoc.exists(), dislikeDoc.exists())
            }
        }
    }

    fun toggleLikeDislike(newsId: String, isLike: Boolean, callback: (Boolean, String?) -> Unit) {
        val userId = auth.currentUser?.uid ?: return callback(false, "User not logged in")

        val targetCollection = if (isLike) "likes" else "dislikes"
        val oppositeCollection = if (isLike) "dislikes" else "likes"

        val targetRef = db.collection("news").document(newsId).collection(targetCollection).document(userId)
        val oppositeRef = db.collection("news").document(newsId).collection(oppositeCollection).document(userId)

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