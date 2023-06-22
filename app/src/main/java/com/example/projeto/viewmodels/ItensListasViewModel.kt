package com.example.projeto.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.projeto.database.Item
import com.example.projeto.database.ItemDao
import com.example.projeto.database.ItemListaRefCruzada
import com.example.projeto.database.ListaPossuiItens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ItensListasViewModel(private val itemDao: ItemDao): ViewModel() {

    fun getTodosItens(): Flow<List<Item>> = itemDao.getItens()
    fun getItensEmLista(idLista: Int): Flow<List<ListaPossuiItens>> {
        return itemDao.getItensdaLista(idLista)
    }

    suspend fun inserirNovoItem(item: Item){
        itemDao.inserirItem(item)
    }

    fun getItem(idItem: Int): Flow<Item> {
        return itemDao.getItem(idItem)
    }

    suspend fun inserirItemEmLista(idLista: Int, idItem: Int, quantidade: Int) {

        // Atualiza a quantidade do item na lista e retorna o número de linhas afetadas
        val linhasAfetadas = itemDao.atualizarQuantidadeItem(idLista, idItem, quantidade)

        // Se não houver linhas afetada, é porque ainda não existe associação entre o item e a lista
        // cria-se a associação e insere-se o item na lista
        if (linhasAfetadas == 0) {
            val itemLista = ItemListaRefCruzada(idLista, idItem, quantidade)
            itemDao.inserirItemEmLista(itemLista)
        }
    }

    fun getItemNaLista(idLista: Int): Flow<List<ItemListaRefCruzada>>{
            return itemDao.getItemNaLista(idLista)
    }

    fun criarItem(item: Item){
        viewModelScope.launch {
            itemDao.inserirItem(item)
        }
    }

    fun removerItem(item: Item) {
        runBlocking {
            // Remover o item das listas primeiro
            itemDao.getItemNaLista(item.idItem).firstOrNull()?.forEach { itemLista ->
                itemDao.removerItemDaLista(itemLista.idLista, itemLista.idItem)
            }
            // Remover o item em si
            itemDao.removerItem(item)
        }
    }

    fun removerItemDaLista(item: Item, listaId: Int){
        viewModelScope.launch {
            itemDao.removerItemDaLista(listaId, item.idItem)
        }
    }

    // Função que permite atualizar múltiplos campos do Item
    private fun atualizarCamposItem(itemId: Int,
                                    nome: String = "",
                                    preco: Double = -1.0,
                                    imagem: String = ""){
        viewModelScope.launch {
            if(nome.isNotEmpty()){
                itemDao.atualizarNomeItem(itemId, nome)
            }

            if(preco >= 0){
                itemDao.atualizarPrecoItem(itemId, preco.toFloat())
            }

            if(imagem.isNotEmpty()){
                itemDao.atualizarImagemItem(itemId, imagem)
            }
        }
    }

    fun atualizarItem(itemId: Int, nome: String){
        atualizarCamposItem(itemId = itemId, nome = nome)
    }

    fun atualizarItem(itemId: Int, preco: Double){
        atualizarCamposItem(itemId = itemId, preco = preco)
    }

    fun atualizarImagemItem(itemId: Int, imagem: String){
        atualizarCamposItem(itemId = itemId, imagem = imagem)
    }

    fun getPrecoTotalLista(idLista: Int): Flow<Float> {
        return itemDao.getPrecoTotalLista(idLista)
    }

}

class ItensListasFactory(private val itemDao: ItemDao): ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(ItensListasViewModel::class.java)){
            @Suppress("UNCHECKED_CAST")
            return ItensListasViewModel(itemDao) as T
        }
        throw IllegalArgumentException("View Model desconhecida")
    }
}
