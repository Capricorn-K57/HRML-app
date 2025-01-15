package com.example.hrml.ui.home

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hrml.DataClasses.JobSharedPrefManager
import com.example.hrml.R

// ViewHolder die verantwoordelijk is voor het binden van de data aan de UI-elementen in een RecyclerView item
class JobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    // Initialiseer de TextViews voor de titel en het aantal reacties
    private val titleTextView: TextView = itemView.findViewById(R.id.txt_jobopenings)
    private val applicantsTextView: TextView = itemView.findViewById(R.id.txt_responses)

    // Initialiseer de ImageView voor de favorietenknop (stericoon)
    val favoriteButton: ImageView = itemView.findViewById(R.id.starIcon)

    // Functie die een vacature bindt aan de UI
    fun bind(job: Job) {
        // Stel de titel en het aantal reacties in voor deze vacature
        titleTextView.text = job.title
        applicantsTextView.text = "${job.reactions} sollicitanten"

        // Controleer de favorietenstatus van de vacature op runtime
        val isFavorite = JobFavoritesManager.isFavorite(job.id)

        // Update de stericoon op basis van de favorietenstatus
        favoriteButton.setImageResource(
            if (isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_empty
        )

        // Voeg een kliklistener toe aan de stericoon om de favorietenstatus te wisselen
        favoriteButton.setOnClickListener {
            val context = itemView.context

            // Als de vacature al favoriet is, verwijder deze uit de favorieten
            if (isFavorite) {
                JobFavoritesManager.setFavorite(job.id, false, context)  // Update de favorietenstatus
                val sharedPrefManager = JobSharedPrefManager(context)  // Haal de SharedPreferences manager op
                sharedPrefManager.removeJobFavorite(job.id)  // Verwijder de vacature uit de opgeslagen favorieten
                favoriteButton.setImageResource(R.drawable.ic_star_empty)  // Verander de icoon naar een lege ster
            } else {
                // Als de vacature nog niet favoriet is, voeg deze toe aan de favorieten
                JobFavoritesManager.setFavorite(job.id, true, context)  // Update de favorietenstatus
                val sharedPrefManager = JobSharedPrefManager(context)  // Haal de SharedPreferences manager op
                sharedPrefManager.addJobFavorite(job.id)  // Voeg de vacature toe aan de opgeslagen favorieten
                favoriteButton.setImageResource(R.drawable.ic_star_filled)  // Verander de icoon naar een gevulde ster
            }
        }
    }
}
