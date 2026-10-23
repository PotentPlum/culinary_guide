package nl.potentplum.mise.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

/** One shared client. No logging interceptor anywhere, so release builds never log HTTP traffic (spec §9). */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun okHttpClient(): OkHttpClient = OkHttpClient.Builder().build()
}
