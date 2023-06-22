package com.example.projeto.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.projeto.database.Utilizador
import com.example.projeto.database.UtilizadorDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class UtilizadorViewModel(private val user: UtilizadorDao): ViewModel() {
    fun getUtilizador(email: String): Flow<List<Utilizador>> = user.getUser(email)

    suspend fun inserirUtilizador(utilizador: Utilizador): Boolean {
        val result = user.inserirUtilizador(utilizador)
        return result != -1L // Não houve conflito
    }


    fun getUtilizadorLogado(): Flow<Utilizador?> {
        return user.getLogin(true)
            .map {
                it.firstOrNull()
            }
    }

    fun fazerLogin(u: Utilizador?){
        if(u != null) {
            viewModelScope.launch {
                user.fazerLogin(u)
            }
        }
    }

    fun fazerLogout(email: String) {
        runBlocking {
            val users = getUtilizador(email).firstOrNull()
            if(users != null) {
                val u = users.first()
                u.login = false
                user.fazerLogout(u)
            }
        }
    }

    fun eliminarUtilizador(email: String){
        viewModelScope.launch {
            val u = user.getUser(email).firstOrNull()
            if(u != null){
                user.eliminarUtilizador(u.first())
            }
        }
    }
}

class UtilizadorFactory(private val user: UtilizadorDao): ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(UtilizadorViewModel::class.java)){
            @Suppress("UNCHECKED_CAST")
            return UtilizadorViewModel(user) as T
        }
        throw IllegalArgumentException("View Model desconhecida")
    }
}