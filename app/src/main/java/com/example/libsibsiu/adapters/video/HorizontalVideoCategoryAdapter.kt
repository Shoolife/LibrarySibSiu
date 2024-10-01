package com.example.libsibsiu.adapters.video

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.libsibsiu.models.VideoCategory
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import android.graphics.drawable.Drawable
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.example.libsibsiu.databinding.ItemVerticalListBinding

class HorizontalVideoCategoryAdapter(
    private val items: List<VideoCategory>,
    private val onCategoryClick: (String, String) -> Unit
) : RecyclerView.Adapter<HorizontalVideoCategoryAdapter.ViewHolder>() {

    companion object {
        private const val TAG = "HorizontalVideoCategoryAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.d(TAG, "onCreateViewHolder")
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemVerticalListBinding.inflate(inflater, parent, false)
        return ViewHolder(binding, onCategoryClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder: Position = $position, Category = ${items[position].title}")
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        val count = items.size
        Log.d(TAG, "getItemCount: Total items = $count")
        return count
    }

    class ViewHolder(
        private val binding: ItemVerticalListBinding,
        private val onCategoryClick: (String, String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: VideoCategory) {
            Log.d(TAG, "bind: Binding category = ${category.title}")
            binding.textView.text = category.title
            Glide.with(binding.imageView.context)
                .load(category.images) // Загрузка изображения категории
                .transform(RoundedCorners(32))
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e(TAG, "Image load failed for URL: ${category.images}", e)
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d(TAG, "Image loaded successfully for URL: ${category.images}")
                        return false
                    }
                })
                .into(binding.imageView)

            itemView.setOnClickListener {
                Log.d(TAG, "Item clicked: ${category.id}")
                onCategoryClick(category.id, category.title) // Передача id и название категории в callback
            }
        }
    }
}