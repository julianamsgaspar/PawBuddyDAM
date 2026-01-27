package pt.ipt.dam2025.pawbuddy.session

import android.content.Context

/**
 * SessionManager
 *
 * Classe responsável pela gestão de sessão do utilizador na aplicação PawBuddy.
 *
 * Esta implementação utiliza o mecanismo de armazenamento persistente
 * SharedPreferences do Android, permitindo guardar e recuperar informação
 * essencial da sessão entre execuções da aplicação.
 *
 * Objetivos principais:
 *  - Determinar se existe um utilizador autenticado
 *  - Armazenar o identificador do utilizador autenticado
 *  - Identificar se o utilizador possui privilégios de administrador
 *  - Gerir o encerramento da sessão (logout)
 *
 * Âmbito académico:
 * Esta classe segue o princípio da separação de responsabilidades,
 * isolando a lógica de sessão da lógica de interface e de negócio,
 * promovendo reutilização, legibilidade e manutenção do código.
 *
 * @param context Contexto da aplicação necessário para acesso às SharedPreferences
 */
class SessionManager(context: Context) {

    /**
     * Instância de SharedPreferences associada à aplicação.
     *
     * "PawBuddyPrefs" identifica o ficheiro de preferências privadas
     * da aplicação, acessível apenas no seu contexto.
     */
    private val prefs =
        context.getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)

    /**
     * Verifica se existe um utilizador autenticado.
     *
     * @return true se o utilizador estiver autenticado, false caso contrário
     */
    fun isLogged(): Boolean =
        prefs.getBoolean("isLogged", false)

    /**
     * Obtém o identificador do utilizador autenticado.
     *
     * @return ID do utilizador ou -1 caso não exista sessão ativa
     */
    fun userId(): Int =
        prefs.getInt("utilizadorId", -1)

    /**
     * Indica se o utilizador autenticado possui permissões de administrador.
     *
     * @return true se for administrador, false caso contrário
     */
    fun isAdmin(): Boolean =
        prefs.getBoolean("isAdmin", false)

    /**
     * Guarda os dados da sessão após autenticação bem-sucedida.
     *
     * Armazena:
     *  - Estado de login
     *  - Identificador do utilizador
     *  - Perfil de permissões (administrador ou utilizador normal)
     *
     * O método apply() é utilizado para efetuar a escrita de forma assíncrona,
     * garantindo melhor desempenho sem bloquear a thread principal.
     *
     * @param userId Identificador único do utilizador
     * @param isAdmin Indica se o utilizador tem privilégios administrativos
     */
    fun saveLogin(userId: Int, isAdmin: Boolean) {
        prefs.edit()
            .putBoolean("isLogged", true)
            .putInt("utilizadorId", userId)
            .putBoolean("isAdmin", isAdmin)
            .apply()
    }

    /**
     * Termina a sessão do utilizador.
     *
     * Remove todos os dados persistidos associados à sessão,
     * garantindo que o utilizador deixa de estar autenticado.
     *
     * Este método deve ser invocado explicitamente durante o processo
     * de logout da aplicação.
     */
    fun logout() {
        prefs.edit().clear().apply()
    }
}
