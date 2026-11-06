package com.upc.asistenteredidbi.presentation.chat

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.upc.asistenteredidbi.data.repository.Asistente007Repository
import com.upc.asistenteredidbi.databinding.SheetChatHelpBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * Ayuda del Chat IA (0.0.7): el técnico pregunta con texto o dictado y responde ChatGPT con las reglas de la empresa
 * (mismo endpoint que el Asistente IA). Lo que se conversa aquí no entra a la evaluación ni a la minuta.
 */
@AndroidEntryPoint
class ChatHelpSheet : BottomSheetDialogFragment() {

    @Inject lateinit var repo: Asistente007Repository

    private var _binding: SheetChatHelpBinding? = null
    private val binding get() = _binding!!
    private var busy = false

    private val dictate = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { binding.etHelp.setText(it) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SheetChatHelpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val question = arguments?.getString(ARG_QUESTION).orEmpty()
        bubble(
            "Escríbeme lo que quieras. Puedo:\n• explicarte cualquier cosa de la visita (equipos, fotos, pasos);\n" +
                "• tomar los datos que me cuentes y pasarlos a la minuta por ti." +
                if (question.isNotBlank()) "\n\nVas en: $question" else "",
            mine = false
        )
        binding.btnHelpBack.setOnClickListener { dismiss() }
        binding.btnHelpSend.setOnClickListener { send(question) }
        binding.etHelp.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_SEND) { send(question); true } else false
        }
        binding.btnHelpMic.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("es", "PE"))
            }
            try { dictate.launch(intent) } catch (_: ActivityNotFoundException) {
                Toast.makeText(requireContext(), "Este celular no tiene dictado por voz.", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnHelpCamera.setOnClickListener {
            Toast.makeText(requireContext(), "Enviar fotos a la Ayuda llegará en una próxima versión. Por ahora descríbelo con texto.", Toast.LENGTH_LONG).show()
        }
    }

    private fun send(question: String) {
        val text = binding.etHelp.text?.toString()?.trim().orEmpty()
        if (text.isEmpty() || busy) return
        binding.etHelp.text?.clear()
        bubble(text, mine = true)
        busy = true
        val waiting = bubble("Escribiendo…", mine = false)
        viewLifecycleOwner.lifecycleScope.launch {
            val context = if (question.isNotBlank()) "[Ayuda en la visita; pregunta actual: «$question»] " else ""
            val reply = runCatching { repo.mensaje(context + text) }.getOrNull()
            busy = false
            if (_binding == null) return@launch
            waiting.text = when {
                reply == null -> "No pude conectarme. Revisa tu conexión e inténtalo de nuevo."
                else -> reply["texto"]?.toString()?.takeIf { it.isNotBlank() } ?: "No tengo una respuesta para eso. Prueba con otras palabras."
            }
            scrollDown()
        }
    }

    private fun bubble(text: String, mine: Boolean): TextView {
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val tv = TextView(requireContext()).apply {
            this.text = text
            textSize = 15f
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setTextColor(if (mine) Color.WHITE else Color.parseColor("#1F2937"))
            setBackgroundColor(Color.TRANSPARENT)
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(if (mine) Color.parseColor("#2F63B8") else Color.parseColor("#F1F5FB"))
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = if (mine) Gravity.END else Gravity.START
                topMargin = dp(8)
                if (mine) marginStart = dp(48) else marginEnd = dp(48)
            }
        }
        binding.helpMessages.addView(tv)
        scrollDown()
        return tv
    }

    private fun scrollDown() = binding.helpScroll.post { binding.helpScroll.fullScroll(View.FOCUS_DOWN) }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "ChatHelpSheet"
        private const val ARG_QUESTION = "question"
        fun newInstance(question: String) = ChatHelpSheet().apply { arguments = Bundle().apply { putString(ARG_QUESTION, question) } }
    }
}
