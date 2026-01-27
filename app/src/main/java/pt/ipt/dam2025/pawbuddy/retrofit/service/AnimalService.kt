package pt.ipt.dam2025.pawbuddy.retrofit.service

import okhttp3.MultipartBody
import okhttp3.RequestBody
import pt.ipt.dam2025.pawbuddy.model.Animal
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * Serviço Retrofit responsável pela gestão de animais.
 *
 * Esta interface define os endpoints da API REST relacionados com a entidade
 * Animal, permitindo operações de listagem, consulta, criação, atualização
 * e remoção de animais no sistema PawBuddy.
 *
 * Cada método corresponde diretamente a um endpoint REST do backend,
 * seguindo o padrão Service do Retrofit.
 */
interface AnimalService {

    /**
     * Obtém a lista completa de animais registados no sistema.
     *
     * Normalmente utilizado em contextos administrativos.
     *
     * @return Lista de todos os animais.
     */
    @GET("api/Animal")
    suspend fun listarAnimais(): List<Animal>

    /**
     * Obtém a lista de animais disponíveis para adoção.
     *
     * Corresponde a um endpoint específico do backend que filtra
     * apenas os animais que ainda não foram adotados.
     *
     * @return Lista de animais disponíveis para adoção.
     */
    @GET("api/Animal/disponiveis")
    suspend fun getDisponiveis(): List<Animal>

    /**
     * Obtém os detalhes de um animal específico.
     *
     * @param id Identificador único do animal.
     * @return Objeto [Animal] correspondente ao identificador fornecido.
     */
    @GET("api/Animal/{id}")
    suspend fun getAnimal(
        @Path("id") id: Int
    ): Animal

    /**
     * Cria um novo registo de animal no sistema.
     *
     * Este método utiliza um pedido multipart/form-data para permitir
     * o envio simultâneo de dados textuais e de um ficheiro de imagem.
     *
     * @param nome Nome do animal.
     * @param raca Raça do animal.
     * @param especie Espécie do animal (ex.: cão, gato).
     * @param idade Idade do animal.
     * @param genero Género do animal.
     * @param cor Cor predominante do animal.
     * @param imagem Ficheiro de imagem associado ao animal.
     * @return Objeto [Animal] criado.
     */
    @Multipart
    @POST("api/Animal")
    suspend fun criarAnimal(
        @Part("nome") nome: RequestBody,
        @Part("raca") raca: RequestBody,
        @Part("especie") especie: RequestBody,
        @Part("idade") idade: RequestBody,
        @Part("genero") genero: RequestBody,
        @Part("cor") cor: RequestBody,
        @Part imagem: MultipartBody.Part
    ): Animal

    /**
     * Atualiza os dados de um animal existente.
     *
     * Permite a atualização dos campos do animal e, opcionalmente,
     * a substituição da imagem associada.
     *
     * @param id Identificador do animal a atualizar.
     * @param idPart Identificador do animal enviado no corpo do pedido,
     *               necessário para compatibilidade com o backend.
     * @param nome Nome do animal.
     * @param raca Raça do animal.
     * @param idade Idade do animal.
     * @param genero Género do animal.
     * @param especie Espécie do animal.
     * @param cor Cor predominante do animal.
     * @param imagem Nova imagem do animal (opcional).
     */
    @Multipart
    @PUT("api/Animal/{id}")
    suspend fun atualizarAnimal(
        @Path("id") id: Int,
        @Part("id") idPart: RequestBody,
        @Part("nome") nome: RequestBody,
        @Part("raca") raca: RequestBody,
        @Part("idade") idade: RequestBody,
        @Part("genero") genero: RequestBody,
        @Part("especie") especie: RequestBody,
        @Part("cor") cor: RequestBody,
        @Part imagem: MultipartBody.Part? = null
    )

    /**
     * Remove um animal do sistema.
     *
     * Corresponde a um pedido HTTP DELETE ao endpoint `/api/Animal/{id}`.
     *
     * @param id Identificador do animal a eliminar.
     */
    @DELETE("api/Animal/{id}")
    suspend fun eliminarAnimal(
        @Path("id") id: Int
    )

    /**
     * Obtém a lista de animais associados a um utilizador específico.
     *
     * Utilizado para listar animais registados ou geridos por um determinado
     * utilizador no sistema.
     *
     * @param utilizadorId Identificador do utilizador.
     * @return Lista de animais associados ao utilizador.
     */
    @GET("utilizadores/{id}/Animal")
    suspend fun listarAnimaisPorUtilizador(
        @Path("id") utilizadorId: Int
    ): List<Animal>
}
