package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import pt.ipt.dam2025.pawbuddy.databinding.FragmentAlterarAnimalBinding
import android.Manifest
import androidx.navigation.fragment.findNavController

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts

import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AlterarAnimalFragment : Fragment() {

    private var _binding: FragmentAlterarAnimalBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    private var imagemUri: Uri? = null
    private var animalId: Int = 0

    private var imagemOriginalUrl: String? = null


    private val animalApi = RetrofitProvider.animalService

    // -----------------------------
    // GALERIA
    // -----------------------------
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imagemUri = uri
            binding.ivPreviewImagem.visibility = View.VISIBLE
            binding.ivPreviewImagem.setImageURI(uri)
        }
    }

    // -----------------------------
    // CAMERA
    // -----------------------------
    private val cameraPreview = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val uri = salvarBitmap(bitmap)
            imagemUri = uri
            binding.ivPreviewImagem.visibility = View.VISIBLE
            binding.ivPreviewImagem.setImageBitmap(bitmap)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        animalId = arguments?.getInt("animalId") ?: 0

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlterarAnimalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {


        cameraExecutor = Executors.newSingleThreadExecutor()

        // Carrega dados do animal existente
        carregarAnimalExistente()

        // Botões
        binding.btnTirarFoto.setOnClickListener {
            if (allPermissionsGranted())
                cameraPreview.launch(null) else
                    requestPermissions()
        }

        binding.btnGaleria.setOnClickListener { pickImage.launch("image/*") }

        binding.btnVoltarLista.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSalvarAlteracoes.setOnClickListener { enviarAlteracoes() }

    }

    private fun carregarAnimalExistente() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val animal = animalApi.getAnimal(animalId)
                withContext(Dispatchers.Main) {
                    binding.etNome.setText(animal.nome)
                    binding.etRaca.setText(animal.raca)
                    binding.etIdade.setText(animal.idade)
                    binding.etGenero.setText(animal.genero)
                    binding.etEspecie.setText(animal.especie)
                    binding.etCor.setText(animal.cor)
                    animal.imagem?.let {   nomeImagem ->
                        val urlCompleta = RetrofitInitializer.fullImageUrl(nomeImagem)
                        imagemOriginalUrl = nomeImagem
                        Glide.with(requireContext())
                            .load(urlCompleta)
                            .placeholder(R.drawable.animal0)
                            .error(R.drawable.ic_pet_placeholder)
                            .into(binding.ivPreviewImagem)
                        binding.ivPreviewImagem.visibility = View.VISIBLE

                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Erro ao carregar animal: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    private fun uriToFile(uri: Uri): File? {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(requireContext().cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }
            return tempFile
        } catch (e: Exception) {
            return null
        }
    }

    private fun enviarAlteracoes() {


        val nome = binding.etNome.text.toString()
        val raca = binding.etRaca.text.toString()
        val idade = binding.etIdade.text.toString()
        val genero = binding.etGenero.text.toString()
        val especie = binding.etEspecie.text.toString()
        val cor = binding.etCor.text.toString()



        if (nome.isBlank() || raca.isBlank() || idade.isBlank() )  {
            Toast.makeText(requireContext(), "Preencha Nome, Raça e Idade ", Toast.LENGTH_SHORT).show()
            return
        }
        if (imagemOriginalUrl == null ) {
            Toast.makeText(requireContext(), "Selecione ou tire uma foto", Toast.LENGTH_SHORT).show()
            return
        }
        val nomeImagemParaEnviar = imagemOriginalUrl?.substringAfterLast("/")


        val imagePart: MultipartBody.Part? = imagemUri?.let { uri ->
            uriToFile(uri)?.let { file ->
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("imagem", file.name, requestFile)
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {

                val response = animalApi.atualizarAnimal(
                    id = animalId,
                    idPart = animalId.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                    nome = nome.toRequestBody("text/plain".toMediaTypeOrNull()),
                    raca = raca.toRequestBody("text/plain".toMediaTypeOrNull()),
                    idade = idade.toRequestBody("text/plain".toMediaTypeOrNull()),
                    genero = genero.toRequestBody("text/plain".toMediaTypeOrNull()),
                    especie = especie.toRequestBody("text/plain".toMediaTypeOrNull()),
                    cor = cor.toRequestBody("text/plain".toMediaTypeOrNull()),
                    imagem = imagePart
                )


                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Animal atualizado com sucesso!", Toast.LENGTH_LONG).show()
                    parentFragmentManager.popBackStack()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun salvarBitmap(bitmap: Bitmap): Uri {
        val file = File(requireActivity().cacheDir, "foto_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        }
        return Uri.fromFile(file)
    }

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
            ContextCompat.checkSelfPermission(
                requireContext(), it
            ) == PackageManager.PERMISSION_GRANTED
        }


    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->

            var permissionGranted = true

            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }

            if (!permissionGranted) {
                Toast.makeText(requireContext(), "Permissão negada", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }

    companion object {

        fun newInstance(id: Int): AlterarAnimalFragment {
            val fragment = AlterarAnimalFragment()
            fragment.arguments = Bundle().apply { putInt("id", id) }
            return fragment
        }
    }
}