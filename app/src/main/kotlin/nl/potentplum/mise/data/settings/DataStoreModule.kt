package nl.potentplum.mise.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/** `datastore/settings.preferences_pb`: backed up by Auto Backup. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SettingsStore

/** `datastore/secrets.preferences_pb`: excluded from every backup (spec §9). Only `SecretStore` uses it. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SecretsStore

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    const val SETTINGS_FILE = "settings"
    const val SECRETS_FILE = "secrets"

    @Provides
    @Singleton
    @SettingsStore
    fun settings(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(SETTINGS_FILE) }

    @Provides
    @Singleton
    @SecretsStore
    fun secrets(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(SECRETS_FILE) }
}
