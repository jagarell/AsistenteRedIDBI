package com.upc.asistenteredidbi.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.databinding.FragmentAssistantHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.getValue


@AndroidEntryPoint
class AssistantHomeFragment : Fragment() {

    private var _binding: FragmentAssistantHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AssistantHomeViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAssistantHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_perfil -> { findNavController().navigate(R.id.action_home_to_perfil); true }
                R.id.action_config -> { findNavController().navigate(R.id.action_home_to_config); true }
                else -> false
            }
        }

        binding.cardNewEvaluation.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_chat)
        }

        binding.cardHistorial.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_historial)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    /*binding.tvGreeting.text = "Hola, ${state.userName}"
                    if (state.isLoggedOut) {
                        findNavController().navigate(R.id.action_home_to_logout)
                    }*/
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}