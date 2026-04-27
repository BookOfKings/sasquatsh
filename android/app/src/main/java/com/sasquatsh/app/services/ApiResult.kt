package com.sasquatsh.app.services

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw ApiException(message, code)
    }

    fun <R> map(transform: (T) -> R): ApiResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    companion object {
        suspend fun <T> from(block: suspend () -> retrofit2.Response<T>): ApiResult<T> {
            return try {
                val response = block()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Success(body)
                    } else {
                        @Suppress("UNCHECKED_CAST")
                        Success(Unit as T)
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val message = parseErrorMessage(errorBody) ?: "Request failed"
                    Error(message, response.code())
                }
            } catch (e: Exception) {
                Error(e.message ?: "Unknown error")
            }
        }

        private fun parseErrorMessage(errorBody: String?): String? {
            if (errorBody.isNullOrBlank()) return null
            return try {
                val adapter = com.squareup.moshi.Moshi.Builder().build()
                    .adapter(ErrorResponse::class.java)
                adapter.fromJson(errorBody)?.error
                    ?: adapter.fromJson(errorBody)?.message
            } catch (_: Exception) {
                errorBody
            }
        }
    }
}

class ApiException(message: String, val code: Int? = null) : Exception(message)

private data class ErrorResponse(
    val error: String? = null,
    val message: String? = null
)
