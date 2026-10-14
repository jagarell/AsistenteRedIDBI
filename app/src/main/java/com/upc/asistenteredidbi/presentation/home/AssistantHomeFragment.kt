package com.upc.asistenteredidbi.presentation.home

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
import com.upc.asistenteredidbi.MainActivity
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.databinding.FragmentAssistantHomeBinding
import com.upc.asistenteredidbi.domain.model.EvaluationStage
import com.upc.asistenteredidbi.domain.model.EvaluationSummaryItem
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
                    if (state.userFullName.isNotBlank()) {
                        binding.tvUserName.text = state.userFullName
                        // Iniciales del usuario en el círculo del perfil (antes decía "CR" fijo).
                        binding.btnProfile.text = state.userFullName.split(" ").filter { it.isNotBlank() }
                            .take(2).joinToString("") { it.first().uppercase() }
                    }
                    val roleLabel = if (state.isSupervisor) "Supervisor" else "Técnico"
                    binding.tvUserRole.text = "$roleLabel · ${state.userCompany.ifBlank { "Asistente de Red" }}"

                    // Mismos números que el prototipo: evaluaciones, propuestas (chat terminado) y validadas.
                    binding.tvStatEvaluations.text = state.evaluations.size.toString()
                    binding.tvStatProposals.text = state.evaluations.count { it.stage.isFinished }.toString()
                    binding.tvStatSent.text = state.evaluations.count { it.stage == EvaluationStage.VALIDADA }.toString()

                    recentAdapter.submitList(state.evaluations.take(4).map { it.toHomeRecentItem() })

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

    private fun EvaluationSummaryItem.toHomeRecentItem() = HomeRecentItem(
        evaluationId = id,
        title = establishmentName,
        date = com.upc.asistenteredidbi.presentation.common.formatShortDate(createdAt),
        stage = stage
    )

    private fun setupRecycler() {
        recentAdapter = HomeRecentAdapter { item ->
            // Como en el prototipo: con propuesta abre el análisis; si el chat no terminó, lo retoma.
            findNavController().navigate(
                if (item.stage.isFinished) R.id.action_home_to_minuta else R.id.action_home_to_chat,
                Bundle().apply { putString("evaluationId", item.evaluationId) }
            )
        }

        binding.rvRecentes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentes.adapter = recentAdapter
    }

    @javax.inject.Inject
    lateinit var notificationApi: com.upc.asistenteredidbi.data.remote.NotificationApiService

    override fun onResume() {
        super.onResume()
        // Punto rojo en la campana cuando hay notificaciones sin leer.
        viewLifecycleOwner.lifecycleScope.launch {
            val unread = runCatching { notificationApi.listNotifications().unread }.getOrDefault(0)
            _binding?.notificationBadge?.isVisible = unread > 0
        }
    }

    private fun setupClicks() {
        binding.btnMenu.setOnClickListener {
            (activity as MainActivity).openDrawer()
        }

        binding.btnNotification.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_notifications)
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