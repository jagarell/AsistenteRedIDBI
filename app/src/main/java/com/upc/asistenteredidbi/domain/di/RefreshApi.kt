package com.upc.asistenteredidbi.domain.di

import javax.inject.Qualifier

/** Distingue el AuthApiService "pelado" (sin AuthInterceptor ni
 *  TokenAuthenticator) que usa TokenAuthenticator para llamar
 *  `/api/auth/refresh` sin recursión, del AuthApiService normal. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class RefreshApi
