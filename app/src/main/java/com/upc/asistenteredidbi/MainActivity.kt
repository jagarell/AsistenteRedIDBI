package com.upc.asistenteredidbi

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.upc.asistenteredidbi.data.notification.NotificationTokenSync
import com.upc.asistenteredidbi.databinding.ActivityMainBinding
import com.upc.asistenteredidbi.presentation.main.SessionDestination
import com.upc.asistenteredidbi.presentation.main.SessionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val sessionViewModel: SessionViewModel by viewModels()

    @Inject
    lateinit var notificationTokenSync: NotificationTokenSync

    @Inject
    lateinit var notificationApi: com.upc.asistenteredidbi.data.remote.NotificationApiService

    @Inject
    lateinit var api007: com.upc.asistenteredidbi.data.remote.Api007Service

    @javax.inject.Inject
    lateinit var conexion: com.upc.asistenteredidbi.data.session.ConnectivityMonitor

    @javax.inject.Inject
    lateinit var cola: com.upc.asistenteredidbi.data.session.OutboxStore

    @javax.inject.Inject
    lateinit var copiaLocal: com.upc.asistenteredidbi.data.session.OfflineCacheInterceptor

    @javax.inject.Inject
    lateinit var fotoPerfil: com.upc.asistenteredidbi.presentation.perfil.FotoPerfilStore
    private var fondoAvatarMenu: android.graphics.drawable.Drawable? = null

    @Inject
    lateinit var appPreferences: com.upc.asistenteredidbi.data.session.AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.upc.asistenteredidbi.presentation.common.NightColors.sync(resources.configuration)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        aplicarBarrasDelSistema()

        setupNavigation()
        setupDrawer()
        observeSession()

        onBackPressedDispatcher.addCallback(this) {
            if (binding.drawerLayoutRoot.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayoutRoot.closeDrawer(GravityCompat.START)
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }

        if (savedInstanceState == null) {
            sessionViewModel.checkSession()
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        navController = navHostFragment.navController
        // El menú lateral solo se abre en Inicio: en Iniciar sesión, el Chat IA o la Propuesta el deslizamiento desde el borde
        // lo abría y sus opciones no llevaban a ningún lado (QA F2).
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.drawerLayoutRoot.setDrawerLockMode(
                if (destination.id == R.id.assistantHomeFragment) DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED
            )
        }
    }

    /** Punto rojo y «Nuevo» en Notificaciones si hay avisos sin leer; el administrador ve su plan («Empresa · N de 10 usuarios»). */
    private fun refrescarMenu() {
        lifecycleScope.launch {
            val sinLeer = runCatching { notificationApi.listNotifications().unread }.getOrDefault(0)
            val d = binding.navDrawerContent
            d.navNotifDot.visibility = if (sinLeer > 0) View.VISIBLE else View.GONE
            d.navNotifBadge.visibility = if (sinLeer > 0) View.VISIBLE else View.GONE
            if (d.navPreguntas.visibility == View.VISIBLE) {
                val usuarios = (runCatching { api007.panel(14)["usuarios"] }.getOrNull() as? Number)?.toInt()
                if (usuarios != null) {
                    d.navPlan.visibility = View.VISIBLE
                    d.tvPlanTexto.text = "Empresa · $usuarios de 10 usuarios"
                    d.pbPlan.progress = usuarios.coerceAtMost(10)
                }
            } else d.navPlan.visibility = View.GONE
        }
    }

    /** Plan de la empresa (solo administrador): plan actual, usuarios usados y la lista de personas del plan. */
    private fun mostrarPlan() {
        lifecycleScope.launch {
            val personas = runCatching { api007.equipo() }.getOrDefault(emptyList())
            val dp = resources.displayMetrics.density
            val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(this@MainActivity)
            val col = android.widget.LinearLayout(this@MainActivity).apply { orientation = android.widget.LinearLayout.VERTICAL; setPadding((20 * dp).toInt(), (20 * dp).toInt(), (20 * dp).toInt(), (24 * dp).toInt()) }
            fun t(texto: String, size: Float, bold: Boolean = false, color: Int = 0xFF1F2937.toInt()) = android.widget.TextView(this@MainActivity).apply {
                text = texto; textSize = size; setTextColor(color); if (bold) typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            col.addView(t("Plan", 20f, true))
            col.addView(t("Empresa · plan actual", 15f, true, 0xFF1565C0.toInt()).apply { setPadding(0, (6 * dp).toInt(), 0, 0) })
            col.addView(t("${personas.size} de 10 usuarios (cuenta al administrador)", 14f, false, 0xFF6B7280.toInt()))
            col.addView(android.widget.ProgressBar(this@MainActivity, null, android.R.attr.progressBarStyleHorizontal).apply { max = 10; progress = personas.size.coerceAtMost(10); setPadding(0, (8 * dp).toInt(), 0, (8 * dp).toInt()) })
            col.addView(t("Usuarios del plan", 14f, true).apply { setPadding(0, (8 * dp).toInt(), 0, (4 * dp).toInt()) })
            personas.take(30).forEach { p ->
                val rol = p["rol"].toString().lowercase().replaceFirstChar { c -> c.uppercase() }
                val sup = (p["supervisorNombre"] as? String)?.let { " · supervisor: $it" }.orEmpty()
                val sinAcceso = p["conAcceso"] == false
                val estado = if (sinAcceso) "  — Sin acceso" else if (p["pidioCodigo"] == true) "  — Pidió código" else ""
                col.addView(t("• ${p["nombre"]} · $rol$sup$estado", 13.5f, false, if (sinAcceso) 0xFFB91C1C.toInt() else if (p["pidioCodigo"] == true) 0xFF8A5A00.toInt() else 0xFF1F2937.toInt()).apply { setPadding(0, (3 * dp).toInt(), 0, 0) })
            }
            // «Pedir más usuarios» y «Cambiar a un plan superior» no tenían acción: se quitan hasta que existan los planes.
            col.addView(t("Para agregar personas, entra a Equipo y accesos.", 12f, false, 0xFF6B7280.toInt()).apply { setPadding(0, (12 * dp).toInt(), 0, 0) })
            sheet.setContentView(android.widget.ScrollView(this@MainActivity).apply { addView(col) })
            sheet.show()
        }
    }

    fun openDrawer() {
        sessionViewModel.loadDrawerProfile()
        refrescarMenu()
        binding.drawerLayoutRoot.openDrawer(GravityCompat.START)
    }

    private fun setupDrawer() {
        val drawer = binding.navDrawerContent

        // Versión real de la app en el pie del menú (antes decía "v1.0" fijo).
        val versionName = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull().orEmpty()
        drawer.tvDrawerVersion.text = "v$versionName · Asistente de Red"

        drawer.btnCloseDrawer.setOnClickListener { closeDrawer() }
        // Al abrirlo deslizando (no con el botón) el menú salía sin nombre ni opciones de rol: se cargan al abrirse.
        binding.drawerLayoutRoot.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) { sessionViewModel.loadDrawerProfile(); refrescarMenu() }
        })
        // Modo noche: mismo valor que el interruptor de Perfil y configuración.
        val isNight = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        drawer.switchNightDrawer.isChecked = isNight
        drawer.switchNightDrawer.setOnCheckedChangeListener { _, checked ->
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { appPreferences.setDarkMode(checked) } // fuera del ciclo: la actividad se recrea
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                if (checked) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            )
        }
        drawer.navLogout.setOnClickListener {
            closeDrawer()
            fotoPerfil.limpiar()
            copiaLocal.limpiar()
            sessionViewModel.logout()
        }

        drawer.navInicio.setOnClickListener { closeDrawer() }
        drawer.navNuevaEvaluacion.setOnClickListener {
            closeDrawer()
            // Mismo flujo que la tarjeta "Nueva Evaluación" del inicio (crea la evaluación y confirma si hay un chat previo).
            val host = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            (host?.childFragmentManager?.primaryNavigationFragment as? com.upc.asistenteredidbi.presentation.home.AssistantHomeFragment)
                ?.requestNewEvaluation()
        }
        drawer.navHistorial.setOnClickListener { navigateFromDrawer(R.id.action_home_to_historial) }
        drawer.navAsistente.setOnClickListener { navigateFromDrawer(R.id.action_home_to_asistente) }
        drawer.navPropuestas.setOnClickListener { navigateFromDrawer(R.id.action_home_to_minutas) }
        drawer.navPlan.setOnClickListener { closeDrawer(); mostrarPlan() }
        drawer.navPanel.setOnClickListener { navigateFromDrawer(R.id.action_home_to_panel) }
        drawer.navPreguntas.setOnClickListener { navigateFromDrawer(R.id.action_home_to_preguntas) }
        drawer.navEquipo.setOnClickListener { navigateFromDrawer(R.id.action_home_to_equipo) }
        drawer.navNotificaciones.setOnClickListener { navigateFromDrawer(R.id.action_home_to_notifications) }
        drawer.navConfiguracion.setOnClickListener { navigateFromDrawer(R.id.action_home_to_config) }
        // Mi perfil sale del menú (doc 0.0.7 §13): se abre tocando el avatar o el nombre de la cabecera.
        val openPerfil = View.OnClickListener { navigateFromDrawer(R.id.action_home_to_perfil) }
        fondoAvatarMenu = drawer.tvDrawerInitials.background
        drawer.tvDrawerInitials.setOnClickListener(openPerfil)
        drawer.tvDrawerName.setOnClickListener(openPerfil)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sessionViewModel.drawerProfile.collect { profile ->
                    if (profile != null) {
                        launch { fotoPerfil.sincronizar() }
                        drawer.tvDrawerName.text = profile.fullName
                        drawer.tvDrawerRole.text = profile.roleLabel
                        drawer.tvDrawerCompany.text = profile.company
                        // Panel: administrador y supervisor. Equipo y accesos: solo administrador (doc 0.0.7 §13-15).
                        drawer.navPanel.visibility = if (profile.role != com.upc.asistenteredidbi.domain.model.Role.TECNICO) View.VISIBLE else View.GONE
                        drawer.navPreguntas.visibility = if (profile.role == com.upc.asistenteredidbi.domain.model.Role.ADMINISTRADOR) View.VISIBLE else View.GONE
                        drawer.navEquipo.visibility = if (profile.role == com.upc.asistenteredidbi.domain.model.Role.ADMINISTRADOR) View.VISIBLE else View.GONE
                        refrescarMenu()
                        val ini = profile.fullName.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
                        fotoPerfil.pintar(drawer.tvDrawerInitials, ini, fondoAvatarMenu, fotoPerfil.foto.value)
                    }
                }
            }
        }
        iniciarModoSinConexion()
        // Foto de perfil en la cabecera del menú (doc 0.0.7 §35); sin foto se ven las iniciales.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                fotoPerfil.foto.collect { f ->
                    val ini = drawer.tvDrawerName.text.toString().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { drawer.tvDrawerInitials.text.toString() }
                    fotoPerfil.pintar(drawer.tvDrawerInitials, ini, fondoAvatarMenu, f)
                }
            }
        }
    }

    /**
     * Modo sin conexión (doc 0.0.7 §5): barra «Sin conexión · se guarda en el celular · N por subir» con «Ver»; al volver la señal se sube
     * la cola («Sincronizando N cambios…» y luego «Se subieron N cambios»). Las fotos suben con WiFi o con datos móviles (§34).
     */
    private fun iniciarModoSinConexion() {
        conexion.iniciar()
        val barra = binding.root.findViewById<android.widget.TextView>(R.id.tvOfflineBar)
        barra.setOnClickListener { mostrarCola() }
        var sincronizando = false
        fun pintar(online: Boolean, n: Int) {
            barra.visibility = if (!online || n > 0) View.VISIBLE else View.GONE
            barra.text = when {
                !online -> "📵 Sin conexión · se guarda en el celular" + (if (n > 0) " · $n por subir" else "") + "   Ver"
                sincronizando -> "Sincronizando $n ${if (n == 1) "cambio" else "cambios"}…"
                else -> "$n por subir · toca para sincronizar"
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    kotlinx.coroutines.flow.combine(conexion.online, cola.cola) { o, c -> o to c.size }.collect { (o, n) ->
                        pintar(o, n)
                        if (o && n > 0 && !sincronizando) sincronizar { sincronizando = it; pintar(true, cola.cola.value.size) }
                    }
                }
            }
        }
    }

    private fun sincronizar(estado: (Boolean) -> Unit) {
        lifecycleScope.launch {
            val antes = cola.cola.value.size
            estado(true)
            val subidos = cola.subir()
            estado(false)
            if (subidos > 0) android.widget.Toast.makeText(this@MainActivity, "Se subieron $subidos ${if (subidos == 1) "cambio" else "cambios"}.", android.widget.Toast.LENGTH_LONG).show()
            else if (antes > 0 && cola.cola.value.isNotEmpty() && conexion.online.value) android.widget.Toast.makeText(this@MainActivity, "No se pudo subir todo todavía. Se reintentará.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarCola() {
        val l = cola.cola.value
        val texto = if (l.isEmpty()) "No hay nada por subir.\n\nSin conexión puedes tomar fotos y pedir la validación de una minuta: quedan en el celular y se envían solas al volver la señal."
        else l.joinToString("\n") { "• " + (if (it.tipo == "completar") "Solicitar la validación" else "Foto") + " · " + it.etiqueta } +
            "\n\nSe suben solas al volver la señal (WiFi o datos móviles)."
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Modo sin conexión")
            .setMessage(texto)
            .setPositiveButton("Sincronizar ahora") { _, _ -> if (conexion.online.value) sincronizar { } else android.widget.Toast.makeText(this, "Sigues sin conexión.", android.widget.Toast.LENGTH_SHORT).show() }
            .setNegativeButton("Cerrar", null).show()
    }

    /**
     * Barras del sistema (Android 15 dibuja las apps detrás de ellas, y en las demás versiones se hace igual para que todas se vean
     * igual): la barra de estado se pinta con el azul de la cabecera, los botones del sistema y el teclado empujan la pantalla hacia
     * arriba, y cada bloque toma su alto de los insets del sistema.
     */
    private fun aplicarBarrasDelSistema() {
        val noche = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        run {
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (android.os.Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        }
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = !noche
        }
        val estado = binding.root.findViewById<View>(R.id.statusScrim)
        val botones = binding.root.findViewById<View>(R.id.navScrim)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root.findViewById(R.id.contentRoot)) { _, insets ->
            val barras = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars() or androidx.core.view.WindowInsetsCompat.Type.displayCutout())
            val teclado = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime()).bottom
            estado.layoutParams = estado.layoutParams.apply { height = barras.top }
            botones.layoutParams = botones.layoutParams.apply { height = maxOf(barras.bottom, teclado) }
            androidx.core.view.WindowInsetsCompat.CONSUMED
        }
        androidx.core.view.ViewCompat.requestApplyInsets(binding.root)
    }

    private fun closeDrawer() {
        binding.drawerLayoutRoot.closeDrawer(GravityCompat.START)
    }

    /** Las acciones del drawer solo existen como hijas de assistantHomeFragment en el nav graph. */
    private fun navigateFromDrawer(actionId: Int) {
        closeDrawer()
        if (navController.currentDestination?.id == R.id.assistantHomeFragment) {
            navController.navigate(actionId)
        }
    }

    private fun observeSession() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sessionViewModel.destination.collect { destination ->
                    when (destination) {
                        SessionDestination.Loading -> Unit

                        SessionDestination.Login -> {
                            navigateToLogin()
                            sessionViewModel.consumeDestination()
                        }

                        SessionDestination.Home -> {
                            navigateToHome()
                            sessionViewModel.consumeDestination()
                            lifecycleScope.launch {
                                notificationTokenSync.syncCurrentToken()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun navigateToLogin() {
        if (navController.currentDestination?.id == R.id.loginFragment) {
            return
        }

        navController.navigate(
            R.id.loginFragment,
            null,
            NavOptions.Builder()
                .setPopUpTo(
                    navController.graph.startDestinationId,
                    true
                )
                .build()
        )
    }

    /** Al tocar una notificación push se abre la minuta de la que habla (tipo, minutaId y evaluationId vienen en el push). */
    private fun openFromNotification(intent: android.content.Intent?) {
        val evaluationId = intent?.getStringExtra("evaluationId")?.takeIf { it.isNotBlank() } ?: return
        val minutaId = intent.getStringExtra("minutaId")?.toLongOrNull() ?: -1L
        // QA F7: el dato se borra solo al navegar; si aún está en Iniciar sesión se conserva para abrirla al entrar.
        if (navController.currentDestination?.id == R.id.loginFragment) return
        intent.removeExtra("evaluationId")
        navController.navigate(R.id.minutaProposalFragment, Bundle().apply {
            putString("evaluationId", evaluationId)
            putLong("minutaId", minutaId)
        })
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openFromNotification(intent)
    }

    private fun navigateToHome() {
        if (navController.currentDestination?.id == R.id.assistantHomeFragment) {
            openFromNotification(intent)
            return
        }

        navController.navigate(
            R.id.assistantHomeFragment,
            null,
            NavOptions.Builder()
                .setPopUpTo(
                    navController.graph.startDestinationId,
                    true
                )
                .build()
        )
        openFromNotification(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() ||
                super.onSupportNavigateUp()
    }
}