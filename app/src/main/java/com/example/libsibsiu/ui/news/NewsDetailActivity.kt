package com.example.libsibsiu.ui.news

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.libsibsiu.R
import com.example.libsibsiu.databinding.ActivityNewsDetailBinding
import com.example.libsibsiu.models.News
import com.google.firebase.storage.FirebaseStorage

class NewsDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewsDetailBinding
    private val viewModel: NewsDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewsDetailBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        val newsId = intent.getStringExtra("news_id") ?: run {
            Toast.makeText(this, "News ID is missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel.loadNewsDetails(newsId)
        viewModel.getLikeDislikeCounts(newsId)
        viewModel.getUserLikeDislikeStatus(newsId)

        setupObservers()
        setupClickListeners(newsId)

        binding.backArrow.setOnClickListener {
            finish()
        }

        // Установка обратного вызова для обработки нажатия на кнопку "назад"
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent()
                setResult(RESULT_OK, intent)
                finish()
            }
        })
    }

    private fun setupObservers() {
        viewModel.newsDetails.observe(this, Observer { news ->
            news?.let {
                updateUI(it)
            }
        })

        viewModel.likeCount.observe(this, Observer { likes ->
            binding.tvCollLike.text = likes.toString()
        })

        viewModel.dislikeCount.observe(this, Observer { dislikes ->
            binding.tvCollDislike.text = dislikes.toString()
        })

        viewModel.userLiked.observe(this, Observer { liked ->
            if (liked) {
                binding.imgLike.setImageResource(R.drawable.like_full)
            } else {
                binding.imgLike.setImageResource(R.drawable.like)
            }
        })

        viewModel.userDisliked.observe(this, Observer { disliked ->
            if (disliked) {
                binding.imgDislike.setImageResource(R.drawable.dislike_full)
            } else {
                binding.imgDislike.setImageResource(R.drawable.dislike)
            }
        })

        viewModel.errorMessages.observe(this, Observer { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearErrors()
            }
        })
    }

    private fun setupClickListeners(newsId: String) {
        binding.imgLike.setOnClickListener {
            viewModel.toggleLikeDislike(newsId, isLike = true)
        }

        binding.imgDislike.setOnClickListener {
            viewModel.toggleLikeDislike(newsId, isLike = false)
        }
    }

    private fun updateUI(news: News) {
        binding.tvNewsName.text = news.title
        binding.tvNewsDate.text = news.date
        binding.tvNewsDescription.text = news.description

        // Обновляем изображение новости
        if (news.imageUrl.isNotEmpty()) {
            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(news.imageUrl)
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(this)
                    .load(uri)
                    .into(binding.imageView5)
            }.addOnFailureListener { e ->
                Log.e("NewsDetailActivity", "Error loading image for news: ${news.title}", e)
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
            }
        }

        // Добавляем теги
        addTags(news.tags)
    }

    private fun addTags(tags: List<String>) {
        val tagsLayout = findViewById<LinearLayout>(R.id.tagsLayout)
        tagsLayout.removeAllViews() // Очищаем предыдущие теги
        val layoutInflater = LayoutInflater.from(this)
        for (tag in tags) {
            val tagView = layoutInflater.inflate(R.layout.tag_item, tagsLayout, false) as TextView
            tagView.text = tag
            tagsLayout.addView(tagView)
        }
    }
}