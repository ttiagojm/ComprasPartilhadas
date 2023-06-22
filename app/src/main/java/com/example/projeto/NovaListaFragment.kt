package com.example.projeto

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.coroutineScope
import androidx.navigation.findNavController
import com.example.projeto.placeholder.PlaceholderContent
import com.example.projeto.viewmodels.ListasUtilizadoresFactory
import com.example.projeto.viewmodels.ListasUtilizadoresViewModel
import kotlinx.coroutines.launch

class NovaListaFragment: Fragment() {

    private val viewModel: ListasUtilizadoresViewModel by activityViewModels {
        ListasUtilizadoresFactory(
            (activity?.application as ComprasPartilhadasApp).database.listaDao()
        )
    }

    // Remove a barra de pesquisa
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        // Remove o botão de pesquisa
        menu.removeItem(R.id.search_bar)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)

        val view = inflater.inflate(R.layout.add_lista_fragment, container, false)

        val btn: Button = view.findViewById(R.id.nova_lista_btn)
        val inputText: EditText = view.findViewById(R.id.nova_lista_text)

        btn.setOnClickListener {
            // Adicionar lista à base de dados
            lifecycle.coroutineScope.launch {
                viewModel.inserirNovaLista(
                    nome = inputText.text.toString(),
                    email = PlaceholderContent.DEFAULT_USER
                )
            }

            Toast.makeText(context, getString(R.string.lista_adicionada), Toast.LENGTH_LONG)

            val action = NovaListaFragmentDirections
                .actionNovaListaFragmentToListasFragment()
            view.findNavController().navigate(action)
        }
        return view
    }
}