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

    protected abstract fun setAuthorizeHeader(requestBuilder: Request.Builder): Request.Builder

    protected abstract fun setRefreshToken(continuation: Continuation<Request.Builder?>, requestBuilder: Request.Builder): Request.Builder?

    protected abstract val maxRetryCount: Int

    private val authenticator by lazy {
        object : Authenticator {
            private fun Response.retryCount(): Int = generateSequence(priorResponse) { it.priorResponse }
                .count()

            override fun authenticate(
                route: Route?,
                response: Response
            ): Request? = response
                .takeIf { it.retryCount().run {
                    Timber.d("Auth::refreshTokenRetryCount${this}")
                    maxRetryCount > this
                } }
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
            addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
        }
    }

    protected open fun adjustOkHttpClient(okHttpClientBuilder: OkHttpClient.Builder): OkHttpClient.Builder = okHttpClientBuilder

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .debugLog()
        .addInterceptor(authorizeInterceptor)
        .authenticator(authenticator)
        .cache(null)
        .let { adjustOkHttpClient(it) }
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