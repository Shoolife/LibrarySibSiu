package com.example.libsibsiu.ui.catalog

import android.app.DownloadManager
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Environment
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.libsibsiu.databinding.ActivityBookDetailBinding
import com.example.libsibsiu.models.Book
import com.example.libsibsiu.utils.CustomTypefaceSpan
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import android.content.Context
import android.widget.Button
import android.widget.TextView
import android.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.libsibsiu.R
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.Calendar


class BookDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBookDetailBinding
    private val db = FirebaseFirestore.getInstance()
    private var isFavorite: Boolean = false
    private var userRatingChanged = false
    private var baseBookTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookDetailBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        val bookId = intent.getStringExtra("BOOK_ID") ?: ""
        if (bookId.isBlank()) {
            Toast.makeText(this, "Ошибка: идентификатор книги отсутствует или пуст", Toast.LENGTH_LONG).show()
            finish()
        } else {
            loadBookDetails(bookId)
            loadUserRating(bookId)
            loadFavoriteState(bookId)
            checkAndUpdateButton(bookId)
        }

        setupFavoriteButton(bookId)

        binding.backArrow.setOnClickListener {
            finish()
        }

        binding.myRatingBar.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (fromUser) {
                userRatingChanged = true
                setBookRating(bookId, rating)
            }
        }

        binding.btnRead.setOnClickListener {
            bookId?.let { it ->
                val intent = Intent(this, ReadBookActivity::class.java).apply {
                    putExtra("BOOK_ID", it)
                }
                startActivity(intent)
            }
        }

        binding.btnDownload.setOnClickListener {
            val bookId = intent.getStringExtra("BOOK_ID")
            if (bookId != null && baseBookTitle != null) {
                downloadPdf(bookId, baseBookTitle!!)
            } else {
                Toast.makeText(this, "Ошибка: идентификатор или название книги отсутствуют", Toast.LENGTH_LONG).show()
            }
        }

        binding.btnArmor.setOnClickListener {
            val bookId = intent.getStringExtra("BOOK_ID") ?: ""
            if (bookId.isNotBlank()) {
                showBookingDialog(bookId)
            } else {
                Toast.makeText(this, "Ошибка: идентификатор книги отсутствует", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadFavoriteState(bookId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("users").document(userId).collection("favorites").document(bookId).get()
            .addOnSuccessListener { document ->
                isFavorite = document.exists()
                updateFavoriteIcon()
            }
    }

    private fun setupFavoriteButton(bookId: String) {
        binding.favoriteImageView.setOnClickListener {
            isFavorite = !isFavorite
            updateFavoriteIcon()
            saveFavoriteState(bookId, isFavorite)
        }
    }

    private fun updateFavoriteIcon() {
        if (isFavorite) {
            binding.favoriteImageView.setImageResource(R.drawable.heart_red)
        } else {
            binding.favoriteImageView.setImageResource(R.drawable.heart)
        }
    }

    private fun saveFavoriteState(bookId: String, isFavorite: Boolean) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userDocRef = db.collection("users").document(userId)
        val favoritesRef = userDocRef.collection("favorites")

        if (isFavorite) {
            val favoriteData = hashMapOf(
                "bookId" to bookId,
                "timestamp" to FieldValue.serverTimestamp()
            )
            favoritesRef.document(bookId).set(favoriteData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Книга добавлена в избранное", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Ошибка при добавлении в избранное: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        } else {
            favoritesRef.document(bookId).delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Книга удалена из избранного", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Ошибка при удалении из избранного: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun checkAndUpdateButton(bookId: String) {
        db.collection("books").document(bookId).get().addOnSuccessListener { documentSnapshot ->
            val availableCopies = documentSnapshot.getLong("availableCopies") ?: 0
            if (availableCopies > 0) {
                binding.btnArmor.isEnabled = true
                binding.btnArmor.backgroundTintList = ContextCompat.getColorStateList(this,
                    R.color.armorColor
                )
            } else {
                binding.btnArmor.isEnabled = false
                binding.btnArmor.backgroundTintList = ContextCompat.getColorStateList(this,
                    R.color.disabledColor
                ) // Установите подходящий цвет для неактивной кнопки
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Ошибка при получении данных: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showBookingDialog(bookId: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_book_booking, null)
        val editTextNumber = dialogView.findViewById<TextView>(R.id.editTextNumber)
        val buttonIncrement = dialogView.findViewById<Button>(R.id.buttonIncrement)
        val buttonDecrement = dialogView.findViewById<Button>(R.id.buttonDecrement)
        val textAvailableBooks = dialogView.findViewById<TextView>(R.id.textAvailableBooks)

        val dialog = AlertDialog.Builder(this, R.style.RoundedAlertDialog)
            .setView(dialogView)
            .setPositiveButton("Забронировать", null) // initially null
            .setNegativeButton("Отмена", null) // initially null
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val quantity = editTextNumber.text.toString().toInt()
                bookBooks(bookId, quantity)
                dialog.dismiss()
            }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()

        db.collection("books").document(bookId).get().addOnSuccessListener { documentSnapshot ->
            val available = documentSnapshot.getLong("availableCopies") ?: 0
            textAvailableBooks.text = "Доступно книг: $available"
            editTextNumber.text = "1"
            buttonIncrement.setOnClickListener {
                val currentQuantity = editTextNumber.text.toString().toInt()
                if (currentQuantity < available) {
                    editTextNumber.text = (currentQuantity + 1).toString()
                }
            }
            buttonDecrement.setOnClickListener {
                val currentQuantity = editTextNumber.text.toString().toInt()
                if (currentQuantity > 1) {
                    editTextNumber.text = (currentQuantity - 1).toString()
                }
            }
        }

        // Set button text colors
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this,
            R.color.textColor
        ))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this,
            R.color.textColor
        ))
    }

    private fun bookBooks(bookId: String, quantity: Int) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userId = currentUser.uid
        val bookRef = db.collection("books").document(bookId)
        val pendingRef = db.collection("users").document(userId).collection("pending_books").document()

        val now = Calendar.getInstance()
        val checkoutDate = now.time
        now.add(Calendar.DAY_OF_MONTH, 3)  // Добавляем 3 дня как срок для забора книги
        val dueDate = now.time

        db.runTransaction { transaction ->
            val bookSnapshot = transaction.get(bookRef)
            val availableCopies = bookSnapshot.getLong("availableCopies") ?: 0
            if (availableCopies >= quantity) {
                transaction.update(bookRef, "availableCopies", availableCopies - quantity)
                val bookingData = hashMapOf(
                    "bookId" to bookId,
                    "checkoutDate" to checkoutDate,
                    "dueDate" to dueDate,
                    "quantity" to quantity,
                    "status" to "В ожидании выдачи"
                )
                transaction.set(pendingRef, bookingData)
            } else {
                throw FirebaseFirestoreException("Недостаточно копий", FirebaseFirestoreException.Code.ABORTED)
            }
        }.addOnSuccessListener {
            Toast.makeText(this, "Книга успешно забронирована и ожидает выдачи", Toast.LENGTH_SHORT).show()
            checkAndUpdateButton(bookId) // Обновление UI кнопки бронирования
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Ошибка при бронировании книги: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }


    private fun downloadPdf(bookId: String, bookTitle: String) {
        val storageReference = FirebaseStorage.getInstance().reference.child("pdfs/$bookId.pdf")
        storageReference.downloadUrl.addOnSuccessListener { uri ->
            val request = DownloadManager.Request(uri)
                .setTitle(bookTitle)
                .setDescription("Downloading $bookTitle")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "$bookTitle.pdf")

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            Toast.makeText(this, "Загрузка началась, отслеживать во всплывающем окне", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Ошибка получения файла: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadBookDetails(bookId: String) {
        db.collection("books").document(bookId).get()
            .addOnSuccessListener { documentSnapshot ->
                val book = documentSnapshot.toObject(Book::class.java)
                book?.let {
                    baseBookTitle = it.title  // Сохраняем базовое название книги
                    updateUI(it)
                    calculateAverageRating(bookId)  // Вызов расчета среднего рейтинга
                } ?: run {
                    Toast.makeText(this, "Книга не найдена", Toast.LENGTH_LONG).show()
                    finish()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Ошибка при загрузке деталей книги", Toast.LENGTH_LONG).show()
            }
    }

    private fun loadUserRating(bookId: String) {
        FirebaseAuth.getInstance().currentUser?.let { user ->
            db.collection("books").document(bookId).collection("ratings").document(user.uid).get()
                .addOnSuccessListener { document ->
                    val rating = document.getDouble("rating")?.toFloat() ?: 0f
                    binding.myRatingBar.rating = rating
                    userRatingChanged = false  // Сброс флага после загрузки рейтинга
                }
        }
    }

    private fun setBookRating(bookId: String, rating: Float) {
        if (userRatingChanged) {  // Отправляем в базу только если было изменение
            FirebaseAuth.getInstance().currentUser?.let { user ->
                val userRating = hashMapOf("rating" to rating)
                db.collection("books").document(bookId).
                        collection("ratings").document(user.uid)
                    .set(userRating)
                    .addOnSuccessListener {
                        // Toast.makeText(this, "Ваша оценка сохранена", Toast.LENGTH_SHORT).show()
                        calculateAverageRating(bookId)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this,
                            "Ошибка сохранения оценки: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun calculateAverageRating(bookId: String) {
        db.collection("books").document(bookId).collection("ratings")
            .get()
            .addOnSuccessListener { documents ->
                val ratings = documents.mapNotNull { it.getDouble("rating") }
                val averageRating = if (ratings.isNotEmpty()) ratings.average() else 0.0  // Обработка случая с пустым списком оценок
                val roundedAvgRating = String.format("%.1f", averageRating)

                val titleAndRatingSpannable = SpannableString("$baseBookTitle $roundedAvgRating")
                val robotoMedium = Typeface.create(ResourcesCompat.getFont(this,
                    R.font.roboto_medium
                ), Typeface.NORMAL)
                val robotoLight = Typeface.create(ResourcesCompat.getFont(this, R.font.roboto_light), Typeface.NORMAL)

                // Применение шрифта Roboto Medium к названию книги
                titleAndRatingSpannable.setSpan(CustomTypefaceSpan("", robotoMedium), 0, baseBookTitle?.length ?: 0, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                // Применение шрифта Roboto Light и уменьшенного размера к среднему рейтингу
                titleAndRatingSpannable.setSpan(CustomTypefaceSpan("", robotoLight), baseBookTitle?.length ?: 0, titleAndRatingSpannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                titleAndRatingSpannable.setSpan(RelativeSizeSpan(0.85f), baseBookTitle?.length ?: 0, titleAndRatingSpannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                binding.textNameBook.text = titleAndRatingSpannable
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка при расчете средней оценки: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)

    private fun updateUI(book: Book) {
        with(binding) {
            textNameBook.text = baseBookTitle  // Используем базовое название для обновления UI
            textNameAuthor.text = book.author
            textAboutTheBook.text = book.aboutTheBook.replace("\\n", "\n")
            textQuote.text = book.quote
            Glide.with(this@BookDetailActivity)
                .load(book.coverUrl)
                .placeholder(R.drawable.placeholder_image)
                .transform(RoundedCorners(25))
                .into(imageView3)
        }
    }
}




