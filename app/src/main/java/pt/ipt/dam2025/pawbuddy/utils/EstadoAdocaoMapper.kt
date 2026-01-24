package pt.ipt.dam2025.pawbuddy.utils

import android.content.Context
import pt.ipt.dam2025.pawbuddy.R

object EstadoAdocaoMapper {

    fun toText(context: Context, estado: Int): String {
        return when (estado) {
            0 -> context.getString(R.string.estado_reservado)
            1 -> context.getString(R.string.estado_em_processo)
            2 -> context.getString(R.string.estado_em_validacao)
            3 -> context.getString(R.string.estado_concluido)
            4 -> context.getString(R.string.estado_rejeitado)
            else -> context.getString(R.string.estado_desconhecido)
        }
    }
}
