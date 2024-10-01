package com.example.libsibsiu.ui.video

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.libsibsiu.R
import com.example.libsibsiu.databinding.ActivityVideoDetailBinding
import com.example.libsibsiu.models.Video
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView


class VideoDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVideoDetailBinding
    private val viewModel: VideoDetailViewModel by viewModels()
    private var currentPlaybackPosition: Float = 0f
    private lateinit var youTubePlayerView: YouTubePlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoDetailBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        val videoId = intent.getStringExtra("VIDEO_ID") ?: run {
            Toast.makeText(this, "Video ID is missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val categoryId = intent.getStringExtra("CATEGORY_ID") ?: run {
            Toast.makeText(this, "Category ID is missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        savedInstanceState?.let {
            currentPlaybackPosition = it.getFloat("playback_position")
        }

        youTubePlayerView = findViewById(R.id.youtube_player_view)
        lifecycle.addObserver(youTubePlayerView)

        viewModel.loadVideoDetails(categoryId, videoId)
        viewModel.getLikeDislikeCounts(categoryId, videoId)
        viewModel.getUserLikeDislikeStatus(categoryId, videoId)

        setupObservers()
        setupClickListeners(categoryId, videoId)

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
        viewModel.videoDetails.observe(this, Observer { video ->
            video?.let {
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

    private fun setupClickListeners(categoryId: String, videoId: String) {
        binding.imgLike.setOnClickListener {
            viewModel.toggleLikeDislike(categoryId, videoId, isLike = true)
        }

        binding.imgDislike.setOnClickListener {
            viewModel.toggleLikeDislike(categoryId, videoId, isLike = false)
        }
    }

    private fun updateUI(video: Video) {
        binding.tvVideoName.text = video.title
        binding.tvVideoDate.text = video.date
        binding.tvVideoDescription.text = video.description

        if (video.videoUrl.isNotEmpty()) {
            initializePlayer(video.videoUrl)
        } else {
            Toast.makeText(this, "Video URL is empty", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializePlayer(videoUrl: String) {
        val iFramePlayerOptions = IFramePlayerOptions.Builder()
            .controls(1)
            .fullscreen(1)
            .build()

        youTubePlayerView.initialize(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                val videoId = extractVideoIdFromUrl(videoUrl)
                youTubePlayer.loadVideo(videoId, currentPlaybackPosition)

                youTubePlayer.addListener(object : AbstractYouTubePlayerListener() {
                    override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                        currentPlaybackPosition = second
                    }
                })
            }
        }, true, iFramePlayerOptions)

        youTubePlayerView.addFullscreenListener(object : FullscreenListener {
            override fun onEnterFullscreen(fullscreenView: View, exitFullscreen: Function0<Unit>) {
                setContentView(fullscreenView)
            }

            override fun onExitFullscreen() {
                setContentView(binding.root)
            }
        })
    }

    private fun extractVideoIdFromUrl(url: String): String {
        return url.split("v=")[1].split("&")[0]
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putFloat("playback_position", currentPlaybackPosition)
    }

    override fun onDestroy() {
        super.onDestroy()
        youTubePlayerView.release()
    }
}