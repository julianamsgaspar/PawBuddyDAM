package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentAlterarAnimalBinding
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitProvider
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Fragment responsável pela edição de um Animal existente.
 *
 * Objetivos funcionais:
 * - Carregar os dados atuais do animal (GET por ID) e preencher o formulário.
 * - Permitir alteração de campos textuais e seleção de novas opções (dropdowns).
 * - Permitir substituição opcional da imagem (galeria ou câmara).
 * - Submeter alterações para o backend via multipart form-data (PUT/PATCH conforme API).
 *
 * Considerações técnicas:
 * - Usa ViewBinding (evita findViewById e reduz erros de nullability).
 * - Usa Coroutines: operações de rede e I/O em Dispatchers.IO, UI em Dispatchers.Main.
 * - Para imagens remotas, usa Glide e desativa cache para garantir atualização imediata após edição.
 */
class AlterarAnimalFragment : Fragment() {

    /**
     * ViewBinding: válido apenas entre onCreateView() e onDestroyView().
     */
    private var _binding: FragmentAlterarAnimalBinding? = null
    private val binding get() = _binding!!

    /**
     * Serviço Retrofit para operações sobre animais.
     */
    private val animalApi = RetrofitProvider.animalService

    /**
     * Identificador do animal a editar, recebido via arguments (Bundle).
     * Se inválido (<=0) o Fragment termina com navigateUp().
     */
    private var animalId: Int = 0

    /**
     * URI da nova imagem escolhida pelo utilizador (opcional).
     * - null: mantém a imagem existente no backend
     * - não-null: envia uma nova imagem em multipart
     */
    private var imagemUri: Uri? = null

    /**
     * Nome/identificador da imagem original no backend (opcional).
     * É útil para contexto, debugging e controlo de UI, mas não é usado para upload.
     */
    private var imagemOriginalNome: String? = null

    // -----------------------------
    // GALERIA / CÂMARA
    // -----------------------------

    /**
     * Seleção de imagem na galeria (GetContent).
     * Atualiza imagemUri e faz preview imediato.
     */
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                imagemUri = uri
                showImagePreview()
                binding.ivPreviewImagem.setImageURI(uri)
            }
        }

    /**
     * Captura de imagem via câmara (TakePicturePreview).
     * Recebe um Bitmap e persiste-o num ficheiro temporário, gerando Uri para upload posterior.
     */
    private val cameraPreview =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                imagemUri = salvarBitmap(bitmap)
                showImagePreview()
                binding.ivPreviewImagem.setImageBitmap(bitmap)
            }
        }

    // -----------------------------
    // PERMISSÕES
    // -----------------------------

    /**
     * Permissões necessárias para captura com câmara.
     *
     * Nota:
     * - WRITE_EXTERNAL_STORAGE é condicionado a SDK <= P por compatibilidade com fluxos antigos.
     */
    private val REQUIRED_PERMISSIONS = mutableListOf(
        Manifest.permission.CAMERA
    ).apply {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }.toTypedArray()

    /**
     * Pedido de permissões em runtime.
     * Se todas forem concedidas, abre de imediato a câmara (decisão de UX deste ecrã).
     */
    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val ok = REQUIRED_PERMISSIONS.all { perm -> permissions[perm] == true }
            if (!ok) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Permissões concedidas: permite captura imediata (sem clique adicional).
                cameraPreview.launch(null)
            }
        }

    /**
     * Lê o animalId do Bundle logo no início do ciclo de vida (antes do inflate).
     * Permite validar e evitar chamadas ao backend com ID inválido.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        animalId = arguments?.getInt("animalId") ?: 0
    }

    /**
     * Infla o layout e inicializa o ViewBinding.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlterarAnimalBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Configura UI e inicia carregamento do animal.
     *
     * Fluxo:
     * - Configura dropdowns.
     * - Valida animalId.
     * - Mostra placeholder e estado de loading.
     * - Faz GET do animal e popula o formulário.
     * - Configura listeners: galeria, câmara, voltar e guardar.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDropdowns()

        // Pré-condição: necessita de um ID válido.
        if (animalId <= 0) {
            Toast.makeText(requireContext(), getString(R.string.error_generic), Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        // Estado inicial: placeholder visível enquanto carrega do backend.
        showNoImage()
        setLoading(true)

        carregarAnimalExistente()

        binding.btnGaleria.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnTirarFoto.setOnClickListener {
            if (allPermissionsGranted()) {
                cameraPreview.launch(null)
            } else {
                requestPermissions()
            }
        }

        binding.btnVoltarLista.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSalvarAlteracoes.setOnClickListener {
            enviarAlteracoes()
        }
    }

    /**
     * Configura os dropdowns (espécie, género, idade).
     *
     * Observação:
     * - Não define defaults; os valores são preenchidos posteriormente com dados do backend.
     */
    private fun setupDropdowns() {
        val especies = listOf("Cão", "Gato", "Pássaro", "Coelho")
        val generos = listOf("Fêmea", "Macho")
        val idades = listOf("Bebé", "Junior", "Adulto", "Idoso")

        fun adapter(list: List<String>) =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, list)

        binding.spEspecie.setAdapter(adapter(especies))
        binding.spGenero.setAdapter(adapter(generos))
        binding.spIdade.setAdapter(adapter(idades))
    }

    /**
     * Obtém o animal do backend e preenche o formulário.
     *
     * Inclui carregamento de imagem remota:
     * - Usa RetrofitInitializer.fullImageUrl(nomeImagem) para construir URL.
     * - Usa Glide com cache desativada para evitar apresentar imagem desatualizada após alterações.
     *
     * Tratamento de estado:
     * - Em sucesso: preenche inputs e atualiza preview/placeholder conforme haja imagem.
     * - Em erro: apresenta mensagem (Toast) com detalhe.
     * - Em qualquer caso: repõe estado de loading no finally.
     */
    private fun carregarAnimalExistente() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val animal = animalApi.getAnimal(animalId)

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext

                    // Campos básicos.
                    binding.etNome.setText(animal.nome ?: "")
                    binding.etRaca.setText(animal.raca ?: "")

                    // Dropdowns (strings).
                    binding.spEspecie.setText(animal.especie ?: "", false)
                    binding.spGenero.setText(animal.genero ?: "", false)
                    binding.spIdade.setText(animal.idade?.toString() ?: "", false)

                    binding.etCor.setText(animal.cor ?: "")

                    // Imagem remota (se existir).
                    val nomeImagem = animal.imagem
                    if (!nomeImagem.isNullOrBlank()) {
                        imagemOriginalNome = nomeImagem

                        val urlCompleta = RetrofitInitializer.fullImageUrl(nomeImagem)
                        showImagePreview()

                        // Anti-cache para garantir atualização imediata após alterações de imagem.
                        Glide.with(requireContext())
                            .load(urlCompleta)
                            .placeholder(R.drawable.ic_pet_placeholder)
                            .error(R.drawable.ic_pet_placeholder)
                            .centerCrop()
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .signature(ObjectKey(System.currentTimeMillis().toString()))
                            .into(binding.ivPreviewImagem)

                    } else {
                        showNoImage()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    Toast.makeText(
                        requireContext(),
                        getString(
                            R.string.error_generic_with_message,
                            e.message ?: getString(R.string.error_generic)
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    if (isAdded) setLoading(false)
                }
            }
        }
    }

    /**
     * Submete alterações do formulário para o backend.
     *
     * Regras:
     * - Valida campos mínimos (nome, raça, idade) no cliente.
     * - A imagem é opcional: só é enviada se imagemUri != null.
     *
     * Implementação multipart:
     * - Converte strings em RequestBody "text/plain".
     * - Converte imagem (quando existe) em MultipartBody.Part com mime inferido via ContentResolver.
     * - Envia idPart com o ID em texto para compatibilidade com o contrato do backend.
     */
    private fun enviarAlteracoes() {
        val nomeTxt = normalizeText(binding.etNome.text?.toString().orEmpty())
        val racaTxt = normalizeText(binding.etRaca.text?.toString().orEmpty())
        val especieTxt = binding.spEspecie.text?.toString().orEmpty()
        val generoTxt = binding.spGenero.text?.toString().orEmpty()
        val idadeTxt = binding.spIdade.text?.toString().orEmpty()
        val corTxt = normalizeText(binding.etCor.text?.toString().orEmpty())

        // Validação client-side (mínima) antes do pedido de rede.
        if (nomeTxt.isBlank() || racaTxt.isBlank() || idadeTxt.isBlank()) {
            Toast.makeText(requireContext(), "Preenche Nome, Raça e Idade.", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Imagem opcional: apenas enviada se o utilizador a selecionar/capturar.
                val imagePart: MultipartBody.Part? = imagemUri?.let { uri ->
                    val file = uriToTempFile(uri)

                    // Usa o MIME real quando disponível.
                    val mime = requireContext().contentResolver.getType(uri) ?: "image/jpeg"
                    val requestFile = file.asRequestBody(mime.toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("imagem", file.name, requestFile)
                }

                val response = animalApi.atualizarAnimal(
                    id = animalId,
                    idPart = animalId.toString().toPlainRequestBody(),
                    nome = nomeTxt.toPlainRequestBody(),
                    raca = racaTxt.toPlainRequestBody(),
                    idade = idadeTxt.toPlainRequestBody(),
                    genero = generoTxt.toPlainRequestBody(),
                    especie = especieTxt.toPlainRequestBody(),
                    cor = corTxt.toPlainRequestBody(),
                    imagem = imagePart // pode ser null
                )

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext

                    Toast.makeText(
                        requireContext(),
                        getString(R.string.success_animal_modified),
                        Toast.LENGTH_LONG
                    ).show()

                    // Volta ao ecrã anterior (tipicamente lista/detalhe).
                    findNavController().navigateUp()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    Toast.makeText(
                        requireContext(),
                        getString(
                            R.string.error_generic_with_message,
                            e.message ?: getString(R.string.error_generic)
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    if (isAdded) setLoading(false)
                }
            }
        }
    }

    // -----------------------------
    // UI helpers
    // -----------------------------

    /**
     * Controla estado de loading:
     * - Bloqueia interações para evitar submits repetidos.
     * - Atualiza o texto do botão principal para feedback imediato.
     */
    private fun setLoading(loading: Boolean) {
        binding.btnSalvarAlteracoes.isEnabled = !loading
        binding.btnVoltarLista.isEnabled = !loading
        binding.btnGaleria.isEnabled = !loading
        binding.btnTirarFoto.isEnabled = !loading

        binding.btnSalvarAlteracoes.text =
            if (loading) getString(R.string.loading) else getString(R.string.action_save_changes)
    }

    /**
     * Alterna UI para estado com imagem (preview visível e placeholder oculto).
     */
    private fun showImagePreview() {
        binding.ivPreviewImagem.visibility = View.VISIBLE
        // Existe no layout “bonito”.
        binding.llPlaceholderImagem.visibility = View.GONE
    }

    /**
     * Alterna UI para estado sem imagem (placeholder visível).
     */
    private fun showNoImage() {
        binding.ivPreviewImagem.visibility = View.GONE
        // Existe no layout “bonito”.
        binding.llPlaceholderImagem.visibility = View.VISIBLE
    }

    // -----------------------------
    // Helpers (iguais ao Adicionar)
    // -----------------------------

    /**
     * Normalização leve de inputs:
     * - trim, colapsa espaços
     * - lowercase + capitalização inicial (pt-PT)
     *
     * Nota: esta função serve consistência/UX; a validação final deve existir também no backend.
     */
    private fun normalizeText(input: String): String {
        val cleaned = input.trim().replace("\\s+".toRegex(), " ")
        if (cleaned.isBlank()) return ""

        val pt = Locale.forLanguageTag("pt-PT")
        val lower = cleaned.lowercase(pt)

        return lower.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(pt) else ch.toString()
        }
    }

    /**
     * Guarda um Bitmap (capturado pela câmara) como JPEG temporário em cacheDir e devolve Uri.
     */
    private fun salvarBitmap(bitmap: Bitmap): Uri {
        val file = File(requireActivity().cacheDir, "foto_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        }
        return Uri.fromFile(file)
    }

    /**
     * Copia o conteúdo de um Uri (ContentResolver) para um ficheiro temporário local (cacheDir).
     * Necessário para construir RequestBody a partir de File para o upload multipart.
     */
    private fun uriToTempFile(uri: Uri): File {
        val input = requireContext().contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Não foi possível abrir a imagem")

        val file = File(requireContext().cacheDir, "upload_${System.currentTimeMillis()}.jpg")

        input.use { ins ->
            FileOutputStream(file).use { outs ->
                ins.copyTo(outs)
            }
        }
        return file
    }

    /**
     * Cria RequestBody "text/plain" a partir de String, adequado para multipart/form-data.
     */
    private fun String.toPlainRequestBody(): RequestBody =
        this.toRequestBody("text/plain".toMediaTypeOrNull())

    /**
     * Solicita permissões necessárias para captura pela câmara.
     */
    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    /**
     * Verifica se todas as permissões necessárias estão concedidas.
     */
    private fun allPermissionsGranted(): Boolean =
        REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

    /**
     * Liberta binding ao destruir a View para prevenir memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
