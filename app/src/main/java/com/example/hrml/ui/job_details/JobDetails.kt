package com.example.hrml.ui.job_details

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hrml.DataClasses.ApplicantManager.ApplicantManager
import com.example.hrml.R
import com.example.hrml.ui.home.Applicant
import com.example.hrml.ui.popup_cv.PopupCV
import okhttp3.*
import org.json.JSONArray
import java.io.IOException

// Fragment dat de details van een vacature toont en de sollicitanten weergeeft
class JobDetails : Fragment(), PopupCV.FavoriteStatusChangedListener {

    // Map om sollicitantgegevens lokaal op te slaan voor snelle toegang
    private val applicantCache = mutableMapOf<String, Applicant>()

    // Argumenten die zijn meegegeven vanuit het vorige fragment (bijvoorbeeld jobId, jobTitle, jobDescription)
    private val args: JobDetailsArgs by navArgs()

    // Map om sollicitanten per vacature-ID op te slaan
    private val applicantsPerJob: MutableMap<String, List<Applicant>> = mutableMapOf()

    // UI-componenten voor het weergeven van sollicitanten
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PersonAdapter // Adapter voor het beheren van sollicitantengegevens in de RecyclerView
    private lateinit var applicantManager: ApplicantManager

    // Functie om de jobId op te slaan in SharedPreferences voor later gebruik
    private fun saveJobIdToPreferences(jobId: String) {
        val sharedPref = requireContext().getSharedPreferences("FavoriteApplicants", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("lastJobId", jobId)
            apply()
        }
        Log.d("JobDetails", "Saved jobId to SharedPreferences: $jobId")
    }

    // Het creëren van de view voor dit fragment
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Zet de titel van de action bar uit voor dit fragment
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)

        // Inflate de layout voor dit fragment
        val view = inflater.inflate(R.layout.fragment_job_details, container, false)

        // Initialiseer applicantManager om sollicitanten op te halen
        applicantManager = ApplicantManager(requireContext())

        // Vul de vacaturetitel en -beschrijving met de gegevens uit de argumenten
        val jobTitleTextView = view.findViewById<TextView>(R.id.txt_jobtitle)
        jobTitleTextView.text = args.jobTitle

        val jobDescriptionTextView = view.findViewById<TextView>(R.id.txt_jobdescription)
        jobDescriptionTextView.text = args.jobDescription

        Log.d("JobDetails", "Received jobId: ${args.jobId}")
        // Sla de jobId op in SharedPreferences voor later gebruik
        saveJobIdToPreferences(args.jobId)

        // Initialiseer de RecyclerView voor sollicitanten en stel de layout manager en adapter in
        recyclerView = view.findViewById(R.id.list_people_response)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = PersonAdapter(requireContext(), mutableListOf(), args.jobId, listener = this)
        recyclerView.adapter = adapter

        // Stel de functie in voor de sluitknop om terug te navigeren
        val closeButton = view.findViewById<ImageView>(R.id.btn_back)
        closeButton.setOnClickListener {
            findNavController().navigateUp() // Navigeer terug naar het vorige scherm
        }

        // Haal sollicitanten op via ApplicantManager
        applicantManager.fetchApplicantsForJob(args.jobId) { applicants ->
            requireActivity().runOnUiThread {
                if (applicants != null) {
                    adapter.updateData(applicants) // Werk de adapter bij met de opgehaalde sollicitanten
                } else {
                    Log.e("JobDetails", "Failed to fetch applicants") // Log een foutmelding als de sollicitanten niet kunnen worden opgehaald
                }
            }
        }

        return view
    }

    // Functie om sollicitanten op te halen voor de vacature (jobId) via een API-aanroep
    private fun fetchApplicantsForJob(jobId: String) {
        val url = "https://hrml-t4-system-p4wm.onrender.com/get_vacancy_applicants?vacature_id=$jobId"
        val request = Request.Builder().url(url).get().build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            // Als de API-aanroep mislukt
            override fun onFailure(call: Call, e: IOException) {
                Log.e("JobDetails", "Failed to fetch applicants", e) // Log de foutmelding
            }

            // Als de API-aanroep succesvol is
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    val applicants = parseApplicants(responseBody) // Parse de JSON-respons om een lijst van sollicitanten te verkrijgen

                    // Vul de cache met de opgehaalde sollicitanten
                    applicants.forEach { applicant ->
                        ApplicantFavoritesManager.cacheApplicant(jobId, applicant)
                    }

                    // Werk de UI bij op de hoofdthread
                    activity?.runOnUiThread {
                        adapter.updateData(applicants) // Werk de adapter bij met de nieuwe lijst sollicitanten
                        adapter.notifyDataSetChanged() // Notify de adapter dat de data is veranderd
                    }
                } else {
                    Log.e("JobDetails", "Failed to fetch applicants: ${response.message}") // Log een foutmelding als de response niet succesvol is
                }
            }
        })
    }

    // Functie om de JSON-antwoordbody te parseren en een lijst van sollicitanten te maken
    private fun parseApplicants(responseBody: String): List<Applicant> {
        val applicants = mutableListOf<Applicant>()
        val jsonArray = JSONArray(responseBody) // Parse de JSON-array uit de responsebody
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i) // Haal de objecten op uit de array
            val matchingScore = jsonObject.optDouble("matchingscore", 0.0)
            val applicant = Applicant(
                id = jsonObject.getString("user_id"),
                name = jsonObject.getString("name"),
                matchingScore = matchingScore,
                isFavorite = false // Sollicitant is standaard geen favoriet
            )
            applicants.add(applicant)
        }
        return applicants // Retourneer de lijst van sollicitanten
    }

    // Callback voor wanneer de favorietenstatus van een sollicitant wordt gewijzigd
    override fun onFavoriteStatusChanged(personId: String, isFavorite: Boolean) {
        // Werk de favorietenstatus bij in de lijst van sollicitanten
        ApplicantFavoritesManager.setFavorite(requireContext(), args.jobId, personId, isFavorite)

        // Werk de UI bij door de lijst van sollicitanten opnieuw op te halen
        fetchApplicantsForJob(args.jobId)
    }
}
