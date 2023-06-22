package com.example.projeto


import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projeto.adapters.AdapterItemListaCompras
import com.example.projeto.database.Item
import com.example.projeto.databinding.FragmentItemListBinding
import com.example.projeto.placeholder.ItemEliminadoEvento
import com.example.projeto.viewmodels.ItensListasFactory
import com.example.projeto.viewmodels.ItensListasViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class ItemFragment : Fragment() {

    // Obtem o layout fragment certo
    private var _binding: FragmentItemListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: RecyclerView

    private val viewModel: ItensListasViewModel by activityViewModels {
        ItensListasFactory(
            (activity?.application as ComprasPartilhadasApp).database.itemDao()
        )
    }

    // Argumentos vindo do ListasFragment
    private val args: ItemFragmentArgs by navArgs()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemListBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        // Sempre que voltar para esta lista recalcula o preço
        calcularPrecoTotal()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu, menu) // Inflate your menu XML file
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                // Handle the selected menu item (e.g., perform an action)
                return false
            }
            // Add additional cases for other menu items if needed
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = binding.itemRecycler
        adapter.layoutManager = LinearLayoutManager(requireContext())

        // Define o item no list adapter que ao ser clicado vai para o próximo fragment
        val novoAdapter = criarNovoAdapter()

        adapter.adapter = novoAdapter

        // Obter todos os itens na lista
        lifecycle.coroutineScope.launch {
            viewModel.getItensEmLista(args.listId.toInt()).collect() { it ->
                val novaLista = mutableListOf<Item>()

                // é uma lista de listas, desconstruímos numa lista só
                it.forEach { l ->
                    novaLista.addAll(l.itens)
                }
                novoAdapter.submitList(novaLista.toList())
            }
        }

        // Botão para partilhar Lista de itens
        val btnSMS = view.findViewById<FloatingActionButton>(R.id.associarBtn)
        btnSMS.setOnClickListener {
            val action = ItemFragmentDirections
                .actionItemFragmentToAssociarFragment(
                    listId = args.listId,
                )
            binding.root.findNavController().navigate(action)
        }

        // Botão para adicionar novo item à base de dados
        val btn = view.findViewById<FloatingActionButton>(R.id.addItemBtn)
        btn.setOnClickListener {
            val action = ItemFragmentDirections
                .actionItemFragmentToAdicionaItem(
                    args.listId,
                    args.listName
                )
            binding.root.findNavController().navigate(action)
        }

        // Calcular preço total
        calcularPrecoTotal()
    }

    // Mostra apenas os itens filtrados por substring na barra de pesquisa
    fun filterItems(query: String) {
        val novoAdapter = criarNovoAdapter(procuraItem = true)
        adapter.adapter = novoAdapter

        lifecycleScope.launch {
            viewModel.getTodosItens().collect() {
                val filtradas = it.filter {it2 ->
                    it2.nome.startsWith(query, ignoreCase = true)
                }
                novoAdapter.submitList(filtradas)
            }
        }
    }

    private fun criarNovoAdapter(procuraItem: Boolean = false): AdapterItemListaCompras{
        // Definir novamente a navegação após criar novo array de listas

        val listaId = args.listId.toInt()

        val novoAdapter = AdapterItemListaCompras(procuraItem, viewModel, listaId) {
            val action = ItemFragmentDirections
                .actionItemFragmentToProdutoFragment(
                    listId = args.listId,
                    listName = args.listName,
                    itemId = it.idItem.toLong(),
                    nomeProduto = it.nome,
                    precoProduto = it.preco,
                    imagePath = it.imagem
                )
            binding.root.findNavController().navigate(action)
        }

        // Calcular quantidade do item na lista
        lifecycleScope.launch {
            viewModel.getItemNaLista(listaId)
                .filter { itemListaRefCruzada -> itemListaRefCruzada.any { it.idLista == listaId } }
                .map { itemListaRefCruzada ->
                    val itemQuantidadeMap = itemListaRefCruzada
                        .filter { it.idLista == listaId }
                        .associate { it.idItem to it.quantidade }
                    itemQuantidadeMap
                }
                .onEach { itemQuantidadeMap ->
                    withContext(Dispatchers.Main) {
                        novoAdapter.itemQuantidadeMap = itemQuantidadeMap
                        novoAdapter.notifyDataSetChanged()
                    }
                }
                .collect{}
        }

        return novoAdapter
    }

    private fun calcularPrecoTotal(){
        lifecycleScope.launch {
            val precoTotal = viewModel.getPrecoTotalLista(args.listId.toInt()).firstOrNull() ?: 0f
            binding.fixedTextView.text = getString(R.string.total_price_textview, precoTotal)
        }
    }

    // Evento subscrito para avisar que o preço total da lista mudou
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onListIdEvent(evento: ItemEliminadoEvento) {
        calcularPrecoTotal()
    }
}
