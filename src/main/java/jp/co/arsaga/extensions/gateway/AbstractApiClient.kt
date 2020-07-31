package jp.co.arsaga.extensions.gateway

import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit

abstract class AbstractApiClient<IApiType> {

    protected abstract val baseUrl: String

    protected abstract val isDebug: Boolean

    protected abstract val jsonConverterFactory: Converter.Factory

    private val authorizeInterceptor by lazy {
        Interceptor {chain ->
            chain
                .request()
                .newBuilder()
                .also {
                    setAuthorizeHeader(it)
                }
                .build().let {
                    chain.proceed(it)
                }
        }
    }

    protected abstract fun setAuthorizeHeader(requestBuilder: Request.Builder)

    protected abstract fun setRefreshToken(requestBuilder: Request.Builder)

    private val authenticator by lazy {
        object : Authenticator {
            override fun authenticate(
                route: Route?,
                response: Response
            ): Request? = response
                    .request
                    .newBuilder()
                    .also { setRefreshToken(it) }
                    .build()
        }
    }

    private fun OkHttpClient.Builder.debugLog(): OkHttpClient.Builder = this.apply {
        if (isDebug) {
            addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
        }
    }

    protected open fun adjustOkHttpClient(okHttpClientBuilder: OkHttpClient.Builder) {}

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .debugLog()
        .addInterceptor(authorizeInterceptor)
        .authenticator(authenticator)
        .cache(null)
        .also { adjustOkHttpClient(it) }
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