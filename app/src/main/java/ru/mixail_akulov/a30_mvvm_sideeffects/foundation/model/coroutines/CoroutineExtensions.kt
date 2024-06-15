package ru.mixail_akulov.a30_mvvm_sideeffects.foundation.model.coroutines

import kotlinx.coroutines.CancellableContinuation
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.model.ErrorResult
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.model.FinalResult
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.model.SuccessResult
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Преобразование экземпляра сопрограммы [CancellableContinuation] в более простой экземпляр [Emitter].
 */
fun <T> CancellableContinuation<T>.toEmitter(): Emitter<T> {

    return object : Emitter<T> {

        var done = AtomicBoolean(false)

        override fun emit(finalResult: FinalResult<T>) {
            if (done.compareAndSet(false, true)) {
                when (finalResult) {
                    is SuccessResult -> resume(finalResult.data)
                    is ErrorResult -> resumeWithException(finalResult.exception)
                }
            }
        }

        override fun setCancelListener(cancelListener: CancelListener) {
            invokeOnCancellation { cancelListener() }
        }
    }
}