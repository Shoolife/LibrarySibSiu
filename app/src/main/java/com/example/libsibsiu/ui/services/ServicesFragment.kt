package com.example.libsibsiu.ui.services

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.libsibsiu.CatalogActivity
import com.example.libsibsiu.NewsActivity
import com.example.libsibsiu.RoomActivity
import com.example.libsibsiu.VideoActivity
import com.example.libsibsiu.databinding.FragmentServicesBinding


class ServicesFragment : Fragment() {

    private var _binding: FragmentServicesBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ServicesViewModel =
            ViewModelProvider(this).get(ServicesViewModel::class.java)

        _binding = FragmentServicesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textServices
        ServicesViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        // Обработчик нажатия на ImageButton
        binding.image1.setOnClickListener {
            // Создание интента для запуска CatalogActivity
            val intent = Intent(activity, CatalogActivity::class.java)
            startActivity(intent)
        }

        // Обработчик нажатия на ImageButton
        binding.image2.setOnClickListener {
            // Создание интента для запуска RoomActivity
            val intent = Intent(activity, RoomActivity::class.java)
            startActivity(intent)
        }

        // Обработчик нажатия на ImageButton
        binding.image3.setOnClickListener {
            // Создание интента для запуска NewsActivity
            val intent = Intent(activity, NewsActivity::class.java)
            startActivity(intent)
        }

        // Обработчик нажатия на ImageButton
        binding.image4.setOnClickListener {
            // Создание интента для запуска VideoActivity
            val intent = Intent(activity, VideoActivity::class.java)
            startActivity(intent)
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}