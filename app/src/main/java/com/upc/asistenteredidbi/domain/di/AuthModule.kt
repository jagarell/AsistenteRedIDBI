package com.upc.asistenteredidbi.domain.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.upc.asistenteredidbi.data.remote.AuthApiService
import com.upc.asistenteredidbi.data.remote.ChatApiService
import com.upc.asistenteredidbi.data.remote.EvaluationApiService
import com.upc.asistenteredidbi.data.remote.EvidenceApiService
import com.upc.asistenteredidbi.data.remote.ProfileApiService
import com.upc.asistenteredidbi.data.repository.AuthRepositoryImpl
import com.upc.asistenteredidbi.data.repository.ChatRepositoryImpl
import com.upc.asistenteredidbi.data.repository.EvaluationRepositoryImpl
import com.upc.asistenteredidbi.data.repository.EvidenceRepositoryImpl
import com.upc.asistenteredidbi.data.repository.ProfileRepositoryImpl
import com.upc.asistenteredidbi.data.session.SessionManager
import com.upc.asistenteredidbi.domain.repository.AuthRepository
import com.upc.asistenteredidbi.domain.repository.ChatRepository
import com.upc.asistenteredidbi.domain.repository.EvaluationRepository
import com.upc.asistenteredidbi.domain.repository.EvidenceRepository
import com.upc.asistenteredidbi.domain.repository.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

/**
 * Único punto donde se define `BASE_URL` para toda la app. El
 * `NetworkModule` del Sprint 2 reutiliza el `Retrofit` provisto aquí
 * (no vuelve a crear uno propio) para evitar dos clientes HTTP
 * distintos con configuraciones divergentes.
 */
private const val BASE_URL =
    "https://api.networkassistant.idbi.pe/" // configurar por flavor (dev/staging/prod)

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @Provides
    @Singleton
    fun provideAuthInterceptor(sessionManager: SessionManager): Interceptor =
        Interceptor { chain ->
            val token = sessionManager.getJwtTokenBlocking()
            val request = chain.request().newBuilder().apply {
                if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer $token")
            }.build()
            chain.proceed(request)
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: Interceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService =
        retrofit.create(AuthApiService::class.java)

    @Provides
    @Singleton
    fun provideEvaluationApiService(retrofit: Retrofit): EvaluationApiService =
        retrofit.create(EvaluationApiService::class.java)

    @Provides
    @Singleton
    fun provideChatApiService(retrofit: Retrofit): ChatApiService =
        retrofit.create(ChatApiService::class.java)

    @Provides
    @Singleton
    fun provideEvidenceApiService(retrofit: Retrofit): EvidenceApiService =
        retrofit.create(EvidenceApiService::class.java)

    @Provides
    @Singleton
    fun provideProfileApiService(retrofit: Retrofit): ProfileApiService =
        retrofit.create(ProfileApiService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindEvaluationRepository(impl: EvaluationRepositoryImpl): EvaluationRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindEvidenceRepository(impl: EvidenceRepositoryImpl): EvidenceRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository
}
