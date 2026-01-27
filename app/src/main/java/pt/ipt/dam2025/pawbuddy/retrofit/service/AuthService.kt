package pt.ipt.dam2025.pawbuddy.retrofit.service

import pt.ipt.dam2025.pawbuddy.model.LoginRequest
import pt.ipt.dam2025.pawbuddy.model.LoginResponse
import pt.ipt.dam2025.pawbuddy.model.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Serviço Retrofit responsável pela autenticação e gestão de utilizadores.
 *
 * Esta interface define os endpoints da API REST associados aos processos
 * de autenticação (login/logout) e registo de utilizadores no sistema PawBuddy.
 *
 * Segue o padrão Service do Retrofit, permitindo o mapeamento direto
 * entre métodos Kotlin e operações HTTP expostas pelo backend.
 */
interface AuthService {

    /**
     * Efetua a autenticação de um utilizador.
     *
     * Corresponde a um pedido HTTP POST ao endpoint `/api/AuthController/login`,
     * enviando as credenciais do utilizador no corpo do pedido.
     *
     * @param body Objeto [LoginRequest] contendo as credenciais de autenticação.
     * @return Objeto [LoginResponse] com a informação do utilizador autenticado.
     */
    @POST("api/AuthController/login")
    suspend fun login(
        @Body body: LoginRequest
    ): LoginResponse

    /**
     * Regista um novo utilizador no sistema.
     *
     * Corresponde a um pedido HTTP POST ao endpoint `/api/AuthController/register`,
     * enviando os dados de registo no corpo do pedido.
     *
     * @param body Objeto [RegisterRequest] com os dados do novo utilizador.
     * @return Objeto [LoginResponse] com a informação do utilizador registado.
     */
    @POST("api/AuthController/register")
    suspend fun register(
        @Body body: RegisterRequest
    ): LoginResponse

    /**
     * Termina a sessão do utilizador autenticado.
     *
     * Corresponde a um pedido HTTP POST ao endpoint `/api/AuthController/logout`.
     * O backend invalida o cookie ou sessão associada ao utilizador.
     */
    @POST("api/AuthController/logout")
    suspend fun logout(): Unit
}
