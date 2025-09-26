package uk.co.zlurgg.mybookshelf.core.domain

sealed interface Result<out D, out E: Error> {
    data class Success<out D>(val data: D): Result<D, Nothing>
    data class Error<out E: uk.co.zlurgg.mybookshelf.core.domain.Error>(val error: E):
        Result<Nothing, E>
}

inline fun <T, E: Error, R> Result<T, E>.map(map: (T) -> R): Result<R, E> {
    return when(this) {
        is Result.Error -> Result.Error(error)
        is Result.Success -> Result.Success(map(data))
    }
}

inline fun <T, E: Error, R> Result<T, E>.flatMap(transform: (T) -> Result<R, E>): Result<R, E> {
    return when(this) {
        is Result.Error -> Result.Error(error)
        is Result.Success -> transform(data)
    }
}

fun <T, E: Error> Result<T, E>.asEmptyDataResult(): EmptyResult<E> {
    return map {  }
}

inline fun <T, E: Error> Result<T, E>.onSuccess(action: (T) -> Unit): Result<T, E> {
    return when(this) {
        is Result.Error -> this
        is Result.Success -> {
            action(data)
            this
        }
    }
}
inline fun <T, E: Error> Result<T, E>.onError(action: (E) -> Unit): Result<T, E> {
    return when(this) {
        is Result.Error -> {
            action(error)
            this
        }
        is Result.Success -> this
    }
}

typealias EmptyResult<E> = Result<Unit, E>

// Utility functions for better ergonomics
fun <T, E: Error> Result<T, E>.getOrNull(): T? {
    return when(this) {
        is Result.Success -> data
        is Result.Error -> null
    }
}

fun <T, E: Error> Result<T, E>.getOrThrow(): T {
    return when(this) {
        is Result.Success -> data
        is Result.Error -> throw Exception("Result failed with error: $error")
    }
}

fun <T, E: Error> Result<T, E>.getOrDefault(defaultValue: T): T {
    return when(this) {
        is Result.Success -> data
        is Result.Error -> defaultValue
    }
}

inline fun <T, E: Error> Result<T, E>.getOrElse(onError: (E) -> T): T {
    return when(this) {
        is Result.Success -> data
        is Result.Error -> onError(error)
    }
}

// Combine multiple Results
inline fun <T1, T2, E: Error, R> Result<T1, E>.combine(
    other: Result<T2, E>,
    transform: (T1, T2) -> R
): Result<R, E> {
    return when(this) {
        is Result.Error -> this
        is Result.Success -> when(other) {
            is Result.Error -> other
            is Result.Success -> Result.Success(transform(data, other.data))
        }
    }
}

// Convert exceptions to Result
inline fun <T> runCatching(action: () -> T): Result<T, DataError.Local> {
    return try {
        Result.Success(action())
    } catch (e: Exception) {
        Result.Error(DataError.Local.UNKNOWN)
    }
}