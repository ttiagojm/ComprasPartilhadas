package com.example.projeto



import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.example.projeto.database.Item
import com.example.projeto.databinding.FragmentAdicionaItemBinding
import com.example.projeto.placeholder.getBitmapFromUri
import com.example.projeto.viewmodels.ItensListasFactory
import com.example.projeto.viewmodels.ItensListasViewModel

class AdicionaItem : Fragment() {

    private val args: ItemFragmentArgs by navArgs()

    private val viewModel: ItensListasViewModel by activityViewModels {
        ItensListasFactory(
            (activity?.application as ComprasPartilhadasApp).database.itemDao()
        )
    }

    // Obtem o layout fragment certo
    private var _binding: FragmentAdicionaItemBinding? = null
    private val binding get() = _binding!!

    // Guarda a URI da imagem carregada
    private var uriImagem: String = ""

    // Seleciona a imagem a partir da galeria
    private val selecionarImagem =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                val bitmap: Bitmap? = getBitmapFromUri(requireContext(), uri)
                // Guardar uri
                uriImagem = uri.toString()

                if (bitmap != null) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.image_loaded),
                        Toast.LENGTH_SHORT
                    ).show()
                }else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_while_loading_image),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdicionaItemBinding.inflate(inflater, container, false)

        // Clica em adicionar uma imagem
        binding.inserirImg.setOnClickListener {
            selecionarImagem.launch(arrayOf("image/*"))
        }

        return binding.root
    }

    // Remove a barra de pesquisa
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        // Remove o botão de pesquisa
        menu.removeItem(R.id.search_bar)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        // Clica em adicionar novo item
        view.findViewById<Button>(R.id.novo_item).setOnClickListener {
            val nome = binding.nomeItem.text.toString()
            val preco = binding.precoItem.text.toString()

            if(nome.isNullOrEmpty() || preco.isNullOrEmpty()){
                Toast.makeText(context, getString(R.string.name_and_price_are_required), Toast.LENGTH_LONG).show()
            } else {

                // Selecionar imagem, caso não haja usar a default
                var imagem = ""
                if(uriImagem.isNullOrEmpty()) {
                    imagem = R.drawable.no_img.toString()
                } else {
                    imagem = uriImagem
                }

                viewModel.criarItem(Item(nome = nome,
                                         preco = preco.toFloat(),
                                         imagem = imagem))
                Toast.makeText(context, getString(R.string.item_added), Toast.LENGTH_SHORT).show()

                val action = AdicionaItemDirections.actionAdicionaItemToItemFragment(
                    args.listId,
                    args.listName
                )
                binding.root.findNavController().navigate(action)
            }
        }
    }
}