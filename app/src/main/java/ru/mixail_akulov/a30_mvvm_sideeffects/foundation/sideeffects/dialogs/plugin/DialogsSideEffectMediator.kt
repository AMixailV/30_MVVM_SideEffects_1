package ru.mixail_akulov.a30_mvvm_sideeffects.foundation.sideeffects.dialogs.plugin

import kotlinx.coroutines.suspendCancellableCoroutine
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.model.ErrorResult
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.model.coroutines.Emitter
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.model.coroutines.toEmitter
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.sideeffects.dialogs.Dialogs
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.sideeffects.SideEffectMediator

class DialogsSideEffectMediator : SideEffectMediator<DialogsSideEffectImpl>(), Dialogs {

    var retainedState = RetainedState()

    override suspend fun show(dialogConfig: DialogConfig): Boolean = suspendCancellableCoroutine { continuatione ->
        val emitter = continuatione.toEmitter()
        if (retainedState.record != null) {
            // на данный момент разрешено только 1 активное диалоговое окно за раз
            emitter.emit(ErrorResult(IllegalStateException("Can't launch more than 1 dialog at a time")))
            return@suspendCancellableCoroutine
        }

        val wrappedEmitter = Emitter.wrap(emitter) {
            retainedState.record = null
        }

        val record = DialogRecord(wrappedEmitter, dialogConfig)
        wrappedEmitter.setCancelListener {
            target { implementation ->
                implementation.removeDialog()
            }
        }

        target { implementation ->
            implementation.showDialog(record)
        }

        retainedState.record = record
    }

    class DialogRecord(
        val emitter: Emitter<Boolean>,
        val config: DialogConfig
    )

    class RetainedState(
        var record: DialogRecord? = null
    )
}