package com.example.planova.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.planova.R
import com.example.planova.databinding.ItemPlanBinding

/**
 * Адаптер для отображения списка планов в RecyclerView.
 * Принимает список объектов PlanItem и слушатель нажатий.
 */
class PlanAdapter(
    private var items: List<PlanItem>,
    private val onItemClick: (PlanItem) -> Unit = {}
) : RecyclerView.Adapter<PlanAdapter.PlanViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val binding = ItemPlanBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    /**
     * Обновляет список и уведомляет адаптер об изменениях
     */
    fun updateItems(newItems: List<PlanItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    /**
     * ViewHolder хранит ссылки на все View элемента списка
     */
    inner class PlanViewHolder(
        private val binding: ItemPlanBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            // Обработка клика по элементу
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(items[position])
                }
            }
        }

        /**
         * Заполняет поля данными из PlanItem
         */
        fun bind(item: PlanItem) {
            binding.tvPlanTitle.text = item.title
            binding.progressBar.progress = item.progress
            binding.tvProgressPercent.text = "${item.progress}%"
            // TODO: заменить иконку категории на реальную
            // binding.ivCategoryIcon.setImageResource(item.iconRes)
        }
    }
}

/**
 * Модель данных для элемента списка планов
 */
data class PlanItem(
    val id: Long,
    val title: String,
    val category: String,
    val progress: Int, // от 0 до 100
    val iconRes: Int = R.drawable.ic_book // пока заглушка
)