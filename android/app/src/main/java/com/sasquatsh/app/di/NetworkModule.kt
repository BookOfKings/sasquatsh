package com.sasquatsh.app.di

import com.sasquatsh.app.data.remote.AuthInterceptor
import com.sasquatsh.app.data.remote.api.AuthApi
import com.sasquatsh.app.data.remote.api.BggApi
import com.sasquatsh.app.data.remote.api.BillingApi
import com.sasquatsh.app.data.remote.api.ChatApi
import com.sasquatsh.app.data.remote.api.EventsApi
import com.sasquatsh.app.data.remote.api.GroupsApi
import com.sasquatsh.app.data.remote.api.MtgDeckApi
import com.sasquatsh.app.data.remote.api.PlanningApi
import com.sasquatsh.app.data.remote.api.PlayerRequestsApi
import com.sasquatsh.app.data.remote.api.ProfileApi
import com.sasquatsh.app.data.remote.api.ScryfallApi
import com.sasquatsh.app.util.Constants
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit = Retrofit.Builder()
        .baseUrl(Constants.SUPABASE_FUNCTIONS_URL + "/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideProfileApi(retrofit: Retrofit): ProfileApi = retrofit.create(ProfileApi::class.java)

    @Provides
    @Singleton
    fun provideEventsApi(retrofit: Retrofit): EventsApi = retrofit.create(EventsApi::class.java)

    @Provides
    @Singleton
    fun provideGroupsApi(retrofit: Retrofit): GroupsApi = retrofit.create(GroupsApi::class.java)

    @Provides
    @Singleton
    fun providePlanningApi(retrofit: Retrofit): PlanningApi = retrofit.create(PlanningApi::class.java)

    @Provides
    @Singleton
    fun provideChatApi(retrofit: Retrofit): ChatApi = retrofit.create(ChatApi::class.java)

    @Provides
    @Singleton
    fun providePlayerRequestsApi(retrofit: Retrofit): PlayerRequestsApi = retrofit.create(PlayerRequestsApi::class.java)

    @Provides
    @Singleton
    fun provideBggApi(retrofit: Retrofit): BggApi = retrofit.create(BggApi::class.java)

    @Provides
    @Singleton
    fun provideMtgDeckApi(retrofit: Retrofit): MtgDeckApi = retrofit.create(MtgDeckApi::class.java)

    @Provides
    @Singleton
    fun provideScryfallApi(retrofit: Retrofit): ScryfallApi = retrofit.create(ScryfallApi::class.java)

    @Provides
    @Singleton
    fun provideBillingApi(retrofit: Retrofit): BillingApi = retrofit.create(BillingApi::class.java)
}
