package com.example.libsibsiu.adapters.video

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.libsibsiu.databinding.ItemVideoListBinding
import com.example.libsibsiu.models.Video

class VideoListAdapter(var videos: List<Video>) : RecyclerView.Adapter<VideoListAdapter.VideoViewHolder>() {

    private var onItemClickListener: ((String) -> Unit)? = null

    fun setOnItemClickListener(listener: (String) -> Unit) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemVideoListBinding.inflate(inflater, parent, false)
        return VideoViewHolder(binding, onItemClickListener)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(videos[position])
    }

    override fun getItemCount(): Int = videos.size

    fun updateData(newVideos: List<Video>) {
        videos = newVideos
        notifyDataSetChanged()
    }

    class VideoViewHolder(
        private val binding: ItemVideoListBinding,
        private val onItemClickListener: ((String) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(video: Video) {
            binding.textViewTitle.text = video.title
            binding.textViewDate.text = video.date
            Glide.with(binding.imageViewCover.context)
                .load(video.imageUrl)
                .transform(RoundedCorners(25))
                .into(binding.imageViewCover)

            binding.tvCollLike.text = video.likeCount.toString()
            binding.tvCollDislike.text = video.dislikeCount.toString()

            itemView.setOnClickListener {
                onItemClickListener?.invoke(video.id)
            }
        }
    }
}