
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import pt.ipt.dam2025.pawbuddy.retrofit.service.AdotamService
import pt.ipt.dam2025.pawbuddy.retrofit.service.AnimalService
import pt.ipt.dam2025.pawbuddy.retrofit.service.AuthService
import pt.ipt.dam2025.pawbuddy.retrofit.service.IntencaoDeAdocaoService
import pt.ipt.dam2025.pawbuddy.retrofit.service.UtilizadorService
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

object RetrofitProvider {

    const val BASE_URL =
        "https://pawbuddy-api-htdcasgkh8gphqa5.westeurope-01.azurewebsites.net/"
    private const val IMAGE_BASE_URL = BASE_URL

    fun fullImageUrl(imageName: String): String = IMAGE_BASE_URL + imageName

    private val cookieJar: CookieJar = object : CookieJar {
        private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val host = url.host
            val stored = cookieStore.getOrPut(host) { mutableListOf() }
            cookies.forEach { newCookie ->
                stored.removeAll { it.name == newCookie.name }
                stored.add(newCookie)
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: emptyList()
        }
    }

    private val gson: Gson = GsonBuilder().setLenient().create()

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(logging)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create(gson))
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()

    val animalService: AnimalService = retrofit.create(AnimalService::class.java)
    val intencaoService: IntencaoDeAdocaoService = retrofit.create(IntencaoDeAdocaoService::class.java)
    val utilizadorService: UtilizadorService = retrofit.create(UtilizadorService::class.java)
    val adotamService: AdotamService = retrofit.create(AdotamService::class.java)
    val authService: AuthService = retrofit.create(AuthService::class.java)
}
