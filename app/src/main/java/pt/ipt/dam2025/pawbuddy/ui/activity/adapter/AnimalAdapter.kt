package pt.ipt.dam2025.pawbuddy.ui.activity.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.ItemAnimalBinding
import pt.ipt.dam2025.pawbuddy.model.Animal
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer

/**
 * Adapter para apresentação de uma lista de [Animal] numa [RecyclerView].
 *
 * Enquadramento académico:
 * - A RecyclerView implementa reciclagem de vistas (padrão ViewHolder), reduzindo o custo de criação
 *   e contribuindo para desempenho em listas extensas.
 * - Este adapter utiliza ViewBinding ([ItemAnimalBinding]) para acesso seguro e tipado às views,
 *   mitigando erros típicos de findViewById e melhorando manutenibilidade.
 * - A seleção de um item é externalizada através de um *callback* ([onClick]), favorecendo
 *   separação de responsabilidades: o adapter apenas emite eventos; a lógica de navegação/ações
 *   pertence ao Fragment/Activity/ViewModel.
 *
 * @param onClick Função invocada quando o utilizador seleciona um animal na lista.
 */
class AnimalAdapter(
    private val onClick: (Animal) -> Unit
) : RecyclerView.Adapter<AnimalAdapter.ViewHolder>() {

    /**
     * Coleção interna de dados apresentada pela RecyclerView.
     *
     * Nota:
     * - Mantida como [List] para impor imutabilidade externa (o chamador fornece uma lista,
     *   mas o adapter não expõe operações de mutação granular).
     * - Inicialmente vazia, evitando nullability e simplificando a lógica do ciclo de vida.
     */
    private var lista: List<Animal> = emptyList()

    /**
     * Substitui a lista interna e solicita re-renderização total.
     *
     * Observação académica:
     * - notifyDataSetChanged() é simples e funcional, mas invalida todos os itens e pode ser menos eficiente.
     * - Em cenários de grande escala/atualizações frequentes, é comum recorrer a DiffUtil/ListAdapter
     *   para atualizações incrementais e animações mais fiéis.
     *
     * @param novaLista Nova lista de [Animal] a apresentar.
     */
    fun submitList(novaLista: List<Animal>) {
        lista = novaLista
        notifyDataSetChanged()
    }

    /**
     * ViewHolder responsável por associar (bind) os dados de [Animal] às vistas do item.
     *
     * Justificação:
     * - Encapsula a lógica de apresentação por item.
     * - Reutiliza a mesma instância de vistas ao longo do scroll, aumentando performance.
     */
    inner class ViewHolder(private val binding: ItemAnimalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        /**
         * Realiza o binding dos campos do modelo [Animal] para o layout (ItemAnimalBinding).
         *
         * Regras de apresentação implementadas:
         * - Linhas compactas (ex.: "Espécie · Raça", "Idade · Género") para evitar poluição visual
         *   em grelhas (grid).
         * - Tratamento defensivo de nulos e strings vazias, evitando placeholders redundantes.
         * - Carregamento de imagem com Glide, com placeholder e imagem de erro.
         *
         * @param animal Instância do modelo a apresentar.
         */
        fun bind(animal: Animal) {
            // Nome (linha 1)
            /**
             * Nome do animal:
             * - Usa string vazia quando inexistente (evita "null" na UI).
             * - Poderia alternativamente usar placeholder via resources; aqui privilegia-se UI limpa.
             */
            binding.tvNome.text = animal.nome ?: ""

            // Linha 2: Espécie · Raça (compacto)
            /**
             * Linha informativa 2 (compacta):
             * - Concatena "Espécie · Raça" quando ambos existem.
             * - Caso exista apenas um dos campos, apresenta apenas esse.
             * - Caso nenhum exista, apresenta vazio.
             *
             * Nota:
             * - O separador "·" é hardcoded; para i18n/consistência, poderia ser recurso de string.
             * - A escolha de vazio evita ruído visual em itens com dados incompletos.
             */
            val especie = animal.especie ?: ""
            val raca = animal.raca ?: ""
            binding.tvEspecie.text = when {
                especie.isNotBlank() && raca.isNotBlank() -> "$especie · $raca"
                especie.isNotBlank() -> especie
                raca.isNotBlank() -> raca
                else -> ""
            }

            // Linha 3: Idade · Género (compacto)
            /**
             * Linha informativa 3 (compacta):
             * - Idade é convertida para String quando existe.
             * - Aplica regra semelhante à linha 2:
             *   - "Idade · Género" quando ambos existem,
             *   - caso contrário o campo disponível,
             *   - vazio se nenhum existir.
             *
             * Nota:
             * - O formato da idade não explicita unidade ("anos"). Se necessário, deveria ser resolvido
             *   com recursos de string plural (plurals) para suportar i18n corretamente.
             */
            val idade = animal.idade?.toString() ?: ""
            val genero = animal.genero ?: ""
            binding.tvIdade.text = when {
                idade.isNotBlank() && genero.isNotBlank() -> "$idade · $genero"
                idade.isNotBlank() -> "$idade"
                genero.isNotBlank() -> genero
                else -> ""
            }

            // Campos “longos” (no grid ficam feios) — só se existirem no layout
            // Se no teu novo XML estes TextViews estiverem GONE, não faz mal setar ou não.
            /**
             * Campos adicionais “longos”:
             * - O comentário indica que, num layout em grelha, campos extensos (raça, cor, género isolado)
             *   podem degradar a estética/legibilidade.
             * - A implementação seta explicitamente strings vazias.
             *
             * Consideração:
             * - Se as views estiverem definidas como GONE no XML, estas atribuições não causam erro,
             *   mas também são redundantes do ponto de vista visual.
             */
            binding.tvRaca.text = ""    // ou remove completamente se no XML estiver gone
            binding.tvCor.text = ""
            binding.tvGenero.text = ""

            // Imagem
            /**
             * Carregamento de imagem com Glide:
             * - Constrói o URL absoluto com [RetrofitInitializer.fullImageUrl].
             * - Usa placeholder enquanto a imagem carrega (R.drawable.R.drawable.ic_pet_placeholder).
             * - Usa imagem de erro (R.drawable.ic_pet_placeholder) se falhar o carregamento.
             * - centerCrop() assegura preenchimento do ImageView com corte proporcional.
             *
             * Nota técnica:
             * - O uso de animal.imagem.toString() pode produzir "null" se imagem == null.
             *   Glide tende a tratar como falha e cair no .error(), mas esta decisão afeta logs e cache.
             *   Uma abordagem alternativa seria passar null explicitamente; contudo, não se altera código.
             */
            Glide.with(binding.ivAnimal.context)
                .load(RetrofitInitializer.fullImageUrl(animal.imagem.toString()))
                .placeholder(R.drawable.ic_pet_placeholder)
                .error(R.drawable.ic_pet_placeholder)
                .centerCrop()
                .into(binding.ivAnimal)

            /**
             * Evento de clique no item:
             * - Emite a seleção para o exterior através de [onClick].
             * - Mantém o adapter desacoplado de navegação/negócio.
             */
            binding.root.setOnClickListener { onClick(animal) }
        }
    }

    /**
     * Infla o layout do item e cria o respetivo [ViewHolder].
     *
     * @param parent ViewGroup contentor do item.
     * @param viewType Tipo de view (não utilizado para múltiplos tipos neste adapter).
     * @return Instância de [ViewHolder] com binding inicializado.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAnimalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    /**
     * Devolve o número de itens atualmente disponíveis na lista.
     */
    override fun getItemCount(): Int = lista.size

    /**
     * Liga (bind) o item na posição [position] ao [holder].
     *
     * Nota:
     * - A obtenção do elemento é direta (lista[position]); pressupõe que a lista está coerente
     *   com o valor devolvido por [getItemCount].
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(lista[position])
    }
}
