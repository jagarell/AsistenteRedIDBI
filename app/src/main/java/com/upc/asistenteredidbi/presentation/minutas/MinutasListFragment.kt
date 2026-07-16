package com.upc.asistenteredidbi.presentation.minutas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.upc.asistenteredidbi.databinding.FragmentMinutasListBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MinutasListFragment : Fragment() {

    private var _binding: FragmentMinutasListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MinutasListViewModel by viewModels()
    private lateinit var adapter: MinutaRecordAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMinutasListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecycler()
        setupClicks()
        observeViewModel()
        viewModel.load()
    }

    private fun setupRecycler() {
        adapter = MinutaRecordAdapter(
            onComplete = { viewModel.complete(it.id) },
            onValidate = { viewModel.validate(it.id) }
        )
        binding.rvMinutas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMinutas.adapter = adapter
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progress.isVisible = state.isLoading
                    binding.tvEmpty.isVisible =
                        !state.isLoading && state.minutas.isEmpty() && state.errorMessage == null

                    adapter.submitList(state.minutas, state.isSupervisor)

                    state.errorMessage?.let { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
