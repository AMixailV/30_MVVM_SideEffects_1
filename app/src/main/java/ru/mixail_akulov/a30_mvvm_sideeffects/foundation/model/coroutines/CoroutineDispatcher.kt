package ru.mixail_akulov.a30_mvvm_sideeffects.foundation.model.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Держатель для диспетчера сопрограмм, который следует использовать для операций с интенсивным вводом-выводом.
 */
class IoDispatcher(
    val value: CoroutineDispatcher = Dispatchers.IO
)

/**
 * Держатель для диспетчера сопрограмм, который следует использовать для операций с интенсивным использованием ЦП.
 */
class WorkerDispatcher(
    val value: CoroutineDispatcher = Dispatchers.Default
)