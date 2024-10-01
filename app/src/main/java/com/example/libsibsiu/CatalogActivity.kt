package com.example.libsibsiu

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.libsibsiu.adapters.book.BookAdapter
import com.example.libsibsiu.databinding.ActivityCatalogBinding
import com.example.libsibsiu.models.Book
import com.google.firebase.firestore.FirebaseFirestore
import android.Manifest
import android.icu.util.Calendar
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import java.util.Locale
import android.widget.NumberPicker
import android.widget.RadioGroup
import com.example.libsibsiu.ui.catalog.BookDetailActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query

class CatalogActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCatalogBinding
    private lateinit var bookAdapter: BookAdapter
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechRecognizerIntent: Intent
    private val db = FirebaseFirestore.getInstance()
    private var lastFirstVisiblePosition: Int = 0 // Переменная для сохранения позиции скролла

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCatalogBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        checkAudioPermission()
        setupRecyclerView()
        loadAllBooks()
        setupSpeechRecognizer()
        setupListeners()
    }

    private fun setupRecyclerView() {
        bookAdapter = BookAdapter(this, mutableListOf())
        binding.rvBooks.layoutManager = LinearLayoutManager(this)
        binding.rvBooks.adapter = bookAdapter

        bookAdapter.setOnItemClickListener { book ->
            val intent = Intent(this, BookDetailActivity::class.java)
            intent.putExtra("BOOK_ID", book.id)
            startActivity(intent)
        }
    }

    private fun loadAllBooks() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val favoritesRef = db.collection("users").document(userId).collection("favorites")

        db.collection("books").get().addOnSuccessListener { documents ->
            val booksList = documents.mapNotNull { document ->
                document.toObject(Book::class.java).apply { id = document.id }
            }.toMutableList()

            // Загрузка избранных книг
            favoritesRef.get().addOnSuccessListener { favoritesDocs ->
                val favoriteBookIds = favoritesDocs.documents.map { it.id }
                booksList.forEach { book ->
                    book.isFavorite = book.id in favoriteBookIds
                }
                bookAdapter.updateBooks(booksList)
            }.addOnFailureListener { e ->
                showError("Ошибка загрузки избранных книг: ${e.localizedMessage}")
            }
        }.addOnFailureListener { e ->
            showError("Ошибка загрузки книг: ${e.localizedMessage}")
        }
    }

    private fun updateBooksStateWithFavorites(books: MutableList<Book>) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userDocRef = db.collection("users").document(userId)
        val favoritesRef = userDocRef.collection("favorites")

        favoritesRef.get().addOnSuccessListener { snapshot ->
            val favoriteBookIds = snapshot.documents.map { it.id }
            books.forEach { book ->
                book.isFavorite = favoriteBookIds.contains(book.id)
            }
            updateBooks(books) // Обновляем адаптер с обновленным состоянием избранных и восстанавливаем позицию
        }.addOnFailureListener { e ->
            Log.e("CatalogActivity", "Ошибка при загрузке избранных книг: ${e.localizedMessage}")
        }
    }

    private fun searchBooks(query: String) {
        val searchQuery = query.lowercase(Locale.getDefault()).split(" ").filter { it.isNotBlank() }

        if (searchQuery.isEmpty()) {
            bookAdapter.updateBooks(emptyList()) // Очищаем список книг
            return
        }

        db.collection("books").whereArrayContainsAny("searchKeywords", searchQuery).get()
            .addOnSuccessListener { result ->
                val booksList = result.mapNotNull { documentSnapshot ->
                    documentSnapshot.toObject(Book::class.java)?.apply {
                        id = documentSnapshot.id
                    }
                }
                updateBooksStateWithFavorites(booksList.toMutableList()) // Обновляем состояние избранных перед обновлением адаптера
            }
            .addOnFailureListener { exception ->
                Log.e("CatalogActivity", "Ошибка поиска книг: ${exception.localizedMessage}")
            }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
                    val query = it.lowercase(Locale.getDefault())
                    binding.searchEditText.setText(query)
                    searchBooks(query)
                }
            }

            override fun onError(error: Int) { showError("Ошибка ввода голоса") }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startVoiceInput() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            speechRecognizer.startListening(speechRecognizerIntent)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
    }

    private fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun setupListeners() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrEmpty()) {
                    saveCurrentScrollPosition() // Сохраняем позицию перед поиском
                    searchBooks(s.toString())
                } else {
                    loadAllBooks()
                }
            }
        })

        binding.microphoneIcon.setOnClickListener {
            startVoiceInput()
        }

        binding.filterIcon.setOnClickListener {
            showFilterDialog()
        }

        binding.backArrow.setOnClickListener {
            finish()
        }
    }

    private fun showFilterDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_filter, null)
        val datePickerFrom = dialogView.findViewById<EditText>(R.id.editTextDateFrom)
        val datePickerTo = dialogView.findViewById<EditText>(R.id.editTextDateTo)
        val radioGroupSort = dialogView.findViewById<RadioGroup>(R.id.radioGroupSort)

        datePickerFrom.setOnClickListener { showYearPickerDialog(datePickerFrom) }
        datePickerTo.setOnClickListener { showYearPickerDialog(datePickerTo) }

        val dialog = AlertDialog.Builder(this,R.style.RoundedAlertDialog)
            .setTitle("Фильтры")
            .setView(dialogView)
            .setPositiveButton("Применить") { _, _ ->
                val sortOrder = when (radioGroupSort.checkedRadioButtonId) {
                    R.id.radio_asc -> "ASC"
                    R.id.radio_desc -> "DESC"
                    else -> null
                }
                val fromYear = datePickerFrom.text.toString()
                val toYear = datePickerTo.text.toString()
                applyFilters(sortOrder, fromYear, toYear)
            }
            .setNegativeButton("Отмена", null)
            .create()

        dialog.show()
    }

    private fun showYearPickerDialog(editText: EditText) {
        val dialogBuilder = AlertDialog.Builder(this, R.style.RoundedAlertDialog)
        val numberPickerView = layoutInflater.inflate(R.layout.dialog_year_picker, null)
        dialogBuilder.setView(numberPickerView)

        val numberPicker = numberPickerView.findViewById<NumberPicker>(R.id.numberPickerYear)
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)

        numberPicker.minValue = 1000
        numberPicker.maxValue = currentYear
        numberPicker.value = currentYear

        dialogBuilder
            .setTitle("Выберите год")
            .setPositiveButton("OK") { dialog, _ ->
                editText.setText(numberPicker.value.toString())
                dialog.dismiss()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.cancel()
            }
            .create()
            .show()
    }

    private fun applyFilters(sortOrder: String?, fromYear: String, toYear: String) {
        var query: Query = db.collection("books")

        if (fromYear.isNotEmpty()) {
            query = query.whereGreaterThanOrEqualTo("publishYear", fromYear)
        }
        if (toYear.isNotEmpty()) {
            query = query.whereLessThanOrEqualTo("publishYear", toYear)
        }

        if (sortOrder != null) {
            query = query.orderBy("title", if (sortOrder == "ASC") Query.Direction.ASCENDING else Query.Direction.DESCENDING)
        }

        saveCurrentScrollPosition() // Переместил сохранение позиции перед выполнением запроса
        executeQuery(query)
    }

    private fun executeQuery(query: Query) {
        query.get().addOnSuccessListener { documents ->
            val booksList = documents.mapNotNull { it.toObject(Book::class.java)?.apply {
                id = it.id
            } }.toMutableList()

            if (booksList.isEmpty()) {
                showError("Книги не найдены.")
            } else {
                updateBooksStateWithFavorites(booksList) // Обновляем состояние избранных перед обновлением адаптера
            }
        }.addOnFailureListener { exception ->
            showError("Ошибка при фильтрации: ${exception.localizedMessage}")
        }
    }

    private fun saveCurrentScrollPosition() {
        val layoutManager = binding.rvBooks.layoutManager as LinearLayoutManager
        lastFirstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
    }

    private fun restoreScrollPosition() {
        binding.rvBooks.layoutManager?.scrollToPosition(lastFirstVisiblePosition)
    }

    private fun updateBooks(booksList: MutableList<Book>) {
        bookAdapter.updateBooks(booksList)
        restoreScrollPosition() // Восстанавливаем позицию после обновления
    }

    override fun onResume() {
        super.onResume()
        // Обновление списка книг, возможно, с проверкой на изменения в избранных
        loadAllBooks() // или другой метод, который обновляет данные
    }
}

