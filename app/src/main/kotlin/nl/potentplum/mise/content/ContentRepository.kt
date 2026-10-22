package nl.potentplum.mise.content

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import nl.potentplum.mise.BuildConfig
import nl.potentplum.mise.core.util.IoDispatcher
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface ContentRepository {
    /** Loads the content once, off the main thread; later calls return the same instance. */
    suspend fun content(): Content
}

@Singleton
class AssetContentRepository
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
    ) : ContentRepository {
        private val mutex = Mutex()
        private var loaded: Content? = null

        override suspend fun content(): Content =
            loaded ?: mutex.withLock {
                loaded ?: withContext(dispatcher) {
                    ContentParser(strict = BuildConfig.DEBUG, onWarning = { Timber.w(it) })
                        .parse { name ->
                            context.assets
                                .open(name)
                                .bufferedReader()
                                .use { it.readText() }
                        }
                }.also { loaded = it }
            }
    }
