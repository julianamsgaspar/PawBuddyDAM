package pt.ipt.dam2025.pawbuddy.retrofit

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import pt.ipt.dam2025.pawbuddy.network.CookieInterceptor
import pt.ipt.dam2025.pawbuddy.retrofit.service.AdotamService
import pt.ipt.dam2025.pawbuddy.retrofit.service.AnimalService
import pt.ipt.dam2025.pawbuddy.retrofit.service.IntencaoDeAdocaoService
import pt.ipt.dam2025.pawbuddy.retrofit.service.UtilizadorService
import pt.ipt.dam2025.pawbuddy.retrofit.service.AuthService
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.stream.Stream.builder

class RetrofitInitializer {
/*
    // CookieJar confiável usando URL como chave
    private val cookieJar = object : CookieJar {
        private val cookieStore = mutableMapOf<String, List<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.toString()] = cookies
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            // procura cookie exato para a URL ou retorna empty
            return cookieStore[url.toString()] ?: emptyList()
        }
    }*/

    // Gson leniente
    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    private val client = OkHttpClient.Builder()
        .addInterceptor(CookieInterceptor())   // <- AQUI
        .build()

    /*private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .build()*/
    companion object {
        // 🌍 BASE URL GLOBAL
        const val BASE_URL = "http://10.0.2.2:5053/"
        private const val IMAGE_BASE_URL = "http://10.0.2.2:5053/"

        // 🔗 Constrói URL completa para imagens
        fun fullImageUrl(imageName: String): String {
            return IMAGE_BASE_URL + imageName
        }
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
       //.client(client)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create(gson))
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()


        /*
        .addConverterFactory(ScalarsConverterFactory.create()) //important
        .addConverterFactory(GsonConverterFactory.create(gson))
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()*/

    fun animalService(): AnimalService = retrofit.create(AnimalService::class.java)
    fun intencaoService(): IntencaoDeAdocaoService = retrofit.create(IntencaoDeAdocaoService::class.java)
    fun utilizadorService(): UtilizadorService = retrofit.create(UtilizadorService::class.java)
    fun adotamService(): AdotamService = retrofit.create(AdotamService::class.java)

    fun authService(): AuthService = retrofit.create(AuthService::class.java)

    }


