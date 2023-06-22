package com.example.projeto

import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.projeto.database.AppDataBase
import com.example.projeto.database.Item
import com.example.projeto.database.Lista
import com.example.projeto.database.Utilizador
import com.example.projeto.placeholder.PlaceholderContent
import com.example.projeto.viewmodels.UtilizadorFactory
import com.example.projeto.viewmodels.UtilizadorViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(){
    private lateinit var navController: NavController

    // viewModel para a tabela de utilizadores
    private val viewModel: UtilizadorViewModel by viewModels {
        UtilizadorFactory(
            (this?.application as ComprasPartilhadasApp).database.utilizadorDao()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Convertemos o fragmentContainer num NavHostFragment e usamo-lo para configurar a toolbar
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        navController = navHostFragment.findNavController()

        setSupportActionBar(findViewById(R.id.toolbar))
        setupActionBarWithNavController(navController)


        // TODO: Descomentar sempre que quiser-se popular a DB
        //insertData()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu.findItem(R.id.search_bar).actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                val listasFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull() as? ListasFragment
                listasFragment?.filterItems(newText)

                val navHostFragmentItem = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                val itemsFragment = navHostFragmentItem?.childFragmentManager?.fragments?.firstOrNull() as? ItemFragment
                itemsFragment?.filterItems(newText)

                return true
            }

        })

        return true
    }

    /*
        Método que coloca a seta para retornar para o ecrã anterior
     */
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_settings -> {
            viewModel.fazerLogout(PlaceholderContent.DEFAULT_USER)
            navController.navigate(R.id.loginFragment)
            true
        }
        R.id.action_listas ->{
            navController.navigate(R.id.listasFragment)
            true
        }
        R.id.action_delete_user -> {

            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.confirmation))
            builder.setMessage(getString(R.string.do_you_want_to_delete_this_user))
            builder.setPositiveButton(getString(R.string.yes)) { dialogInterface, _ ->
                dialogInterface.dismiss()

                // Eliminar utilizador
                viewModel.eliminarUtilizador(PlaceholderContent.DEFAULT_USER)
                viewModel.fazerLogout(PlaceholderContent.DEFAULT_USER)
                navController.navigate(R.id.loginFragment)
            }
            builder.setNegativeButton(getString(R.string.no)) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }

            builder.create().show()
            true
        } else -> {
            super.onOptionsItemSelected(item)
        }
    }


    // Metodo usado nos testes do app para popular a base de dados
    private fun insertData() {
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {

            // Perform the insertion here
            val appDatabase = AppDataBase.getDatabase(applicationContext)
            val utilizadorDao = appDatabase.utilizadorDao()
            val listaDao = appDatabase.listaDao()
            val itemDao = appDatabase.itemDao()

            val utilizador1 = Utilizador(email = "joao@gmail.com", nome = "João", password = "joao")
            val utilizador2 = Utilizador(email = "goncalo@gmail.com", nome = "Gonçalo", password = "goncalo")
            val utilizador3 = Utilizador(email = "tiago@gmail.com", nome = "Tiago", password = "tiago")

            listaDao.inserirLista(Lista(nome = "Lista João", emailUser = "joao@gmail.com"))

            listaDao.inserirLista(Lista(nome = "Lista Gonçalo", emailUser = "goncalo@gmail.com"))
            listaDao.inserirLista(Lista(nome = "Lista Tiago", emailUser = "tiago@gmail.com"))

            itemDao.inserirItem(Item(nome = "Aveia", preco=2.90f, imagem = R.drawable.aveia.toString()))
            itemDao.inserirItem(Item(nome = "Bifes de Peru", preco=3.10f, imagem = R.drawable.bifesperu.toString()))
            itemDao.inserirItem(Item(nome = "Cenoura", preco=0.90f, imagem = R.drawable.cenoura.toString()))
            itemDao.inserirItem(Item(nome = "Couve", preco=1.30f, imagem = R.drawable.couve.toString()))
            itemDao.inserirItem(Item(nome = "Banana", preco=1.70f, imagem = R.drawable.banana.toString()))
            itemDao.inserirItem(Item(nome = "Maça", preco=0.60f, imagem = R.drawable.maca.toString()))
            itemDao.inserirItem(Item(nome = "Costeletas de Porco", preco=2.30f, imagem = R.drawable.costoletasporco.toString()))
            itemDao.inserirItem(Item(nome = "Bolachas de Arroz", preco=1.10f, imagem = R.drawable.bolachaarroz.toString()))
            itemDao.inserirItem(Item(nome = "Leite", preco=2.50f, imagem = R.drawable.leite.toString()))
            itemDao.inserirItem(Item(nome = "Ovos", preco=1.80f, imagem = R.drawable.ovos.toString()))
            itemDao.inserirItem(Item(nome = "Tomate", preco=0.70f, imagem = R.drawable.tomate.toString()))
            itemDao.inserirItem(Item(nome = "Pão", preco=1.20f, imagem = R.drawable.pao.toString()))
            itemDao.inserirItem(Item(nome = "Iogurte", preco=1.50f, imagem = R.drawable.iogurte.toString()))
            itemDao.inserirItem(Item(nome = "Cebola", preco=0.80f, imagem = R.drawable.cebola.toString()))
            itemDao.inserirItem(Item(nome = "Laranja", preco=1.10f, imagem = R.drawable.laranja.toString()))
            itemDao.inserirItem(Item(nome = "Chocolate", preco=2.70f, imagem = R.drawable.chocolate.toString()))
            itemDao.inserirItem(Item(nome = "Arroz", preco=3.50f, imagem = R.drawable.arroz.toString()))
            itemDao.inserirItem(Item(nome = "Feijão", preco=2.80f, imagem = R.drawable.feijao.toString()))
            itemDao.inserirItem(Item(nome = "Macarrão", preco=1.90f, imagem = R.drawable.macarrao.toString()))
            itemDao.inserirItem(Item(nome = "Cerveja", preco=2.20f, imagem = R.drawable.cerveja.toString()))
            itemDao.inserirItem(Item(nome = "Água Mineral", preco=0.80f, imagem = R.drawable.agua.toString()))
            itemDao.inserirItem(Item(nome = "Sabonete", preco=1.50f, imagem = R.drawable.sabonete.toString()))
            itemDao.inserirItem(Item(nome = "Detergente", preco=1.10f, imagem = R.drawable.detergente.toString()))
            itemDao.inserirItem(Item(nome = "Papel Higiênico", preco=2.40f, imagem = R.drawable.papelhigienico.toString()))
            itemDao.inserirItem(Item(nome = "Azeite de Oliva", preco=4.50f, imagem = R.drawable.azeite.toString()))
            itemDao.inserirItem(Item(nome = "Café", preco=3.20f, imagem = R.drawable.cafe.toString()))
            itemDao.inserirItem(Item(nome = "Manteiga", preco=2.70f, imagem = R.drawable.manteiga.toString()))
            itemDao.inserirItem(Item(nome = "Queijo Cheddar", preco=2.90f, imagem = R.drawable.queijo.toString()))
            itemDao.inserirItem(Item(nome = "Presunto", preco=2.40f, imagem = R.drawable.presunto.toString()))
            itemDao.inserirItem(Item(nome = "Refrigerante", preco=1.80f, imagem = R.drawable.refrigerante.toString()))
            itemDao.inserirItem(Item(nome = "Sabão em Pó", preco=3.10f, imagem = R.drawable.sabao.toString()))
            itemDao.inserirItem(Item(nome = "Escova de Dentes", preco=1.50f, imagem = R.drawable.escovadentes.toString()))
            itemDao.inserirItem(Item(nome = "Shampoo", preco=2.20f, imagem = R.drawable.shampoo.toString()))
            itemDao.inserirItem(Item(nome = "Condicionador", preco=2.20f, imagem = R.drawable.condicionador.toString()))

            try {
                utilizadorDao.inserirUtilizador(utilizador1)
                utilizadorDao.inserirUtilizador(utilizador2)

                utilizadorDao.inserirUtilizador(utilizador3)


                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Data inserted successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Failed to insert data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}