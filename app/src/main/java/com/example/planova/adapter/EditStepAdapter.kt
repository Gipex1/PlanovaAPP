package com.example.planova.adapter

import android.text.Editable
import android.text.TextWatcher
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
        holder.bind(steps[position])
    }

    override fun getItemCount(): Int = steps.size

    fun addStep(description: String = "") {
        steps.add(StepDto(description, steps.size + 1))
        notifyItemInserted(steps.size - 1)
    }

    inner class EditStepViewHolder(val binding: ItemEditStepBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var textWatcher: TextWatcher? = null

        fun bind(step: StepDto) {
            // Remove old watcher to avoid multiple triggers during recycling
            textWatcher?.let { binding.etStepDescription.removeTextChangedListener(it) }

            binding.tvStepNumber.text = "${bindingAdapterPosition + 1}."
            binding.etStepDescription.setText(step.description)

            textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        onTextChange(pos, s?.toString() ?: "")
                    }
                }
            }
            binding.etStepDescription.addTextChangedListener(textWatcher)

            binding.ivDeleteStep.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onDelete(pos)
                }
            }
        }
    }
}