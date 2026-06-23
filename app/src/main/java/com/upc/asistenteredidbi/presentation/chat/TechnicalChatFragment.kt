package com.upc.asistenteredidbi.presentation.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.databinding.FragmentTechnicalChatBinding
import com.upc.asistenteredidbi.databinding.ItemConnectionMapRowBinding
import com.upc.asistenteredidbi.domain.model.ChatInputType
import com.upc.asistenteredidbi.domain.model.ROOT_CONNECTION_LABEL
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.getValue

@AndroidEntryPoint
class TechnicalChatFragment : Fragment() {

    private var _binding: FragmentTechnicalChatBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TechnicalChatViewModel by viewModels()

    private lateinit var messagesAdapter: ChatMessagesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTechnicalChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        messagesAdapter = ChatMessagesAdapter()
        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        binding.rvMessages.adapter = messagesAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    messagesAdapter.submitList(state.messages)
                    binding.rvMessages.scrollToPosition(state.messages.size - 1)
                    binding.progress.isVisible = state.isLoading
                    binding.tvError.isVisible = state.errorMessage != null
                    binding.tvError.text = state.errorMessage

                    if (state.isComplete) {
                       // findNavController().navigate(R.id.action_chat_to_minuta, bundleOf("evaluationId" to state.evaluationId))
                        return@collect
                    }

                    renderInputArea(state)
                }
            }
        }
    }

    private fun renderInputArea(state: TechnicalChatUiState) {
        val node = state.currentNode ?: return

        // Ocultar todo primero
        binding.tilTextInput.isVisible = false
        binding.chipGroupSingle.isVisible = false
        binding.chipGroupMulti.isVisible = false
        binding.containerConnectionMap.isVisible = false
        binding.btnCamera.isVisible = false
        binding.imgCapturePreview.isVisible = false
        binding.btnSubmit.isVisible = false

        when (node.inputType) {
            ChatInputType.TEXT, ChatInputType.NUMBER -> {
                binding.tilTextInput.isVisible = true
                binding.tilTextInput.hint = node.promptText
                binding.etTextInput.inputType = if (node.inputType == ChatInputType.NUMBER)
                    android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                else
                    android.text.InputType.TYPE_CLASS_TEXT
                binding.btnSubmit.isVisible = true
                binding.btnSubmit.setOnClickListener {
                    val text = binding.etTextInput.text?.toString().orEmpty().trim()
                    if (text.isNotBlank()) {
                        //viewModel.submitText(text)
                        binding.etTextInput.text?.clear()
                    }
                }
            }

            ChatInputType.SINGLE_SELECT -> {
                binding.chipGroupSingle.isVisible = true
                binding.chipGroupSingle.removeAllViews()
                node.selectOptions?.forEach { option ->
                    val chip = Chip(requireContext()).apply {
                        text = option
                        isCheckable = true
                        setOnCheckedChangeListener { _, checked ->
                            //if (checked) viewModel.onSingleSelectOption(option)
                        }
                    }
                    binding.chipGroupSingle.addView(chip)
                }
                binding.btnSubmit.isVisible = true
                binding.btnSubmit.setOnClickListener {
                    //viewModel.submitSingleSelect()
                }
            }

            ChatInputType.MULTI_SELECT -> {
                binding.chipGroupMulti.isVisible = true
                binding.chipGroupMulti.removeAllViews()
                node.selectOptions?.forEach { option ->
                    val chip = Chip(requireContext()).apply {
                        text = option
                        isCheckable = true
                        isChecked = state.draftSelectedOptions.contains(option)
                        setOnCheckedChangeListener { _, checked ->
                            viewModel.toggleSelectOption(option)
                        }
                    }
                    binding.chipGroupMulti.addView(chip)
                }
                binding.btnSubmit.isVisible = true
                binding.btnSubmit.setOnClickListener { viewModel.submitMultiSelect() }
            }

            ChatInputType.CONNECTION_MAP -> {
                renderConnectionMapInput(state, node.connectionMapItems.orEmpty())
            }

            ChatInputType.IMAGE_CAPTURE -> {
                binding.btnCamera.isVisible = true
                binding.btnCamera.setOnClickListener {
                    //viewModel.onCameraRequested()
                }
                if (state.draftAnswer.isNotBlank()) {
                    binding.imgCapturePreview.isVisible = true
                    binding.btnSubmit.isVisible = true
                    binding.btnSubmit.setOnClickListener {
                        //viewModel.submitImage()
                    }
                }
            }

            else -> {
                binding.btnSubmit.isVisible = true
                binding.btnSubmit.setOnClickListener {
                    //viewModel.submitText("")
                }
            }
        }
    }

    /**
     * Construye dinámicamente la lista de items del mapa de conexiones:
     * por cada equipo declarado, muestra su nombre y un Spinner
     * con opciones [Router principal, equipo1, equipo2, ...].
     */
    private fun renderConnectionMapInput(state: TechnicalChatUiState, items: List<String>) {
        binding.containerConnectionMap.isVisible = true
        binding.containerConnectionMap.removeAllViews()

        if (items.isEmpty()) {
            binding.btnSubmit.isVisible = true
            binding.btnSubmit.setOnClickListener { viewModel.submitConnectionMap() }
            return
        }

        val spinnerOptions = listOf(ROOT_CONNECTION_LABEL) + items

        items.forEach { item ->
            val rowBinding = ItemConnectionMapRowBinding.inflate(
                layoutInflater, binding.containerConnectionMap, false
            )
            rowBinding.tvItemLabel.text = item

            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, spinnerOptions)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            rowBinding.spinnerParent.adapter = adapter

            val currentParent = state.draftConnections[item] ?: ROOT_CONNECTION_LABEL
            rowBinding.spinnerParent.setSelection(spinnerOptions.indexOf(currentParent).coerceAtLeast(0))

            rowBinding.spinnerParent.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    viewModel.onConnectionItemParentChange(item, spinnerOptions[pos])
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }

            binding.containerConnectionMap.addView(rowBinding.root)
        }

        binding.btnSubmit.isVisible = true
        binding.btnSubmit.setOnClickListener { viewModel.submitConnectionMap() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/** Adapter para los mensajes del chat conversacional. */
class ChatMessagesAdapter : androidx.recyclerview.widget.ListAdapter<ChatMessage, ChatMessagesAdapter.VH>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(a: ChatMessage, b: ChatMessage) = a === b
        override fun areContentsTheSame(a: ChatMessage, b: ChatMessage) = a == b
    }
) {
    inner class VH(val tv: TextView) : androidx.recyclerview.widget.RecyclerView.ViewHolder(tv)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val tv = TextView(parent.context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(16.dpToPx(context), 4.dpToPx(context), 16.dpToPx(context), 4.dpToPx(context)) }
            setPadding(12.dpToPx(context), 10.dpToPx(context), 12.dpToPx(context), 10.dpToPx(context))
            maxWidth = 720
            textSize = 14f
            //lineSpacingMultiplier = 1.3f
        }
        return VH(tv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val msg = getItem(position)
        holder.tv.text = msg.text
        val gravity = if (msg.isFromUser) android.view.Gravity.END else android.view.Gravity.START
        (holder.tv.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
            if (msg.isFromUser) lp.marginStart = 80.dpToPx(holder.tv.context)
            else lp.marginEnd = 80.dpToPx(holder.tv.context)
        }
        holder.tv.setBackgroundColor(
            if (msg.isFromUser) 0xFF1565C0.toInt() else 0xFFF0F4FF.toInt()
        )
        holder.tv.setTextColor(
            if (msg.isFromUser) 0xFFFFFFFF.toInt() else 0xFF1A1C1E.toInt()
        )
        //(holder.tv.parent as? ViewGroup)?.gravity = gravity
    }

    private fun Int.dpToPx(ctx: android.content.Context): Int =
        (this * ctx.resources.displayMetrics.density).toInt()
}
