package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AdicionarAnimalFragment : Fragment() {

    private var _binding: FragmentAdicionarAnimalBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    private var imagemUri: Uri? = null

    private val animalApi = RetrofitProvider.animalService

    // -----------------------------
    //   GALERIA
    // -----------------------------
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                imagemUri = uri
                binding.ivPreviewImagem.visibility = View.VISIBLE
                binding.ivPreviewImagem.setImageURI(uri)
            }
        }

    // -----------------------------
    //   CAMERA PREVIEW
    // -----------------------------
    private val cameraPreview =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                val uri = salvarBitmap(bitmap)
                imagemUri = uri
                binding.ivPreviewImagem.visibility = View.VISIBLE
                binding.ivPreviewImagem.setImageBitmap(bitmap)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdicionarAnimalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        setupSpinners()

        binding.btnGaleria.setOnClickListener { pickImage.launch("image/*") }

        binding.btnTirarFoto.setOnClickListener {
            if (allPermissionsGranted()) cameraPreview.launch(null) else requestPermissions()
        }

        binding.btnAdicionar.setOnClickListener { enviarAnimal() }
    }

    // -----------------------------
    //   SPINNERS
    // -----------------------------
    private fun setupSpinners() {
        val especies = listOf("Cão", "Gato", "Pássaro", "Coelho")
        val generos = listOf("Fêmea", "Macho")
        val idades = listOf("Bebé", "Junior", "Adulto", "Idoso")

        fun <T> spinnerAdapter(list: List<T>) =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, list).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        binding.spEspecie.adapter = spinnerAdapter(especies)
        binding.spGenero.adapter = spinnerAdapter(generos)
        binding.spIdade.adapter = spinnerAdapter(idades)

        binding.spEspecie.setSelection(0)
        binding.spGenero.setSelection(0)
        binding.spIdade.setSelection(0)
    }

    // -----------------------------
    //   NORMALIZAÇÃO (capitalização)
    // -----------------------------
    private fun normalizeText(input: String): String {
        val cleaned = input.trim().replace("\\s+".toRegex(), " ")
        if (cleaned.isBlank()) return ""

        val pt = Locale.forLanguageTag("pt-PT")
        val lower = cleaned.lowercase(pt)

        return lower.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(pt) else ch.toString()
        }
    }

    private fun enviarAnimal() {
        if (imagemUri == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_select_or_take_photo),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val nomeTxt = normalizeText(binding.etNome.text?.toString().orEmpty())
        val racaTxt = normalizeText(binding.etRaca.text?.toString().orEmpty())
        val corTxt = normalizeText(binding.etCor.text?.toString().orEmpty())

        val especieTxt = binding.spEspecie.selectedItem?.toString().orEmpty()
        val generoTxt = binding.spGenero.selectedItem?.toString().orEmpty()
        val idadeTxt = binding.spIdade.selectedItem?.toString().orEmpty()

        if (nomeTxt.isBlank() || racaTxt.isBlank() || corTxt.isBlank()) {
            Toast.makeText(requireContext(), "Preenche Nome, Raça e Cor.", Toast.LENGTH_SHORT).show()
            return
        }

        // (Opcional, mas recomendado) bloquear UI enquanto envia
        setLoading(true)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val file = uriToTempFile(imagemUri!!)
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("imagem", file.name, requestFile)

                val nome = nomeTxt.toPlainRequestBody()
                val raca = racaTxt.toPlainRequestBody()
                val idade = idadeTxt.toPlainRequestBody()
                val genero = generoTxt.toPlainRequestBody()
                val especie = especieTxt.toPlainRequestBody()
                val cor = corTxt.toPlainRequestBody()

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
                    if (!isAdded) return@withContext

                    Toast.makeText(
                        requireContext(),
                        getString(R.string.success_animal_created),
                        Toast.LENGTH_LONG
                    ).show()

                    // ✅ IMPORTANTE: remove este formulário da backstack
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
                withContext(Dispatchers.Main) { if (isAdded) setLoading(false) }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnAdicionar.isEnabled = !loading
        binding.btnGaleria.isEnabled = !loading
        binding.btnTirarFoto.isEnabled = !loading

        binding.btnAdicionar.text =
            if (loading) getString(R.string.loading) else getString(R.string.action_add_animal)
    }

    // -----------------------------
    //   HELPERS
    // -----------------------------
    private fun salvarBitmap(bitmap: Bitmap): Uri {
        val file = File(requireActivity().cacheDir, "foto_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        }
        return Uri.fromFile(file)
    }

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

    private fun String.toPlainRequestBody(): RequestBody =
        this.toRequestBody("text/plain".toMediaTypeOrNull())

    // -----------------------------
    //   PERMISSÕES
    // -----------------------------
    private val REQUIRED_PERMISSIONS = mutableListOf(
        Manifest.permission.CAMERA
    ).apply {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }.toTypedArray()

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() =
        REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val ok = permissions.entries.all { (perm, granted) ->
                perm !in REQUIRED_PERMISSIONS || granted
            }
            if (!ok) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}
