package com.example.projeto.database

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity
data class Utilizador (
    @PrimaryKey val email: String,
    @NonNull @ColumnInfo(name = "nome") val nome: String,
    @NonNull val password: String,
    @NonNull var login: Boolean = false
)

@Dao
interface UtilizadorDao{
    // Retorna todos os utilizadores
    @Transaction
    @Query("SELECT * FROM utilizador")
    fun getUsers(): Flow<List<Utilizador>>

    @Transaction
    // Retorna apenas um utilizador
    @Query("SELECT * FROM utilizador WHERE email = :email")
    fun getUser(email: String): Flow<List<Utilizador>>

    // Obter logins
    @Query("SELECT * FROM utilizador WHERE login = :login")
    fun getLogin(login: Boolean): Flow<List<Utilizador>>

    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserirUtilizador(user: Utilizador): Long

    @Transaction
    @Update
    suspend fun atualizarUtilizador(user: Utilizador)

    @Transaction
    @Delete
    suspend fun removerUtilizador(user: Utilizador)

    @Transaction
    @Update()
    suspend fun fazerLogin(user: Utilizador)

    @Transaction
    @Update()
    suspend fun fazerLogout(user: Utilizador)

    @Transaction
    @Delete
    suspend fun eliminarUtilizador(user: Utilizador)
}