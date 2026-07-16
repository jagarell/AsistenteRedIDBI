package com.upc.asistenteredidbi.domain.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.upc.asistenteredidbi.data.remote.AuthApiService
import com.upc.asistenteredidbi.data.remote.ChatApiService
import com.upc.asistenteredidbi.data.remote.EvaluationApiService
import com.upc.asistenteredidbi.data.remote.EvidenceApiService
import com.upc.asistenteredidbi.data.remote.MinutaApiService
import com.upc.asistenteredidbi.data.remote.ProfileApiService
import com.upc.asistenteredidbi.data.repository.AuthRepositoryImpl
import com.upc.asistenteredidbi.data.repository.ChatRepositoryImpl
import com.upc.asistenteredidbi.data.repository.EvaluationRepositoryImpl
import com.upc.asistenteredidbi.data.repository.EvidenceRepositoryImpl
import com.upc.asistenteredidbi.data.repository.MinutaRecordRepositoryImpl
import com.upc.asistenteredidbi.data.repository.ProfileRepositoryImpl
import com.upc.asistenteredidbi.data.session.SessionManager
import com.upc.asistenteredidbi.domain.repository.AuthRepository
import com.upc.asistenteredidbi.domain.repository.ChatRepository
import com.upc.asistenteredidbi.domain.repository.EvaluationRepository
import com.upc.asistenteredidbi.domain.repository.EvidenceRepository
import com.upc.asistenteredidbi.domain.repository.MinutaRecordRepository
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
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private const val BASE_URL = "http://10.0.2.2:8080/"

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        sessionManager: SessionManager
    ): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val token = sessionManager.getJwtTokenBlocking()

            val request = originalRequest
                .newBuilder()
                .apply {
                    if (!token.isNullOrBlank()) {
                        header(
                            "Authorization",
                            "Bearer $token"
                        )
                    }
                }
                .build()

            chain.proceed(request)
        }
    }

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            android.util.Log.d("OkHttp", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
            redactHeader("Authorization")
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: Interceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(
                MoshiConverterFactory.create(moshi)
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(
        retrofit: Retrofit
    ): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideEvaluationApiService(
        retrofit: Retrofit
    ): EvaluationApiService {
        return retrofit.create(EvaluationApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideChatApiService(
        retrofit: Retrofit
    ): ChatApiService {
        return retrofit.create(ChatApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideEvidenceApiService(
        retrofit: Retrofit
    ): EvidenceApiService {
        return retrofit.create(EvidenceApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideProfileApiService(
        retrofit: Retrofit
    ): ProfileApiService {
        return retrofit.create(ProfileApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideMinutaApiService(
        retrofit: Retrofit
    ): MinutaApiService {
        return retrofit.create(MinutaApiService::class.java)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindEvaluationRepository(
        impl: EvaluationRepositoryImpl
    ): EvaluationRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        impl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindEvidenceRepository(
        impl: EvidenceRepositoryImpl
    ): EvidenceRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        impl: ProfileRepositoryImpl
    ): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindMinutaRecordRepository(
        impl: MinutaRecordRepositoryImpl
    ): MinutaRecordRepository
}