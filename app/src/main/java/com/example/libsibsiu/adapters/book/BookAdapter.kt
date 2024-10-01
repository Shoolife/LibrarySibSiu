package com.example.libsibsiu.adapters.book

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.libsibsiu.R
import com.example.libsibsiu.databinding.ItemBookBinding
import com.example.libsibsiu.models.Book
import android.content.Context
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class BookAdapter(private val context: Context, private var books: MutableList<Book>) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {
    private var onItemClickListener: ((Book) -> Unit)? = null

    fun setOnItemClickListener(listener: (Book) -> Unit) {
        onItemClickListener = listener
    }

    class BookViewHolder(private val binding: ItemBookBinding, private val context: Context, private val clickListener: ((Book) -> Unit)?) : RecyclerView.ViewHolder(binding.root) {
        fun bind(book: Book) {
            binding.titleTextView.text = createStyledText(context.getString(R.string.title_prefix), book.title)
            binding.authorTextView.text = createStyledText(context.getString(R.string.author_prefix), book.author)
            binding.publishYearTextView.text = createStyledText(context.getString(R.string.publish_year_prefix), book.publishYear)
            binding.isbnTextView.text = createStyledText(context.getString(R.string.isbn_prefix), book.isbn)
            binding.summaryTextView.text = createStyledText(context.getString(R.string.summary_prefix), book.summary)

            Glide.with(context)
                .load(book.coverUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .fallback(R.drawable.fallback_image)
                .centerCrop()
                .transform(RoundedCorners(25))
                .into(binding.coverImageView)

            updateFavoriteIcon(book.isFavorite)
            binding.favoriteImageView.setOnClickListener {
                book.isFavorite = !book.isFavorite
                updateFavoriteIcon(book.isFavorite)
                saveFavoriteState(book)
                it.isClickable = true  // Make sure this view consumes the click event
                it.isFocusable = true  // Make sure this view consumes the focus event
            }

            binding.root.setOnClickListener { clickListener?.invoke(book) }
        }

        private fun updateFavoriteIcon(isFavorite: Boolean) {
            if (isFavorite) {
                binding.favoriteImageView.setImageResource(R.drawable.heart_red)
            } else {
                binding.favoriteImageView.setImageResource(R.drawable.heart)
            }
        }

        private fun createStyledText(prefix: String, value: String): Spannable {
            val styledText = SpannableString("$prefix $value")
            styledText.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                prefix.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            styledText.setSpan(
                StyleSpan(Typeface.NORMAL),
                prefix.length,
                styledText.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            return styledText
        }

        private fun saveFavoriteState(book: Book) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val db = FirebaseFirestore.getInstance()
            val userDocRef = db.collection("users").document(userId)
            val favoritesRef = userDocRef.collection("favorites")

            if (book.isFavorite) {
                val favoriteData = hashMapOf(
                    "bookId" to book.id,
                    "timestamp" to FieldValue.serverTimestamp()
                )
                favoritesRef.document(book.id).set(favoriteData).addOnFailureListener { e ->
                    Toast.makeText(context, "Ошибка при добавлении в избранное: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            } else {
                favoritesRef.document(book.id).delete().addOnFailureListener { e ->
                    Toast.makeText(context, "Ошибка при удалении из избранного: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookViewHolder(binding, context, onItemClickListener)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = books[position]
        holder.bind(book)
    }

    override fun getItemCount() = books.size

    fun updateBooks(newBooks: List<Book>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = books.size

            override fun getNewListSize() = newBooks.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return books[oldItemPosition].id == newBooks[newItemPosition].id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                // Сравните содержимое книги по всем параметрам, которые могут отображаться или влиять на логику отображения
                return books[oldItemPosition].title == newBooks[newItemPosition].title &&
                        books[oldItemPosition].author == newBooks[newItemPosition].author &&
                        books[oldItemPosition].isbn == newBooks[newItemPosition].isbn &&
                        books[oldItemPosition].publishYear == newBooks[newItemPosition].publishYear &&
                        books[oldItemPosition].summary == newBooks[newItemPosition].summary &&
                        books[oldItemPosition].isFavorite == newBooks[newItemPosition].isFavorite
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        books.clear()
        books.addAll(newBooks)
        diffResult.dispatchUpdatesTo(this)
    }
}
