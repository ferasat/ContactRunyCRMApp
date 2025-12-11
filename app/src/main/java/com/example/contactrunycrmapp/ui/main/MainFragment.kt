package com.example.contactrunycrmapp.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.contactrunycrmapp.R
import com.example.contactrunycrmapp.databinding.FragmentMainBinding
import com.example.contactrunycrmapp.util.PermissionHelper
import java.text.DateFormat

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SyncViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { updatePermissionStatus() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupClickListeners()
        updatePermissionStatus()
        viewModel.schedulePeriodicSync()
    }

    private fun setupObservers() {
        viewModel.lastSync.observe(viewLifecycleOwner) { timestamp ->
            val text = if (timestamp == null || timestamp == 0L) {
                getString(R.string.last_sync) + " Not synced yet"
            } else {
                getString(R.string.last_sync) + " " + DateFormat.getDateTimeInstance().format(timestamp)
            }
            binding.txtLastSync.text = text
        }
        viewModel.contactSynced.observe(viewLifecycleOwner) { count ->
            binding.txtContactStats.text = getString(R.string.stats_contacts) + " $count"
        }
        viewModel.callSynced.observe(viewLifecycleOwner) { count ->
            binding.txtCallStats.text = getString(R.string.stats_calls) + " $count"
        }
        viewModel.info.observe(viewLifecycleOwner) { message ->
            binding.txtInfo.text = message
        }
    }

    private fun setupClickListeners() {
        binding.btnRequestPermissions.setOnClickListener {
            // Explanation text is visible above this button; calling this launches the system dialog.
            permissionLauncher.launch(PermissionHelper.requiredPermissions.toTypedArray())
        }
        binding.btnSyncNow.setOnClickListener {
            if (!PermissionHelper.hasReadContacts(requireContext())) {
                Toast.makeText(requireContext(), "Contacts permission is required to sync", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            viewModel.syncNow(PermissionHelper.hasCallLog(requireContext()))
        }
        binding.btnAddContact.setOnClickListener {
            if (PermissionHelper.hasWriteContacts(requireContext())) {
                viewModel.addSampleContact("Sample CRM Contact", "+123456789", "sample@example.com")
            } else {
                Toast.makeText(requireContext(), "Write contacts permission required", Toast.LENGTH_LONG).show()
            }
        }
        binding.btnDeleteContact.setOnClickListener {
            if (PermissionHelper.hasWriteContacts(requireContext())) {
                viewModel.deleteContactByName("Sample CRM Contact")
            } else {
                Toast.makeText(requireContext(), "Write contacts permission required", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updatePermissionStatus() {
        val hasContacts = PermissionHelper.hasReadContacts(requireContext()) && PermissionHelper.hasWriteContacts(requireContext())
        val hasCalls = PermissionHelper.hasCallLog(requireContext())
        val status = buildString {
            append("Contacts: ")
            append(if (hasContacts) "granted" else "missing")
            append(" | Call log: ")
            append(if (hasCalls) "granted" else "missing (feature disabled)")
        }
        binding.txtPermissionStatus.text = status
        binding.btnSyncNow.isEnabled = hasContacts
        binding.btnAddContact.isEnabled = PermissionHelper.hasWriteContacts(requireContext())
        binding.btnDeleteContact.isEnabled = PermissionHelper.hasWriteContacts(requireContext())
        if (!hasCalls) {
            binding.txtCallStats.text = getString(R.string.stats_calls) + " (disabled)"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
