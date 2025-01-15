package com.example.hrml.ui.favorite

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hrml.DataClasses.ApplicantManager.ApplicantManager
import com.example.hrml.DataClasses.VacancyManager.VacancyManager
import com.example.hrml.R
import com.example.hrml.ui.home.Applicant
import com.example.hrml.ui.home.Job
import com.example.hrml.ui.home.JobFavoritesManager
import com.example.hrml.ui.job_details.ApplicantFavoritesManager
import com.example.hrml.ui.popup_cv.PopupCV

/**
 * Fragment voor het weergeven van favoriete sollicitanten en vacatures.
 * Dit fragment toont een lijst van favoriete vacatures en sollicitanten voor een geselecteerde vacature.
 *
 * Het fragment bevat zoekfunctionaliteit en biedt de mogelijkheid om de favorietstatus van sollicitanten aan te passen.
 */
class FavoriteFragment : Fragment(), PopupCV.FavoriteStatusChangedListener {

    private lateinit var jobAdapter: FavoriteJobAdapter
    private lateinit var favoriteApplicantAdapter: FavoriteApplicantAdapter
    private val favoriteJobs = mutableListOf<FavoriteJob>()
    private val allJobs = mutableListOf<Job>()
    private lateinit var vacancyManager: VacancyManager
    private lateinit var favoritePeopleRecyclerView: RecyclerView
    private lateinit var emptyPeopleTextView: TextView
    private lateinit var jobListView: ListView
    private lateinit var emptyJobsTextView: TextView
    private var currentJobId: String? = null
    private lateinit var applicantManager: ApplicantManager

    /**
     * Maakt de view voor het fragment en initialiseert alle benodigde managers en adapters.
     *
     * @param inflater De LayoutInflater die gebruikt wordt om de view te maken.
     * @param container De ViewGroup waar de view aan toegevoegd wordt.
     * @param savedInstanceState De opgeslagen instantie staat, indien aanwezig.
     * @return De view voor het fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Initialiseer de job favorites manager
        JobFavoritesManager.initialize(requireContext())
        val view = inflater.inflate(R.layout.fragment_favorite, container, false)
        val favoriteJobIds = JobFavoritesManager.getFavoriteJobIds().toSet()

        // Observeer veranderingen in de favoriete vacatures
        JobFavoritesManager.favoriteJobsLiveData.observe(viewLifecycleOwner) { favoriteJobIds ->
            updateFavoriteJobs(favoriteJobIds)
        }

        // Haal het opgeslagen jobId op uit SharedPreferences
        retrieveJobIdFromPreferences()

        // Initialiseer applicantManager
        applicantManager = ApplicantManager(requireContext())

        // Maak de FavoriteApplicantAdapter aan met de juiste callback voor sollicitanten
        favoriteApplicantAdapter = FavoriteApplicantAdapter(
            context = requireContext(),
            persons = mutableListOf(),
            onApplicantClick = { applicant ->
                // Logica voor het tonen van de PopupCV
                val popup = if (currentJobId.isNullOrEmpty()) {
                    PopupCV.newInstanceForFavorite(personId = applicant.id)
                } else {
                    PopupCV.newInstance(jobId = currentJobId!!, personId = applicant.id)
                }
                popup.favoriteStatusChangedListener = this
                popup.show(parentFragmentManager, PopupCV.TAG)
            },
            applicantManager = applicantManager,
            jobId = currentJobId // Geef het jobId door
        )

        // Initialiseer VacancyManager voor het ophalen van vacatures
        vacancyManager = VacancyManager(requireContext())
        jobAdapter = FavoriteJobAdapter(requireContext(), favoriteJobs)
        jobListView = view.findViewById(R.id.list_favo_jobs)
        jobListView.adapter = jobAdapter

        emptyJobsTextView = view.findViewById(R.id.empty_jobs_text)
        jobListView.emptyView = emptyJobsTextView

        favoritePeopleRecyclerView = view.findViewById(R.id.list_favo_people)

        // Stel de LayoutManager in voor de RecyclerView
        val layoutManager = LinearLayoutManager(requireContext())
        favoritePeopleRecyclerView.visibility = View.GONE
        favoritePeopleRecyclerView.layoutManager = layoutManager
        favoritePeopleRecyclerView.adapter = favoriteApplicantAdapter

        // Voeg een scheidingslijn toe tussen items in de RecyclerView
        val dividerItemDecoration = DividerItemDecoration(
            favoritePeopleRecyclerView.context,
            layoutManager.orientation
        )
        favoritePeopleRecyclerView.addItemDecoration(dividerItemDecoration)

        emptyPeopleTextView = view.findViewById(R.id.empty_people_text)

        // Stel de clicklistener in voor vacatures in de lijst
        jobListView.setOnItemClickListener { _, _, position, _ ->
            val selectedJob = favoriteJobs[position]
            currentJobId = selectedJob.jobId.toString()

            // Verwijder de cache voor sollicitanten van de geselecteerde vacature
            ApplicantFavoritesManager.clearApplicantCacheForJob(currentJobId!!)

            // Bewaar het geselecteerde jobId in SharedPreferences
            saveJobIdToPreferences(currentJobId!!)

            // Maak de RecyclerView zichtbaar wanneer er op een vacature wordt geklikt
            favoritePeopleRecyclerView.visibility = View.VISIBLE

            // Haal de sollicitanten op voor de geselecteerde vacature
            updateFavoritePeopleForJob(currentJobId!!)
        }

        // Stel de zoekfunctionaliteit in
        setupSearch(view)
        fetchAndDisplayFavoriteJobs()
        updateFavoriteJobs(favoriteJobIds)
        currentJobId?.let { updateFavoritePeopleForJob(it) }

        return view
    }

    /**
     * Haalt de vacatures op en werkt de lijst van favoriete vacatures bij.
     */
    private fun fetchAndDisplayFavoriteJobs() {
        vacancyManager.fetchVacancies { jobs: List<Job>? ->
            requireActivity().runOnUiThread {
                if (jobs != null) {
                    allJobs.clear()
                    allJobs.addAll(jobs)

                    // Haal favoriete job-ID's op
                    val favoriteJobIds = JobFavoritesManager.getFavoriteJobIds().toSet()

                    // Werk de lijst met favoriete vacatures bij
                    updateFavoriteJobs(favoriteJobIds)
                } else {
                    Log.e("FavoriteFragment", "Failed to fetch jobs")
                }
            }
        }
    }

    /**
     * Werk de lijst met favoriete vacatures bij op basis van de opgehaalde job-ID's.
     *
     * @param favoriteJobIds De set van favoriete job-ID's.
     */
    private fun updateFavoriteJobs(favoriteJobIds: Set<String>) {
        favoriteJobs.clear()
        favoriteJobs.addAll(
            allJobs.filter { it.id in favoriteJobIds }
                .map { job ->
                    FavoriteJob(
                        title = job.title,
                        favoriteCount = job.reactions,
                        jobId = job.id
                    )
                }
        )
        jobAdapter.notifyDataSetChanged()
    }

    /**
     * Haalt het jobId op uit SharedPreferences.
     */
    private fun retrieveJobIdFromPreferences() {
        val sharedPref = requireContext().getSharedPreferences("FavoriteApplicants", Context.MODE_PRIVATE)
        currentJobId = sharedPref.getString("lastJobId", null) ?: "defaultJobId"
        Log.d("FavoriteFragment", "Opgehaald jobId: $currentJobId")
    }

    /**
     * Slaat het jobId op in SharedPreferences.
     *
     * @param jobId Het jobId om op te slaan.
     */
    private fun saveJobIdToPreferences(jobId: String) {
        val sharedPref = requireContext().getSharedPreferences("FavoriteApplicants", Context.MODE_PRIVATE)
        val currentJobId = sharedPref.getString("lastJobId", null)
        if (currentJobId != jobId) {
            with(sharedPref.edit()) {
                putString("lastJobId", jobId)
                apply()
            }
            Log.d("FavoriteFragment", "Saved jobId to SharedPreferences: $jobId")
        }
    }

    /**
     * Haalt de sollicitanten op voor een specifieke vacature en werkt de RecyclerView bij.
     *
     * @param jobId Het jobId waarvoor de sollicitanten opgehaald moeten worden.
     */
    private fun updateFavoritePeopleForJob(jobId: String) {
        applicantManager.fetchApplicantsForJob(jobId) { applicants ->
            requireActivity().runOnUiThread {
                if (applicants != null) {
                    val favoriteApplicants = applicants.filter { it.isFavorite }
                    Log.d("FavoriteFragment", "Filtered favorite applicants: ${favoriteApplicants.size}")

                    favoriteApplicantAdapter.updateData(favoriteApplicants)
                    favoritePeopleRecyclerView.visibility =
                        if (favoriteApplicants.isNotEmpty()) View.VISIBLE else View.GONE
                    Log.d("FavoriteFragment", "Favorite applicants: ${favoriteApplicants.size}")
                    Log.d("FavoriteFragment", "Fetching favorite people for jobId: $jobId")
                    Log.d("FavoriteFragment", "Updating adapter with applicants: $applicants")

                    emptyPeopleTextView.visibility =
                        if (favoriteApplicants.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    Log.e("FavoriteFragment", "Failed to fetch applicants for jobId: $jobId")
                }
            }
        }
    }

    /**
     * Stelt de zoekfunctionaliteit in voor vacatures.
     *
     * @param view De view van het fragment.
     */
    private fun setupSearch(view: View) {
        val searchEditText: EditText = view.findViewById(R.id.txt_favo_search)
        val searchButton: ImageView = view.findViewById(R.id.img_favo_search)

        searchButton.setOnClickListener {
            val query = searchEditText.text.toString().trim()
            performSearch(query)
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                performSearch(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    /**
     * Voert een zoekopdracht uit en filtert de lijst met favoriete vacatures.
     *
     * @param query De zoekterm.
     */
    private fun performSearch(query: String) {
        val filteredList = if (query.isEmpty()) {
            allJobs.filter { it.id in JobFavoritesManager.getFavoriteJobIds() }
        } else {
            allJobs.filter { it.title.contains(query, ignoreCase = true) && it.id in JobFavoritesManager.getFavoriteJobIds() }
        }
        favoriteJobs.clear()
        favoriteJobs.addAll(convertToFavoriteJobs(filteredList))
        jobAdapter.notifyDataSetChanged()
        currentJobId?.let { updateFavoritePeopleForJob(it) }
    }

    /**
     * Zet een lijst van Jobs om naar een lijst van FavoriteJobs.
     *
     * @param jobs De lijst van jobs om om te zetten.
     * @return De lijst van FavoriteJobs.
     */
    private fun convertToFavoriteJobs(jobs: List<Job>): List<FavoriteJob> {
        return jobs.map { job ->
            FavoriteJob(
                title = job.title,
                favoriteCount = job.reactions,
                jobId = job.id
            )
        }
    }

    /**
     * Callback voor het wijzigen van de favorietstatus van een sollicitant.
     *
     * @param personId Het ID van de sollicitant.
     * @param isFavorite De nieuwe favorietstatus.
     */
    override fun onFavoriteStatusChanged(personId: String, isFavorite: Boolean) {
        Log.d("FavoriteFragment", "Favorite status changed for personId: $personId, isFavorite: $isFavorite")
        currentJobId?.let { jobId ->
            applicantManager.toggleFavoriteStatus(Applicant(id = personId, name = "", matchingScore = 0.0, isFavorite = isFavorite), jobId)
            favoriteApplicantAdapter.updateFavoritePersonsForJob(jobId)
        }
    }
}
