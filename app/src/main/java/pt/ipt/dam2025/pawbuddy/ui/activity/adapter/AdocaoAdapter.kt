package pt.ipt.dam2025.pawbuddy.ui.activity.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import pt.ipt.dam2025.pawbuddy.databinding.ItemAdocaoBinding
import pt.ipt.dam2025.pawbuddy.model.Adotam
import pt.ipt.dam2025.pawbuddy.R
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter responsável pela ligação (binding) entre uma coleção de objetos de domínio [Adotam]
 * e a sua representação visual numa [RecyclerView].
 *
 * Enquadramento (académico):
 * - A RecyclerView implementa o padrão *ViewHolder* para minimizar chamadas a findViewById e reduzir
 *   o custo de criação de vistas, promovendo eficiência no *scroll* e reutilização de itens.
 * - Este adapter usa *ViewBinding* ([ItemAdocaoBinding]), o que aumenta a segurança (type-safety),
 *   reduz erros de nullability em vistas e melhora a manutenibilidade.
 * - A ação de eliminação é externalizada via *callback* ([onEliminarClick]), promovendo separação
 *   de responsabilidades (o adapter apenas emite eventos; o Fragment/Activity decide a lógica).
 *
 * @property lista Lista (imutável do ponto de vista do chamador) de adoções a apresentar.
 * @property onEliminarClick Função de retorno invocada quando o utilizador solicita eliminar um item.
 */
class AdocaoAdapter(
    private var lista: List<Adotam>,
    private val onEliminarClick: (Adotam) -> Unit
) : RecyclerView.Adapter<AdocaoAdapter.AdocaoViewHolder>() {

    init {
        /**
         * Ativa *stable ids* na RecyclerView.
         *
         * Justificação:
         * - Permite à RecyclerView identificar de forma consistente cada item entre atualizações,
         *   melhorando animações, preservação de estado e eficiência no *diffing* interno.
         * - É particularmente útil quando o backend ainda não fornece um identificador (id) fiável
         *   (ex.: id=0), recorrendo-se a uma chave alternativa considerada única no contexto.
         *
         * Nota metodológica:
         * - A estabilidade do ID pressupõe que [animalFK] seja único por registo de adoção e não se altere.
         */
        setHasStableIds(true)
    }

    /**
     * ViewHolder que encapsula as referências às vistas do item através de [ItemAdocaoBinding].
     *
     * Papel no padrão ViewHolder:
     * - Armazenar referências às vistas para evitar buscas repetidas.
     * - Servir como unidade de reutilização (recycling) entre diferentes posições da lista.
     */
    inner class AdocaoViewHolder(val binding: ItemAdocaoBinding) :
        RecyclerView.ViewHolder(binding.root)

    /**
     * Devolve o identificador estável do item na posição indicada.
     *
     * Critério adotado:
     * - Utiliza [animalFK] como chave única enquanto o backend devolve id=0.
     *
     * Implicações:
     * - Se existirem múltiplas adoções com o mesmo animalFK, ocorrerão colisões de ID,
     *   podendo originar comportamento incorreto na RecyclerView (itens trocados/estado errado).
     */
    override fun getItemId(position: Int): Long {
        // ✅ chave única (enquanto o backend devolve id=0)
        return lista[position].animalFK.toLong()
    }

    /**
     * Cria e instancia a vista de um item (inflation), encapsulando-a num [AdocaoViewHolder].
     *
     * @param parent ViewGroup que irá conter os itens (RecyclerView).
     * @param viewType Tipo de vista (útil em listas heterogéneas; aqui existe apenas 1 tipo).
     * @return ViewHolder pronto a ser reutilizado e associado a dados.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdocaoViewHolder {
        val binding = ItemAdocaoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AdocaoViewHolder(binding)
    }

    /**
     * Associa os dados do item na posição [position] às vistas do [holder].
     *
     * Boas práticas evidenciadas:
     * - Uso de recursos de *strings* para i18n (ex.: labels e separadores).
     * - Tratamento de valores nulos (safe calls) e placeholders consistentes (dash).
     * - Construção robusta da linha "espécie • raça" evitando separadores quando há campos vazios.
     * - Encaminhamento de eventos de UI para fora do adapter (callback).
     */
    override fun onBindViewHolder(holder: AdocaoViewHolder, position: Int) {
        val adocao = lista[position]
        val ctx = holder.itemView.context

        /**
         * Placeholder visual padronizado, tipicamente "-" (ou equivalente).
         * Mantém consistência quando a API não devolve valores ou quando o modelo está incompleto.
         */
        val dash = ctx.getString(R.string.placeholder_dash)

        /**
         * Nome do animal:
         * - Se existir o objeto aninhado animal e o nome estiver definido, apresenta-o.
         * - Caso contrário, utiliza o placeholder.
         */
        holder.binding.tvNomeAnimal.text = adocao.animal?.nome ?: dash

        // Evita " • " quando campos estão vazios
        /**
         * Composição de informação do animal (espécie e raça).
         *
         * Estratégia:
         * - Normaliza os campos com trim() e orEmpty() para evitar NPE e espaços.
         * - Filtra apenas valores não vazios (notBlank).
         * - Caso a lista resultante esteja vazia, utiliza placeholder.
         * - Caso contrário, junta com um separador configurado em strings.xml (i18n-friendly).
         */
        val especie = adocao.animal?.especie?.trim().orEmpty()
        val raca = adocao.animal?.raca?.trim().orEmpty()
        val info = listOf(especie, raca).filter { it.isNotBlank() }

        holder.binding.tvAnimalInfo.text =
            if (info.isEmpty()) dash else info.joinToString(ctx.getString(R.string.separator_dot))

        /**
         * Nome do utilizador que adotou:
         * - Se existir utilizador aninhado, usa o campo nome.
         * - Caso contrário, placeholder.
         * - A string final é formatada via recurso (label_adopted_by), facilitando tradução e consistência.
         */
        val nomeUser = adocao.utilizador?.nome ?: dash
        holder.binding.tvUtilizador.text = ctx.getString(R.string.label_adopted_by, nomeUser)

        /**
         * Data de adoção:
         * - Converte a data recebida (provavelmente em ISO 8601) para "dd/MM/yyyy".
         * - Caso não consiga interpretar, devolve a string original (estratégia de tolerância a falhas).
         * - Apresenta a data integrada num label (label_date) para suporte a i18n.
         */
        val dataFmt = formatarData(adocao.dateA)
        holder.binding.tvData.text = ctx.getString(R.string.label_date, dataFmt)

        /**
         * Evento de eliminação:
         * - O adapter não executa operações de rede/BD; apenas comunica a intenção.
         * - A eliminação efetiva (incluindo chamada à API) deve residir no Fragment/ViewModel,
         *   onde existe contexto de ciclo de vida e gestão de estado.
         */
        holder.binding.btnEliminar.setOnClickListener {
            // ✅ o fragment é que deve eliminar pelo animalFK
            onEliminarClick(adocao)
        }
    }

    /**
     * Número total de itens.
     *
     * Nota:
     * - A RecyclerView usa este valor para determinar limites de scroll e reciclagem.
     */
    override fun getItemCount(): Int = lista.size

    /**
     * Atualiza a coleção interna e força atualização completa da lista.
     *
     * Observação académica:
     * - notifyDataSetChanged() invalida toda a lista e é simples, porém menos eficiente.
     * - Para listas grandes, seria comum usar DiffUtil/ListAdapter para atualizações incrementais.
     * - Aqui, a opção privilegia simplicidade e previsibilidade.
     *
     * @param novaLista Nova lista de objetos [Adotam] a apresentar.
     */
    fun updateData(novaLista: List<Adotam>) {
        this.lista = novaLista
        notifyDataSetChanged()
    }

    /**
     * Converte uma data recebida como String (tipicamente ISO 8601) para o formato "dd/MM/yyyy".
     *
     * Estratégia de parsing:
     * - Tenta múltiplos padrões para acomodar variações comuns:
     *   - sem milissegundos
     *   - com milissegundos
     *   - com offset/timezone (XXX)
     * - Ao falhar um padrão, tenta o próximo.
     *
     * Robustez:
     * - Em caso de entrada nula/vazia retorna string vazia (evita ruído visual).
     * - Se nenhum padrão funcionar, devolve o valor original (fail-soft).
     *
     * Nota técnica:
     * - SimpleDateFormat é sensível a Locale e não é thread-safe; aqui está encapsulado e
     *   instanciado por chamada, logo sem concorrência e com comportamento determinístico.
     *
     * @param data Data em formato textual vinda do backend.
     * @return Data formatada (dd/MM/yyyy), string vazia, ou a string original se não for parseável.
     */
    private fun formatarData(data: String?): String {
        if (data.isNullOrBlank()) return ""

        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
        )

        for (pattern in formats) {
            try {
                val input = SimpleDateFormat(pattern, Locale.getDefault())
                val output = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                return output.format(input.parse(data)!!)
            } catch (_: Exception) {
                // tenta o próximo
                /**
                 * Exceções esperadas:
                 * - ParseException (formato incompatível)
                 * - IllegalArgumentException (padrão inválido)
                 * - NullPointerException evitada via checks anteriores, mas parse() pode retornar null em casos anómalos
                 *
                 * A captura é intencional para permitir *fallback* progressivo entre padrões.
                 */
            }
        }
        return data
    }
}
