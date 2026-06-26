package com.example.gamelock.data.remote

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import coil.ImageLoader
import android.content.Context

class RetryInterceptor(private val maxRetries: Int) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var retries = 0
        while (true) {
            try {
                val response = chain.proceed(chain.request())
                if (!response.isSuccessful && response.code >= 500 && retries < maxRetries) {
                    response.close()
                    retries++
                    Thread.sleep(1000L * retries)
                    continue
                }
                return response
            } catch (e: IOException) {
                if (retries >= maxRetries) throw e
                retries++
                Thread.sleep(1000L * retries)
            }
        }
    }
}

object RetrofitClient {
    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    })

    private val sslContext = SSLContext.getInstance("SSL").apply {
        init(null, trustAllCerts, java.security.SecureRandom())
    }

    private val okHttpClient = OkHttpClient.Builder()
        .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
        .hostnameVerifier { _, _ -> true }
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(RetryInterceptor(2))
        .build()

    private val steamHeadersInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()
        chain.proceed(request)
    }

    private val okHttpSteamClient = OkHttpClient.Builder()
        .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
        .hostnameVerifier { _, _ -> true }
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(steamHeadersInterceptor)
        .addInterceptor(RetryInterceptor(1))
        .build()

    val api: RawgApiService = Retrofit.Builder()
        .baseUrl("https://api.rawg.io/api/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(RawgApiService::class.java)

    val steamApi: SteamApiService = Retrofit.Builder()
        .baseUrl("https://store.steampowered.com/")
        .client(okHttpSteamClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SteamApiService::class.java)

    fun createImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .okHttpClient(okHttpClient)
            .build()
    }
}
