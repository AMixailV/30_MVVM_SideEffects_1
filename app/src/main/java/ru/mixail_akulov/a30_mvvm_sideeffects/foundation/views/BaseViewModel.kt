package ru.mixail_akulov.a30_mvvm_sideeffects.foundation.views

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.model.ErrorResult
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.model.Result
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.model.SuccessResult
import java.lang.Exception
import kotlin.coroutines.cancellation.CancellationException

// Альтернативные записи для сокращени кода
typealias LiveResult<T> = LiveData<Result<T>>

typealias MutableLiveResult<T> = MutableLiveData<Result<T>>

/**
 * Base class for all view-models.
 */

open class BaseViewModel(
) : ViewModel() {

    private val coroutineContext = SupervisorJob() + Dispatchers.Main.immediate
    protected val viewModelScope: CoroutineScope = CoroutineScope(coroutineContext)

    override fun onCleared() {
        clearViewModelScope()
    }

    /**
     * Переопределите этот метод в дочерних классах, если вы хотите прослушивать результаты с других экранов.
     */
    open fun onResult(result: Any) {

    }

    /**
     * Переопределите этот метод в дочерних классах, если вы хотите контролировать поведение возврата.
     * Верните `true`, если вы хотите прервать закрытие этого экрана
     */
    open fun onBackPressed(): Boolean {
        return false
    }

    /**
     * Запустить указанный приостанавливающий [block] и использовать его результат
     * в качестве значения предоставленного [liveResult].
     */
    fun <T> into(liveResult: MutableLiveResult<T>, block: suspend () -> T) {
        viewModelScope.launch {
            try {
                liveResult.postValue(SuccessResult(block()))
            } catch (e: Exception) {
                if (e !is CancellationException) liveResult.postValue(ErrorResult(e))
            }
        }
    }

    /**
     * Запустить указанный приостанавливающий [block] и использовать его результат
     * в качестве значения предоставленного [stateFlow].
     */
    fun <T> into(stateFlow: MutableStateFlow<Result<T>>, block: suspend () -> T) {
        viewModelScope.launch {
            try {
                stateFlow.value = SuccessResult(block())
            } catch (e: Exception) {
                if (e !is CancellationException) stateFlow.value = ErrorResult(e)
            }
        }
    }

    /**
     * Создайте [MutableStateFlow], который отражает состояние значения с указанным ключом,
     * управляемым [SavedStateHandle]. Когда значение обновляется,
     * экземпляр [MutableStateFlow] создает новый элемент с обновленным значением.
     * Когда какое-то новое значение присваивается [MutableStateFlow]
     * через [MutableStateFlow.value], оно записывается в [SavedStateHandle].
     * Так что на самом деле этот метод создает [MutableStateFlow],
     * который работает так же, как [MutableLiveData], возвращаемый [SavedStateHandle.getLiveData]..
     */
    fun <T> SavedStateHandle.getsStateFlow(key: String, initialValue: T): MutableStateFlow<T> {
        val savedStateHandle = this
        val mutableFlow = MutableStateFlow(savedStateHandle[key] ?: initialValue)

        viewModelScope.launch {
            mutableFlow.collect {
                savedStateHandle[key] = it
            }
        }

        viewModelScope.launch {
            savedStateHandle.getLiveData<T>(key).asFlow().collect {
                mutableFlow.value = it
            }
        }

        return mutableFlow
    }

    private fun clearViewModelScope() {
        viewModelScope.cancel()
    }

}

