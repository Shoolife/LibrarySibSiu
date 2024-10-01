package com.example.libsibsiu

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.libsibsiu.adapters.news.NewsAdapter
import com.example.libsibsiu.databinding.ActivityNewsBinding
import com.example.libsibsiu.models.News
import com.example.libsibsiu.ui.news.NewsDetailActivity
import com.example.libsibsiu.ui.news.NewsRepository
import com.example.libsibsiu.ui.news.NewsViewModel
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.Locale

class NewsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewsBinding
    private lateinit var newsAdapter: NewsAdapter
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechRecognizerIntent: Intent
    private val viewModel: NewsViewModel by viewModels()
    private var lastFirstVisiblePosition: Int = 0 // Переменная для сохранения позиции скролла

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d("NewsActivity", "Returning from NewsDetailActivity with RESULT_OK")
            val newsId = result.data?.getStringExtra("news_id")
            if (newsId != null) {
                viewModel.updateNewsItem(newsId)
            } else {
                viewModel.loadAllNews() // Если id новости неизвестен, перезагружаем все новости
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewsBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        checkAudioPermission()
        setupRecyclerView()
        setupObservers()
        setupSpeechRecognizer()
        setupListeners()

        viewModel.loadAllNews()
    }

    private fun setupRecyclerView() {
        newsAdapter = NewsAdapter(mutableListOf())
        binding.rvBooks.layoutManager = LinearLayoutManager(this)
        binding.rvBooks.adapter = newsAdapter

        newsAdapter.setOnItemClickListener { newsId ->
            val intent = Intent(this, NewsDetailActivity::class.java)
            intent.putExtra("news_id", newsId)
            startForResult.launch(intent)
        }
    }

    private fun setupObservers() {
        viewModel.newsList.observe(this) { newsList ->
            newsAdapter.updateNews(newsList)
            restoreScrollPosition()
        }

        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                showLoadingIndicator()
            } else {
                hideLoadingIndicator()
            }
        }

        viewModel.tags.observe(this) { allTags ->
            showFilterDialog(allTags)
        }
    }

    private fun showFilterDialog(allTags: List<String>) {
        val selectedTags = mutableListOf<String>()
        val tagsArray = allTags.toTypedArray()
        val checkedTags = BooleanArray(tagsArray.size) { false }

        val builder = AlertDialog.Builder(this,R.style.RoundedAlertDialog)
        builder.setTitle("Выберите теги")
        builder.setMultiChoiceItems(tagsArray, checkedTags) { _, which, isChecked ->
            if (isChecked) {
                selectedTags.add(tagsArray[which])
            } else {
                selectedTags.remove(tagsArray[which])
            }
        }

        builder.setPositiveButton("Применить") { dialog, _ ->
            viewModel.filterNewsByTags(selectedTags)
            dialog.dismiss()
        }

        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.dismiss()
        }

        builder.create().show()
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
                    viewModel.searchNews(query)
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
                    viewModel.searchNews(s.toString())
                } else {
                    viewModel.loadAllNews()
                }
            }
        })

        binding.microphoneIcon.setOnClickListener {
            startVoiceInput()
        }

        binding.filterIcon.setOnClickListener {
            viewModel.loadAllTags()
        }

        binding.backArrow.setOnClickListener {
            finish()
        }
    }

    private fun saveCurrentScrollPosition() {
        val layoutManager = binding.rvBooks.layoutManager as LinearLayoutManager
        lastFirstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
    }

    private fun restoreScrollPosition() {
        binding.rvBooks.layoutManager?.scrollToPosition(lastFirstVisiblePosition)
    }

    private fun showLoadingIndicator() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideLoadingIndicator() {
        binding.progressBar.visibility = View.GONE
    }
}