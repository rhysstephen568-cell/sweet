package com.sweet.app.core.result

sealed interface NetworkError {
    data object Unauthorized : NetworkError
    data object Forbidden : NetworkError
    data object NotFound : NetworkError
    data object Timeout : NetworkError
    data object NoInternet : NetworkError
    data class ServerError(val code: Int, val message: String) : NetworkError
    data class Unknown(val error: Throwable) : NetworkError
}
