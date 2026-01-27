package pt.ipt.dam2025.pawbuddy.utils

import android.content.Context
import pt.ipt.dam2025.pawbuddy.R

object EstadoAdocaoMapper {

    /**
     * Mapper utilitário responsável por converter o estado lógico de uma intenção de adoção
     * (representado internamente por um valor inteiro) para uma representação textual
     * adequada à interface gráfica do utilizador.
     *
     * @param context Contexto Android necessário para acesso aos recursos de string.
     * @param estado Código inteiro que representa o estado da adoção.
     * @return String localizada correspondente ao estado fornecido.
     */
    fun toText(context: Context, estado: Int): String {
        return when (estado) {
            /**
             * Estado 0 — Reservado
             * Indica que o animal se encontra reservado, não estando disponível para novas intenções.
             */
            0 -> context.getString(R.string.estado_reservado)

            /**
             * Estado 1 — Em processo
             * Indica que a intenção de adoção foi submetida e se encontra em análise inicial.
             */
            1 -> context.getString(R.string.estado_em_processo)

            /**
             * Estado 2 — Em validação
             * Indica que a intenção passou fases preliminares e está em validação administrativa
             * ou documental.
             */
            2 -> context.getString(R.string.estado_em_validacao)

            /**
             * Estado 3 — Concluído
             * Indica que o processo de adoção foi concluído com sucesso.
             */
            3 -> context.getString(R.string.estado_concluido)

            /**
             * Estado 4 — Rejeitado
             * Indica que a intenção de adoção foi rejeitada.
             */
            4 -> context.getString(R.string.estado_rejeitado)

            /**
             * Estado desconhecido
             * Fallback defensivo para qualquer valor não reconhecido.
             * Garante estabilidade da UI mesmo perante dados inconsistentes
             * ou evoluções futuras do backend.
             */
            else -> context.getString(R.string.estado_desconhecido)
        }
    }
}
