package nl.potentplum.mise.content

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface ContentModule {
    @Binds
    fun contentRepository(impl: AssetContentRepository): ContentRepository
}
