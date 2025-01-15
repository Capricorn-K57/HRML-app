package com.example.hrml.ui.popup_cv

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import com.example.hrml.R
import com.example.hrml.ui.home.Applicant
import com.example.hrml.ui.job_details.ApplicantFavoritesManager
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.io.IOException

// DialogFragment voor het tonen van sollicitantinformatie, inclusief de mogelijkheid om een favoriet toe te voegen en het CV te downloaden.
class PopupCV : DialogFragment() {

    private var jobId: String = ""             // Het ID van de vacature, gebruikt voor het beheren van favorieten.
    private var personId: String = ""          // Het ID van de sollicitant.
    private lateinit var favoriteIcon: ImageView // Het icoon dat de favorietstatus weergeeft en togglebaar is.
    private lateinit var nameTextView: TextView  // TextView voor de naam van de sollicitant.
    private lateinit var cityTextView: TextView  // TextView voor de woonplaats van de sollicitant.
    private lateinit var phoneTextView: TextView // TextView voor het telefoonnummer van de sollicitant.
    private lateinit var emailTextView: TextView // TextView voor het e-mailadres van de sollicitant.
    private lateinit var downloadButton: ImageView // Knop voor het downloaden van het CV.

    // Listener voor veranderingen in de favorietstatus.
    var favoriteStatusChangedListener: FavoriteStatusChangedListener? = null

    // Interface om de favorietstatus door te geven aan de activiteit of ander fragment.
    interface FavoriteStatusChangedListener {
        fun onFavoriteStatusChanged(personId: String, isFavorite: Boolean)
    }

    // Initialisatie van het fragment met de vereiste argumenten: jobId en personId.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            jobId = it.getString("jobId", "")  // Leeg als geen jobId voor favorietenfragment
            personId = it.getString("personId", "")
        }
    }

    // Inflate de view en verbind UI-elementen.
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate de layout voor het popup-scherm.
        val view = inflater.inflate(R.layout.fragment_popup_c_v, container, false)

        // Verbind de UI-elementen met hun respectieve id's.
        favoriteIcon = view.findViewById(R.id.cv_favorite)
        nameTextView = view.findViewById(R.id.txt_cv_name)
        cityTextView = view.findViewById(R.id.txt_cv_zipandplace)
        phoneTextView = view.findViewById(R.id.txt_cv_phone)
        emailTextView = view.findViewById(R.id.txt_cv_email)
        downloadButton = view.findViewById(R.id.cv_download)
        val closeButton = view.findViewById<ImageView>(R.id.cv_close)

        // Haal gegevens van de gebruiker op en update de UI.
        fetchUserDetails(personId)

        // Update het favorietenicoon op basis van de huidige status.
        updateFavoriteIcon()

        // Voeg click listener toe aan het favorietenicoon om de favorietstatus te toggelen.
        favoriteIcon.setOnClickListener {
            toggleFavoriteStatus()
        }

        // Stel de downloadknop in om het CV te downloaden.
        downloadButton.setOnClickListener {
            downloadCV(personId)
        }

        // Sluit de popup bij het klikken op de sluitknop.
        closeButton.setOnClickListener {
            dismiss()
        }

        return view
    }

    // Haalt de details van de gebruiker op via een netwerkrequest en werkt de UI bij met de ontvangen gegevens.
    private fun fetchUserDetails(userId: String) {
        val url = "https://hrml-t4-system-p4wm.onrender.com/get_user_profile?_id=$userId"
        val request = Request.Builder().url(url).get().build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            // Foutafhandelingsfunctie bij mislukte aanvraag.
            override fun onFailure(call: Call, e: IOException) {
                Log.e("PopupCV", "Failed to fetch user details", e)
            }

            // Succesvolle response: update de UI met de gebruikersgegevens.
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    val user = JSONObject(responseBody)
                    val cvTekst = user.optJSONObject("cv_tekst") // Haal CV-tekst op.
                    activity?.runOnUiThread {
                        nameTextView.text = user.optString("name", "N/A")
                        cityTextView.text = cvTekst?.optString("woonplaats", "N/A")
                        phoneTextView.text = cvTekst?.optString("telefoon_nummer", "N/A")
                        emailTextView.text = user.optString("email", "N/A")
                    }
                } else {
                    Log.e("PopupCV", "Failed to fetch user details: ${response.message}")
                }
            }
        })
    }

    // Functie om het CV van de gebruiker te downloaden en automatisch te openen.
    private fun downloadCV(userId: String) {
        val url = "https://hrml-t4-system-p4wm.onrender.com/download_pdf?_id=$userId"
        val request = Request.Builder().url(url).get().build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            // Foutafhandelingsfunctie bij mislukte download.
            override fun onFailure(call: Call, e: IOException) {
                Log.e("PopupCV", "Failed to download CV", e)
            }

            // Succesvolle download: sla het PDF-bestand op en open het.
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val pdfData = response.body?.bytes()
                    if (pdfData != null) {
                        // Sla het PDF-bestand op in het externe opslaggeheugen van het apparaat.
                        val fileName = "cv_$userId.pdf"
                        val file = File(context?.getExternalFilesDir(null), fileName)
                        file.writeBytes(pdfData)
                        Log.i("PopupCV", "CV downloaded and saved as $fileName")

                        // Open het PDF-bestand automatisch.
                        openPdf(file)
                    }
                } else {
                    Log.e("PopupCV", "Failed to download CV: ${response.message}")
                }
            }
        })
    }

    // Open het gedownloade PDF-bestand met een geschikte PDF-viewer.
    private fun openPdf(file: File) {
        val context = context ?: return  // Controleer of context beschikbaar is.

        // Maak een URI voor het bestand met behulp van FileProvider.
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

        // Intent om het PDF-bestand weer te geven.
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        try {
            // Kies een app voor het openen van het PDF-bestand.
            val chooser = Intent.createChooser(intent, "Open PDF met")
            startActivity(chooser)
        } catch (e: Exception) {
            Log.e("PopupCV", "No application found to open PDF", e)
            activity?.runOnUiThread {
                Toast.makeText(context, "Geen applicatie gevonden om PDF te openen. Installeer een PDF-viewer.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Update het favorietenicoon op basis van de huidige favorietstatus.
    private fun updateFavoriteIcon() {
        if (jobId.isNotEmpty()) {
            val isFavorite = ApplicantFavoritesManager.isFavorite(requireContext(), jobId, personId)
            favoriteIcon.setImageResource(
                if (isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_empty
            )
        }
    }

    // Toggle de favorietstatus en werk het icoon bij.
    private fun toggleFavoriteStatus() {
        val currentStatus = ApplicantFavoritesManager.isFavorite(requireContext(), jobId, personId)
        val newStatus = !currentStatus
        ApplicantFavoritesManager.setFavorite(requireContext(), jobId, personId, newStatus)

        updateFavoriteIcon()  // Werk het icoon bij.
        favoriteStatusChangedListener?.onFavoriteStatusChanged(personId, newStatus)
    }

    companion object {
        const val TAG = "PopupCV"

        // Maak een nieuwe instantie van PopupCV voor een specifieke sollicitant en vacature.
        fun newInstance(jobId: String, personId: String): PopupCV {
            val fragment = PopupCV()
            val args = Bundle()
            args.putString("jobId", jobId)
            args.putString("personId", personId)
            fragment.arguments = args
            return fragment
        }

        // Maak een nieuwe instantie van PopupCV voor een sollicitant zonder vacature.
        fun newInstanceForFavorite(personId: String): PopupCV {
            val fragment = PopupCV()
            val args = Bundle()
            args.putString("personId", personId)
            fragment.arguments = args
            return fragment
        }
    }
}
