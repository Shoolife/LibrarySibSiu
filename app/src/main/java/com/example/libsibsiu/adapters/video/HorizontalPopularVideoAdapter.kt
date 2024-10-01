package com.example.libsibsiu.adapters.video

import com.example.libsibsiu.models.Video
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.libsibsiu.databinding.ItemVideoBinding
import android.util.Log

class HorizontalPopularVideoAdapter(
    private var videos: List<Video>,
    private val onVideoClick: (Video) -> Unit
) : RecyclerView.Adapter<HorizontalPopularVideoAdapter.VideoViewHolder>() {

    companion object {
        private const val TAG = "HorizontalPopularVideoAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        Log.d(TAG, "onCreateViewHolder")
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemVideoBinding.inflate(inflater, parent, false)
        return VideoViewHolder(binding, onVideoClick)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder: Position = $position, Video = ${videos[position].title}")
        holder.bind(videos[position])
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount: Total videos = ${videos.size}")
        return videos.size
    }

    class VideoViewHolder(
        private val binding: ItemVideoBinding,
        private val onVideoClick: (Video) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(video: Video) {
            binding.textViewVideoName.text = video.title
            binding.textViewVideoDate.text = video.date
            Glide.with(binding.imageViewVideo.context)
                .load(video.imageUrl)
                .transform(RoundedCorners(40))
                .into(binding.imageViewVideo)

            itemView.setOnClickListener {
                Log.d(TAG, "Video clicked: ${video.id}")
                onVideoClick(video)
            }
        }
    }

    // Метод для обновления списка видео в адаптере
    fun updateData(newVideos: List<Video>) {
        Log.d(TAG, "updateData: newVideos size = ${newVideos.size}")
        videos = newVideos
        notifyDataSetChanged()
    }
}