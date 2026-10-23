package nl.potentplum.mise.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/** Whether a network with internet is up; Claude features are disabled with an explanation when it is not (spec §6.11). */
interface NetworkStatus {
    val online: Flow<Boolean>
}

class ConnectivityNetworkStatus
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : NetworkStatus {
        override val online: Flow<Boolean> =
            callbackFlow {
                val manager = context.getSystemService(ConnectivityManager::class.java)

                fun current() =
                    manager.getNetworkCapabilities(manager.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ==
                        true
                val callback =
                    object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            trySend(true)
                        }

                        override fun onLost(network: Network) {
                            trySend(current())
                        }

                        override fun onCapabilitiesChanged(
                            network: Network,
                            capabilities: NetworkCapabilities,
                        ) {
                            trySend(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                        }
                    }
                trySend(current())
                manager.registerDefaultNetworkCallback(callback)
                awaitClose { manager.unregisterNetworkCallback(callback) }
            }.distinctUntilChanged()
    }

@Module
@InstallIn(SingletonComponent::class)
interface NetworkStatusModule {
    @Binds
    fun networkStatus(impl: ConnectivityNetworkStatus): NetworkStatus
}
