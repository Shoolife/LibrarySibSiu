package com.example.libsibsiu.adapters.video

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.libsibsiu.databinding.ItemVideoBinding
import com.example.libsibsiu.models.Video

class HorizontalLatestVideoAdapter(
    var videos: List<Video>,
    private val onVideoClick: (Video) -> Unit
) : RecyclerView.Adapter<HorizontalLatestVideoAdapter.VideoViewHolder>() {

    companion object {
        private const val TAG = "HorizontalLatestVideoAdapter"
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

    fun updateData(newVideos: List<Video>) {
        Log.d(TAG, "updateData: newVideos size = ${newVideos.size}")
        videos = newVideos
        notifyDataSetChanged()
    }
}