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

    private val session = SessionManager(context)

    suspend fun login(email: String, pass: String): Result<Unit> {
        return try {
            val resp = RetrofitProvider.authService
                .login(LoginRequest(email, pass))

            // Guarda sessão completa (id, nome, permissões)
            session.saveLogin(
                userId = resp.id,
                userName = resp.user,
                isAdmin = resp.isAdmin
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

