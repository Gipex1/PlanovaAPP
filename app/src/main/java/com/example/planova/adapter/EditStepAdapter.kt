package com.example.planova.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.planova.databinding.ItemEditStepBinding
import com.example.planova.data.StepDto

class EditStepAdapter(
    private var steps: MutableList<StepDto>,
    private val onDelete: (Int) -> Unit,
    private val onTextChange: (Int, String) -> Unit
) : RecyclerView.Adapter<EditStepAdapter.EditStepViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EditStepViewHolder {
        val binding = ItemEditStepBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EditStepViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EditStepViewHolder, position: Int) {
        val step = steps[position]
        holder.bind(step, position)

        holder.binding.etStepDescription.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val newText = holder.binding.etStepDescription.text.toString().trim()
                if (newText != step.description) {
                    onTextChange(position, newText)
                }
            }
        }

        holder.binding.ivDeleteStep.setOnClickListener {
            onDelete(position)
        }
    }

    override fun getItemCount(): Int = steps.size

    fun updateList(newSteps: List<StepDto>) {
        steps.clear()
        steps.addAll(newSteps)
        notifyDataSetChanged()
    }

    fun addStep(description: String = "") {
        steps.add(StepDto(description, steps.size + 1))
        notifyItemInserted(steps.size - 1)
    }

    inner class EditStepViewHolder(val binding: ItemEditStepBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(step: StepDto, position: Int) {
            binding.tvStepNumber.text = "${position + 1}."
            binding.etStepDescription.setText(step.description)
        }
    }
}