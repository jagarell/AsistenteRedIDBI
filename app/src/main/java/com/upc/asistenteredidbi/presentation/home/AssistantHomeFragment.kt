package com.upc.asistenteredidbi.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.upc.asistenteredidbi.MainActivity
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.databinding.FragmentAssistantHomeBinding
import com.upc.asistenteredidbi.domain.model.MinutaRecord
import com.upc.asistenteredidbi.domain.model.MinutaRecordStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
        observeViewModel()
        viewModel.loadHome()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.userFirstName.isNotBlank()) {
                        binding.tvUserName.text = state.userFirstName
                    }
                    if (state.userCompany.isNotBlank()) {
                        binding.tvUserRole.text = state.userCompany
                    }

                    state.stats?.let { stats ->
                        binding.tvStatEvaluations.text = stats.totalEvaluations.toString()
                        binding.tvStatProposals.text = stats.totalProposals.toString()
                        binding.tvStatSent.text = stats.sentProposals.toString()
                    }

                    recentAdapter.submitList(state.recentMinutas.map { it.toHomeRecentItem() })

                    binding.cardNewEvaluation.isEnabled = !state.isStartingEvaluation

                    state.newEvaluationId?.let { evaluationId ->
                        viewModel.consumeNewEvaluationId()
                        findNavController().navigate(
                            R.id.action_home_to_chat,
                            Bundle().apply { putString("evaluationId", evaluationId.toString()) }
                        )
                    }

                    state.resumeEvaluationId?.let { evaluationId ->
                        viewModel.consumeResumeEvaluationId()
                        findNavController().navigate(
                            R.id.action_home_to_chat,
                            Bundle().apply { putString("evaluationId", evaluationId.toString()) }
                        )
                    }

                    if (state.confirmDiscardPrevious) showDiscardDialog()

                    state.errorMessage?.let { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    private fun MinutaRecord.toHomeRecentItem(): HomeRecentItem {
        val statusType = when (status) {
            MinutaRecordStatus.BORRADOR -> StatusType.BORRADOR
            MinutaRecordStatus.COMPLETA -> StatusType.ENVIADO
            MinutaRecordStatus.VALIDADA -> StatusType.COMPLETADO
        }
        return HomeRecentItem(
            title = clientName,
            date = com.upc.asistenteredidbi.presentation.common.formatShortDate(createdAt),
            status = statusType.name.lowercase().replaceFirstChar { it.uppercase() },
            statusType = statusType
        )
    }

    private fun setupRecycler() {
        recentAdapter = HomeRecentAdapter {
            Toast.makeText(requireContext(), it.title, Toast.LENGTH_SHORT).show()
        }

        binding.rvRecentes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentes.adapter = recentAdapter
    }

    private fun setupClicks() {
        binding.btnMenu.setOnClickListener {
            (activity as MainActivity).openDrawer()
        }

        binding.btnNotification.setOnClickListener {
            Toast.makeText(requireContext(), "Notificaciones", Toast.LENGTH_SHORT).show()
        }

        binding.btnProfile.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_perfil)
        }

        binding.cardNewEvaluation.setOnClickListener { requestNewEvaluation() }

        // Retoma el chat donde se quedó; solo "Nueva Evaluación" lo borra.
        binding.cardContinue.setOnClickListener { viewModel.continuePrevious() }

        binding.cardHistorial.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_historial)
        }

        binding.tvSeeAll.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_historial)
        }
    }

    /** También lo usa el menú lateral. */
    fun requestNewEvaluation() = viewModel.requestNewEvaluation()

    private var discardDialog: androidx.appcompat.app.AlertDialog? = null

    private fun showDiscardDialog() {
        if (discardDialog?.isShowing == true) return
        discardDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Empezar una evaluación nueva")
            .setMessage("Tienes una evaluación previa guardada. Si empiezas una nueva, el chat anterior se borra. ¿Quieres continuar la anterior o empezar de cero?")
            .setPositiveButton("Empezar nueva") { _, _ -> viewModel.discardPreviousAndStartNew() }
            .setNeutralButton("Continuar la anterior") { _, _ ->
                viewModel.cancelDiscardPrevious()
                viewModel.continuePrevious()
            }
            .setNegativeButton("Cancelar") { _, _ -> viewModel.cancelDiscardPrevious() }
            .setOnCancelListener { viewModel.cancelDiscardPrevious() }
            .show()
    }

    override fun onDestroyView() {
        discardDialog?.dismiss()
        discardDialog = null
        super.onDestroyView()
        _binding = null
    }
}