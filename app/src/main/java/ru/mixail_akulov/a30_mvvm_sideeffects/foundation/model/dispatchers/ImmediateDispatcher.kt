package ru.mixail_akulov.a30_mvvm_sideeffects.foundation.model.dispatchers

class ImmediateDispatcher: Dispatcher {
    override fun dispatch(block: () -> Unit) {
        block()
    }

}