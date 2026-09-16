package com.upc.asistenteredidbi.domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.nerdythings.okhttp.profiler.OkHttpProfilerInterceptor
import okhttp3.Interceptor

/**
 * Solo existe en el sourceSet debug — muestra las tramas HTTP en el panel
 * del plugin "OkHttp Profiler" de Android Studio. Nunca se compila en una
 * build release, así que nunca llega al APK que se le entrega a alguien
 * fuera del equipo de desarrollo.
 */
@Module
@InstallIn(SingletonComponent::class)
object DebugNetworkModule {

    @Provides
    @IntoSet
    fun provideOkHttpProfilerInterceptor(): Interceptor = OkHttpProfilerInterceptor()
}
