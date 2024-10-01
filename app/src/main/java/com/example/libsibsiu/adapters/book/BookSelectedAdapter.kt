package com.example.libsibsiu.adapters.book

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.libsibsiu.ui.catalog.BookDetailActivity
import com.example.libsibsiu.R
import com.example.libsibsiu.databinding.ItemBookSelectedBinding
import com.example.libsibsiu.models.BookSelected

class BookSelectedAdapter(
    private var books: MutableList<BookSelected>,
    private val toggleFavorite: (BookSelected, Int) -> Unit
) : RecyclerView.Adapter<BookSelectedAdapter.BookViewHolder>() {
    inner class BookViewHolder(private val binding: ItemBookSelectedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(book: BookSelected, position: Int) {
            binding.textViewTitle.text = book.title
            Glide.with(binding.imageViewCover.context)
                .load(book.coverUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .fallback(R.drawable.fallback_image)
                .transform(RoundedCorners(65))
                .into(binding.imageViewCover)

            updateFavoriteIcon(book.isFavorite)

            binding.favoriteImageView.setOnClickListener {
                book.isFavorite = !book.isFavorite
                updateFavoriteIcon(book.isFavorite)
                toggleFavorite(book, position)  // Передаем объект и его позицию
            }

            binding.root.setOnClickListener {
                val context = it.context
                val intent = Intent(context, BookDetailActivity::class.java).apply {
                    putExtra("BOOK_ID", book.id)  // Проверяем, что book.id не пуст
                }
                context.startActivity(intent)
            }

        }


        private fun updateFavoriteIcon(isFavorite: Boolean) {
            if (isFavorite) {
                binding.favoriteImageView.setImageResource(R.drawable.heart_red)
            } else {
                binding.favoriteImageView.setImageResource(R.drawable.heart)
            }
        }

    }

    fun updateBooks(newBooks: List<BookSelected>) {
        books.clear()
        books.addAll(newBooks)
        notifyDataSetChanged()  // Уведомляем адаптер об изменении данных
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookSelectedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(books[position], position)
    }

    override fun getItemCount(): Int = books.size
}