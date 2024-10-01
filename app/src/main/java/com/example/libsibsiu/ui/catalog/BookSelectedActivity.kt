package com.example.libsibsiu.ui.catalog

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.libsibsiu.adapters.book.BookSelectedAdapter
import com.example.libsibsiu.databinding.ActivityBookSelectedBinding
import com.example.libsibsiu.models.Book
import com.example.libsibsiu.models.BookSelected
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class BookSelectedActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBookSelectedBinding
    private val db = FirebaseFirestore.getInstance()
    private val books = mutableListOf<BookSelected>()
    private lateinit var adapter: BookSelectedAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookSelectedBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        setupRecyclerView()
        loadFavoriteBooks()

        binding.backArrow.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        val layoutManager = GridLayoutManager(this, 2)
        adapter = BookSelectedAdapter(books, this::toggleFavorite)
        binding.rvBooks.layoutManager = layoutManager
        binding.rvBooks.adapter = adapter
    }

    private fun loadFavoriteBooks() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userDocRef = db.collection("users").document(userId)
        val favoritesRef = userDocRef.collection("favorites")

        favoritesRef.get().addOnSuccessListener { snapshot ->
            val favoriteDetails = snapshot.documents.mapNotNull { doc ->
                val bookId = doc.getString("bookId")
                bookId?.let { Pair(bookId, doc.id) }  // Получаем пару bookId и favoriteId
            }
            if (favoriteDetails.isEmpty()) {
                //Toast.makeText(this, "У вас нет избранных книг", Toast.LENGTH_SHORT).show()
            } else {
                loadBooksDetails(favoriteDetails)
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this,
                "Ошибка при загрузке избранных книг: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadBooksDetails(favoriteDetails: List<Pair<String, String>>) {
        val tasks = favoriteDetails.map { (bookId, _) ->
            db.collection("books").document(bookId).get()
        }

        Tasks.whenAllSuccess<DocumentSnapshot>(tasks).addOnSuccessListener { documentSnapshots ->
            val booksList = documentSnapshots.mapNotNull { document ->
                val book = document.toObject(Book::class.java)
                if (book != null) {
                    val favoriteId = favoriteDetails.firstOrNull { it.first == document.id }?.second
                    BookSelected(document.id, book.title, book.coverUrl, true, favoriteId ?: "")
                } else {
                    null
                }
            }
            if (booksList.isEmpty()) {
                Toast.makeText(this, "Детали книг не загружены.", Toast.LENGTH_SHORT).show()
            } else {
                adapter.updateBooks(booksList)
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Ошибка при загрузке деталей книг: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleFavorite(book: BookSelected, position: Int) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val favoriteRef = db.collection("users").document(userId).collection("favorites").document(book.favoriteId)

        favoriteRef.delete().addOnSuccessListener {
            Toast.makeText(this, "Удалено из избранного", Toast.LENGTH_SHORT).show()
            // Проверяем, существует ли индекс в списке перед удалением
            if (position < books.size) {
                books.removeAt(position)
                adapter.notifyItemRemoved(position)
                adapter.notifyItemRangeChanged(position, books.size - position)  // Обновляем позиции всех следующих элементов
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Ошибка при удалении из избранного: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

}