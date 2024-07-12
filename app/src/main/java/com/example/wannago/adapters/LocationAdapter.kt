package com.example.wannago.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.wannago.databinding.ListItemLocationBinding
import com.example.wannago.model.Location

class LocationHolder(private val binding: ListItemLocationBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(loc: Location) {
        binding.place.text = loc.name.toString()
        binding.latitude.text = loc.latitude.toString()
        binding.longitude.text = loc.longitude.toString()
    }
}

class LocationAdapter(private val locations: ArrayList<Location>) : RecyclerView.Adapter<LocationHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemLocationBinding.inflate(inflater, parent, false)
        return LocationHolder(binding)
    }

    override fun onBindViewHolder(holder: LocationHolder, position: Int) {
        val location = locations[position]
        holder.bind(location)
    }

    override fun getItemCount() = locations.size
}