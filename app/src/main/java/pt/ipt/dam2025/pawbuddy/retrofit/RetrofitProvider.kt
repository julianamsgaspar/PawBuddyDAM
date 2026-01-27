package pt.ipt.dam2025.pawbuddy.retrofit

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import pt.ipt.dam2025.pawbuddy.BuildConfig
import pt.ipt.dam2025.pawbuddy.retrofit.service.AdotamService
import pt.ipt.dam2025.pawbuddy.retrofit.service.AnimalService
import pt.ipt.dam2025.pawbuddy.retrofit.service.AuthService
import pt.ipt.dam2025.pawbuddy.retrofit.service.IntencaoDeAdocaoService
import pt.ipt.dam2025.pawbuddy.retrofit.service.UtilizadorService
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

/**
 * Provedor central da infraestrutura de comunicação remota (Retrofit + OkHttp).
 *
 * Este objeto configura e disponibiliza:
 *  - URL base da API;
 *  - construção de URLs completos para recursos (ex.: imagens);
 *  - cliente HTTP (OkHttp) com suporte a cookies e logging;
 *  - instância Retrofit com conversores de payload e adaptadores de chamadas;
 *  - instâncias tipadas dos serviços REST (interfaces Retrofit).
 *
 * No contexto da aplicação PawBuddy, este componente constitui o ponto único de
 * configuração da camada de rede, promovendo consistência e reutilização.
 */
object RetrofitProvider {

    /**
     * URL base do backend (API REST).
     *
     * A origem deste valor é o BuildConfig (definido em build.gradle.kts),
     * permitindo separar ambientes (debug/release) sem alterar o código.
     *
     * Observação: deve terminar com "/" para compatibilidade com Retrofit.
     */
    private const val BASE_URL: String = BuildConfig.BASE_URL

    /**
     * URL base utilizada para construção de URLs de imagens.
     *
     * Nesta implementação coincide com [BASE_URL], pressupondo que o backend
     * serve recursos de imagem a partir do mesmo domínio.
     */
    private const val IMAGE_BASE_URL: String = BASE_URL

    /**
     * Constrói o URL completo para uma imagem remota a partir do seu nome/caminho.
     *
     * @param imageName Nome do ficheiro ou caminho relativo devolvido pela API.
     * @return URL completo para acesso ao recurso de imagem.
     */
    fun fullImageUrl(imageName: String): String = IMAGE_BASE_URL + imageName

    /**
     * Implementação de [CookieJar] para gestão de cookies de sessão em memória.
     *
     * Esta estrutura permite:
     *  - armazenar cookies devolvidos pelo backend (ex.: após login);
     *  - anexar automaticamente cookies relevantes a pedidos subsequentes.
     *
     * Nota: os cookies são armazenados apenas em memória (lifetime do processo).
     * Após encerramento da aplicação, a sessão deixa de estar disponível.
     */
    private val cookieJar: CookieJar = object : CookieJar {

        /**
         * Armazém de cookies indexado por host.
         *
         * Chave: hostname (ex.: api.exemplo.com)
         * Valor: lista mutável de cookies associados ao host.
         */
        private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

        /**
         * Guarda cookies devolvidos numa resposta HTTP.
         *
         * Substitui cookies com o mesmo nome (política simples de atualização),
         * garantindo que a versão mais recente permanece ativa.
         *
         * @param url URL do endpoint que devolveu os cookies.
         * @param cookies Lista de cookies devolvidos no cabeçalho Set-Cookie.
         */
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val host = url.host
            val stored = cookieStore.getOrPut(host) { mutableListOf() }

            cookies.forEach { newCookie ->
                stored.removeAll { it.name == newCookie.name }
                stored.add(newCookie)
            }
        }

        /**
         * Carrega cookies a anexar a um pedido HTTP.
         *
         * Retorna os cookies armazenados para o host do pedido; caso não existam,
         * retorna lista vazia.
         *
         * @param url URL de destino do pedido.
         * @return Lista de cookies aplicáveis ao pedido.
         */
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: emptyList()
        }
    }

    /**
     * Instância Gson utilizada pelo Retrofit para conversão JSON.
     *
     * A opção `setLenient()` permite parsing menos estrito (tolerante a pequenas
     * inconsistências no JSON), útil em fases de desenvolvimento e integração.
     */
    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    /**
     * Interceptor de logging HTTP.
     *
     * Em debug, regista BODY (headers e corpo). Em release, recomenda-se reduzir
     * ou desativar para evitar exposição de dados sensíveis.
     */
    private val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    /**
     * Cliente HTTP (OkHttp) configurado com:
     *  - gestão de cookies de sessão através de [cookieJar];
     *  - logging de requests/responses através de [logging].
     */
    private val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(logging)
        .build()

    /**
     * Instância Retrofit configurada para consumo da API REST.
     *
     * Inclui:
     *  - [ScalarsConverterFactory] para respostas simples (ex.: texto puro);
     *  - [GsonConverterFactory] para conversão JSON <-> objetos Kotlin;
     *  - [RxJava2CallAdapterFactory] para compatibilidade com chamadas RxJava,
     *    caso alguns componentes do projeto a utilizem.
     */
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create(gson))
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()

    /**
     * Serviço de animais (operações CRUD e listagens).
     */
    val animalService: AnimalService =
        retrofit.create(AnimalService::class.java)

    /**
     * Serviço de intenções de adoção (consulta, criação, atualização e remoção).
     */
    val intencaoService: IntencaoDeAdocaoService =
        retrofit.create(IntencaoDeAdocaoService::class.java)

    /**
     * Serviço de utilizadores (operações de consulta e remoção).
     */
    val utilizadorService: UtilizadorService =
        retrofit.create(UtilizadorService::class.java)

    /**
     * Serviço de adoções (consulta e remoção de registos).
     */
    val adotamService: AdotamService =
        retrofit.create(AdotamService::class.java)

    /**
     * Serviço de autenticação (login, registo e logout).
     */
    val authService: AuthService =
        retrofit.create(AuthService::class.java)
}
