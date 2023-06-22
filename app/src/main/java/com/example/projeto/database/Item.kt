package com.example.projeto.database

import androidx.annotation.NonNull
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.Junction
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity
data class Item (
    @PrimaryKey(autoGenerate = true) val idItem: Int = 0,
    @NonNull val nome: String,
    @NonNull val preco: Float,
    @NonNull val imagem: String
)

// Faz a relação N-M entre Item e Lista
@Entity(primaryKeys = ["idItem", "idLista"])
data class ItemListaRefCruzada(
    val idLista: Int,
    val idItem: Int,
    @NonNull val quantidade: Int = 1
)

// Junção entre Lista e Item e retorna os itens que têm associação com a lista
data class ListaPossuiItens(
    @Embedded val lista: Lista,
    @Relation(
        parentColumn = "idLista",
        entityColumn = "idItem",
        associateBy = Junction(ItemListaRefCruzada::class)
    )
    val itens: List<Item>
)

// Junção entre Lista e Item e retorna as listas que têm associação com o item
data class ItemEmListas(
    @Embedded val item: Item,
    @Relation(
        parentColumn = "idItem",
        entityColumn = "idLista",
        associateBy = Junction(ItemListaRefCruzada::class)
    )
    val listas: List<Lista>
)

@Dao interface ItemDao{
    // Obtém item específico
    @Transaction
    @Query("SELECT * FROM Item WHERE idItem = :idItem")
    fun getItem(idItem: Int): Flow<Item>

    // Obtém todos os itens
    @Transaction
    @Query("SELECT * FROM Item")
    fun getItens(): Flow<List<Item>>

    // Obter listas que têm um dado item
    @Transaction
    @Query("SELECT * FROM Item WHERE idItem = :idItem")
    fun getListasComItem(idItem: Int): Flow<List<ItemEmListas>>

    // Obter itens numa dada lista
    @Transaction
    @Query("SELECT * FROM Lista WHERE idLista = :idLista")
    fun getItensdaLista(idLista: Int): Flow<List<ListaPossuiItens>>

    @Transaction
    @Query("SELECT * FROM Lista WHERE idLista = :idLista")
    fun getIfItensdaLista(idLista: Int): Flow<ListaPossuiItens?>

    @Transaction
    @Query("UPDATE ItemListaRefCruzada SET quantidade = :quantidade WHERE idLista = :idLista AND idItem = :idItem")
    suspend fun atualizarQuantidadeItem(idLista: Int, idItem: Int, quantidade: Int): Int

    @Transaction
    @Query("SELECT * FROM ItemListaRefCruzada WHERE idLista = :idLista")
    fun getItemNaLista(idLista: Int): Flow<List<ItemListaRefCruzada>>

    @Transaction
    @Query("UPDATE Item SET nome = :nome WHERE idItem = :idItem")
    suspend fun atualizarNomeItem(idItem: Int, nome: String)

    @Transaction
    @Query("UPDATE Item SET preco = :preco WHERE idItem = :idItem")
    suspend fun atualizarPrecoItem(idItem: Int, preco: Float)

    @Transaction
    @Query("UPDATE Item SET imagem = :imagem WHERE idItem = :idItem")
    suspend fun atualizarImagemItem(idItem: Int, imagem: String)

    @Transaction
    @Query("SELECT SUM(i.preco * ilrc.quantidade) FROM Item i INNER JOIN ItemListaRefCruzada ilrc ON i.idItem = ilrc.idItem WHERE ilrc.idLista = :idLista")
    fun getPrecoTotalLista(idLista: Int): Flow<Float>

    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserirItem(item: Item)

    @Transaction
    @Update(onConflict = OnConflictStrategy.IGNORE)
    suspend fun atualizarItem(item: Item)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserirItemEmLista(itemLista: ItemListaRefCruzada)

    @Transaction
    @Delete
    suspend fun removerItem(item: Item)


    // Remover item duma lista
    @Transaction
    @Query("DELETE FROM ItemListaRefCruzada WHERE idLista = :idLista AND idItem = :idItem")
    suspend fun removerItemDaLista(idLista: Int, idItem: Int)
}