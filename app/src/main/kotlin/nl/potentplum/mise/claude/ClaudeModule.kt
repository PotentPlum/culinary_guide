package nl.potentplum.mise.claude

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** P1 ships the raw-HTTPS client; the Anthropic Java SDK spike on a device is still open (spec §8.1). */
@Module
@InstallIn(SingletonComponent::class)
interface ClaudeModule {
    @Binds
    fun claudeClient(impl: OkHttpClaudeClient): ClaudeClient
}
