package pt.ipt.dam2025.pawbuddy.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Armazenamento centralizado do cookie de sessão.
 *
 * Este objeto mantém em memória o cookie de sessão devolvido pelo backend
 * após a autenticação do utilizador, permitindo que seja reutilizado
 * automaticamente em pedidos HTTP subsequentes.
 *
 * O cookie representa o estado de sessão do utilizador autenticado,
 * sendo essencial para a manutenção de sessões baseadas em cookies.
 */
object CookieStorage {

    /**
     * Cookie de sessão atualmente ativo.
     *
     * Pode ser nulo caso o utilizador ainda não tenha efetuado login
     * ou após término explícito da sessão.
     */
    var sessionCookie: String? = null
}

/**
 * Interceptor responsável pela gestão automática de cookies HTTP.
 *
 * Esta classe implementa a interface [Interceptor] da biblioteca OkHttp
 * e tem como objetivo:
 *  - anexar o cookie de sessão aos pedidos HTTP enviados para a API;
 *  - capturar o cookie devolvido pelo backend após autenticação.
 *
 * Desta forma, garante-se a persistência da sessão do utilizador
 * sem necessidade de reenvio de credenciais em cada pedido.
 */
class CookieInterceptor : Interceptor {

    /**
     * Interceta pedidos e respostas HTTP.
     *
     * Antes do envio do pedido, adiciona o cookie de sessão, caso exista.
     * Após a receção da resposta, verifica a presença do cabeçalho
     * "Set-Cookie" e atualiza o armazenamento local do cookie.
     *
     * @param chain Cadeia de interceptação do OkHttp.
     * @return Resposta HTTP resultante do pedido interceptado.
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        // Adiciona o cookie de sessão ao pedido, caso exista
        CookieStorage.sessionCookie?.let {
            requestBuilder.addHeader("Cookie", it)
        }

        val response = chain.proceed(requestBuilder.build())

        // Captura o cookie devolvido pelo backend (ex.: após login)
        val setCookie = response.headers("Set-Cookie")
        if (setCookie.isNotEmpty()) {
            CookieStorage.sessionCookie = setCookie[0]
        }

        return response
    }
}
