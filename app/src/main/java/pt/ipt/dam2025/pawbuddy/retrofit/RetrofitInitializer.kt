package pt.ipt.dam2025.pawbuddy.retrofit

/**
 * Utilitário de apoio à construção de URLs para recursos remotos.
 *
 * Esta classe existe para centralizar operações simples relacionadas com a
 * obtenção de recursos servidos pelo backend (por exemplo, imagens), evitando
 * concatenações repetidas de strings ao longo da aplicação.
 *
 * A lógica efetiva de construção do URL encontra-se no [RetrofitProvider],
 * mantendo a configuração da camada de rede num único ponto. O presente
 * inicializador funciona como uma fachada (facade) mínima, expondo apenas
 * funcionalidades utilitárias relevantes para a camada de apresentação.
 */
class RetrofitInitializer {

    companion object {

        /**
         * Constrói o URL completo para uma imagem remota a partir do seu nome/caminho.
         *
         * Este método delega a construção do URL ao [RetrofitProvider], garantindo
         * consistência na forma como os recursos são endereçados e permitindo
         * alterações futuras (ex.: migração para CDN ou alteração do caminho base)
         * sem necessidade de modificar a camada de apresentação.
         *
         * @param imageName Nome do ficheiro ou caminho relativo devolvido pela API.
         * @return URL completo para acesso ao recurso de imagem.
         */
        fun fullImageUrl(imageName: String): String {
            return RetrofitProvider.fullImageUrl(imageName)
        }
    }
}
