package com.example.hrml.ui.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hrml.DataClasses.JobSharedPrefManager
import com.example.hrml.DataClasses.VacancyManager.VacancyManager
import com.example.hrml.R
import com.example.hrml.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: JobAdapter
    private lateinit var vacancyManager: VacancyManager

    private val originalJobList = mutableListOf<Job>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        JobFavoritesManager.initialize(requireContext())

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialiseer de VacancyManager om vacatures op te halen
        vacancyManager = VacancyManager(requireContext())

        // Initialiseer de adapter met een click listener
        adapter = JobAdapter(originalJobList) { job, view ->
            // Controleer op welk UI-element er is geklikt
            if (view.id == R.id.starIcon) { // Stel in dat de favorietenknop een id heeft zoals 'favoriteIcon'
                // Wissel de favorietenstatus
                val isFavorite = !JobFavoritesManager.isFavorite(job.id)
                JobFavoritesManager.setFavorite(job.id, isFavorite, requireContext())
                adapter.notifyDataSetChanged() // Update de lijstweergave
            } else {
                // Navigeer naar het JobDetails-fragment
                val action = HomeFragmentDirections.actionHomeFragmentToJobDetails(
                    jobId = job.id,
                    jobTitle = job.title,
                    jobDescription = job.description
                )
                findNavController().navigate(action)
            }
        }

        // Stel de LayoutManager in voor de RecyclerView
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewJobs.layoutManager = layoutManager
        binding.recyclerViewJobs.adapter = adapter

        // Voeg een scheidingslijn toe tussen items in de RecyclerView
        val dividerItemDecoration = DividerItemDecoration(
            binding.recyclerViewJobs.context,
            layoutManager.orientation
        )
        dividerItemDecoration.setDrawable(resources.getDrawable(R.drawable.divider, null))
        binding.recyclerViewJobs.addItemDecoration(dividerItemDecoration)

        // Haal vacatures op via VacancyManager
        vacancyManager.fetchVacancies { vacancies ->
            requireActivity().runOnUiThread {
                if (vacancies != null) {
                    // Maak gebruik van JobSharedPrefManager om de favorietenstatus op te halen
                    val sharedPrefManager = JobSharedPrefManager(requireContext())

                    println("Vacancies fetched: ${vacancies.size}")
                    // Voeg de vacatures toe aan de originele lijst en update de isFavorite status
                    originalJobList.clear()
                    originalJobList.addAll(
                        vacancies.map { job ->
                            val isFavorite = sharedPrefManager.isJobFavorite(job.id)
                            println("Job: ${job.title}, isFavorite: $isFavorite")
                            job.copy(isFavorite = isFavorite)
                        }
                    )
                    // Werk de adapter bij om de lijst van vacatures weer te geven
                    adapter.notifyDataSetChanged()

                    // Bereken het totale aantal vacatures en reacties (sollicitaties)
                    val totalVacancies = vacancies.size
                    val totalApplicants = vacancies.sumOf { it.reactions }

                    // Update de UI met de totaal aantallen
                    binding.txtOpenings.text = getString(R.string.txt_openings, totalVacancies)
                    binding.txtResponse.text = getString(R.string.txt_responses, totalApplicants)
                }
            }
        }

        // Stel de zoekfunctionaliteit in
        setupSearch()
        return root
    }

    /**
     * Stel de zoekfunctionaliteit in voor de vacatures.
     * Dit zorgt ervoor dat de lijst van vacatures gefilterd wordt op basis van de zoekopdracht.
     */
    private fun setupSearch() {
        val searchEditText = binding.txtSearch
        val searchButton = binding.imgSearch

        // Luister naar klikken op de zoekknop
        searchButton.setOnClickListener {
            val query = searchEditText.text.toString().trim()
            performSearch(query)
        }

        // Luister naar tekstwijzigingen in het zoekveld
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                performSearch(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    /**
     * Voer een zoekopdracht uit en filter de vacatures op basis van de ingevoerde zoekterm.
     *
     * @param query De zoekterm die is ingevoerd door de gebruiker.
     */
    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            // Als er geen zoekterm is, toon dan de volledige lijst
            adapter.updateData(originalJobList)
        } else {
            // Filter de vacatures op basis van de zoekterm in de titel
            val filteredList = originalJobList.filter { it.title.contains(query, ignoreCase = true) }
            adapter.updateData(filteredList)
        }
    }

    // Maak de binding vrij wanneer het fragment wordt vernietigd
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
