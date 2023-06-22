package com.example.projeto.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [
    Item::class,
    Lista::class,
    ItemListaRefCruzada::class,
    Utilizador::class
], version = 5)
abstract class AppDataBase: RoomDatabase() {
    abstract fun utilizadorDao(): UtilizadorDao
    abstract fun itemDao(): ItemDao
    abstract fun listaDao(): ListaDao

    companion object{
        @Volatile
        private var INSTANCE: AppDataBase? = null

        fun getDatabase(context: Context): AppDataBase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context,
                    AppDataBase::class.java,
                    "app_database")
                    .fallbackToDestructiveMigration()
                    .createFromAsset("databases/app_database.db")
                    .build()
                INSTANCE = instance

                instance
            }
        }
    }


}