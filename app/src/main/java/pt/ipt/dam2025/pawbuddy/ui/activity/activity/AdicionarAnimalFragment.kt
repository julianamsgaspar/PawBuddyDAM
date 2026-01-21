package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentAdicionarAnimalBinding
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import okhttp3.RequestBody.Companion.asRequestBody
import androidx.navigation.fragment.findNavController





/**
 * A simple [Fragment] subclass.
 * Use the [AdicionarAnimalFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AdicionarAnimalFragment  : Fragment() {

    private var _binding: FragmentAdicionarAnimalBinding? = null
    private val binding get() = _binding!!
    private lateinit var cameraExecutor: ExecutorService
    private var photoFile: String? = null
    private var imagemUri: Uri? = null

    // -----------------------------
    //   GALERIA
    // -----------------------------
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                imagemUri = uri
//                photoFile = File(requireActivity().cacheDir, "img_${System.currentTimeMillis()}.jpg").toString()
//                requireActivity().contentResolver.openInputStream(uri)?.use { input ->
//                    FileOutputStream(photoFile!!).use { output -> input.copyTo(output) }
//                }
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
                //photoUri = uri.toString()
//                val file = File(requireActivity().cacheDir, "foto_${System.currentTimeMillis()}.jpg")
//                FileOutputStream(file).use {
//                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
//                }
                //photoFile = file.toString()
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
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.btnAdicionar.setOnClickListener { enviarAnimal() }
        binding.btnGaleria.setOnClickListener { pickImage.launch("image/*") }

        binding.btnTirarFoto.setOnClickListener {
            if (allPermissionsGranted()) cameraPreview.launch(null) else requestPermissions()
        }

    }


    // -----------------------------
    //   SALVAR BITMAP
    // -----------------------------
    private fun salvarBitmap(bitmap: Bitmap): Uri {
        /*val file = File(requireActivity().cacheDir, "foto_${System.currentTimeMillis()}.jpg")
        val output = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
        output.flush()
        output.close()*/
        val file = File(requireActivity().cacheDir, "foto_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        }
        return Uri.fromFile(file)
    }


    private fun enviarAnimal() {

        if (imagemUri == null) {
            Toast.makeText(requireContext(), getString(R.string.error_select_or_take_photo), Toast.LENGTH_SHORT).show()
            return
        }

        val file = uriToTempFile(imagemUri!!)
        val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("imagem", file.name, requestFile)

        val nome = RequestBody.create("text/plain".toMediaTypeOrNull(), binding.etNome.text.toString())
        val raca = RequestBody.create("text/plain".toMediaTypeOrNull(), binding.etRaca.text.toString())
        val idade = RequestBody.create("text/plain".toMediaTypeOrNull(), binding.etIdade.text.toString())
        val genero = RequestBody.create("text/plain".toMediaTypeOrNull(), binding.etGenero.text.toString())
        val especie = RequestBody.create("text/plain".toMediaTypeOrNull(), binding.etEspecie.text.toString())
        val cor = RequestBody.create("text/plain".toMediaTypeOrNull(), binding.etCor.text.toString())


        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitInitializer().animalService().criarAnimal(
                    nome, raca, idade, genero, especie, cor, imagePart
                )

                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), getString(R.string.success_animal_created), Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.gestaoFragment)

                }

            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_generic_with_message, e.message ?: getString(R.string.error_generic)),
                        Toast.LENGTH_LONG
                    ).show()

                }
            }
        }
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

    private fun String.toRequest(): RequestBody =
        RequestBody.create("text/plain".toMediaTypeOrNull(), this)

        fun fileToMultipart(uri: Uri, fieldName: String, context: Context): MultipartBody.Part {
            val file = File(uri.path!!)
            val req = RequestBody.create("image/*".toMediaTypeOrNull(), file)
            return MultipartBody.Part.createFormData(fieldName, file.name, req)
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
                    Toast.makeText(requireContext(), getString(R.string.error_permission_denied), Toast.LENGTH_SHORT).show()
                }
            }

        override fun onDestroyView() {
            super.onDestroyView()
            cameraExecutor.shutdown()
            _binding = null
        }

        companion object {

        }
}