package com.example.projeto.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.projeto.database.Lista
import com.example.projeto.database.ListaDao
import com.example.projeto.database.ListasDoUtilizador
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ListasUtilizadoresViewModel(private val listaDao: ListaDao): ViewModel() {

    suspend fun inserirNovaLista(nome: String, email: String){
        val novaLista = novaLista(nome, email)
        inserirLista(novaLista)
    }

    suspend fun inserirNovaLista(lista: Lista){
        inserirLista(lista)
    }

    fun getListasDoUtilizador(email: String):
            Flow<List<ListasDoUtilizador>> = listaDao.getListasDoUtilizador(email)


    private suspend fun inserirLista(lista: Lista){
        listaDao.inserirLista(lista)
    }

    private fun novaLista(nome: String, email: String): Lista{
        return Lista(nome = nome, emailUser = email)
    }

    fun removerLista(lista: Lista) {
        viewModelScope.launch {
            listaDao.removerLista(lista)
        }
    }

    fun atualizarNome(lista: Lista, nome: String){
        viewModelScope.launch(Dispatchers.IO) {
            listaDao.atualizarNomeLista(lista.idLista, nome)
        }
    }

}

class ListasUtilizadoresFactory(private val lista: ListaDao): ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(ListasUtilizadoresViewModel::class.java)){
            @Suppress("UNCHECKED_CAST")
            return ListasUtilizadoresViewModel(lista) as T
        }
        throw IllegalArgumentException("View Model desconhecida")
    }
}