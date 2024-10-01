package com.example.libsibsiu.adapters.book

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.libsibsiu.R
import com.example.libsibsiu.databinding.ItemBookStatusBinding
import com.example.libsibsiu.models.BookStatus

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class BookStatusAdapter(
    private var books: List<BookStatus>,
    private val onBookClick: (BookStatus) -> Unit,
    private val onPickUpBook: (BookStatus) -> Unit,
    private val onReturnBook: (BookStatus) -> Unit
) : RecyclerView.Adapter<BookStatusAdapter.BookStatusViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookStatusViewHolder {
        val binding = ItemBookStatusBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookStatusViewHolder(binding, onBookClick, onPickUpBook, onReturnBook)
    }

    override fun onBindViewHolder(holder: BookStatusViewHolder, position: Int) {
        holder.bind(books[position])
    }

    override fun getItemCount() = books.size

    fun updateBooks(newBooks: List<BookStatus>) {
        books = newBooks
        notifyDataSetChanged()
    }

    class BookStatusViewHolder(
        private val binding: ItemBookStatusBinding,
        private val onBookClick: (BookStatus) -> Unit,
        private val onPickUpBook: (BookStatus) -> Unit,
        private val onReturnBook: (BookStatus) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(book: BookStatus) {
            itemView.setOnClickListener {
                onBookClick(book)
            }

            val dateFormat = SimpleDateFormat("dd.MM.yyyy / HH:mm", Locale.getDefault())
            binding.tvTitle.text = createStyledText("Название: ", book.title)
            binding.tvStatus.text = createStyledText("Статус: ", book.status)
            Glide.with(itemView.context)
                .load(book.coverUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .fallback(R.drawable.fallback_image)
                .transform(RoundedCorners(25))
                .into(binding.coverImageView)

            binding.btnPickUpBook.setOnClickListener { onPickUpBook(book) }
            binding.btnReturnBook.setOnClickListener { onReturnBook(book) }

            when (book.status) {
                "Выдана" -> {
                    binding.tvQuantity.text = createStyledText("Количество взятых книг: ", book.quantity.toString())
                    binding.tvCheckoutDate.text = createStyledText("Дата выдачи: ", dateFormat.format(book.checkoutDate))
                    binding.tvDueDate.text = createStyledText("Дата сдачи: ", dateFormat.format(book.dueDate))
                    binding.tvRemainingTime.text = createStyledText("Оставшееся время: ", calculateRemainingTime(book.dueDate))
                    binding.btnPickUpBook.visibility = View.GONE
                    binding.btnReturnBook.visibility = View.VISIBLE
                }
                "В ожидании выдачи" -> {
                    binding.tvQuantity.text = createStyledText("Количество забронированных книг: ", book.quantity.toString())
                    binding.tvCheckoutDate.text = createStyledText("Дата бронирования: ", dateFormat.format(book.checkoutDate))
                    binding.tvDueDate.text = createStyledText("Дата окончания: ", dateFormat.format(book.dueDate))
                    binding.tvRemainingTime.text = createStyledText("Оставшееся время до выдачи: ", calculateRemainingTime(book.dueDate))
                }
                "Просрочена" -> {
                    binding.tvQuantity.text = createStyledText("Количество просроченных книг: ", book.quantity.toString())
                    binding.tvCheckoutDate.text = createStyledText("Дата выдачи: ", dateFormat.format(book.checkoutDate))
                    binding.tvDueDate.text = createStyledText("Дата сдачи: ", dateFormat.format(book.dueDate))
                    binding.tvRemainingTime.text = createStyledText("Просрочено на: ", calculateOverdueTime(book.dueDate))
                    binding.btnPickUpBook.visibility = View.GONE
                    binding.btnReturnBook.visibility = View.VISIBLE
                }
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
    }



    companion object {
        fun calculateRemainingTime(dueDate: Date): String {
            val now = Date()
            val remainingTime = dueDate.time - now.time
            if (remainingTime > 0) {
                val days = TimeUnit.MILLISECONDS.toDays(remainingTime)
                val hours = TimeUnit.MILLISECONDS.toHours(remainingTime) % 24
                return "$days дней и $hours часов"
            } else {
                return "Время истекло"
            }
        }

        fun calculateOverdueTime(dueDate: Date): String {
            val now = Date()
            val overdueTime = now.time - dueDate.time
            if (overdueTime > 0) {
                val days = TimeUnit.MILLISECONDS.toDays(overdueTime)
                val hours = TimeUnit.MILLISECONDS.toHours(overdueTime) % 24
                return "$days дней и $hours часов"
            } else {
                return "Просрочено"
            }
        }
    }
}

