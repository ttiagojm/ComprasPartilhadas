package com.example.projeto


import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.coroutineScope
import androidx.navigation.findNavController
import com.example.projeto.database.Utilizador
import com.example.projeto.viewmodels.UtilizadorFactory
import com.example.projeto.viewmodels.UtilizadorViewModel
import kotlinx.coroutines.launch

class RegistoFragment : Fragment() {

    private val viewModel: UtilizadorViewModel by activityViewModels {
        UtilizadorFactory(
            (activity?.application as ComprasPartilhadasApp).database.utilizadorDao()
        )
    }

    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var editTextConfirmPassword: EditText
    private lateinit var buttonRegister: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_registo, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        // Tirar o botão de pesquisa e opções
        menu.clear()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Esconder as opções da barra de navegação na MainActivity
        setHasOptionsMenu(true)

        editTextEmail = view.findViewById(R.id.editTextEmail)
        editTextPassword = view.findViewById(R.id.editTextPassword)
        editTextConfirmPassword = view.findViewById(R.id.editTextConfirmPassword)
        buttonRegister = view.findViewById(R.id.buttonRegister)

        buttonRegister.setOnClickListener {
            val email = editTextEmail.text.toString()
            val password = editTextPassword.text.toString()
            val confirmPassword = editTextConfirmPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {

                if (password != confirmPassword) {
                    Toast.makeText(requireContext(), getString(R.string.passwords_do_not_match), Toast.LENGTH_SHORT).show()
                } else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.invalid_email_address),
                        Toast.LENGTH_SHORT
                    ).show()
                }else if(password.length < 8){
                    Toast.makeText(requireContext(), getString(R.string.password_must_have_8_characters), Toast.LENGTH_SHORT).show()
                } else {

                    lifecycle.coroutineScope.launch {
                        if(registerUser(email, password)) {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.user_register_success),
                                Toast.LENGTH_SHORT
                            ).show()

                            // Ir para a página de login
                            val navController = view.findNavController()
                            navController.navigate(R.id.action_blankFragment_to_loginFragment)
                        } else{
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.this_email_is_already_registered),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } else {
                Toast.makeText(requireContext(), getString(R.string.please_fill_in_all_fields), Toast.LENGTH_SHORT).show()
            }
        }
    }

    suspend private fun registerUser(email: String, password: String): Boolean {
        val nome = email.split("@")[0]
        val login = false

        val utilizador = Utilizador(email, nome, password, login)

        return viewModel.inserirUtilizador(utilizador)
    }

}
