package com.example.projeto

import android.app.Application
import com.example.projeto.database.AppDataBase

class ComprasPartilhadasApp: Application() {
    val database: AppDataBase by lazy {AppDataBase.getDatabase(this)}
}