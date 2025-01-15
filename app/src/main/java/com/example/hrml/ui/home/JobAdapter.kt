package com.example.hrml.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.hrml.R

// Adapter voor het weergeven van vacatures in een RecyclerView
class JobAdapter(
    private var jobs: List<Job>,                       // Lijst van vacatures die weergegeven moeten worden
    private val onItemClicked: (Job, View) -> Unit      // Functie die uitgevoerd wordt wanneer op een item wordt geklikt
) : RecyclerView.Adapter<JobViewHolder>() {

    // Wordt aangeroepen om een nieuw ViewHolder object te creëren
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        // Inflate de layout voor het item in de RecyclerView
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        // Retourneer een nieuwe JobViewHolder met de geïnflate view
        return JobViewHolder(view)
    }

    // Wordt aangeroepen om de data aan een ViewHolder te binden
    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        // Haal de vacature op die overeenkomt met de huidige positie in de lijst
        val job = jobs[position]

        // Bind de vacaturegegevens aan de ViewHolder
        holder.bind(job)

        // Click listener voor het hele item in de RecyclerView
        holder.itemView.setOnClickListener {
            // Roep de onItemClicked functie aan met de vacature en de view waarop geklikt is
            onItemClicked(job, it)
        }

        // Click listener specifiek voor de favorietenknop in de itemview
        holder.favoriteButton.setOnClickListener {
            // Roep de onItemClicked functie aan wanneer de favorietenknop wordt geklikt
            onItemClicked(job, it)
        }
    }

    // Retourneer het aantal items in de lijst
    override fun getItemCount(): Int {
        return jobs.size
    }

    // Methode om de data in de adapter bij te werken
    fun updateData(newList: List<Job>) {
        // Update de lijst van vacatures
        jobs = newList
        // Informeer de adapter dat de data is gewijzigd zodat de weergave kan worden bijgewerkt
        notifyDataSetChanged()
    }
}
