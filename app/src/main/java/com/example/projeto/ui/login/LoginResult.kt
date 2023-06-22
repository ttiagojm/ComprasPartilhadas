package com.example.projeto.ui.login

/**
 * Resultado da autenticação
 */
data class LoginResult(
    val success: LoggedInUserView? = null,
    val error: Int? = null
)