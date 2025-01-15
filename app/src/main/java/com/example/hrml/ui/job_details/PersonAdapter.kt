package com.example.hrml.ui.job_details

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.hrml.R
import com.example.hrml.ui.home.Applicant
import com.example.hrml.ui.popup_cv.PopupCV

// Adapter voor het beheren en weergeven van een lijst met sollicitanten in een RecyclerView
class PersonAdapter(
    private val context: Context,                      // Context om toegang te krijgen tot resources en het tonen van pop-ups
    private var applicants: MutableList<Applicant>,     // Lijst van sollicitanten die wordt weergegeven in de adapter
    private val jobId: String,                          // Vacature-ID, gebruikt om favorieten per vacature te beheren
    private val listener: PopupCV.FavoriteStatusChangedListener // Listener om de favorietstatus te kunnen bijwerken
) : RecyclerView.Adapter<PersonAdapter.ViewHolder>() {

    // Functie om de favorietstatus van een specifieke sollicitant bij te werken en de UI te vernieuwen
    fun updateFavoriteStatus(applicantId: String, isFavorite: Boolean) {
        val applicant = applicants.find { it.id == applicantId } // Zoek de sollicitant op basis van het ID
        if (applicant != null) {
            applicant.isFavorite = isFavorite                    // Update de favorietstatus
            notifyDataSetChanged()                               // Vernieuw de UI om de wijziging weer te geven
        }
    }

    // Update de volledige lijst van sollicitanten en vernieuw de UI
    fun updateData(newApplicants: List<Applicant>) {
        applicants.clear()
        applicants.addAll(newApplicants)                         // Vervang de huidige lijst met de nieuwe lijst
        notifyDataSetChanged()                                   // Informeer de adapter over de gewijzigde data
    }

    // Creëert een nieuwe ViewHolder wanneer er geen bestaande views zijn om opnieuw te gebruiken
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.list_people, parent, false)
        return ViewHolder(view)
    }

    // Bindt de gegevens van een sollicitant aan de views in de opgegeven ViewHolder
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Update de lijst van sollicitanten met hun favorietstatussen
        val updatedApplicants = ApplicantFavoritesManager.updateApplicantsWithFavorites(context, applicants, jobId)
        applicants.clear()
        applicants.addAll(updatedApplicants)                     // Werk de lijst bij met de favorietstatus
        holder.bind(applicants[position])                        // Koppel de sollicitant aan de ViewHolder
    }

    // Geeft het totale aantal sollicitanten in de lijst terug
    override fun getItemCount(): Int = applicants.size

    // ViewHolder klasse voor het beheren van de views binnen een enkel sollicitant-item
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Verbind de UI-elementen in het item
        private val nameTextView: TextView = itemView.findViewById(R.id.item_name)        // TextView voor de naam van de sollicitant
        private val scoreTextView: TextView = itemView.findViewById(R.id.item_percentage) // TextView voor de matching score van de sollicitant
        private val starImageView: ImageView = itemView.findViewById(R.id.item_icon)      // ImageView voor de favorietstatus (ster-icoon)

        // Bind de gegevens van een sollicitant aan de UI-elementen in het item
        fun bind(applicant: Applicant) {
            nameTextView.text = applicant.name                         // Stel de naam van de sollicitant in
            scoreTextView.text = "${applicant.matchingScore}%"         // Stel de matchingscore van de sollicitant in
            starImageView.setImageResource(
                if (applicant.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_empty
            ) // Stel het juiste ster-icoon in op basis van de favorietstatus

            // Stel een click listener in voor het hele item om de details van de sollicitant te tonen in een pop-up
            itemView.setOnClickListener {
                // Maak een nieuwe pop-up aan met het sollicitant-ID
                val popup = PopupCV.newInstance(jobId, applicant.id)
                popup.favoriteStatusChangedListener = listener         // Koppel de favorietstatus listener
                // Toon de pop-up
                popup.show((context as AppCompatActivity).supportFragmentManager, "PopupCV")
            }
        }
    }
}
