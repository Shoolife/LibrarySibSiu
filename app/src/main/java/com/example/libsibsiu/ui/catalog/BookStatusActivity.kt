package com.example.libsibsiu.ui.catalog

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.libsibsiu.adapters.book.BookStatusAdapter
import com.example.libsibsiu.databinding.ActivityBookStatusBinding
import com.example.libsibsiu.models.Book
import com.example.libsibsiu.models.BookStatus
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Date

class BookStatusActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBookStatusBinding
    private lateinit var adapter: BookStatusAdapter
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        db = FirebaseFirestore.getInstance()

        adapter = BookStatusAdapter(emptyList(), this::onBookClicked, this::onPickUpBook, this::onReturnBook)
        binding.rvBooks.adapter = adapter
        binding.rvBooks.layoutManager = LinearLayoutManager(this)

        loadBookStatuses()

        binding.backArrow.setOnClickListener {
            finish()
        }

    }

    private fun onPickUpBook(book: BookStatus) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userDocRef = db.collection("users").document(userId)
        val pendingBooksRef = userDocRef.collection("pending_books")

        pendingBooksRef.whereEqualTo("bookId", book.bookId).get().addOnSuccessListener { documents ->
            if (documents.isEmpty) {
                Toast.makeText(this, "Книга не найдена в ожидании", Toast.LENGTH_LONG).show()
                return@addOnSuccessListener
            }

            val bookingDoc = documents.documents.first()
            val bookingId = bookingDoc.id
            val pendingRef = pendingBooksRef.document(bookingId)
            val issuedRef = userDocRef.collection("issued_books").document(bookingId)

            db.runTransaction { transaction ->
                val bookData = transaction.get(pendingRef).data ?: return@runTransaction
                transaction.delete(pendingRef)

                // Установка новых дат
                val currentDate = Calendar.getInstance().time
                val dueDate = Calendar.getInstance().apply {
                    add(Calendar.MONTH, 1)
                }.time

                bookData["status"] = "Выдана"
                bookData["checkoutDate"] = currentDate
                bookData["dueDate"] = dueDate

                transaction.set(issuedRef, bookData)
            }.addOnSuccessListener {
                loadBookStatuses()  // Обновление списка
                Toast.makeText(this, "Вы забрали книгу", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка при переносе книги: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Ошибка при поиске книги: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun onReturnBook(book: BookStatus) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userDocRef = db.collection("users").document(userId)
        val currentCollectionName = when (book.status) {
            "Выдана" -> "issued_books"
            "Просрочена" -> "overdue_books"
            else -> return
        }
        val booksRef = userDocRef.collection(currentCollectionName)

        booksRef.whereEqualTo("bookId", book.bookId).get().addOnSuccessListener { documents ->
            if (documents.isEmpty) {
                Toast.makeText(this, "Книга не найдена в текущей коллекции", Toast.LENGTH_LONG).show()
                return@addOnSuccessListener
            }

            val bookingDoc = documents.documents.first()
            val bookingId = bookingDoc.id
            val bookData = bookingDoc.data ?: return@addOnSuccessListener
            val quantity = (bookData["quantity"] as Long).toInt()

            val currentRef = booksRef.document(bookingId)
            val bookMainRef = db.collection("books").document(book.bookId)

            db.runTransaction { transaction ->
                val bookSnapshot = transaction.get(bookMainRef)
                val availableCopies = (bookSnapshot.getLong("availableCopies") ?: 0) + quantity

                transaction.delete(currentRef)
                transaction.update(bookMainRef, "availableCopies", availableCopies)
            }.addOnSuccessListener {
                loadBookStatuses()  // Обновление списка
                Toast.makeText(this, "Книга успешно возвращена", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка при возврате книги: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Ошибка при поиске книги: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun onBookClicked(bookStatus: BookStatus) {
        // Код для открытия новой активности с деталями книги
        val intent = Intent(this, BookDetailActivity::class.java).apply {
            putExtra("BOOK_ID", bookStatus.bookId)
        }
        startActivity(intent)
    }

    private fun loadBookStatuses() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val userDocRef = db.collection("users").document(userId)

        // Запросы к трем коллекциям
        val pendingBooks = userDocRef.collection("pending_books").get()
        val issuedBooks = userDocRef.collection("issued_books").get()
        val overdueBooks = userDocRef.collection("overdue_books").get()

        // Объединение результатов запросов
        Tasks.whenAll(pendingBooks, issuedBooks, overdueBooks).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val books = mutableListOf<BookStatus>()
                val allTasks = listOf(pendingBooks, issuedBooks, overdueBooks)
                allTasks.forEach { task ->
                    task.result?.forEach { document ->
                        val bookId = document.getString("bookId") ?: return@forEach
                        fetchBookInfo(bookId) { book, coverUrl ->
                            val status = when (task) {
                                pendingBooks -> "В ожидании выдачи"
                                issuedBooks -> "Выдана"
                                overdueBooks -> "Просрочена"
                                else -> "Неизвестный статус"
                            }
                            val bookStatus = BookStatus(
                                bookId = bookId,
                                title = book.title,
                                status = status,
                                quantity = document.getLong("quantity")?.toInt() ?: 0,
                                checkoutDate = document.getDate("checkoutDate") ?: Date(),
                                dueDate = document.getDate("dueDate") ?: Date(),
                                coverUrl = coverUrl
                            )
                            books.add(bookStatus)
                            adapter.updateBooks(books)
                        }
                    }
                }
                adapter.updateBooks(books) // Уведомляем адаптер о новом списке книг
            } else {
                Toast.makeText(this, "Ошибка загрузки данных: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun fetchBookInfo(bookId: String, callback: (Book, String) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("books").document(bookId).get().addOnSuccessListener { documentSnapshot ->
            val book = documentSnapshot.toObject(Book::class.java)
            book?.let {
                val coverUrl = documentSnapshot.getString("coverUrl") ?: ""  // Получение URL обложки
                callback(it, coverUrl)
            }
        }.addOnFailureListener { e ->
            Log.d("Firestore", "Error getting book details: ", e)
        }
    }
}