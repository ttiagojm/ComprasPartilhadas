package com.example.projeto


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.example.projeto.viewmodels.ItensListasFactory
import com.example.projeto.viewmodels.ItensListasViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


// Fragment de enviar lista por SMS
class AssociarFragment: Fragment() {

    private val args: AssociarFragmentArgs by navArgs()

    // Request Code para a permissão de enviar SMS
    private val PERMISSIONREQUESTCODE = 1
    private lateinit var inputText: EditText


    private val viewModelI: ItensListasViewModel by activityViewModels {
        ItensListasFactory(
            (activity?.application as ComprasPartilhadasApp).database.itemDao()
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        // Remove o botão de pesquisa
        menu.removeItem(R.id.search_bar)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        setHasOptionsMenu(true)

        val view = inflater.inflate(R.layout.send_list_fragment, container, false)

        val btn: Button = view.findViewById(R.id.nova_associacao)
        inputText = view.findViewById(R.id.phone_user)

        btn.setOnClickListener {
            // Caso não seja escrito um número de telefone, faz-se um intent para a lista telefónica
            if(inputText.text.isEmpty()){
                val pickContact = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                pickContact.type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                startActivityForResult(pickContact, 1)
            }else {
                val phoneNumber =
                    inputText.text.toString()
                lifecycleScope.launch {
                    var message = ""

                    // Passa-se por todos os itens da lista
                    viewModelI.getItemNaLista(args.listId.toInt())
                        .filter { itemListaRefCruzada ->
                            itemListaRefCruzada.any { it.idLista == args.listId.toInt() }
                        }
                        .map { itemListaRefCruzada ->
                            val itemQuantidadeMap = itemListaRefCruzada
                                .filter { it.idLista == args.listId.toInt() }
                                // Para cada item, obter a quantidade dele na lista através da associação
                                .associateBy(
                                    { itemListaRef -> viewModelI.getItem(itemListaRef.idItem).firstOrNull()?.nome
                                        ?: "" },
                                    { itemListaRef -> itemListaRef.quantidade }
                                )
                            itemQuantidadeMap
                        }
                        // Tendo a quantidade de cada item criar mensagem com nome e quantiade do produto
                        .onEach { itemQuantidadeMap ->
                            itemQuantidadeMap.forEach{
                                message += "x" + it.value + " " + it.key + "\n"
                            }

                            // Garantir as permissões para enviar SMS
                            if (ContextCompat.checkSelfPermission(
                                    requireContext(),
                                    Manifest.permission.SEND_SMS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                ActivityCompat.requestPermissions(
                                    requireActivity(),
                                    arrayOf(Manifest.permission.SEND_SMS),
                                    PERMISSIONREQUESTCODE
                                )
                            } else {
                                sendSms(phoneNumber, message)
                            }
                        }
                        .collect {}
                }
            }
        }
        return view
    }

    // Chamado quando é selecionado um contacto da lista telefónica
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val contactData: Uri? = data?.data
        val c: Cursor? =
            contactData?.let { activity?.contentResolver?.query(it, null, null, null, null) }
        if (c?.moveToFirst() == true) {
            val phoneIndex: Int = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val num: String? = c.getString(phoneIndex)

            lifecycleScope.launch {
                var message = ""

                viewModelI.getItemNaLista(args.listId.toInt())
                    .filter { itemListaRefCruzada ->
                        itemListaRefCruzada.any { it.idLista == args.listId.toInt() }
                    }
                    .map { itemListaRefCruzada ->
                        val itemQuantidadeMap = itemListaRefCruzada
                            .filter { it.idLista == args.listId.toInt() }
                            .associateBy(
                                { itemListaRef -> viewModelI.getItem(itemListaRef.idItem).firstOrNull()?.nome
                                    ?: "" },
                                { itemListaRef -> itemListaRef.quantidade }
                            )
                        itemQuantidadeMap
                    }
                    .onEach { itemQuantidadeMap ->
                        itemQuantidadeMap.forEach{
                            message += "x" + it.value + " " + it.key + "\n"
                        }
                        if (ContextCompat.checkSelfPermission(
                                requireContext(),
                                Manifest.permission.SEND_SMS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            ActivityCompat.requestPermissions(
                                requireActivity(),
                                arrayOf(Manifest.permission.SEND_SMS),
                                PERMISSIONREQUESTCODE
                            )
                        } else {
                            if (num != null) {
                                inputText.setText(num)
                                sendSms(num, message)
                            }
                        }
                    }
                    .collect {}
            }
        }

    }

    // Envia o SMS
    private fun sendSms(phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Toast.makeText(context, getString(R.string.message_sent_successfully), Toast.LENGTH_SHORT).show()
        } catch (ex: Exception) {
            Toast.makeText(context, getString(R.string.failed_to_send_message), Toast.LENGTH_SHORT).show()
            ex.printStackTrace()
        }
    }

    // Pedido de permissão para enviar SMS e envia uma mensagem de teste
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONREQUESTCODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val phoneNumber = "1234567890" // Replace with the recipient's phone number
                val message = "Hello, this is a test message." // Replace with your desired message
                sendSms(phoneNumber, message)
            } else {
                Toast.makeText(context, "Permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

