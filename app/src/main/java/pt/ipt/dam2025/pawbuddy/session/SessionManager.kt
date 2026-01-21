package pt.ipt.dam2025.pawbuddy.session

import android.content.Context

class SessionManager(context: Context) {

    private val prefs =
        context.getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)

    fun isLogged(): Boolean =
        prefs.getBoolean("isLogged", false)

    fun userId(): Int =
        prefs.getInt("utilizadorId", -1)

    fun isAdmin(): Boolean =
        prefs.getBoolean("isAdmin", false)

    fun saveLogin(userId: Int, isAdmin: Boolean) {
        prefs.edit()
            .putBoolean("isLogged", true)
            .putInt("utilizadorId", userId)
            .putBoolean("isAdmin", isAdmin)
            .apply()
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}
