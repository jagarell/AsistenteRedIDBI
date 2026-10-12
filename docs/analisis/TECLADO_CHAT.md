# Teclado del chat: la caja de texto queda tapada

Versión 2026-10-11 · Asistente Red IDBI (app Android)

## Problema

En el chat del Asistente IA, al tocar la caja de texto, el teclado tapa lo que se escribe.

## Causa

Revisado en la copia local del repo, último commit `0e2aa95` del 2026-10-10:

- La app tiene `targetSdk = 35`. En Android 15, con target 35, la app pasa a borde a borde (edge-to-edge) de forma obligatoria.
- En borde a borde, `adjustResize` ya no achica la ventana. La app tiene que aplicar los insets del teclado (IME) por su cuenta.
- `TechnicalChatFragment` no maneja insets.
- `MainActivity` no declara `windowSoftInputMode`.
- Por eso `inputArea` se queda en el fondo de la pantalla, detrás del teclado.

## Cambio propuesto

### 1. `app/src/main/AndroidManifest.xml`

Esto cubre Android 14 o menos:

```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:windowSoftInputMode="adjustResize">
```

### 2. `presentation/chat/TechnicalChatFragment.kt`

```kotlin
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupRecycler()
    setupInitialState()
    setupClicks()
    observeViewModel()
    setupKeyboardInsets()   // nuevo
    ...
}

/** Sube la barra de escritura sobre el teclado (Android 15 borde a borde) y muestra el último mensaje. */
private fun setupKeyboardInsets() {
    val basePadding = binding.inputArea.paddingBottom          // 24dp del diseño actual
    val gap = (8 * resources.displayMetrics.density).toInt()
    ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
        val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        binding.inputArea.updatePadding(bottom = if (imeVisible && ime > 0) ime + gap else basePadding)
        if (imeVisible) {
            val last = (binding.rvMessages.adapter?.itemCount ?: 0) - 1
            if (last >= 0) binding.rvMessages.post { binding.rvMessages.scrollToPosition(last) }
        }
        insets
    }
    ViewCompat.requestApplyInsets(binding.root)
}
```

- **Sin teclado:** el diseño no cambia, el padding sigue en 24dp.
- **Con teclado:** la barra sube y el RecyclerView se achica, porque tiene `layout_weight=1`. La cabecera y el último mensaje quedan visibles.
- **Android 14 o menos:** `adjustResize` achica la ventana y el IME llega consumido (0), así que no hay doble espacio.

## Cómo probar

| Equipo | Qué hacer | Qué debe pasar |
|---|---|---|
| Android 15 (o emulador API 35) | Abrir el chat, tocar la caja y escribir | El texto se ve sobre el teclado |
| Android 15 | Mismo caso, con navegación por gestos y con 3 botones | Igual en los dos modos |
| Android 13 o 14 | Mismo caso | El texto se ve sobre el teclado, sin espacio doble |
| Cualquiera | Pregunta de fecha (date picker) o de número (RUC) | La caja de texto sigue visible |
| Cualquiera | Escribir, enviar y cerrar el teclado | La barra vuelve a su lugar |

## Simulador

El simulador hace lo mismo en el navegador del celular:

- **Al escribir:** el chat ocupa solo el área visible sobre el teclado (`visualViewport`). Ver `p3p.js`.
- **Tamaño de letra:** la caja de texto usa 16px en celular para que iOS no haga zoom.
