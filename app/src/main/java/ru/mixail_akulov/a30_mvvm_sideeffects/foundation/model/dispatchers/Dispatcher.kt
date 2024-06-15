package ru.mixail_akulov.a30_mvvm_sideeffects.foundation.model.dispatchers

/**
 * Диспетчеры каким-то образом запускают указанный блок кода.
 */
interface Dispatcher {

    fun dispatch(block: () -> Unit)

}