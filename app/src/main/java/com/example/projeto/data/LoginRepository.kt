package com.example.projeto.data

import com.example.projeto.data.model.LoggedInUser


class LoginRepository(val dataSource: LoginDataSource) {

    // Cache de utilizador logado
    var user: LoggedInUser? = null
        private set

    val isLoggedIn: Boolean
        get() = user != null

    init {
        user = null
    }

    fun logout() {
        user = null
        dataSource.logout()
    }

    // Fazer login na DB
    fun login(email: String, username: String): Result<LoggedInUser> {

        val result = dataSource.login(email, username)

        if (result is Result.Success) {
            setLoggedInUser(result.data)
        }

        return result
    }

    private fun setLoggedInUser(loggedInUser: LoggedInUser) {
        this.user = loggedInUser
    }
}