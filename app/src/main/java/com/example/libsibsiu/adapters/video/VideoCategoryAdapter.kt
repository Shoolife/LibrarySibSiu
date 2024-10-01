package com.example.libsibsiu.adapters.video

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.libsibsiu.ui.video.VideoListActivity
import com.example.libsibsiu.databinding.ItemVideoCategoryBinding
import com.example.libsibsiu.models.VideoCategory

class VideoCategoryAdapter(private var categories: List<VideoCategory>) : RecyclerView.Adapter<VideoCategoryAdapter.CategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemVideoCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    fun updateData(newCategories: List<VideoCategory>) {
        val diffCallback = CategoryDiffCallback(categories, newCategories)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        categories = newCategories
        diffResult.dispatchUpdatesTo(this)
    }

    class CategoryViewHolder(private val binding: ItemVideoCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: VideoCategory) {
            binding.textViewCategoryName.text = category.title
            Glide.with(binding.imageViewVideo.context)
                .load(category.images)
                .transform(RoundedCorners(40))
                .into(binding.imageViewVideo)

            itemView.setOnClickListener {
                val context = binding.root.context
                val intent = Intent(context, VideoListActivity::class.java).apply {
                    putExtra("category_id", category.id)
                    putExtra("category_name", category.title)
                }
                context.startActivity(intent)
            }
        }
    }
}

class CategoryDiffCallback(private val oldList: List<VideoCategory>, private val newList: List<VideoCategory>) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].title == newList[newItemPosition].title &&
                oldList[oldItemPosition].images == newList[newItemPosition].images
    }
}