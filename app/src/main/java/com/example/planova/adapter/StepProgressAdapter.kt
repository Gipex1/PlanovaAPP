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
    private var items: MutableList<StepProgressItem>,
    private val onStatusToggle: (Int, Boolean) -> Unit
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
            val newStatus = !items[position].isCompleted
            items[position] = items[position].copy(isCompleted = newStatus)
            holder.updateStatusIcon(newStatus)
            onStatusToggle(position, newStatus)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<StepProgressItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun updateItemStatus(position: Int, newStatus: Boolean) {
        if (position in items.indices) {
            items[position] = items[position].copy(isCompleted = newStatus)
            notifyItemChanged(position)
        }
    }

    inner class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val tvDay: TextView = itemView.findViewById(R.id.tvDay)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val ivStatus: ImageView = itemView.findViewById(R.id.ivStatus)

        fun bind(item: StepProgressItem) {
            tvDay.text = "День ${item.day}"
            tvDescription.text = item.description
            updateStatusIcon(item.isCompleted)
        }

        fun updateStatusIcon(isCompleted: Boolean) {
            val iconRes = if (isCompleted) R.drawable.ic_accept3 else R.drawable.icc_error
            ivStatus.setImageResource(iconRes)
            val tintColor = if (isCompleted) R.color.neon_green else R.color.error
            ivStatus.setColorFilter(
                ContextCompat.getColor(ivStatus.context, tintColor)
            )
        }
    }
}