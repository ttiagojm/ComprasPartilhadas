package com.example.projeto.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.util.Patterns
import com.example.projeto.data.LoginRepository
import com.example.projeto.data.Result

import com.example.projeto.R

class LoginViewModel(private val loginRepository: LoginRepository) : ViewModel() {

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    fun login(email: String, username: String) {
        val result = loginRepository.login(email, username)

        if (result is Result.Success) {
            _loginResult.value =
                LoginResult(success = LoggedInUserView(email = email,displayName = result.data.displayName))
        } else {
            _loginResult.value = LoginResult(error = R.string.login_failed)
        }
    }

    fun loginDataChanged(username: String) {
        if (!isUserNameValid(username)) {
            _loginForm.value = LoginFormState(usernameError = R.string.invalid_email)
        } else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    // Validar se é um email válido
    private fun isUserNameValid(email: String): Boolean {
        return if(!email.contains("@")) {
            Patterns.EMAIL_ADDRESS.matcher(email).matches()
        } else {
            email.isNotBlank()
        }
    }

}