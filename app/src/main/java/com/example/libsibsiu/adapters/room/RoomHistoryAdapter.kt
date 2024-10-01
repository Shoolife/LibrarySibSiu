package com.example.libsibsiu.adapters.room

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.libsibsiu.R
import com.example.libsibsiu.databinding.ItemRoomHistoryBinding
import com.example.libsibsiu.models.RoomHistory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RoomHistoryAdapter(
    private var historyList: List<RoomHistory>,
    private val onDeleteClick: (RoomHistory) -> Unit
) : RecyclerView.Adapter<RoomHistoryAdapter.RoomHistoryViewHolder>() {

    inner class RoomHistoryViewHolder(private val binding: ItemRoomHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(history: RoomHistory) {
            binding.RoomTitle.text = history.title
            binding.tvwDate.text = history.date
            binding.tvTime.text = history.time

            val bookingDateTime = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).parse("${history.date} ${history.time.split(" - ")[0]}")
            val currentDateTime = Calendar.getInstance().time

            if (bookingDateTime != null && currentDateTime.after(bookingDateTime)) {
                binding.RoomTitle.setTextColor(ContextCompat.getColor(binding.root.context, R.color.gray))
                binding.RoomTitle.paintFlags = binding.RoomTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvwDate.setTextColor(ContextCompat.getColor(binding.root.context, R.color.gray))
                binding.tvTime.setTextColor(ContextCompat.getColor(binding.root.context, R.color.gray))
                binding.trashcan.visibility = View.VISIBLE  // Показываем корзину, если время прошло
            } else {
                binding.RoomTitle.setTextColor(ContextCompat.getColor(binding.root.context, R.color.textColor))
                binding.RoomTitle.paintFlags = binding.RoomTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvwDate.setTextColor(ContextCompat.getColor(binding.root.context, R.color.textColor))
                binding.tvTime.setTextColor(ContextCompat.getColor(binding.root.context, R.color.textColor))
                binding.trashcan.visibility = View.GONE  // Скрываем корзину, если время еще не прошло
            }

            binding.trashcan.setOnClickListener {
                onDeleteClick(history)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomHistoryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemRoomHistoryBinding.inflate(inflater, parent, false)
        return RoomHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoomHistoryViewHolder, position: Int) {
        holder.bind(historyList[position])
    }

    override fun getItemCount(): Int = historyList.size

    fun updateData(newHistoryList: List<RoomHistory>) {
        historyList = newHistoryList
        notifyDataSetChanged()
    }
}