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
    private var items: List<StepProgressItem>
) : RecyclerView.Adapter<StepProgressAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chek_step, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<StepProgressItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: android.view.View) :
        RecyclerView.ViewHolder(itemView) {
        private val tvDay: TextView = itemView.findViewById(R.id.tvDay)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val ivHelp: ImageView = itemView.findViewById(R.id.ivHelp)

        fun bind(item: StepProgressItem) {
            tvDay.text = "День ${item.day}"
            tvDescription.text = item.description

            // Если шаг выполнен – меняем цвет
            if (item.isCompleted) {
                tvDescription.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.neon_green)
                )
                ivHelp.setImageResource(R.drawable.ic_accept4)
                ivHelp.setColorFilter(
                    ContextCompat.getColor(itemView.context, R.color.neon_green)
                )
            } else {
                tvDescription.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.white)
                )
                ivHelp.setImageResource(R.drawable.help4)
                ivHelp.setColorFilter(
                    ContextCompat.getColor(itemView.context, R.color.dark_text_secondary)
                )
            }
        }
    }
}