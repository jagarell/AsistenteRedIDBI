package com.upc.asistenteredidbi.presentation.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.databinding.FragmentTechnicalChatBinding
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.fragment.findNavController

@AndroidEntryPoint
class TechnicalChatFragment : Fragment() {

    private var _binding: FragmentTechnicalChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var messagesAdapter: ChatMessagesAdapter
    private val messages = mutableListOf<ChatMessage>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTechnicalChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecycler()
        setupClicks()
        loadInitialMessage()
    }

    private fun setupRecycler() {
        messagesAdapter = ChatMessagesAdapter()
        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMessages.adapter = messagesAdapter
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSubmit.setOnClickListener {
            val answer = binding.etTextInput.text?.toString().orEmpty().trim()
            if (answer.isNotEmpty()) {
                messages.add(ChatMessage(answer, true))
                messagesAdapter.submitList(messages.toList())
                binding.etTextInput.text?.clear()
                binding.rvMessages.scrollToPosition(messages.lastIndex)
            }
        }

        binding.btnGoEvidence.setOnClickListener {
            findNavController().navigate(
                R.id.action_chat_to_evidence,
                bundleOf("evaluationId" to "demo-evaluation-001")
            )
        }
    }

    private fun loadInitialMessage() {
        messages.clear()
        messages.add(
            ChatMessage(
                "¡Hola! Soy tu Asistente de Red con IA.\nComenzaremos con la evaluación de infraestructura. ¿Cuál es el nombre del restaurante a evaluar?",
                false
            )
        )
        messagesAdapter.submitList(messages.toList())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}