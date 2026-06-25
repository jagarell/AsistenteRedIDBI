package com.upc.asistenteredidbi.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.databinding.FragmentAssistantHomeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AssistantHomeFragment : Fragment() {

    private var _binding: FragmentAssistantHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AssistantHomeViewModel by viewModels()
    private lateinit var recentAdapter: HomeRecentAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAssistantHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecycler()
        setupClicks()
        loadMockData()
    }

    private fun setupRecycler() {
        recentAdapter = HomeRecentAdapter {
            Toast.makeText(requireContext(), it.title, Toast.LENGTH_SHORT).show()
        }

        binding.rvRecentes.adapter = recentAdapter
    }

    private fun setupClicks() {
        binding.btnMenu.setOnClickListener {
            Toast.makeText(requireContext(), "Menú", Toast.LENGTH_SHORT).show()
        }

        binding.btnNotification.setOnClickListener {
            Toast.makeText(requireContext(), "Notificaciones", Toast.LENGTH_SHORT).show()
        }

        binding.btnProfile.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_perfil)
        }

        binding.cardNewEvaluation.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_chat)
        }

        binding.cardContinue.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_chat)
        }

        binding.cardHistorial.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_historial)
        }

        binding.tvSeeAll.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_historial)
        }
    }

    private fun loadMockData() {
        recentAdapter.submitList(
            listOf(
                HomeRecentItem(
                    title = "Restaurante El Rincón",
                    date = "20 Jun 2026",
                    status = "Completado",
                    statusType = StatusType.COMPLETADO
                ),
                HomeRecentItem(
                    title = "Café Central Gourmet",
                    date = "18 Jun 2026",
                    status = "Borrador",
                    statusType = StatusType.BORRADOR
                ),
                HomeRecentItem(
                    title = "Pizza Palace Express",
                    date = "10 Jun 2026",
                    status = "Enviado",
                    statusType = StatusType.ENVIADO
                )
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}