package com.example.projeto.adapters

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.EditText
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.projeto.R
import com.example.projeto.database.Lista
import com.example.projeto.databinding.CardListaComprasRowBinding
import com.example.projeto.placeholder.ListaAlteradaEvento
import com.example.projeto.viewmodels.ListasUtilizadoresViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

class AdapterListasListaCompras(private val viewModel: ListasUtilizadoresViewModel,
                                private val onItemClicked: (Lista) -> Unit) :
    androidx.recyclerview.widget.ListAdapter<Lista, AdapterListasListaCompras.ListasListaComprasViewHolder>(DiffCallback){

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Lista>() {
            override fun areItemsTheSame(oldItem: Lista, newItem: Lista): Boolean {
                return oldItem.idLista == newItem.idLista
            }

            override fun areContentsTheSame(oldItem: Lista, newItem: Lista): Boolean {
                return oldItem == newItem
            }
        }
    }

    // Guarda a lista eliminada para reverter
    private var removedItem: Lista? = null
    private lateinit var viewParent: View

    // Guarda as quantidades totais de cada lista
    private val listaQuantidadeMap: MutableMap<Int, Int> = mutableMapOf()

    class ListasListaComprasViewHolder(private var binding: CardListaComprasRowBinding): RecyclerView.ViewHolder(binding.root){

        // Recebe a lista e a quantidade de itens únicos dentro dela
        fun bind(lista: Lista, quantidade: Int){
            binding.nomeLista.text = lista.nome
            binding.quantidade.text = binding.root.context.getString(
                R.string.num_item_str, quantidade
            )
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListasListaComprasViewHolder {

        val viewHolder = ListasListaComprasViewHolder(
            CardListaComprasRowBinding.inflate(
                LayoutInflater.from( parent.context),
                parent,
                false
            )
        )

        // Guardar rootView
        viewParent = parent.rootView

        // Animação de apagar lista ou de editar o nome dela
        viewHolder.itemView.setOnTouchListener { v, event ->
            // Carregar animator
            val swipeAnimation = AnimationUtils.loadAnimation(v.context, R.anim.right_to_left)
            swipeAnimation.duration = 700

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {
                    // Armazenar as coordenadas iniciais do toque e o tempo inicial do toque
                    v.tag = Pair(event.x, System.currentTimeMillis())
                }
                MotionEvent.ACTION_UP -> {
                    val map = v.tag as Pair<Float, Long>

                    // Calcula movimento no eixo X
                    val deltaX = event.x - map.first

                    // Calcula tempo de toque
                    val tempoPassado = System.currentTimeMillis() - map.second

                    // Arrastado para a esquerda
                    if (deltaX < 0) {
                        swipeAnimation.let {
                            // Código para remover o item do RecyclerView após a conclusão da animação
                            it.setAnimationListener(object : Animation.AnimationListener {
                                override fun onAnimationStart(animation: Animation?) {}

                                override fun onAnimationEnd(animation: Animation?) {
                                    val position = viewHolder.adapterPosition
                                    if (position != RecyclerView.NO_POSITION) {
                                        val lista = getItem(position)

                                        // Guarda a lista removida
                                        removedItem = lista

                                        // Remove e notifica
                                        viewModel.removerLista(lista)
                                        notifyItemRemoved(position)
                                        mostrarSnackbarDesfazerRemocao()
                                    }

                                    // Mantém o card fora de vista para garantir uma efeito melhor
                                    v.translationX = -v.width.toFloat()
                                }
                                override fun onAnimationRepeat(animation: Animation?) {}
                            })
                            v.startAnimation(it)
                        }

                    //  Clique longo (+ de 500ms a pressionar)
                    } else if(tempoPassado >= 500){
                        mostrarDialogAlterarNomeLista(getItem(viewHolder.adapterPosition))

                    //Clicado apenas
                    } else{
                        val position = viewHolder.adapterPosition
                        onItemClicked(getItem(position))
                        v.performClick()
                    }
                }
            }
            true
        }

        return viewHolder
    }


    override fun onBindViewHolder(holder: ListasListaComprasViewHolder, position: Int) {
        val lista = getItem(position)
        val quantidade = listaQuantidadeMap[lista.idLista] ?: 0
        holder.bind(lista, quantidade)
    }

    // Setter do Map que controla o número total de itens na lista
    fun setQuantidadeTotalItens(quantidade: Map<Int, Int>) {
        listaQuantidadeMap.clear()
        listaQuantidadeMap.putAll(quantidade)
        notifyDataSetChanged()
    }

    fun getQuantidadeTotalItens(): Map<Int, Int> {
        return listaQuantidadeMap
    }

    // Método que mostra uma avis para se desfazer a ação de remover lista
    private fun mostrarSnackbarDesfazerRemocao() {
        val snackbar = Snackbar.make(
            viewParent.rootView,
            viewParent.context.getString(R.string.removed_list),
            Snackbar.LENGTH_LONG
        )

        val snackbarView = snackbar.view

        // Ajusta a margem inferior para evitar sobreposição com a barra de navegação
        val params = snackbarView.layoutParams as ViewGroup.MarginLayoutParams
        params.bottomMargin += 54
        snackbarView.layoutParams = params

        snackbar.setAction( viewParent.context.getString(R.string.undo)) {
            // Reinsere o item removido na lista e notifica o RecyclerView
            removedItem?.let {
                viewModel.viewModelScope.launch {
                    viewModel.inserirNovaLista(it)
                    notifyDataSetChanged()

                    // O EventBus garante que o card do item reposto é apresentado na UI
                    EventBus.getDefault().post(ListaAlteradaEvento())
                    removedItem = null
                }
            }
        }

        snackbar.show()
    }


    // Dialog mostrado quando há um longPress na lista
    private fun mostrarDialogAlterarNomeLista(lista: Lista) {
        val context = viewParent.context
        val editText = EditText(context)
        editText.setText(lista.nome)

        val dialog = AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.alterar_nome_da_lista))
            .setView(editText)
            .setPositiveButton(context.getString(R.string.guardar)) { _, _ ->
                val novoNome = editText.text.toString()
                viewModel.atualizarNome(lista, novoNome)
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .create()

        dialog.show()
    }

}