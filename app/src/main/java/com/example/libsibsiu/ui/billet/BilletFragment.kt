package com.example.libsibsiu.ui.billet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.libsibsiu.R
import com.example.libsibsiu.databinding.FragmentBilletBinding
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

class BilletFragment : Fragment() {

    private var _binding: FragmentBilletBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBilletBinding.inflate(inflater, container, false)
        val billetViewModel = ViewModelProvider(this).get(BilletViewModel::class.java)

        billetViewModel.text.observe(viewLifecycleOwner) {
            binding.textBillet.text = it
        }

        billetViewModel.userData.observe(viewLifecycleOwner) { userData ->
            binding.tvUserName.text = getString(R.string.user_full_name_format, userData)
        }

        billetViewModel.readerTicketNumber.observe(viewLifecycleOwner) { ticketNumber ->
            displayBarcode(ticketNumber, binding.switchBarcodeType.isChecked)
        }

        // Инициализируем видимость текстовых представлений
        updateTextViewVisibility(binding.switchBarcodeType.isChecked)

        binding.switchBarcodeType.setOnCheckedChangeListener { _, isChecked ->
            // Обновляем видимость текстовых представлений
            updateTextViewVisibility(isChecked)

            // Обновляем отображаемый баркод
            billetViewModel.readerTicketNumber.value?.let { ticketNumber ->
                displayBarcode(ticketNumber, isChecked)
            }
        }

        return binding.root
    }

    // Метод для обновления видимости TextView элементов
    private fun updateTextViewVisibility(isQRCodeSelected: Boolean) {
        binding.tvBR.visibility = if (isQRCodeSelected) View.VISIBLE else View.GONE
        binding.tvQR.visibility = if (isQRCodeSelected) View.GONE else View.VISIBLE
    }

    private fun displayBarcode(data: String, showQRCode: Boolean) {
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = if (showQRCode) {
                barcodeEncoder.encodeBitmap(data, BarcodeFormat.QR_CODE, 1500, 1500)
            } else {
                barcodeEncoder.encodeBitmap(data, BarcodeFormat.CODE_128, 1600, 800)
            }
            binding.ivBarcode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка при генерации кода: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
