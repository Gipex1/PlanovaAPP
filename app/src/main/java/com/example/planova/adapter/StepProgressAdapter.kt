package com.example.planova.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.planova.R
import com.example.planova.data.StepProgressItem

class StepProgressAdapter(
    private var items: List<StepProgressItem>,
    private val onStatusToggle: (Int) -> Unit
) : RecyclerView.Adapter<StepProgressAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chek_step, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)

        holder.ivStatus.setOnClickListener {
            // Меняем статус в объекте
            val newItem = item.copy(isCompleted = !item.isCompleted)
            items = items.toMutableList().apply { this[position] = newItem } // исправлено
            // Обновляем иконку
            holder.updateStatusIcon(newItem.isCompleted)
            // Вызываем callback
            onStatusToggle(position)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<StepProgressItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val tvDay: TextView = itemView.findViewById(R.id.tvDay)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val ivStatus: ImageView = itemView.findViewById(R.id.ivStatus)

        fun bind(item: StepProgressItem) {
            tvDay.text = "День ${item.day}"
            tvDescription.text = item.description
            updateStatusIcon(item.isCompleted)
        }

        fun updateStatusIcon(isCompleted: Boolean) {
            val iconRes = if (isCompleted) R.drawable.ic_accept3 else R.drawable.icc_error
            ivStatus.setImageResource(iconRes)
            val tintColor = if (isCompleted) R.color.neon_green else R.color.dark_text_secondary
            ivStatus.setColorFilter(
                ContextCompat.getColor(ivStatus.context, tintColor)
            )
        }
    }
}