package com.example.libsibsiu.adapters.news

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.libsibsiu.R
import com.example.libsibsiu.databinding.ItemNewsListBinding
import com.example.libsibsiu.models.News
import com.google.firebase.storage.FirebaseStorage

class NewsAdapter(var newsList: List<News>) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    private var onItemClickListener: ((String) -> Unit)? = null

    fun setOnItemClickListener(listener: (String) -> Unit) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemNewsListBinding.inflate(inflater, parent, false)
        return NewsViewHolder(binding, onItemClickListener)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(newsList[position])
    }

    override fun getItemCount(): Int = newsList.size

    fun updateNews(newNewsList: List<News>) {
        newsList = newNewsList
        notifyDataSetChanged()
    }

    class NewsViewHolder(private val binding: ItemNewsListBinding, private val onItemClickListener: ((String) -> Unit)?) : RecyclerView.ViewHolder(binding.root) {
        fun bind(news: News) {
            binding.textViewTitle.text = news.title
            binding.textViewDate.text = news.date

            // Обновление изображения
            if (news.imageUrl.isNotEmpty()) {
                val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(news.imageUrl)
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    Glide.with(binding.imageViewCover.context)
                        .load(uri)
                        .transform(RoundedCorners(25))
                        .into(binding.imageViewCover)
                }
            }

            binding.tvCollLike.text = news.likeCount.toString()
            binding.tvCollDislike.text = news.dislikeCount.toString()

            itemView.setOnClickListener {
                onItemClickListener?.invoke(news.id)
            }

            // Добавление тегов
            addTags(news.tags)
        }

        private fun addTags(tags: List<String>) {
            binding.tagsLayout.removeAllViews() // Очищаем предыдущие теги
            val layoutInflater = LayoutInflater.from(binding.tagsLayout.context)
            for (tag in tags) {
                val tagView = layoutInflater.inflate(R.layout.tag_item_list, binding.tagsLayout, false) as TextView
                tagView.text = tag
                binding.tagsLayout.addView(tagView)
            }
        }
    }
}