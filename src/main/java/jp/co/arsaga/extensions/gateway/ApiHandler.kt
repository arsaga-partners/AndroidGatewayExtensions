package jp.co.arsaga.extensions.gateway

import kotlinx.coroutines.*
import retrofit2.Response
import timber.log.Timber
import java.lang.Exception


data class ApiContext<Res, Req>(
    val callback: suspend (Res) -> Unit = {},
    val fallback: suspend (String) -> Unit = {},
    val serverFallback: suspend (Throwable) -> Unit = {},
    val coroutineScope: CoroutineScope = GlobalScope,
    val request: Req? = null
)

class LocalRequestErrorException : Exception()

abstract class ApiDispatchCommand<Res, Req>(
    private val apiCall: (suspend () -> Response<Res>)?,
    private val apiContext: ApiContext<Res, Req>,
) {

    open suspend fun callBack(response: Response<Res>) {
        response.body()?.run { apiContext.callback(this) }
    }

    open suspend fun fallback(response: Response<Res>) {
        response.errorBody()?.string()?.run {
            apiContext.fallback(this)
            Timber.e("abstractApiDispatch:onSuccessError!:${this}")
        }
    }

    open suspend fun serverFallback(response: Throwable) {
        apiContext.serverFallback(response)
        Timber.e("abstractApiDispatch:onFailureError!:${response.message} ${response.cause}")
    }

    init {
        apiContext.coroutineScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    apiCall?.invoke() ?: throw LocalRequestErrorException()
                }
            }
                .onSuccess {
                    if (it.isSuccessful) callBack(it)
                    else fallback(it)
                }
                .onFailure {
                    serverFallback(it)
                }
        }
    }
}