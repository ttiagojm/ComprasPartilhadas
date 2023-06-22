package com.example.projeto.data

import com.example.projeto.data.model.LoggedInUser
import java.io.IOException

class LoginDataSource {

    // Validar se o utilizador existe
    fun login(email: String, username: String): Result<LoggedInUser> {
        try {
            val user = LoggedInUser(email, username)
            return Result.Success(user)
        } catch (e: Throwable) {
            return Result.Error(IOException("Error logging in", e))
        }
    }

    fun logout() {
        // TODO: revoke authentication
    }
}