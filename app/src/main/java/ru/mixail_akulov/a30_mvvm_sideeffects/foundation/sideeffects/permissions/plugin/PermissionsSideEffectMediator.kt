package ru.mixail_akulov.a30_mvvm_sideeffects.foundation.sideeffects.permissions.plugin

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.model.ErrorResult
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.model.coroutines.Emitter
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.model.coroutines.toEmitter
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.sideeffects.permissions.Permissions
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.sideeffects.SideEffectMediator

class PermissionsSideEffectMediator(
    private val appContext: Context
) : SideEffectMediator<PermissionsSideEffectImpl>(), Permissions {

    val retainedState = RetainedState()

    override fun hasPermissions(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun requestPermission(permission: String): PermissionStatus = suspendCancellableCoroutine { continuation ->
        val emitter = continuation.toEmitter()
        if (retainedState.emitter != null) {
            emitter.emit(ErrorResult(IllegalStateException("Only one permission request can be active")))
            return@suspendCancellableCoroutine
        }
        retainedState.emitter = emitter
        target { implementation ->
            implementation.requestPermission(permission)
        }
    }

    class RetainedState(
        var emitter: Emitter<PermissionStatus>? = null
    )

}