package com.example.libsibsiu.ui.account

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.libsibsiu.R
import com.example.libsibsiu.databinding.FragmentAccountBinding
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import com.example.libsibsiu.ui.catalog.BookSelectedActivity
import com.example.libsibsiu.ui.catalog.BookStatusActivity
import com.example.libsibsiu.SettingsActivity
import com.example.libsibsiu.ui.room.RoomBookingHistoryActivity

class AccountFragment : Fragment() {
    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var imagePickerActivityResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        accountViewModel = ViewModelProvider(this).get(AccountViewModel::class.java)

        accountViewModel.text.observe(viewLifecycleOwner) {
            binding.textAccount.text = it
        }

        accountViewModel.userInfo.observe(viewLifecycleOwner) { userInfo ->
            binding.tvUserName.text = userInfo
        }

        accountViewModel.userPhotoUrl.observe(viewLifecycleOwner) { photoUrl ->
            if (photoUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(R.drawable.default_profile_image)
                    .circleCrop() // Применить круглую обрезку даже к изображению по умолчанию
                    .into(binding.ivUserProfile)
            } else {
                Glide.with(this)
                    .load(photoUrl)
                    .circleCrop() // Применить круглую обрезку
                    .into(binding.ivUserProfile)
            }
        }

        imagePickerActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) { // Используйте Activity.RESULT_OK здесь
                val imageUri = result.data?.data // Получаем URI выбранного изображения
                if (imageUri != null) {
                    accountViewModel.uploadImageToStorage(imageUri) // Загружаем изображение
                }
            } else {
                Toast.makeText(context, "Выбор изображения отменён", Toast.LENGTH_SHORT).show()
            }
        }

        binding.ivUserProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            imagePickerActivityResultLauncher.launch(intent)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Установка слушателя нажатий для logo_settings ImageView
        binding.logoSettings.setOnClickListener {
            // Используйте requireContext() для безопасного вызова контекста фрагмента
            val intent = Intent(requireContext(), SettingsActivity::class.java)
            startActivity(intent)
        }

        // Добавление слушателя кликов для tvStatus TextView
        binding.tvStatus.setOnClickListener {
            // Запуск BookStatusActivity при нажатии
            val intent = Intent(requireContext(), BookStatusActivity::class.java)
            startActivity(intent)
        }

        // Добавление слушателя кликов для tvFavorites TextView
        binding.tvFavorites.setOnClickListener {
            // Запуск BookSelectedActivity при нажатии
            val intent = Intent(requireContext(), BookSelectedActivity::class.java)
            startActivity(intent)
        }

        // Добавление слушателя кликов для tvStory TextView
        binding.tvStory.setOnClickListener {
            // Запуск RoomBookingHistoryActivity при нажатии
            val intent = Intent(requireContext(), RoomBookingHistoryActivity::class.java)
            startActivity(intent)
        }

        accountViewModel.favoritesCount.observe(viewLifecycleOwner) { count ->
            binding.textViewFavoritesCount.text = count  // Подключаем LiveData к TextView
        }

        accountViewModel.bookStatusCount.observe(viewLifecycleOwner) { count ->
            binding.textViewBookStatusCount.text = count  // Подключаем LiveData к TextView
        }

        accountViewModel.activeBookingCount.observe(viewLifecycleOwner) { count ->
            binding.textViewRoomHistoryCount.text = count  // Подключаем LiveData к TextView
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
