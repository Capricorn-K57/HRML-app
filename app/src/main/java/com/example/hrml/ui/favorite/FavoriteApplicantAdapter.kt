package com.example.hrml.ui.favorite

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hrml.DataClasses.ApplicantManager.ApplicantManager
import com.example.hrml.R
import com.example.hrml.ui.home.Applicant
import com.example.hrml.ui.job_details.ApplicantFavoritesManager

/**
 * Adapter voor het weergeven van sollicitanten in een lijst van favorieten.
 * De adapter toont sollicitanten met hun naam, matching score en favorietstatus.
 *
 * @param context De context van de app, wordt gebruikt voor het inflaten van de layout en het uitvoeren van acties.
 * @param persons De lijst van sollicitanten die wordt weergegeven in de RecyclerView.
 * @param onApplicantClick Callback die wordt uitgevoerd wanneer een sollicitant wordt aangeklikt.
 * @param applicantManager Beheerder van sollicitanten, wordt gebruikt om de favorietstatus van sollicitanten bij te werken.
 * @param jobId Het ID van de vacature waarvoor de sollicitanten worden weergegeven, is optioneel.
 */
class FavoriteApplicantAdapter(
    private val context: Context,
    private val persons: MutableList<Applicant>,
    private val onApplicantClick: (Applicant) -> Unit,
    private val applicantManager: ApplicantManager,
    private val jobId: String? // JobId is optioneel
) : RecyclerView.Adapter<FavoriteApplicantAdapter.ApplicantViewHolder>() {

    /**
     * ViewHolder voor het weergeven van een sollicitant in de lijst.
     * Bevat de views voor naam, score en favoriet-icoon.
     */
    inner class ApplicantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.txt_name)
        private val scoreTextView: TextView = itemView.findViewById(R.id.txt_percentage)
        val favoriteImageView: ImageView = itemView.findViewById(R.id.img_favorite)

        /**
         * Bindt de gegevens van een sollicitant aan de views in de item layout.
         *
         * @param applicant De sollicitant waarvan de gegevens getoond worden.
         */
        fun bind(applicant: Applicant) {
            nameTextView.text = applicant.name
            scoreTextView.text = "${applicant.matchingScore}%"
            // Stel het favoriet-icoon in op basis van de favorietstatus
            favoriteImageView.setImageResource(
                if (applicant.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_empty
            )

            // Stel de clicklistener in op de gehele rij (itemView)
            itemView.setOnClickListener {
                onApplicantClick(applicant)  // Voer de callback uit wanneer de sollicitant wordt aangeklikt.
            }
        }
    }

    /**
     * Maakt een nieuwe ViewHolder aan voor de lijstitem-weergave.
     *
     * @param parent Het ViewGroup waarin de ViewHolder wordt geplaatst.
     * @param viewType Het type van de view (doorgaans niet gebruikt in eenvoudige adapters).
     * @return Een nieuwe ViewHolder voor de sollicitant.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicantViewHolder {
        // Inflater voor het creëren van de layout voor elk lijstitem
        val view = LayoutInflater.from(context).inflate(R.layout.list_item_person, parent, false)
        return ApplicantViewHolder(view)
    }

    /**
     * Bindt een sollicitant aan de ViewHolder en stelt de juiste gegevens in voor weergave.
     *
     * @param holder De ViewHolder die de gegevens van de sollicitant weergeeft.
     * @param position De positie van de sollicitant in de lijst.
     */
    override fun onBindViewHolder(holder: ApplicantViewHolder, position: Int) {
        val applicant = persons[position]
        holder.bind(applicant)

        // Stel de clicklistener in voor het favoriet-icoon
        holder.favoriteImageView.setOnClickListener {
            if (jobId != null) {  // Zorg ervoor dat jobId geldig is
                // Wijzig de favorietstatus van de sollicitant
                applicantManager.toggleFavoriteStatus(applicant, jobId)
                notifyDataSetChanged()  // Werk de lijst bij om de wijziging weer te geven
                Log.d("FavoriteApplicantAdapter", "Toggled favorite status for ${applicant.id}")
            } else {
                // Log een fout als jobId ongeldig is
                Log.e("FavoriteApplicantAdapter", "Job ID is null, cannot toggle favorite status")
            }
        }
    }

    /**
     * Retourneert het aantal items in de lijst.
     *
     * @return Het aantal sollicitanten in de lijst.
     */
    override fun getItemCount(): Int = persons.size

    /**
     * Werk de lijst van favoriete sollicitanten bij voor een bepaalde vacature.
     * Haalt de favorieten op via de ApplicantFavoritesManager en werkt de lijst bij.
     *
     * @param jobId Het ID van de vacature waarvoor de favorieten worden bijgewerkt.
     */
    fun updateFavoritePersonsForJob(jobId: String) {
        // Haal de lijst van favoriete sollicitanten voor de opgegeven vacature op
        val favoritePersons = ApplicantFavoritesManager.getFavoriteApplicantsForJob(context, jobId)
        Log.d("FavoriteApplicantAdapter", "Updating favorite persons for job ID $jobId: $favoritePersons")
        // Werk de gegevens in de adapter bij met de nieuwe lijst van favoriete sollicitanten
        updateData(favoritePersons)
    }

    /**
     * Werk de lijst van sollicitanten bij en notify de adapter om de weergave bij te werken.
     *
     * @param newPersons De nieuwe lijst van sollicitanten.
     */
    fun updateData(newPersons: List<Applicant>) {
        Log.d("FavoriteApplicantAdapter", "Updating adapter with ${newPersons.size} applicants")
        persons.clear()  // Maak de oude lijst leeg
        persons.addAll(newPersons)  // Voeg de nieuwe gegevens toe
        notifyDataSetChanged()  // Notify de adapter om de UI bij te werken
    }
}
