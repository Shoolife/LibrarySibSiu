package com.example.libsibsiu.adapters.room

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.libsibsiu.databinding.ItemRoomListBinding
import com.example.libsibsiu.models.Room
import com.google.firebase.storage.FirebaseStorage

class RoomAdapter(private var roomList: List<Room>, private val onBookClick: (String) -> Unit) : RecyclerView.Adapter<RoomAdapter.RoomViewHolder>() {

    inner class RoomViewHolder(private val binding: ItemRoomListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(room: Room) {
            binding.nameRoom.text = room.title

            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(room.imageUrl)
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(binding.imageViewCover.context)
                    .load(uri)
                    .into(binding.imageViewCover)
            }

            binding.roomButton.setOnClickListener {
                onBookClick(room.id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemRoomListBinding.inflate(inflater, parent, false)
        return RoomViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        holder.bind(roomList[position])
    }

    override fun getItemCount(): Int = roomList.size

    fun updateData(newRoomList: List<Room>) {
        roomList = newRoomList
        notifyDataSetChanged()
    }
}