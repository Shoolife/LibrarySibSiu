package com.example.libsibsiu.adapters.video

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.libsibsiu.databinding.ItemHorizontalListCategoryBinding
import com.example.libsibsiu.databinding.ItemHorizontalListLatestBinding
import com.example.libsibsiu.databinding.ItemHorizontalListPopularBinding
import com.example.libsibsiu.models.Video
import com.example.libsibsiu.models.VideoCategory

class VerticalAdapter(
    private var collections: List<List<VideoCategory>>,
    private var popularVideos: MutableList<Video>,
    private var latestVideos: MutableList<Video>,
    private val onCategoryClick: (String, String) -> Unit,
    private val onAllCategoriesClick: () -> Unit,
    private val onVideoClick: (Video) -> Unit,
    private val onAllPopularClick: () -> Unit,
    private val onAllLatestClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_CATEGORY = 0
        private const val TYPE_POPULAR = 1
        private const val TYPE_LATEST = 2
        private const val TAG = "VerticalAdapter"
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            collections.size -> TYPE_POPULAR
            collections.size + 1 -> TYPE_LATEST
            else -> TYPE_CATEGORY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        Log.d(TAG, "onCreateViewHolder: viewType = $viewType")
        return when (viewType) {
            TYPE_CATEGORY -> CategoryViewHolder(
                ItemHorizontalListCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                onCategoryClick,
                onAllCategoriesClick
            )
            TYPE_POPULAR -> PopularVideosViewHolder(
                ItemHorizontalListPopularBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                parent.context,
                onVideoClick,
                onAllPopularClick
            )
            TYPE_LATEST -> LatestVideosViewHolder(
                ItemHorizontalListLatestBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                parent.context,
                onVideoClick,
                onAllLatestClick
            )
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder: position = $position")
        when (holder) {
            is CategoryViewHolder -> holder.setupRecyclerView(collections[position])
            is PopularVideosViewHolder -> holder.bind(popularVideos.take(5)) // Ограничиваем количество видео до 5
            is LatestVideosViewHolder -> holder.bind(latestVideos.take(5)) // Ограничиваем количество видео до 5
        }
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount: ${collections.size + 2}")
        return collections.size + 2
    }

    fun updatePopularVideos(newVideos: MutableList<Video>) {
        Log.d(TAG, "updatePopularVideos: newVideos size = ${newVideos.size}")
        popularVideos = newVideos
        notifyItemChanged(collections.size) // Обновление последнего элемента
    }

    fun updateLatestVideos(newVideos: MutableList<Video>) {
        Log.d(TAG, "updateLatestVideos: newVideos size = ${newVideos.size}")
        latestVideos = newVideos
        notifyItemChanged(collections.size + 1) // Обновление элемента последних видео
    }

    class CategoryViewHolder(
        val binding: ItemHorizontalListCategoryBinding,
        val onCategoryClick: (String, String) -> Unit,
        val onAllCategoriesClick: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun setupRecyclerView(categories: List<VideoCategory>) {
            Log.d(TAG, "setupRecyclerView: categories size = ${categories.size}")
            binding.horizontalRecyclerView.layoutManager = LinearLayoutManager(binding.horizontalRecyclerView.context, LinearLayoutManager.HORIZONTAL, false)
            binding.horizontalRecyclerView.adapter = HorizontalVideoCategoryAdapter(categories, onCategoryClick)
            binding.textViewAll.setOnClickListener {
                Log.d(TAG, "All categories clicked")
                onAllCategoriesClick()
            }
        }
    }

    class PopularVideosViewHolder(
        val binding: ItemHorizontalListPopularBinding,
        private val context: Context,
        private val onVideoClick: (Video) -> Unit,
        private val onAllPopularClick: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(videos: List<Video>) {
            Log.d(TAG, "bind: videos size = ${videos.size}")
            if (binding.horizontalRecyclerView.adapter == null) {
                binding.horizontalRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                binding.horizontalRecyclerView.adapter = HorizontalPopularVideoAdapter(videos, onVideoClick)
            } else {
                (binding.horizontalRecyclerView.adapter as HorizontalPopularVideoAdapter).updateData(videos)
            }
            binding.textViewAll.setOnClickListener {
                onAllPopularClick()
            }
        }
    }

    class LatestVideosViewHolder(
        val binding: ItemHorizontalListLatestBinding,
        private val context: Context,
        private val onVideoClick: (Video) -> Unit,
        private val onAllLatestClick: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(videos: List<Video>) {
            Log.d(TAG, "bind: videos size = ${videos.size}")
            if (binding.horizontalRecyclerView.adapter == null) {
                binding.horizontalRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                binding.horizontalRecyclerView.adapter = HorizontalLatestVideoAdapter(videos, onVideoClick)
            } else {
                (binding.horizontalRecyclerView.adapter as HorizontalLatestVideoAdapter).updateData(videos)
            }
            binding.textViewAll.setOnClickListener {
                onAllLatestClick()
            }
        }
    }
}