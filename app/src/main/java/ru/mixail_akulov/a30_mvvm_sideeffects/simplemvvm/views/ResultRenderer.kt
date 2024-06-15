package ru.mixail_akulov.a30_mvvm_sideeffects.simplemvvm.views

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.view.children
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.model.Result
import ru.mixail_akulov.a30_mvvm_sideeffects.foundation.views.BaseFragment
import ru.mixail_akulov.a30_mvvm_sideeffects.R
import ru.mixail_akulov.a30_mvvm_sideeffects.databinding.PartResultBinding

/**
 * Default [Result] rendering.
 * - if [result] is [PendingResult] -> only progress-bar is displayed
 * - if [result] is [ErrorResult] -> only error container is displayed
 * - if [result] is [SuccessResult] -> error container & progress-bar is hidden, all other views are visible
 */
fun <T> BaseFragment.renderSimpleResult(root: ViewGroup, result: Result<T>, onSuccess: (T) -> Unit) {
    val binding = PartResultBinding.bind(root)

    renderResult(
        root = root,
        result = result,
        onPending = {
            binding.progressBar.visibility = View.VISIBLE
        },
        onError = {
            binding.errorContainer.visibility = View.VISIBLE
        },
        onSuccess = { successData ->
            root.children
                .filter { it.id != R.id.progressBar && it.id != R.id.errorContainer }
                .forEach { it.visibility = View.VISIBLE }
            onSuccess(successData)
        }
    )
}

/**
 * Собирать предметы из указанного [Flow] только тогда, когда фрагмент находится как минимум в состоянии НАЧАЛО.
 */
fun <T> BaseFragment.collectFlow(flow: Flow<T>, onCollect: (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        // эта сопрограмма отменяется в onDestroyView
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            // эта сопрограмма запускается каждый раз при вызове onStart;
            // сбор отменен в onStop
            flow.collect {
                onCollect(it)
            }
        }
    }
}

/**
 * Назначьте прослушиватель onClick для кнопки повторной попытки по умолчанию.
 */
fun BaseFragment.onTryAgain(root: View, onTryAgainPressed: () -> Unit) {
    root.findViewById<Button>(R.id.tryAgainButton).setOnClickListener { onTryAgainPressed() }
}