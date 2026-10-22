package nl.potentplum.mise.security

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface SecurityModule {
    @Binds
    fun secretCipher(impl: KeystoreCipher): SecretCipher
}
