package com.upc.asistenteredidbi.presentation.configuration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.databinding.FragmentConfiguracionBinding
import com.upc.asistenteredidbi.presentation.main.SessionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ConfiguracionFragment : Fragment() {

    private var _binding: FragmentConfiguracionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ConfiguracionViewModel by viewModels()
    private val sessionViewModel: SessionViewModel by activityViewModels()

    @javax.inject.Inject lateinit var api007: com.upc.asistenteredidbi.data.remote.Api007Service
    @javax.inject.Inject lateinit var sessionManager: com.upc.asistenteredidbi.data.session.SessionManager
    private var gestion: GestionCuenta? = null
    @javax.inject.Inject lateinit var conexion: com.upc.asistenteredidbi.data.session.ConnectivityMonitor
    @javax.inject.Inject lateinit var cola: com.upc.asistenteredidbi.data.session.OutboxStore

    /** Logo de la empresa: el administrador elige una imagen (PNG o JPG, hasta 2 MB). */
    private val elegirLogo = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) runCatching {
            val ctx = requireContext()
            val mime = ctx.contentResolver.getType(uri) ?: "image/png"
            val bytes = ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
            gestion?.subirLogo(bytes, if (mime == "image/jpg") "image/jpeg" else mime)
        }.onFailure { Toast.makeText(requireContext(), "No se pudo leer la imagen.", Toast.LENGTH_SHORT).show() }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentConfiguracionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        setupStaticRows()
        setupSwitches()

        binding.rowLogout.setOnClickListener {
            sessionViewModel.logout()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    state.user?.let { user ->
                        // "Mi Perfil" va sin subtítulo, como en el prototipo.
                        binding.rowMiPerfil.tvRowTitle.text = user.fullName
                        binding.rowMiPerfil.tvRowSubtitle.visibility = View.VISIBLE
                        binding.rowMiPerfil.tvRowSubtitle.text = "${when (user.role.name) { "ADMINISTRADOR" -> "Administrador"; "SUPERVISOR" -> "Supervisor"; else -> "Técnico" }} · ${user.company?.takeIf { it.isNotBlank() } ?: "IDBI"}"
                        binding.rowCorreo.tvRowSubtitle.visibility = View.VISIBLE
                        binding.rowCorreo.tvRowSubtitle.text = user.email
                    }

                    binding.switchTwoFactor.isChecked = state.twoFactorAuth
                    binding.switchPush.isChecked = state.pushNotifications
                    binding.switchEmail.isChecked = state.emailNotifications
                    binding.switchSystemAlerts.isChecked = state.systemAlerts
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val admin = runCatching { sessionManager.getRole() }.getOrNull() == com.upc.asistenteredidbi.domain.model.Role.ADMINISTRADOR
            gestion = GestionCuenta(requireContext(), binding.gestionContainer, api007, viewLifecycleOwner.lifecycleScope, admin) { elegirLogo.launch("image/*") }.also { it.cargar() }
        }

        sincronizacion()
        viewModel.load()
    }

    /** «Sincronización» (doc 0.0.7 §5 y §47): estado, última subida, por subir y «Sincronizar ahora». */
    private fun sincronizacion() {
        val ctx = requireContext()
        val padre = binding.gestionContainer.parent as ViewGroup
        val tarjeta = com.upc.asistenteredidbi.presentation.reportes.Ui007.card(ctx)
        val titulo = com.upc.asistenteredidbi.presentation.reportes.Ui007.text(ctx, "MODO SIN CONEXIÓN Y SINCRONIZACIÓN", 11f, com.upc.asistenteredidbi.presentation.reportes.Ui007.GREY, true)
        val estado = com.upc.asistenteredidbi.presentation.reportes.Ui007.text(ctx, "", 14f, com.upc.asistenteredidbi.presentation.reportes.Ui007.INK, true)
        val detalle = com.upc.asistenteredidbi.presentation.reportes.Ui007.text(ctx, "", 12.5f, com.upc.asistenteredidbi.presentation.reportes.Ui007.GREY)
        tarjeta.addView(estado); tarjeta.addView(detalle)
        tarjeta.addView(com.upc.asistenteredidbi.presentation.reportes.Ui007.button(ctx, "Sincronizar ahora", false) {
            if (!conexion.online.value) android.widget.Toast.makeText(ctx, "Sigues sin conexión.", android.widget.Toast.LENGTH_SHORT).show()
            else viewLifecycleOwner.lifecycleScope.launch {
                val n = cola.subir()
                android.widget.Toast.makeText(ctx, if (n > 0) "Se subieron $n cambios." else "No había nada por subir.", android.widget.Toast.LENGTH_SHORT).show()
            }
        })
        val i = padre.indexOfChild(binding.gestionContainer)
        padre.addView(titulo, i + 1, android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = (18 * resources.displayMetrics.density).toInt(); marginStart = (4 * resources.displayMetrics.density).toInt() })
        padre.addView(tarjeta, i + 2)
        fun pintar() {
            val n = cola.cola.value.size
            estado.text = if (conexion.online.value) "✓ Sincronizado con la cuenta" else "📵 Sin conexión · se guarda en el celular"
            val ultima = cola.ultimaSubida.value?.let { java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale("es", "PE")).format(java.util.Date(it)) } ?: "—"
            detalle.text = "Por subir: $n · Última subida: $ultima\nLas fotos se suben con WiFi o con datos móviles."
        }
        pintar()
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { conexion.online.collect { pintar() } }
                launch { cola.cola.collect { pintar() } }
                launch { cola.ultimaSubida.collect { pintar() } }
            }
        }
    }

    private fun simplificarMiCuenta() {
        // La tarjeta de arriba es el usuario (abre Mi perfil): se quitan las filas Empresa y Correo.
        (binding.rowMiPerfil.root.parent as ViewGroup).let { card ->
            for (i in 0 until card.childCount) if (card.getChildAt(i) !== binding.rowMiPerfil.root) card.getChildAt(i).visibility = View.GONE
        }
        // «Apariencia» se quitó: el Modo noche está en el menú lateral, debajo de Mi Cuenta.
        (binding.switchDarkMode.parent as View).visibility = View.GONE
        // Soporte: solo Ayuda y Versión.
        (binding.rowIdioma.root.parent as ViewGroup).let { card ->
            val i = card.indexOfChild(binding.rowIdioma.root)
            binding.rowIdioma.root.visibility = View.GONE
            card.getChildAt(i + 1)?.visibility = View.GONE
        }
    }

    private fun setupStaticRows() {
        simplificarMiCuenta()
        binding.rowMiPerfil.apply {
            ivRowIcon.setImageResource(R.drawable.ic_account_circle)
            tvRowTitle.text = "Mi Cuenta"
            root.setOnClickListener { findNavController().navigate(R.id.action_config_to_perfil) }
        }
        binding.rowEmpresa.apply {
            ivRowIcon.setImageResource(R.drawable.ic_building)
            tvRowTitle.text = "Empresa"
        }
        binding.rowCorreo.apply {
            ivRowIcon.setImageResource(R.drawable.ic_email)
            tvRowTitle.text = "Correo Electrónico"
        }
        binding.rowCambiarPassword.apply {
            ivRowIcon.setImageResource(R.drawable.ic_lock_outline)
            tvRowTitle.text = "Cambiar Contraseña"
            root.setOnClickListener { cambiarClave() }
        }
        binding.rowDispositivos.apply {
            ivRowIcon.setImageResource(R.drawable.ic_devices)
            tvRowTitle.text = "Dispositivos Activos"
        }
        binding.rowIdioma.apply {
            ivRowIcon.setImageResource(R.drawable.ic_language)
            tvRowTitle.text = "Idioma"
            tvRowSubtitle.visibility = View.VISIBLE
            tvRowSubtitle.text = "Español"
            root.setOnClickListener { showComingSoon() }
        }
        binding.rowVersion.apply {
            ivRowIcon.setImageResource(R.drawable.ic_info)
            tvRowTitle.text = "Versión"
            tvRowSubtitle.visibility = View.VISIBLE
            tvRowSubtitle.text = runCatching {
                requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
            }.getOrNull() ?: "—"
            ivChevron.visibility = View.INVISIBLE
        }
        binding.rowSoporte.apply {
            ivRowIcon.setImageResource(R.drawable.ic_help)
            tvRowTitle.text = "Ayuda"
            tvRowSubtitle.visibility = View.VISIBLE
            tvRowSubtitle.text = "Pregúntale al Asistente IA"
            root.setOnClickListener { findNavController().navigate(R.id.asistenteFragment) }
        }
    }

    private fun setupSwitches() {
        binding.switchDarkMode.isChecked = (resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        binding.switchDarkMode.setOnCheckedChangeListener { _, checked ->
            viewModel.setDarkMode(checked)
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                if (checked) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            )
        }
        // QA F5: «Autenticación de 2 factores», «Push», «Email» y «Alertas de sistema» se guardaban en el celular y nada los leía
        // (el de 2 factores daba una falsa sensación de seguridad). Se quitan; lo que sí funciona es «Avisarme cuando…» (se guarda en la
        // cuenta y el servidor lo respeta al enviar), con un interruptor general que apaga o enciende todo.
        (binding.switchTwoFactor.parent as View).visibility = View.GONE
        (binding.rowCambiarPassword.root.parent as ViewGroup).let { card ->
            val i = card.indexOfChild(binding.switchTwoFactor.parent as View)
            card.getChildAt(i - 1)?.visibility = View.GONE   // línea que los separaba
        }
        ocultarSwitchesSinEfecto()
        reordenarSecciones()
        avisosEnCuenta()
    }

    /** Deja en la tarjeta de notificaciones solo el Modo noche (los demás interruptores no hacían nada). */
    private fun ocultarSwitchesSinEfecto() {
        val tarjeta = binding.switchPush.parent.parent as ViewGroup
        for (k in 0 until tarjeta.childCount) {
            val h = tarjeta.getChildAt(k)
            if (h.findViewById<View>(R.id.switchDarkMode) == null) h.visibility = View.GONE
        }
    }

    /** Orden del simulador (QA G11): Notificaciones va antes que Seguridad. */
    private fun reordenarSecciones() {
        val padre = binding.rowCambiarPassword.root.parent.parent as? ViewGroup ?: return
        val segLabel = (0 until padre.childCount).map { padre.getChildAt(it) }
            .firstOrNull { it is TextView && it.text.toString() == "SEGURIDAD" } ?: return
        val notLabel = (0 until padre.childCount).map { padre.getChildAt(it) }
            .firstOrNull { it is TextView && it.text.toString() == "NOTIFICACIONES" } ?: return
        val notCard = padre.getChildAt(padre.indexOfChild(notLabel) + 1)
        padre.removeView(notLabel); padre.removeView(notCard)
        val i = padre.indexOfChild(segLabel)
        padre.addView(notLabel, i); padre.addView(notCard, i + 1)
        this.tarjetaNotificaciones = notCard as? ViewGroup
    }

    private var tarjetaNotificaciones: ViewGroup? = null

    /** «Recibir notificaciones» (apaga o enciende todo) y la lista «Avisarme cuando…» (QA G13), guardadas en la cuenta. */
    private fun avisosEnCuenta() {
        val ctx = requireContext()
        val tarjeta = tarjetaNotificaciones ?: return
        val dens = resources.displayMetrics.density
        fun px(v: Int) = (v * dens).toInt()
        val cont = android.widget.LinearLayout(ctx).apply { orientation = android.widget.LinearLayout.VERTICAL }
        tarjeta.addView(cont, 0)
        viewLifecycleOwner.lifecycleScope.launch {
            val r = runCatching { api007.avisos() }.getOrNull()
            if (_binding == null) return@launch
            @Suppress("UNCHECKED_CAST")
            val ops = (r?.get("opciones") as? List<Map<String, Any?>>).orEmpty()
            if (ops.isEmpty()) {
                cont.addView(TextView(ctx).apply { text = "No se pudieron cargar tus avisos."; textSize = 13f; setPadding(px(16), px(12), px(16), px(12)) })
                return@launch
            }
            val switches = mutableListOf<Pair<String, androidx.appcompat.widget.SwitchCompat>>()
            var masivo = false
            fun fila(texto: String, activo: Boolean, grueso: Boolean, alCambiar: (Boolean) -> Unit): androidx.appcompat.widget.SwitchCompat {
                val sw = androidx.appcompat.widget.SwitchCompat(ctx).apply { isChecked = activo }
                val f = android.widget.LinearLayout(ctx).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(px(16), px(10), px(16), px(10))
                    addView(TextView(ctx).apply { this.text = texto; textSize = if (grueso) 15f else 13.5f; if (grueso) typeface = android.graphics.Typeface.DEFAULT_BOLD }, android.widget.LinearLayout.LayoutParams(0, -2, 1f))
                    addView(sw)
                }
                cont.addView(f)
                sw.setOnCheckedChangeListener { _, on -> if (!masivo) alCambiar(on) }
                return sw
            }
            val general = fila("Recibir notificaciones", ops.any { it["activo"] == true }, true) { on ->
                masivo = true
                switches.forEach { it.second.isChecked = on }
                masivo = false
                viewLifecycleOwner.lifecycleScope.launch {
                    runCatching { api007.avisosGuardar(switches.associate { it.first to on }) }
                        .onFailure { Toast.makeText(ctx, "No se pudo guardar.", Toast.LENGTH_SHORT).show() }
                }
            }
            cont.addView(TextView(ctx).apply { text = "Avisarme cuando…"; textSize = 12f; setPadding(px(16), px(4), px(16), px(2)) })
            ops.forEach { o ->
                val clave = o["clave"].toString()
                val sw = fila(o["texto"].toString(), o["activo"] == true, false) { on ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        runCatching { api007.avisosGuardar(mapOf(clave to on)) }.onFailure { Toast.makeText(ctx, "No se pudo guardar.", Toast.LENGTH_SHORT).show() }
                    }
                    masivo = true; general.isChecked = switches.any { it.second.isChecked } || on; masivo = false
                }
                switches += clave to sw
            }
        }
    }

    /** Cambiar contraseña con la sesión iniciada: pide la actual y la nueva (mínimo 8 caracteres). */
    private fun cambiarClave() {
        val ctx = requireContext()
        val dens = resources.displayMetrics.density
        val col = android.widget.LinearLayout(ctx).apply { orientation = android.widget.LinearLayout.VERTICAL; setPadding((20 * dens).toInt(), (8 * dens).toInt(), (20 * dens).toInt(), 0) }
        fun campo(pista: String) = android.widget.EditText(ctx).apply {
            hint = pista; inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }.also { col.addView(it) }
        val actual = campo("Contraseña actual"); val nueva = campo("Nueva contraseña (mínimo 8)"); val repite = campo("Repite la nueva contraseña")
        androidx.appcompat.app.AlertDialog.Builder(ctx).setTitle("Cambiar contraseña").setView(col)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                val a = actual.text.toString(); val n = nueva.text.toString()
                when {
                    a.isBlank() -> Toast.makeText(ctx, "Escribe tu contraseña actual.", Toast.LENGTH_SHORT).show()
                    n.length < 8 -> Toast.makeText(ctx, "La nueva contraseña debe tener al menos 8 caracteres.", Toast.LENGTH_SHORT).show()
                    n != repite.text.toString() -> Toast.makeText(ctx, "Las contraseñas nuevas no coinciden.", Toast.LENGTH_SHORT).show()
                    else -> viewLifecycleOwner.lifecycleScope.launch {
                        runCatching { api007.cambiarClave(mapOf("currentPassword" to a, "newPassword" to n)) }
                            .onSuccess { Toast.makeText(ctx, "Contraseña actualizada.", Toast.LENGTH_LONG).show() }
                            .onFailure { e ->
                                val msg = (e as? retrofit2.HttpException)?.response()?.errorBody()?.string()
                                    ?.let { Regex(""""message"\s*:\s*"([^"]+)"""").find(it)?.groupValues?.get(1) }
                                Toast.makeText(ctx, msg ?: "No se pudo cambiar la contraseña.", Toast.LENGTH_LONG).show()
                            }
                    }
                }
            }.show()
    }

    private fun showComingSoon() {
        Toast.makeText(requireContext(), "Próximamente", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
