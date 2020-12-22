package jp.co.arsaga.extensions.gateway

import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import timber.log.Timber
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

abstract class AbstractApiClient<IApiType> {

    protected abstract val baseUrl: String

    protected abstract fun isDebug(): Boolean

    protected abstract val jsonConverterFactory: Converter.Factory

    private val authorizeInterceptor by lazy {
        Interceptor {chain ->
            chain
                .request()
                .newBuilder()
                .let { setAuthorizeHeader(it) }
                .build().let {
                    chain.proceed(it)
                }
        }
    }

    protected open fun setAuthorizeHeader(requestBuilder: Request.Builder): Request.Builder = requestBuilder

    protected open fun setRefreshToken(continuation: Continuation<Request.Builder?>, requestBuilder: Request.Builder): Request.Builder? = requestBuilder

    protected open fun onTokenRefreshError() {}

    protected abstract val maxRetryCount: Int

    private val authenticator by lazy {
        object : Authenticator {
            override fun authenticate(
                route: Route?,
                response: Response
            ): Request? = response
                .let {
                    val retryCount = generateSequence(it.priorResponse) { it.priorResponse }.count()
                    Timber.d("Auth::refreshTokenRetryCount${retryCount}")
                    if (maxRetryCount > retryCount) it
                    else {
                        Timber.d("Auth::refreshTokenError")
                        onTokenRefreshError()
                        null
                    }
                }
                ?.request
                ?.newBuilder()
                ?.let { builder -> runBlocking {
                    suspendCoroutine<Request.Builder?> {
                        setRefreshToken(it, builder)
                    }
                } }
                ?.build()
        }
    }

    private fun OkHttpClient.Builder.debugLog(): OkHttpClient.Builder = this.apply {
        if (isDebug()) {
            HttpLoggingInterceptor()
                .apply { level = HttpLoggingInterceptor.Level.BODY }
                .run(::addInterceptor)
        }
    }

    protected open fun adjustOkHttpClient(okHttpClientBuilder: OkHttpClient.Builder): OkHttpClient.Builder = okHttpClientBuilder

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authorizeInterceptor)
        .authenticator(authenticator)
        .cache(null)
        .let { adjustOkHttpClient(it) }
        .debugLog()
        .build()

    protected val retrofitApiBuilder: Retrofit by lazy { Retrofit
        .Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(jsonConverterFactory)
        .client(okHttpClient)
        .build()
    }

    abstract val retrofitApi: IApiType

}