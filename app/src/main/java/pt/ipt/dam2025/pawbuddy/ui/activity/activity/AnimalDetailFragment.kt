package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentAnimalDetailBinding
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer

class AnimalDetailFragment : Fragment() {

    private var _binding: FragmentAnimalDetailBinding? = null
    private val binding get() = _binding!!

    private val retrofit = RetrofitInitializer()
    private var animalId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        animalId = arguments?.getInt("id") ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnimalDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//
//        // ⬇ Verifica login
        val shared = requireContext().getSharedPreferences("PawBuddyPrefs", 0)
        val isLogged = shared.getBoolean("isLogged", false)
        val prefs = requireContext().getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
        val isAdmin = prefs.getBoolean("isAdmin", false)
        val utilizadorId = shared.getInt("utilizadorId", -1)

        if (!isLogged) {
            binding.btnAdotar.visibility = View.GONE
        } else {
            if (isAdmin) {
                binding.btnEliminar.visibility = View.VISIBLE
                binding.btnAlterar.visibility= View.VISIBLE
                binding.btnAdotar.visibility = View.GONE

            } else {
                binding.btnEliminar.visibility = View.GONE
                binding.btnAlterar.visibility = View.GONE
                binding.btnAdotar.visibility = View.VISIBLE
            }
        }
//        binding.btnEliminar.visibility = if (isLogged) View.VISIBLE else View.GONE
//        binding.btnAlterar.visibility = if (isLogged) View.VISIBLE else View.GONE


        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

            try {
                val animal = retrofit.animalService().getAnimal(animalId)

                withContext(Dispatchers.Main) {
                    binding.txtNome.text = animal.nome
                    binding.txtRaca.text = "Raça: ${animal.raca}"
                    binding.txtIdade.text = "Idade: ${animal.idade}"
                    binding.txtEspecie.text = "Espécie: ${animal.especie}"
                    binding.txtGenero.text = "Género: ${animal.genero}"
                    binding.txtCor.text = "Cor: ${animal.cor}"


                   // val baseUrl = "http://10.0.2.2:5053/"
                    Glide.with(requireContext())
                        .load(RetrofitInitializer.fullImageUrl(animal.imagem.toString()))
                        .placeholder(R.drawable.animal0)
                        .error(R.drawable.ic_pet_placeholder)
                        .centerCrop()
                        .into(binding.ivAnimal)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.txtNome.text = "Erro ao buscar detalhes: ${e.message}"
                }
            }
        }
        binding.btnVoltarHome.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    requireActivity().findViewById<View>(R.id.fragmentContainer).id,
                    ListaAnimaisFragment()
                )
                .commit()
        }
        binding.btnAdotar.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    requireActivity().findViewById<View>(R.id.fragmentContainer).id,
                    AdotarFragment.newInstance(animalId, utilizadorId)
                )
                .addToBackStack(null)
                .commit()

        }


        binding.btnAlterar.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    requireActivity().findViewById<View>(R.id.fragmentContainer).id,
                    AlterarAnimalFragment.newInstance(animalId) // envia o id do animal
                )
                //.addToBackStack(null) // para conseguir voltar
                .commit()
        }

        binding.btnEliminar.setOnClickListener {
            // Confirmação simples antes de eliminar
            eliminarAnimal()
        }
    }



    private fun eliminarAnimal() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                retrofit.animalService().eliminarAnimal(animalId)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Animal eliminado com sucesso!", Toast.LENGTH_SHORT).show()
                    // Volta para lista de animais
                    parentFragmentManager.beginTransaction()
                        .replace(requireActivity().findViewById<View>(R.id.fragmentContainer).id, ListaAnimaisFragment())
                        .commit()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Erro ao eliminar animal: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(id: Int): AnimalDetailFragment {
            val fragment = AnimalDetailFragment()
            fragment.arguments = Bundle().apply {
                putInt("id", id)
            }
            return fragment
        }
    }
}