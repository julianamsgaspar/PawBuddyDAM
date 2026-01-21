package pt.ipt.dam2025.pawbuddy.repository

import android.content.Context
import pt.ipt.dam2025.pawbuddy.model.LoginRequest
import pt.ipt.dam2025.pawbuddy.session.SessionManager

class AuthRepository(private val context: Context) {
    private val session = SessionManager(context)

    suspend fun login(email: String, pass: String): Result<Unit> {
        return try {
            val resp = RetrofitProvider.authService.login(LoginRequest(email, pass))
            session.saveLogin(resp.id, resp.isAdmin)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
