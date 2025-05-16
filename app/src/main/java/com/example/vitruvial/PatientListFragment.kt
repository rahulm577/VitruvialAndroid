package com.example.vitruvial

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vitruvial.databinding.FragmentPatientListBinding
import com.example.vitruvial.databinding.ItemPatientBinding
import java.text.SimpleDateFormat
import java.util.Locale

class PatientListFragment : Fragment() {

    private var _binding: FragmentPatientListBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: PatientAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientListBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        loadPatients()
        
        binding.buttonBack.setOnClickListener {
            findNavController().navigate(R.id.action_PatientListFragment_to_FirstFragment)
        }
    }
    
    private fun setupRecyclerView() {
        adapter = PatientAdapter { patientId ->
            val args = Bundle().apply {
                putString("patientId", patientId)
            }
            findNavController().navigate(R.id.action_PatientListFragment_to_PatientDetailFragment, args)
        }
        
        binding.recyclerviewPatients.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PatientListFragment.adapter
        }
    }
    
    private fun loadPatients() {
        val patients = PatientService.getAllPatientsSortedByDate()
        
        if (patients.isEmpty()) {
            binding.textviewEmptyList.visibility = View.VISIBLE
            binding.recyclerviewPatients.visibility = View.GONE
        } else {
            binding.textviewEmptyList.visibility = View.GONE
            binding.recyclerviewPatients.visibility = View.VISIBLE
            adapter.submitList(patients)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    // Inner class for the RecyclerView adapter
    private inner class PatientAdapter(
        private val onPatientClicked: (String) -> Unit
    ) : RecyclerView.Adapter<PatientAdapter.PatientViewHolder>() {
        
        private val patients = mutableListOf<PatientRecord>()
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        
        fun submitList(newPatients: List<PatientRecord>) {
            patients.clear()
            patients.addAll(newPatients)
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
            val itemBinding = ItemPatientBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return PatientViewHolder(itemBinding)
        }
        
        override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
            val patient = patients[position]
            holder.bind(patient)
        }
        
        override fun getItemCount(): Int = patients.size
        
        inner class PatientViewHolder(private val binding: ItemPatientBinding) : 
            RecyclerView.ViewHolder(binding.root) {
            
            init {
                itemView.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onPatientClicked(patients[position].patientId)
                    }
                }
            }
            
            fun bind(patient: PatientRecord) {
                binding.textviewPatientName.text = patient.patientName
                
                val dob = patient.extractedInfo["dateOfBirth"] ?: ""
                binding.textviewPatientDob.text = if (dob.isNotEmpty()) {
                    "DOB: $dob"
                } else {
                    "DOB: Unknown"
                }
                
                binding.textviewPatientDate.text = "Record created: ${dateFormat.format(patient.creationDate)}"
                
                // Check if any billing codes have been emailed and apply green tinge if so
                if (PatientService.hasEmailedBillingCodes(patient.patientId)) {
                    val emailedColor = ContextCompat.getColor(requireContext(), R.color.emailedGreen)
                    binding.root.setCardBackgroundColor(emailedColor)
                    
                    // Add "Emailed" indicator to the record
                    binding.textviewPatientEmailed.visibility = View.VISIBLE
                } else {
                    // Use light background for un-emailed records for better readability
                    val lightColor = ContextCompat.getColor(requireContext(), R.color.unemailedLight)
                    binding.root.setCardBackgroundColor(lightColor)
                    binding.textviewPatientEmailed.visibility = View.GONE
                }
            }
        }
    }
} 