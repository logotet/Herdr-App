package io.github.vladimirvasilev.herdrapp.state

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data class Connected(val bridgeName: String) : ConnectionState
    data class Error(val message: String) : ConnectionState
}
