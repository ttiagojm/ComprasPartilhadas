package com.example.projeto

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.example.projeto.databinding.FragmentProdutoViewBinding
import com.example.projeto.placeholder.ItemEliminadoEvento
import com.example.projeto.placeholder.NovoItemAdicionadoEvento
import com.example.projeto.placeholder.getBitmapFromUri
import com.example.projeto.viewmodels.ItensListasFactory
import com.example.projeto.viewmodels.ItensListasViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import java.text.NumberFormat
import java.util.Currency


class ProdutoFragment : Fragment() {
    // Argumentos vindo do ListasFragment
    private val args: ProdutoFragmentArgs by navArgs()

    private var _binding: FragmentProdutoViewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItensListasViewModel by activityViewModels {
        ItensListasFactory(
            (activity?.application as ComprasPartilhadasApp).database.itemDao()
        )
    }


    // Seleciona a imagem a partir da galeria
    private val selecionarImagem =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                val bitmap: Bitmap? = getBitmapFromUri(requireContext(), uri)
                if (bitmap != null) {
                    binding.imageView2.setImageBitmap(bitmap)
                    // Atualizar na base de dados
                    viewModel.atualizarImagemItem(args.itemId.toInt(), uri.toString())
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.image_loaded),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_while_loading_image),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        // Remove o botão de pesquisa
        menu.removeItem(R.id.search_bar)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        setHasOptionsMenu(true)

        _binding = FragmentProdutoViewBinding.inflate(inflater, container, false)

        // Obtém o nome, o preço e a imagem  do produto
        val nomeProduto = args.nomeProduto
        val precoProduto = args.precoProduto
        val imgProduto = args.imagePath

        val nomeProdutoTextView = binding.nomeProdutoTextView
        val precoProdutoTextView = binding.precoProdutoTextView
        val imagemProdutoImageView = binding.imageView2

        // Coloca o nome
        nomeProdutoTextView.text = Editable.Factory.getInstance().newEditable(nomeProduto)

        // Formatar e colocar preço
        val format = NumberFormat.getNumberInstance()
        format.maximumFractionDigits = 2
        format.currency = Currency.getInstance("EUR")
        precoProdutoTextView.text = Editable.Factory.getInstance().newEditable(format.format(precoProduto))

        // Verificar se é um drawable ou uma imagem do user
        if(imgProduto.toIntOrNull() == null){
            // Criar um bitmap
            try {
                val bitmap = getBitmapFromUri(requireContext(), Uri.parse(imgProduto))
                imagemProdutoImageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                // Caso de erro colocar a imagem default
                imagemProdutoImageView.setImageDrawable(resources.getDrawable(R.drawable.no_img))
            }
        } else{
            imagemProdutoImageView.setImageDrawable(resources.getDrawable(imgProduto.toInt()))
        }


        // Listeners para guardar alterações do produto
        nomeProdutoTextView.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->

            // Só quando não tiver focado, guarda a informação
            if(!hasFocus){
                val nome = nomeProdutoTextView.text.toString()
                if(nome.isNotEmpty()){
                    viewModel.atualizarItem(args.itemId.toInt(), nome)
                }
            }
        }

        precoProdutoTextView.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->

            // Só quando não tiver focado guarda a informação
            if(!hasFocus){
                // Se for uma preco válido, guarda-se
                val preco = precoProdutoTextView.text.toString().toDoubleOrNull()
                if (preco != null && preco >= 0) {
                    viewModel.atualizarItem(args.itemId.toInt(), preco = preco)
                }
            }
        }

        imagemProdutoImageView.setOnClickListener {
            selecionarImagem.launch(arrayOf("image/*"))
        }


        // Listener para adicionar item na lista
        val valor = binding.quantidadeContador
        binding.button.setOnClickListener {

            lifecycle.coroutineScope.launch{
                viewModel.inserirItemEmLista(
                    args.listId.toInt(),
                    args.itemId.toInt(),
                    valor.text.toString().toInt()
                    )

                withContext(Dispatchers.Main){
                    Toast.makeText(context, getString(R.string.produto_adicionado), Toast.LENGTH_LONG).show()
                }
            }

            // Notificar item na lista
            EventBus.getDefault().post(ItemEliminadoEvento())

            // Voltar para a página da lista
            val action = ProdutoFragmentDirections
                .actionProdutoFragmentToItemFragment(
                    args.listId,
                    args.listName
                )
            binding.root.findNavController().navigate(action)
        }

        // Colocar o item na relação lista-item e atualizar a quantidade total de itens na lista,
        // se necessário
        lifecycleScope.launch {
            viewModel.getItemNaLista(args.listId.toInt()).collect { itemListaRefCruzadaList ->
                var quantidadeAtual = itemListaRefCruzadaList
                    .find { it.idItem == args.itemId.toInt() }
                    ?.quantidade ?: 0

                val listaTem = quantidadeAtual > 0

                if(!listaTem) quantidadeAtual = 1

                valor.text = quantidadeAtual.toString()

                // Só envia atualização se o item não existir ainda na lista
                if(!listaTem){
                    val evento = NovoItemAdicionadoEvento(args.listId.toInt())
                    EventBus.getDefault().post(evento)
                }
            }
        }

        // Remover item da base de dados
        binding.removeProd.setOnClickListener {
            lifecycleScope.launch {
                val item = viewModel.getItem(args.itemId.toInt()).first()
                viewModel.removerItem(item)

                // Voltar para a página da lista
                val action = ProdutoFragmentDirections
                    .actionProdutoFragmentToItemFragment(
                        args.listId,
                        args.listName
                    )
                binding.root.findNavController().navigate(action)
            }
        }

        val btnMenos = binding.menos
        btnMenos.setOnClickListener {
            if(valor.text.toString().toInt() > 1){
                var cont = valor.text.toString().toInt()
                cont--
                valor.text = cont.toString()
            }
        }

        val btnMais = binding.mais
        btnMais.setOnClickListener {
            var cont = valor.text.toString().toInt()
            cont++
            valor.text = cont.toString()
        }

        return binding.root
    }
}
