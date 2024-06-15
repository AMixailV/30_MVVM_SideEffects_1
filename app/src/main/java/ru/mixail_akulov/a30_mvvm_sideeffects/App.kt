package ru.mixail_akulov.a30_mvvm_sideeffects

import android.app.Application
import kotlinx.coroutines.Dispatchers
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.BaseApplication
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.model.coroutines.IoDispatcher
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.model.coroutines.WorkerDispatcher
import ru.mixail_akulov.a30_mvvm_sideeffects.simplemvvm.model.colors.InMemoryColorsRepository

/**
 * Здесь мы храним экземпляры классов слоя модели.
 */
class App : Application(), BaseApplication {

    private val ioDispatcher = IoDispatcher(Dispatchers.IO)
    private val workerDispatcher = WorkerDispatcher(Dispatchers.Default)

    /**
     * Поместите здесь свои одноэлементные зависимости области видимости
     */
    override val singletonScopeDependencies: List<Any> = listOf(
        InMemoryColorsRepository(ioDispatcher)
    )
}