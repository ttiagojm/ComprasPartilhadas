package com.example.projeto.placeholder

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import java.io.IOException

// Classe para notificar mudanças entre o Produto e o Lista Fragment
class NovoItemAdicionadoEvento(val listId: Int)

// Classe para notificar mudanças entre o adapter e o fragment de itens
class ItemEliminadoEvento()

// Classe que garante que a lista de listas é atualizada após re-inserção
class ListaAlteradaEvento()

object PlaceholderContent {

    // Guarda o email do utilizador autenticado
    var DEFAULT_USER = ""
}

// Tendo o URI, carrega um bitmap
fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = MediaStore.Images.Media.getBitmap(
            context.contentResolver,
            uri
        )
        inputStream?.close()
        bitmap
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}