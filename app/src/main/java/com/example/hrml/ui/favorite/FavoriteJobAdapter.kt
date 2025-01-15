package com.example.hrml.ui.favorite

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.hrml.R

// Data class die informatie over een favoriete vacature bevat
data class FavoriteJob(
    val title: String,        // Titel van de vacature
    val favoriteCount: Int,   // Aantal favorieten voor deze vacature
    val jobId: String         // Uniek ID voor de vacature
)

// Adapter voor het weergeven van een lijst van favoriete vacatures in een ListView
class FavoriteJobAdapter(
    context: Context,                          // Context voor toegang tot resources, zoals layout inflaters
    private val jobs: MutableList<FavoriteJob> // Lijst van favoriete vacatures om weer te geven in de adapter
) : ArrayAdapter<FavoriteJob>(context, android.R.layout.simple_list_item_1, jobs) {

    /**
     * Haalt de weergave voor elk item in de lijst op en vult deze met data.
     *
     * @param position De positie van het item in de lijst.
     * @param convertView De herbruikbare view die mogelijk al bestaat.
     * @param parent Het parent ViewGroup waarin de nieuwe view moet worden geplaatst.
     * @return De ingevulde view voor weergave in de ListView.
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Recycleer de view als deze al bestaat, anders inflate een nieuwe view vanuit de XML-layout
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.favorite_job, parent, false)

        // Haal het specifieke vacature-item op basis van de positie in de lijst
        val job = getItem(position)

        // Zoek de TextViews in de layout voor titel en favorieten
        val titleTextView = view.findViewById<TextView>(R.id.jobTitle)         // TextView voor de titel van de vacature
        val favoriteCountTextView = view.findViewById<TextView>(R.id.favoriteCount) // TextView voor het aantal favorieten

        // Vul de TextViews met de gegevens van de vacature, indien beschikbaar
        titleTextView.text = job?.title
        favoriteCountTextView.text = "Favorieten: ${job?.favoriteCount}"

        // Geef de volledig gevulde view terug voor weergave in de ListView
        return view
    }

    /**
     * Functie om de data in de adapter bij te werken met een nieuwe lijst van vacatures.
     *
     * @param newList De nieuwe lijst van favoriete vacatures.
     */
    fun updateData(newList: List<FavoriteJob>) {
        jobs.clear()            // Leeg de huidige lijst van vacatures
        jobs.addAll(newList)    // Voeg de nieuwe lijst van vacatures toe aan de bestaande lijst
        notifyDataSetChanged()  // Informeer de adapter dat de data is gewijzigd, zodat de ListView wordt vernieuwd
    }
}
