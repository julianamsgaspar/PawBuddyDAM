package pt.ipt.dam2025.pawbuddy.repository

import android.content.Context
import pt.ipt.dam2025.pawbuddy.model.LoginRequest
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitProvider
import pt.ipt.dam2025.pawbuddy.session.SessionManager

/**
 * Repositório responsável pela autenticação de utilizadores.
 *
 * Esta classe integra a camada de repositório da aplicação, atuando como
 * intermediário entre a camada de apresentação (ViewModel/UI) e a camada
 * de rede (API REST).
 *
 * O AuthRepository encapsula a lógica associada ao processo de login,
 * incluindo a comunicação com o serviço remoto e a persistência do estado
 * de autenticação localmente através do SessionManager.
 *
 * @param context Contexto da aplicação, utilizado para inicialização do gestor de sessão.
 */
class AuthRepository(private val context: Context) {

    /** Gestor responsável pela persistência do estado de sessão do utilizador */
    private val session = SessionManager(context)

    /**
     * Efetua o processo de autenticação do utilizador.
     *
     * Este metodo comunica com a API REST para autenticar o utilizador
     * através das credenciais fornecidas. Em caso de sucesso, a informação
     * relevante da sessão (identificador do utilizador e permissões) é
     * persistida localmente.
     *
     * O metodo é suspenso (suspend) para permitir execução assíncrona
     * utilizando coroutines, evitando bloqueios da thread principal.
     *
     * @param email Endereço de correio eletrónico do utilizador.
     * @param pass Palavra-passe do utilizador.
     * @return Resultado da operação de login encapsulado num objeto [Result].
     *         Em caso de sucesso, retorna [Result.success].
     *         Em caso de erro, retorna [Result.failure] com a exceção associada.
     */
    suspend fun login(email: String, pass: String): Result<Unit> {
        return try {
            val resp = RetrofitProvider.authService
                .login(LoginRequest(email, pass))

            // Guarda informação da sessão após autenticação bem-sucedida
            session.saveLogin(resp.id, resp.isAdmin)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
