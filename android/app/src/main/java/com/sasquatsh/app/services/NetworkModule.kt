package com.sasquatsh.app.services

import com.sasquatsh.app.config.AppConfig
import com.sasquatsh.app.services.api.*
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
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

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
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl(AppConfig.supabaseFunctionsUrl + "/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideEventsApi(retrofit: Retrofit): EventsApi =
        retrofit.create(EventsApi::class.java)

    @Provides
    @Singleton
    fun provideGroupsApi(retrofit: Retrofit): GroupsApi =
        retrofit.create(GroupsApi::class.java)

    @Provides
    @Singleton
    fun providePlanningApi(retrofit: Retrofit): PlanningApi =
        retrofit.create(PlanningApi::class.java)

    @Provides
    @Singleton
    fun provideProfileApi(retrofit: Retrofit): ProfileApi =
        retrofit.create(ProfileApi::class.java)

    @Provides
    @Singleton
    fun provideChatApi(retrofit: Retrofit): ChatApi =
        retrofit.create(ChatApi::class.java)

    @Provides
    @Singleton
    fun provideBillingApi(retrofit: Retrofit): BillingApi =
        retrofit.create(BillingApi::class.java)

    @Provides
    @Singleton
    fun provideBggApi(retrofit: Retrofit): BggApi =
        retrofit.create(BggApi::class.java)

    @Provides
    @Singleton
    fun provideScryfallApi(retrofit: Retrofit): ScryfallApi =
        retrofit.create(ScryfallApi::class.java)

    @Provides
    @Singleton
    fun provideMtgDeckApi(retrofit: Retrofit): MtgDeckApi =
        retrofit.create(MtgDeckApi::class.java)

    @Provides
    @Singleton
    fun provideRaffleApi(retrofit: Retrofit): RaffleApi =
        retrofit.create(RaffleApi::class.java)

    @Provides
    @Singleton
    fun providePlayerRequestsApi(retrofit: Retrofit): PlayerRequestsApi =
        retrofit.create(PlayerRequestsApi::class.java)

    @Provides
    @Singleton
    fun provideEventLocationsApi(retrofit: Retrofit): EventLocationsApi =
        retrofit.create(EventLocationsApi::class.java)

    @Provides
    @Singleton
    fun provideInvitationsApi(retrofit: Retrofit): InvitationsApi =
        retrofit.create(InvitationsApi::class.java)

    @Provides
    @Singleton
    fun provideRecurringGamesApi(retrofit: Retrofit): RecurringGamesApi =
        retrofit.create(RecurringGamesApi::class.java)

    @Provides
    @Singleton
    fun provideSessionsApi(retrofit: Retrofit): SessionsApi =
        retrofit.create(SessionsApi::class.java)

    @Provides
    @Singleton
    fun provideCollectionsApi(retrofit: Retrofit): CollectionsApi =
        retrofit.create(CollectionsApi::class.java)

    @Provides
    @Singleton
    fun provideBadgesApi(retrofit: Retrofit): BadgesApi =
        retrofit.create(BadgesApi::class.java)

    @Provides
    @Singleton
    fun provideShareLinksApi(retrofit: Retrofit): ShareLinksApi =
        retrofit.create(ShareLinksApi::class.java)

    @Provides
    @Singleton
    fun provideGameUpcApi(retrofit: Retrofit): GameUpcApi =
        retrofit.create(GameUpcApi::class.java)
}
