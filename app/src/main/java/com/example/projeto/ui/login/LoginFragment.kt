package com.example.projeto.ui.login

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.coroutineScope
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import com.example.projeto.ComprasPartilhadasApp
import com.example.projeto.R
import com.example.projeto.database.Utilizador
import com.example.projeto.databinding.FragmentLoginBinding
import com.example.projeto.placeholder.PlaceholderContent
import com.example.projeto.viewmodels.UtilizadorFactory
import com.example.projeto.viewmodels.UtilizadorViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class LoginFragment : Fragment() {

    private lateinit var loginViewModel: LoginViewModel
    private var _binding: FragmentLoginBinding? = null

    private val binding get() = _binding!!

    // viewModel para a tabela de utilizadores
    private val viewModel: UtilizadorViewModel by activityViewModels {
        UtilizadorFactory(
            (activity?.application as ComprasPartilhadasApp).database.utilizadorDao()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentLoginBinding.inflate(inflater, container, false)

        return binding.root
    }

    // Garante que a barra de navegação não tem opções
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        // Verificar se existe login feito
        verificarLogin()

        val textView = binding.textView2
        textView.setOnClickListener {

            val actions=LoginFragmentDirections.actionLoginFragmentToBlankFragment()
            view.findNavController().navigate(actions)

        }


        loginViewModel = ViewModelProvider(this, LoginViewModelFactory())
            .get(LoginViewModel::class.java)

        val usernameEditText = binding.username
        val passwordEditText = binding.password
        val loginButton = binding.login
        val loadingProgressBar = binding.loading

        loginViewModel.loginFormState.observe(viewLifecycleOwner,
            Observer { loginFormState ->
                if (loginFormState == null) {
                    return@Observer
                }
                loginButton.isEnabled = loginFormState.isDataValid
                loginFormState.usernameError?.let {
                    usernameEditText.error = getString(it)
                }
                loginFormState.passwordError?.let {
                    passwordEditText.error = getString(it)
                }
            })

        loginViewModel.loginResult.observe(viewLifecycleOwner,
            Observer { loginResult ->
                loginResult ?: return@Observer
                loadingProgressBar.visibility = View.GONE
                loginResult.error?.let {
                    showLoginFailed(it)
                }
            })

        val afterTextChangedListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                loginViewModel.loginDataChanged(
                    usernameEditText.text.toString()
                )
            }
        }


        // No caso de usar a action SEND do teclado, validar os dados fornecidos
        usernameEditText.addTextChangedListener(afterTextChangedListener)
        passwordEditText.addTextChangedListener(afterTextChangedListener)
        passwordEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                verificarDados(
                    usernameEditText.text.toString(),
                    passwordEditText.text.toString()
                )
            }
            false
        }

        // No caso de usar o botão de login
        loginButton.setOnClickListener {
            loadingProgressBar.visibility = View.VISIBLE

            val email: String = usernameEditText.text.toString()
            val passwd: String = passwordEditText.text.toString()

            // Validar dados do utilizador
            verificarDados(email, passwd)
        }
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        val appContext = context?.applicationContext ?: return
        Toast.makeText(appContext, errorString, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    /* Verifica se já há utilizador logado */
    private fun verificarLogin(){

        // Verificar se há algum login
        lifecycle.coroutineScope.launch {

            val user = viewModel.getUtilizadorLogado().firstOrNull()
            if (user != null) {
                PlaceholderContent.DEFAULT_USER = user.email
                Toast.makeText(
                    context,
                    getString(R.string.welcome, user.nome),
                    Toast.LENGTH_LONG
                ).show()
                val navController = Navigation.findNavController(binding.root)
                navController.navigate(R.id.action_loginFragment_to_listasFragment)
            }
        }
    }

    // Verifica se o utilizador existe e se a password está correta
    private fun verificarDados(email: String, passwd: String){
        var existe: Boolean

        runBlocking {

            // Obtém o utilizador, se houver
            val user: Utilizador? = viewModel.getUtilizador(email).first().takeIf {
                it.isNotEmpty()
            }?.first()

            existe = user != null

            if(existe && user?.password == passwd) {
                user.login = true

                // Atualizar o estado na base de dados
                viewModel.fazerLogin(user)

                // Colocar o DEFAULT_USER com o email atual
                PlaceholderContent.DEFAULT_USER = user.email


                // Preparar para sair do login view
                loginViewModel.login(
                    user.email,
                    user.nome
                )

                Toast.makeText(context, getString(R.string.welcome, user.nome), Toast.LENGTH_LONG).show()
                val navController = Navigation.findNavController(binding.root)
                navController.navigate(R.id.action_loginFragment_to_listasFragment)
            } else{
                Toast.makeText(context, getString(R.string.login_failed), Toast.LENGTH_LONG).show()
                val navController = Navigation.findNavController(binding.root)
                navController.navigate(R.id.action_loginFragment_self)
            }
        }
    }
}