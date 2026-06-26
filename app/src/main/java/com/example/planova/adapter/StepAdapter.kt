package com.example.planova.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.planova.data.StepDto
import com.example.planova.databinding.ItemStepBinding

class StepAdapter(private val steps: List<StepDto>) :
    RecyclerView.Adapter<AllStepAdapter.StepViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val binding = ItemStepBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StepViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
        val step = steps[position]
        holder.bind(step)
    }

    override fun getItemCount(): Int = steps.size

    class StepViewHolder(private val binding: ItemStepBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(step: StepDto) {
            binding.tvStepNumber.text ="День " + "${step.sortOrder}."
            binding.tvStepDescription.text = step.description
        }
    }
}