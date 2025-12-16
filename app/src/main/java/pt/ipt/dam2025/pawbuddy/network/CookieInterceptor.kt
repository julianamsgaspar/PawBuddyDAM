package pt.ipt.dam2025.pawbuddy.network


import okhttp3.Interceptor
import okhttp3.Response

object CookieStorage {
    var sessionCookie: String? = null
}

class CookieInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        // envia cookie se existir
        CookieStorage.sessionCookie?.let {
            requestBuilder.addHeader("Cookie", it)
        }

        val response = chain.proceed(requestBuilder.build())

        // captura cookie do login
        val setCookie = response.headers("Set-Cookie")
        if (setCookie.isNotEmpty()) {
            CookieStorage.sessionCookie = setCookie[0]
        }

        return response
    }
}