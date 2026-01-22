package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentAnimalDetailBinding
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer
import pt.ipt.dam2025.pawbuddy.session.SessionManager

class AnimalDetailFragment : Fragment() {

    private var _binding: FragmentAnimalDetailBinding? = null
    private val binding get() = _binding!!

    private val retrofit = RetrofitInitializer()
    private val session by lazy { SessionManager(requireContext()) }

    private var animalId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        animalId = arguments?.getInt("animalId", -1) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnimalDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isLogged = session.isLogged()
        val isAdmin = session.isAdmin()

        // --- Regras de UI ---
        when {
            isAdmin -> {
                // para já: esconder ações admin no detalhe (como tinhas)
                    binding.btnEliminar.visibility = View.VISIBLE
                    binding.btnAlterar.visibility = View.VISIBLE
                    binding.btnAdotar.visibility = View.GONE


            }

            isLogged -> {
                binding.btnEliminar.visibility = View.GONE
                binding.btnAlterar.visibility = View.GONE
                binding.btnAdotar.visibility = View.VISIBLE
            }

            else -> {
                binding.btnEliminar.visibility = View.GONE
                binding.btnAlterar.visibility = View.GONE
                binding.btnAdotar.visibility = View.VISIBLE
            }
        }

        // Carregar detalhes do animal
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val animal = retrofit.animalService().getAnimal(animalId)

                withContext(Dispatchers.Main) {
                    binding.txtNome.text = animal.nome
                    binding.txtRaca.text = getString(R.string.label_breed, animal.raca)
                    binding.txtIdade.text = getString(R.string.label_age, animal.idade.toString())
                    binding.txtEspecie.text = getString(R.string.label_species, animal.especie)
                    binding.txtGenero.text = getString(R.string.label_gender, animal.genero)
                    binding.txtCor.text = getString(R.string.label_color, animal.cor)

                    Glide.with(requireContext())
                        .load(RetrofitInitializer.fullImageUrl(animal.imagem.toString()))
                        .placeholder(R.drawable.animal0)
                        .error(R.drawable.ic_pet_placeholder)
                        .centerCrop()
                        .into(binding.ivAnimal)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.txtNome.text = getString(
                        R.string.error_fetch_animal_details,
                        e.message ?: getString(R.string.error_generic)
                    )
                }
            }
        }

        // Adotar
        binding.btnAdotar.setOnClickListener {
            if (!session.isLogged()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_login_required),
                    Toast.LENGTH_SHORT
                ).show()

                val bundle = Bundle().apply {
                    putBoolean("redirectToAdotar", true)
                    putInt("redirectAnimalId", animalId)
                }

                findNavController().navigate(
                    R.id.loginFragment,
                    bundle,
                    navOptions { launchSingleTop = true }
                )
                return@setOnClickListener
            }

            val bundle = Bundle().apply { putInt("animalId", animalId) }

            findNavController().navigate(
                R.id.adotarFragment,
                bundle,
                navOptions { launchSingleTop = true }
            )
        }

        // Admin (para mais tarde) — corrigido: deve ir para alterarAnimalFragment
        binding.btnAlterar.setOnClickListener {
            val bundle = Bundle().apply { putInt("animalId", animalId) }
            findNavController().navigate(
                R.id.alterarAnimalFragment,
                bundle,
                navOptions { launchSingleTop = true }
            )
        }

        binding.btnEliminar.setOnClickListener {
            eliminarAnimal()
        }
    }

    private fun eliminarAnimal() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                retrofit.animalService().eliminarAnimal(animalId)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.success_animal_deleted),
                        Toast.LENGTH_SHORT
                    ).show()
                    findNavController().navigate(R.id.listaAnimaisFragment)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(
                            R.string.error_delete_animal,
                            e.message ?: getString(R.string.error_generic)
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
