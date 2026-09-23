package com.upc.asistenteredidbi.data.remote

import com.upc.asistenteredidbi.data.remote.dto.RefreshRequestDto
import com.upc.asistenteredidbi.data.session.SessionManager
import com.upc.asistenteredidbi.domain.di.RefreshApi
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hook nativo de OkHttp para "recibí 401 → renueva credencial → reintenta
 * una vez", en vez de cerrar sesión de inmediato (lo que hacía el
 * AuthInterceptor antes). Usa [refreshApiService] — un AuthApiService
 * separado, sin este mismo Authenticator ni el AuthInterceptor — para poder
 * llamar `/api/auth/refresh` sin recursión ni ciclo de Hilt (ver AuthModule).
 *
 * `synchronized` serializa 401s concurrentes: si dos llamadas fallan a la
 * vez, la segunda en entrar ve que el access token ya cambió (lo renovó la
 * primera) y solo reintenta con ese, sin pegarle de nuevo a /refresh.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val sessionManager: SessionManager,
    @RefreshApi private val refreshApiService: AuthApiService
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.url.encodedPath.endsWith("/api/auth/refresh")) {
            return null
        }
        if (responseCount(response) >= 2) {
            return null
        }

        synchronized(this) {
            val failedToken = response.request.header("Authorization")
                ?.removePrefix("Bearer ")
            val currentToken = sessionManager.getJwtTokenBlocking()

            if (!currentToken.isNullOrBlank() && currentToken != failedToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val refreshToken = sessionManager.getRefreshTokenBlocking()
            if (refreshToken.isNullOrBlank()) {
                sessionManager.notifySessionExpired()
                return null
            }

            return try {
                val renewed = runBlocking {
                    refreshApiService.refresh(RefreshRequestDto(refreshToken))
                }
                runBlocking {
                    sessionManager.updateAccessToken(
                        accessToken = renewed.accessToken,
                        refreshToken = renewed.refreshToken,
                        expiresInMinutes = renewed.expiresInMinutes
                    )
                }
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${renewed.accessToken}")
                    .build()
            } catch (e: Exception) {
                // El refresh token también es inválido/expiró (o falló la red):
                // ahí sí no queda otra que cerrar sesión.
                sessionManager.notifySessionExpired()
                null
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
