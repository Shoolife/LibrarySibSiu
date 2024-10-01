package com.example.libsibsiu.ui.catalog

import android.content.BroadcastReceiver
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.libsibsiu.databinding.ActivityReadBookBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import okhttp3.OkHttpClient
import java.io.File
import okhttp3.Request
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import android.view.View

class ReadBookActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReadBookBinding
    private lateinit var progressBar: ProgressBar // Добавляем переменную для ProgressBar
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private lateinit var onComplete: BroadcastReceiver
    private var isReceiverRegistered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReadBookBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        progressBar = binding.progressBar // Инициализация ProgressBar

        val bookId = intent.getStringExtra("BOOK_ID") ?: return // Получите BOOK_ID из Intent
        fetchBookPdfUrl(bookId)
    }

    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE // Показать ProgressBar
    }

    private fun hideProgressBar() {
        progressBar.visibility = View.GONE // Скрыть ProgressBar
    }

    // Остальная часть кода, включая методы fetchBookPdfUrl, downloadAndOpenPdf, displayPdf и onStop
    private fun fetchBookPdfUrl(bookId: String) {
        showProgressBar() // Показать ProgressBar при начале загрузки
        firestore.collection("books").document(bookId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val pdfGsUrl = document.getString("pdfUrl") ?: let {
                        Toast.makeText(applicationContext, "Ссылка на PDF не найдена.", Toast.LENGTH_SHORT).show()
                        hideProgressBar() // Скрыть ProgressBar
                        return@addOnSuccessListener
                    }
                    val fileName = document.id // Используем ID документа как имя файла

                    val file = getFile(fileName)
                    if (file.exists()) {
                        displayPdf(Uri.fromFile(file)) // Открываем существующий файл
                        hideProgressBar() // Скрыть ProgressBar после открытия файла
                    } else {
                        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(pdfGsUrl)
                        storageReference.downloadUrl.addOnSuccessListener { downloadUri ->
                            downloadAndOpenPdf(downloadUri.toString(), fileName)
                        }.addOnFailureListener { exception ->
                            Toast.makeText(applicationContext, "Ошибка загрузки PDF: ${exception.message}", Toast.LENGTH_SHORT).show()
                            hideProgressBar() // Скрыть ProgressBar при ошибке загрузки
                        }
                    }
                } else {
                    Toast.makeText(applicationContext, "Документ не найден.", Toast.LENGTH_SHORT).show()
                    hideProgressBar() // Скрыть ProgressBar, если документ не найден
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(applicationContext, "Ошибка получения данных: ${exception.message}", Toast.LENGTH_SHORT).show()
                hideProgressBar() // Скрыть ProgressBar при ошибке получения документа
            }
    }

    private fun downloadAndOpenPdf(pdfUrl: String, fileName: String) {
        val client = OkHttpClient()
        val request = Request.Builder().url(pdfUrl).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    hideProgressBar() // Скрыть ProgressBar при ошибке загрузки
                    Toast.makeText(applicationContext, "Ошибка загрузки файла: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        hideProgressBar() // Скрыть ProgressBar при HTTP ошибке
                        Toast.makeText(applicationContext, "Ошибка HTTP: ${response.code}", Toast.LENGTH_LONG).show()
                    }
                    return
                }

                val responseBody = response.body
                if (responseBody != null) {
                    val file = getFile(fileName)
                    file.outputStream().use { fos ->
                        fos.write(responseBody.bytes())
                    }

                    runOnUiThread {
                        displayPdf(Uri.fromFile(file))
                        hideProgressBar() // Скрыть ProgressBar после сохранения файла
                    }
                } else {
                    runOnUiThread {
                        hideProgressBar() // Скрыть ProgressBar если тело ответа пусто
                        Toast.makeText(applicationContext, "Ошибка загрузки: тело ответа пусто", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun displayPdf(uri: Uri) {
        binding.pdfView.fromUri(uri)
            .defaultPage(0)
            .spacing(10)
            .load()
    }

    override fun onStop() {
        super.onStop()
        if (isReceiverRegistered) {
            unregisterReceiver(onComplete)
            isReceiverRegistered = false
        }
    }

    private fun getFile(fileName: String): File {
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "$fileName.pdf")
        return file
    }
}



