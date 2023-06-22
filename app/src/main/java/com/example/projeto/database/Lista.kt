package com.example.projeto.database

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity()
data class Lista(
    @PrimaryKey(autoGenerate = true) val idLista: Int = 0,
    @NonNull val nome: String,
    val emailUser: String,
    @ColumnInfo(name = "numItens") val numItens: Int = 0
)

// Relação Lista-Utilizador (N para 1)
data class ListasDoUtilizador(
    @Embedded val user: Utilizador,
    @Relation(
        parentColumn = "email",
        entityColumn = "emailUser"
    )
    val listas: List<Lista>
)

@Dao
interface ListaDao{
    // Retorna uma lista específica
    @Transaction
    @Query("SELECT * FROM Lista WHERE idLista = :idLista")
    fun getLista(idLista: Int): Flow<List<Lista>>

    // Retorna todas as listas
    @Transaction
    @Query("SELECT * FROM Lista")
    fun getListas(): Flow<List<Lista>>

    // Retorna as listas feitas pelo utilizador
    @Transaction
    @Query("SELECT * FROM Utilizador WHERE email = :email")
    fun getListasDoUtilizador(email: String): Flow<List<ListasDoUtilizador>>

    @Transaction
    @Query("UPDATE Lista SET nome = :nome WHERE idLista = :listaId")
    fun atualizarNomeLista(listaId: Int, nome: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserirLista(lista: Lista)

    @Update()
    suspend fun atualizarLista(lista: Lista)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun associarListaAoUtilizador(user: Utilizador, lista: Lista)

    @Delete
    suspend fun removerLista(lista: Lista)
}