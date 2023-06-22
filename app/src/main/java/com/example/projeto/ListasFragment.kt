package com.example.projeto

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.coroutineScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projeto.adapters.AdapterListasListaCompras
import com.example.projeto.database.Lista
import com.example.projeto.databinding.ListasComprasFragmentBinding
import com.example.projeto.placeholder.ListaAlteradaEvento
import com.example.projeto.placeholder.NovoItemAdicionadoEvento
import com.example.projeto.placeholder.PlaceholderContent
import com.example.projeto.viewmodels.ItensListasFactory
import com.example.projeto.viewmodels.ItensListasViewModel
import com.example.projeto.viewmodels.ListasUtilizadoresFactory
import com.example.projeto.viewmodels.ListasUtilizadoresViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

// Classe que mostra as listas de compras adicionadas
class ListasFragment: Fragment() {

    // Obtem o layout fragment certo
    private var _binding: ListasComprasFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: RecyclerView

    private val viewModel: ListasUtilizadoresViewModel by activityViewModels {
        ListasUtilizadoresFactory(
            (activity?.application as ComprasPartilhadasApp).database.listaDao()
        )
    }

    private val viewModelI: ItensListasViewModel by activityViewModels {
        ItensListasFactory(
            (activity?.application as ComprasPartilhadasApp).database.itemDao()
        )
    }

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
        _binding = ListasComprasFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = binding.listasComprasRecyclerView
        adapter.layoutManager = LinearLayoutManager(requireContext())

        // Define o item no list adapter que ao ser clicado vai para o próximo fragment
        val listasListaComprasAdapter = criarNovoAdapter()

        adapter.adapter = listasListaComprasAdapter
        lifecycle.coroutineScope.launch {
            val novaLista = mutableListOf<Lista>()
            viewModel.getListasDoUtilizador(PlaceholderContent.DEFAULT_USER).collect() { it ->
                novaLista.clear()
                // é uma lista de listas, desconstruímos numa lista só
                it.forEach { l ->
                    novaLista.addAll(l.listas)
                }

                // Obter a quantidade total de itens para cada lista
                val quantidadeTotalItens = getQuantidadeTotalItensPorLista(novaLista)

                listasListaComprasAdapter.setQuantidadeTotalItens(quantidadeTotalItens)
                listasListaComprasAdapter.submitList(novaLista)
            }
        }

        // OnClick Listener para criar lista
        val floatBtn:FloatingActionButton = view.findViewById(R.id.adicionarLista)
        floatBtn.setOnClickListener {
            val action = ListasFragmentDirections
                .actionListasFragmentToNovaListaFragment()
            view.findNavController().navigate(action)
        }

        // Bloquear o botão de voltar para trás
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Não faça nada aqui para bloquear o comportamento padrão do botão de voltar
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(false)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Mostra só as listas que começam com a substring na barra de pesquisa
    fun filterItems(query: String) {
        val novoAdapter = criarNovoAdapter()

        adapter.adapter = novoAdapter

        lifecycle.coroutineScope.launch {
            viewModel.getListasDoUtilizador(PlaceholderContent.DEFAULT_USER).collect { listas ->
                val filtradas = listas.flatMap { usuario ->
                    usuario.listas.filter { lista ->
                        lista.nome.startsWith(query, ignoreCase = true)
                    }
                }
                novoAdapter.submitList(filtradas)
            }
        }
    }



    // Função que gera um novo adapter com a navegação pronta
    private fun criarNovoAdapter(): AdapterListasListaCompras{
        // Definir novamente a navegação após criar novo array de listas
        val novoAdapter = AdapterListasListaCompras(viewModel) {
            val action = ListasFragmentDirections
                .actionListasFragmentToItemFragment2(
                    listId = it.idLista.toLong(),
                    listName = it.nome
                )
            binding.root.findNavController().navigate(action)
        }

        return novoAdapter
    }

    // Conta o número de itens únicos na lista
    private suspend fun getQuantidadeTotalItensPorLista(listas: List<Lista>): Map<Int, Int> {
        val quantidadeTotalItens = mutableMapOf<Int, Int>()
        for (lista in listas) {
            val itensLista = viewModelI.getItensEmLista(lista.idLista).firstOrNull()
            val quantidade = if (!itensLista.isNullOrEmpty()) {
                itensLista.first().itens.size
            } else {
                0
            }
            quantidadeTotalItens[lista.idLista] = quantidade
        }
        return quantidadeTotalItens
    }

    // Evento subscrito que atualiza o número de itens unicos na lista quando é adiciona um produto novo
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onListIdEvent(evento: NovoItemAdicionadoEvento) {
        val listaId = evento.listId
        val adap = adapter.adapter as AdapterListasListaCompras

        val quantidadeTotalItens = adap.getQuantidadeTotalItens().toMutableMap()

        quantidadeTotalItens[listaId] = quantidadeTotalItens[listaId]!! + 1

        adap.setQuantidadeTotalItens(quantidadeTotalItens)
    }

    // Evento usado para quando é reposto um item que fora eliminado garantindo que aparece na UI
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onListChangedEvent(evento: ListaAlteradaEvento) {
        adapter = binding.listasComprasRecyclerView
        adapter.layoutManager = LinearLayoutManager(requireContext())

        // Define o item no list adapter que ao ser clicado vai para o próximo fragment
        val listasListaComprasAdapter = criarNovoAdapter()

        adapter.adapter = listasListaComprasAdapter
        lifecycle.coroutineScope.launch {
            val novaLista = mutableListOf<Lista>()
            viewModel.getListasDoUtilizador(PlaceholderContent.DEFAULT_USER).collect() { it ->
                novaLista.clear()
                // é uma lista de listas, desconstruímos numa lista só
                it.forEach { l ->
                    novaLista.addAll(l.listas)
                }

                // Obter a quantidade total de itens para cada lista
                val quantidadeTotalItens = getQuantidadeTotalItensPorLista(novaLista)

                listasListaComprasAdapter.setQuantidadeTotalItens(quantidadeTotalItens)
                listasListaComprasAdapter.submitList(novaLista)
            }
        }
    }
}