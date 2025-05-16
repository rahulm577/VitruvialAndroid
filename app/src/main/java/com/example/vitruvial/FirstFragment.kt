package com.example.vitruvial

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.vitruvial.databinding.FragmentFirstBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Reset any currently active patient
        PatientService.resetCurrentPatient()
        
        // Add new record button
        binding.buttonAddRecord.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_AddRecordFragment)
        }
        
        // View records button
        binding.buttonViewRecords.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_PatientListFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}