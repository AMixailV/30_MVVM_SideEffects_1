package ru.mixail_akulov.a30_mvvm_sideeffects.simplemvvm.views.changecolor

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import ru.mixail_akulov.a30_mvvm_sideeffects.R
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.model.EmptyProgress
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.model.PendingResult
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.model.PercentageProgress
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.model.Progress
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.model.Result
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.model.SuccessResult
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.model.getPercentage
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.model.isInProgress
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.sideeffects.navigator.Navigator
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.sideeffects.resources.Resources
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.sideeffects.toasts.Toasts
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.utils.finiteShareIn
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.views.BaseViewModel
import ru.mixail_akulov.a30_mvvm_sideeffects.simplemvvm.model.colors.ColorsRepository
import ru.mixail_akulov.a30_mvvm_sideeffects.simplemvvm.model.colors.NamedColor
import ru.mixail_akulov.a30_mvvm_sideeffects.simplemvvm.views.changecolor.ChangeColorFragment.Screen
import java.lang.Exception

class ChangeColorViewModel(
    screen: Screen,
    private val navigator: Navigator,
    private val toasts: Toasts,
    private val resources: Resources,
    private val colorsRepository: ColorsRepository,
    savedStateHandle: SavedStateHandle
) : BaseViewModel(), ColorsAdapter.Listener {

    // input sources
    private val _availableColors = MutableStateFlow<Result<List<NamedColor>>>(PendingResult())
    private val _currentColorId = savedStateHandle.getsStateFlow  ("currentColorId", screen.currentColorId)
    private val _instantSaveInProgress = MutableStateFlow<Progress>(EmptyProgress)
    private val _sampledSaveInProgress = MutableStateFlow<Progress>(EmptyProgress)

    // основной пункт назначения (содержит объединенные значения from _availableColors & _currentColorId)
    val viewState: Flow<Result<ViewState>> = combine(
        _availableColors,
        _currentColorId,
        _instantSaveInProgress,
        _sampledSaveInProgress,
        ::mergeSources
    )

    // побочное назначение, также тот же результат может быть достигнут с использованием Transformations.map() function.
    val screenTitle: LiveData<String> = viewState
        .map { result ->
            return@map if (result is SuccessResult) {
                val currentColor = result.data.colorsList.first { it.selected }
                resources.getString(R.string.change_color_screen_title, currentColor.namedColor.name)
            } else {
                resources.getString(R.string.change_color_screen_title_simple)
            }
        }
        .asLiveData()

    init {
        load()
    }

    override fun onColorChosen(namedColor: NamedColor) {
        if (_instantSaveInProgress.value.isInProgress()) return
        _currentColorId.value = namedColor.id
    }

    fun onSavePressed() = viewModelScope.launch {
        try {
            _instantSaveInProgress.value = PercentageProgress.START
            _sampledSaveInProgress.value = PercentageProgress.START

            val currentColorId = _currentColorId.value
            val currentColor = colorsRepository.getById(currentColorId)

            val flow = colorsRepository.setCurrentColor(currentColor)
                .finiteShareIn(this)

            val instantJob = async {
                flow.collect { percentage ->
                    _instantSaveInProgress.value = PercentageProgress(percentage)
                }
            }

            val sampledJob = async {
                flow.sample(200) // выдавать наиболее актуальный прогресс каждые 200 мс
                    .collect { percentage ->
                        _sampledSaveInProgress.value = PercentageProgress(percentage)
                    }
            }

            instantJob.await()
            sampledJob.await()

            navigator.goBack(currentColor)
        } catch (e: Exception) {
            if (e !is CancellationException) toasts.toast(resources.getString(R.string.error_happened))
        } finally {
            _instantSaveInProgress.value = EmptyProgress
            _sampledSaveInProgress.value = EmptyProgress
        }
    }

    fun onCancelPressed() {
        navigator.goBack()
    }

    fun tryAgain() {
        load()
    }

    /**
     * [MediatorLiveData] может прослушивать другие экземпляры LiveData (даже более 1) и комбинировать их значения.
     *
     * Здесь мы слушаем список доступных цветов ([_availableColors] live-data) + current color id
     * ([_currentColorId] live-data), затем мы используем оба этих значения для создания списка
     * [NamedColorListItem], это список, который будет отображаться в RecyclerView.
     */
    private fun mergeSources(colors: Result<List<NamedColor>>, currentColorId: Long,
                             instantSaveInProgress: Progress,sampledSaveInProgress: Progress): Result<ViewState> {

        // map Result<List<NamedColor>> to Result<ViewState>
        return colors.map { colorsList ->
            ViewState(
                // map List<NamedColor> to List<NamedColorListItem>
                colorsList = colorsList.map { NamedColorListItem(it, currentColorId == it.id) },
                showSaveButton = !instantSaveInProgress.isInProgress(),
                showCancelButton = !instantSaveInProgress.isInProgress(),
                showSaveProgressBar = instantSaveInProgress.isInProgress(),

                saveProgressPercentage = instantSaveInProgress.getPercentage(),
                saveProgressPercentageMessage = resources.getString(R.string.percentage_value, sampledSaveInProgress.getPercentage())
            )
        }
    }

    private fun load() = into(_availableColors) { colorsRepository.getAvailableColors() }

    data class ViewState(
        val colorsList: List<NamedColorListItem>,
        val showSaveButton: Boolean,
        val showCancelButton: Boolean,
        val showSaveProgressBar: Boolean,

        val saveProgressPercentage: Int,
        val saveProgressPercentageMessage: String
    )
}