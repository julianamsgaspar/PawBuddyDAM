package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.Manifest
import android.content.pm.PackageManager
import java.text.SimpleDateFormat
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
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentAdicionarAnimalBinding
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitProvider
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import java.util.Locale

/**
 * Fragment responsável pela criação de um novo Animal no backend.
 *
 * Responsabilidades principais:
 * - Recolher dados do formulário (texto + seleções de dropdown).
 * - Permitir seleção de imagem via galeria ou captura via câmara.
 * - Validar campos obrigatórios no cliente antes da submissão.
 * - Efetuar upload multipart (campos text/plain + imagem image/jpeg) para o endpoint criarAnimal().
 *
 * Considerações técnicas:
 * - Usa Activity Result API para galeria/câmara e pedido de permissões.
 * - Usa Coroutines (Dispatchers.IO) para operações de I/O e rede, com retorno ao Main para UI.
 * - Converte Uri -> ficheiro temporário em cacheDir para permitir MultipartBody (contrato típico em Retrofit/OkHttp).
 */
class AdicionarAnimalFragment : Fragment() {

    /**
     * ViewBinding: válido apenas entre onCreateView() e onDestroyView().
     * Deve ser libertado para evitar leaks associados ao ciclo de vida da View.
     */
    private var _binding: FragmentAdicionarAnimalBinding? = null
    private val binding get() = _binding!!

    /**
     * URI da imagem selecionada (galeria) ou gerada (câmara).
     * É requisito funcional para criação do animal nesta implementação.
     */
    private var imagemUri: Uri? = null

    /**
     * Serviço Retrofit para operações relacionadas com animais (CRUD).
     */
    private val animalApi = RetrofitProvider.animalService

    /**
     * Launcher para seleção de conteúdo (galeria).
     * Obtém um Uri e atualiza a pré-visualização na UI.
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
     * URI temporário da fotografia capturada pela câmara.
     * É gerado previamente via FileProvider e usado pelo contrato TakePicture().
     */
    private var cameraPhotoUri: Uri? = null

    /**
     * Launcher para captura de imagem em resolução total através da câmara.
     *
     * - Grava a fotografia diretamente no URI fornecido (cameraPhotoUri);
     * - Em caso de sucesso, atualiza a pré-visualização e guarda o URI para upload;
     * - Em caso de falha, apresenta feedback ao utilizador.
     */
    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraPhotoUri != null) {
                imagemUri = cameraPhotoUri
                showImagePreview()
                binding.ivPreviewImagem.setImageURI(imagemUri)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Não foi possível tirar a foto.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    /**
     * Lista de permissões necessárias.
     *
     * Nota:
     * - Para SDK <= P (Android 9), WRITE_EXTERNAL_STORAGE pode ser necessário em fluxos legados.
     * - Em versões mais recentes, o acesso é tipicamente mediado por APIs/Uri (Scoped Storage).
     */
    private val REQUIRED_PERMISSIONS = mutableListOf(
        Manifest.permission.CAMERA
    ).apply {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }.toTypedArray()

    /**
     * Launcher para pedido de múltiplas permissões.
     * Se alguma falhar, informa o utilizador (não força o fluxo de câmara).
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
            }
        }

    /**
     * Infla o layout e inicializa o ViewBinding.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdicionarAnimalBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Configura a UI e listeners:
     * - Inicializa dropdowns (MaterialAutoCompleteTextView).
     * - Define estado inicial sem imagem.
     * - Listener para galeria, câmara (com permissões) e submissão do formulário.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDropdowns()

        // Estado inicial do placeholder (sem imagem selecionada/capturada).
        showNoImage()

        // Seleção de imagem pela galeria (GetContent).
        binding.btnGaleria.setOnClickListener {
            pickImage.launch("image/*")
        }

        // Captura de imagem via câmara (TakePicturePreview) condicionada a permissões.
        binding.btnTirarFoto.setOnClickListener {
            if (allPermissionsGranted()) {
                cameraPhotoUri = createImageUri()
                takePicture.launch(cameraPhotoUri)
            } else {
                requestPermissions()
            }
        }


        // Submissão do formulário.
        binding.btnAdicionar.setOnClickListener {
            enviarAnimal()
        }
    }
    /**
     * Cria um URI seguro para armazenamento temporário da fotografia capturada.
     *
     * O ficheiro é criado em cacheDir e exposto via FileProvider,
     * permitindo à aplicação da câmara gravar a imagem em resolução total.
     */
    private fun createImageUri(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageFile = File(requireContext().cacheDir, "camera_$timeStamp.jpg")

        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            imageFile
        )
    }


    /**
     * Configura os dropdowns (espécie, género, idade).
     *
     * Implementação:
     * - Usa ArrayAdapter com layout simples (android.R.layout.simple_list_item_1).
     * - Define valores por defeito para evitar estados nulos/indefinidos no formulário.
     */
    private fun setupDropdowns() {
        val especies = listOf("Cão", "Gato", "Pássaro", "Coelho")
        val generos = listOf("Fêmea", "Macho")
        val idades = listOf("Bebé", "Junior", "Adulto", "Idoso")

        fun adapter(list: List<String>) =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, list)

        // No layout “bonito”, spEspecie/spGenero/spIdade são MaterialAutoCompleteTextView.
        binding.spEspecie.setAdapter(adapter(especies))
        binding.spGenero.setAdapter(adapter(generos))
        binding.spIdade.setAdapter(adapter(idades))

        // Valores por defeito (melhora UX e reduz validações de vazio).
        binding.spEspecie.setText(especies.first(), false)
        binding.spGenero.setText(generos.first(), false)
        binding.spIdade.setText(idades.first(), false)
    }

    /**
     * Valida o formulário e executa a criação do animal com upload multipart.
     *
     * Pré-condições (validação client-side):
     * - A imagem é obrigatória (imagemUri != null).
     * - Nome/Raça/Cor não podem ser vazios.
     * - Espécie/Género/Idade devem estar selecionados.
     *
     * Pipeline (alto nível):
     * 1) Converter Uri -> ficheiro temporário (cacheDir).
     * 2) Construir MultipartBody.Part para a imagem.
     * 3) Converter campos de texto em RequestBody "text/plain".
     * 4) Invocar animalApi.criarAnimal(...) em Dispatchers.IO.
     * 5) No sucesso, navegar para o ecrã de gestão e limpar back stack do fragment atual (popUpTo).
     */
    private fun enviarAnimal() {
        // Validação 1: imagem obrigatória nesta versão funcional.
        if (imagemUri == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_select_or_take_photo),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Normalização básica de texto: trim + colapsar espaços + capitalização inicial.
        val nomeTxt = normalizeText(binding.etNome.text?.toString().orEmpty())
        val racaTxt = normalizeText(binding.etRaca.text?.toString().orEmpty())
        val corTxt = normalizeText(binding.etCor.text?.toString().orEmpty())

        val especieTxt = binding.spEspecie.text?.toString().orEmpty()
        val generoTxt = binding.spGenero.text?.toString().orEmpty()
        val idadeTxt = binding.spIdade.text?.toString().orEmpty()

        // Validação 2: campos textuais obrigatórios.
        if (nomeTxt.isBlank() || racaTxt.isBlank() || corTxt.isBlank()) {
            Toast.makeText(requireContext(), "Preenche Nome, Raça e Cor.", Toast.LENGTH_SHORT).show()
            return
        }

        // Validação 3: seleções obrigatórias.
        if (especieTxt.isBlank() || generoTxt.isBlank() || idadeTxt.isBlank()) {
            Toast.makeText(requireContext(), "Seleciona Espécie, Género e Idade.", Toast.LENGTH_SHORT).show()
            return
        }

        // UI: bloqueia ações durante operação de rede.
        setLoading(true)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Converte Uri para um ficheiro temporário (necessário para upload multipart).
                val file = uriToTempFile(imagemUri!!)
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("imagem", file.name, requestFile)

                // Campos textuais como "text/plain" (contrato típico para multipart form-data).
                val nome = nomeTxt.toPlainRequestBody()
                val raca = racaTxt.toPlainRequestBody()
                val idade = idadeTxt.toPlainRequestBody()
                val genero = generoTxt.toPlainRequestBody()
                val especie = especieTxt.toPlainRequestBody()
                val cor = corTxt.toPlainRequestBody()

                // Chamada ao backend (suspending).
                animalApi.criarAnimal(
                    nome = nome,
                    raca = raca,
                    especie = especie,
                    idade = idade,
                    genero = genero,
                    cor = cor,
                    imagem = imagePart
                )

                withContext(Dispatchers.Main) {
                    // Proteção: evita ações se o fragment já não estiver anexado.
                    if (!isAdded) return@withContext

                    Toast.makeText(
                        requireContext(),
                        getString(R.string.success_animal_created),
                        Toast.LENGTH_LONG
                    ).show()

                    // Navegação com limpeza do back stack para evitar voltar ao formulário após sucesso.
                    findNavController().navigate(
                        R.id.gestaoFragment,
                        null,
                        navOptions {
                            popUpTo(R.id.adicionarAnimalFragment) { inclusive = true }
                            launchSingleTop = true
                        }
                    )
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
                // Garante reposição do estado da UI, independentemente de sucesso/erro.
                withContext(Dispatchers.Main) {
                    if (isAdded) setLoading(false)
                }
            }
        }
    }

    /**
     * Controla o estado de loading da UI:
     * - Desativa botões para evitar submissões repetidas.
     * - Atualiza o texto do botão "Adicionar" para feedback imediato.
     */
    private fun setLoading(loading: Boolean) {
        binding.btnAdicionar.isEnabled = !loading
        binding.btnGaleria.isEnabled = !loading
        binding.btnTirarFoto.isEnabled = !loading

        binding.btnAdicionar.text =
            if (loading) getString(R.string.loading) else getString(R.string.action_add_animal)
    }

    /**
     * Alterna UI para estado com pré-visualização de imagem.
     * No layout “bonito”, existe um placeholder separado (llPlaceholderImagem).
     */
    private fun showImagePreview() {
        binding.ivPreviewImagem.visibility = View.VISIBLE
        binding.llPlaceholderImagem.visibility = View.GONE
    }

    /**
     * Alterna UI para estado sem imagem selecionada (placeholder visível).
     */
    private fun showNoImage() {
        binding.ivPreviewImagem.visibility = View.GONE
        binding.llPlaceholderImagem.visibility = View.VISIBLE
    }

    /**
     * Normalização leve de texto para consistência de input:
     * - remove espaços no início/fim
     * - colapsa múltiplos espaços
     * - converte para lowercase e aplica capitalização inicial (pt-PT)
     *
     * Nota: não substitui validação server-side; é apenas higienização/UX no cliente.
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
     * Copia o conteúdo referenciado por Uri para um ficheiro temporário local (cacheDir).
     *
     * Nota:
     * - Evita depender diretamente do ContentResolver no momento do upload.
     * - Lança IllegalArgumentException caso não seja possível abrir o stream.
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
     * Extensão utilitária para criar RequestBody "text/plain" a partir de String.
     * Útil para formulários multipart (campos textuais + ficheiros).
     */
    private fun String.toPlainRequestBody(): RequestBody =
        this.toRequestBody("text/plain".toMediaTypeOrNull())

    /**
     * Solicita permissões necessárias (câmara e, em versões antigas, escrita).
     */
    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    /**
     * Verifica se todas as permissões necessárias foram concedidas.
     */
    private fun allPermissionsGranted(): Boolean =
        REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

    /**
     * Liberta o binding quando a View é destruída, prevenindo memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
