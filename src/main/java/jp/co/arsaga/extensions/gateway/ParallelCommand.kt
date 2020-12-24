package jp.co.arsaga.extensions.gateway

import androidx.lifecycle.LiveData
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger

class ParallelCommand(
    private val list: List<suspend (() -> Unit) -> Unit>,
    private val onComplete: (Boolean) -> Unit,
    val maxPercentage: Int = 100
) : LiveData<Int>(0) {

    private val progressStep by lazy { AtomicInteger(0) }

    private val progressPercentage by lazy { AtomicInteger(0) }

    private val progressPercentUnit = (maxPercentage.toFloat() / list.size.toFloat()).toInt()

    fun start() {
        list
            .map { convertSuccessCommand(it) }
            .let { run(it, convertCompleteCommand(onComplete)) }
    }

    private fun convertSuccessCommand(
        command: suspend (() -> Unit) -> Unit
    ): () -> Unit = {
        runBlocking {
            command(onSuccess())
        }
    }

    private fun convertCompleteCommand(
        command: (Boolean) -> Unit
    ): () -> Unit = {
        (progressStep.get() == list.size).let {
            if (it) {
                progressPercentage.set(maxPercentage)
                postValue(maxPercentage)
            }
            command(it)
        }
    }

    private fun onSuccess(): () -> Unit = {
        progressStep.incrementAndGet()
        if (hasObservers()) {
            repeat(progressPercentUnit) {
                Thread.sleep(PROGRESS_INTERVAL)
                progressPercentage
                    .incrementAndGet()
                    .let { postValue(it) }
            }
        }
    }

    companion object {
        private const val PROGRESS_INTERVAL = 10L
        fun run(
            list: List<() -> Unit>,
            onComplete: () -> Unit
        ) = GlobalScope.launch(Dispatchers.Default) {
            list
                .map { async { it() } }
                .awaitAll()
                .run { onComplete() }
        }
    }
}