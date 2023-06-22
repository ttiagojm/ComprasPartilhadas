package com.example.projeto.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.projeto.R
import com.example.projeto.database.Item
import com.example.projeto.databinding.FragmentItemBinding
import com.example.projeto.placeholder.ItemEliminadoEvento
import com.example.projeto.placeholder.getBitmapFromUri
import com.example.projeto.viewmodels.ItensListasViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus


/* O procuraItem serve para sabermos se o adaptador está a ser usado na lista ou na pesquisa
   O Map garante a sincronização com as quantidades de itens*/
class AdapterItemListaCompras(private val procuraItem: Boolean,
                              private val viewModel: ItensListasViewModel,
                              private val listaId: Int,
                              var itemQuantidadeMap: Map<Int, Int> = mutableMapOf(),
                              private val onItemClicked: (Item) -> Unit) :
    androidx.recyclerview.widget.ListAdapter<Item, AdapterItemListaCompras.ItemListaComprasViewHolder>(DiffCallback){


    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
                return oldItem.idItem == newItem.idItem
            }

            override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
                return oldItem == newItem
            }
        }
    }

    // Guarda a item eliminado para possível reversão
    private var removedItem: Pair<Item, Int>? = null

    // Armazena a view associada ao adaptador para poder chamar elementos da UI
    private lateinit var viewParent: View

    class ItemListaComprasViewHolder(private val itemQuantidadeMap: Map<Int, Int>,
                                     private var binding: FragmentItemBinding): RecyclerView.ViewHolder(binding.root){

        fun bind(item: Item){

            // Esta condicional garante que a imagem do item aparece no card do produto
           if(item.imagem.toIntOrNull() != null) {
               val drawable = ContextCompat.getDrawable(binding.prodImage.context, item.imagem.toInt())
               binding.prodImage.setImageDrawable(drawable)
           } else{
               // Caso dê erro a carregar o bitmap coloca a imagem por omissão
               try {
                   val bitmap = getBitmapFromUri(binding.root.context, Uri.parse(item.imagem))
                   binding.prodImage.setImageBitmap(bitmap)
               } catch (e: Exception){
                   val drawable = ContextCompat.getDrawable(binding.prodImage.context, R.drawable.no_img)
                   binding.prodImage.setImageDrawable(drawable)
               }
           }

            // Nome e design da imagem do item
            binding.prodImage.clipToOutline = true
            binding.nomeItem.text = item.nome

            // Coloca no card, a quantidade do item
            val quantidade = itemQuantidadeMap[item.idItem] ?: 0
            binding.quantidade.text = binding.root.context.getString(
                R.string.quantidade_str, quantidade
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemListaComprasViewHolder {

        // Guardar a view
        viewParent = parent.rootView

        val viewHolder = ItemListaComprasViewHolder(
            itemQuantidadeMap,
            FragmentItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

        // Funcionalidade de eliminar item, apenas se o adaptador não estiver a ser usado
        // na barra de pesquisa
        if (!procuraItem){
            viewHolder.itemView.setOnTouchListener { v, event ->
                // Carregar animator
                val swipeAnimation = AnimationUtils.loadAnimation(v.context, R.anim.right_to_left)
                swipeAnimation.duration = 700

                when (event.action) {

                    MotionEvent.ACTION_DOWN -> {
                        // Armazenar as coordenadas iniciais do toque
                        v.tag = event.x
                    }

                    MotionEvent.ACTION_UP -> {
                        val startX = v.tag as Float
                        val endX = event.x
                        val deltaX = endX - startX

                        // Arrastado para a esquerda
                        if (deltaX < 0) {
                            swipeAnimation.let {
                                // Código para remover o item do RecyclerView após a conclusão da animação
                                it.setAnimationListener(object : Animation.AnimationListener {
                                    override fun onAnimationStart(animation: Animation?) {}

                                    override fun onAnimationEnd(animation: Animation?) {
                                        val position = viewHolder.adapterPosition
                                        if (position != RecyclerView.NO_POSITION) {
                                            val item = getItem(position)

                                            // Guarda a item
                                            removedItem = Pair(item, itemQuantidadeMap[item.idItem]?: 0)

                                            // Remove e notifica
                                            viewModel.removerItemDaLista(item, listaId)
                                            notifyItemRemoved(position)

                                            // Notifica o Item fragment
                                            EventBus.getDefault().post(ItemEliminadoEvento())

                                            mostrarSnackbarDesfazerRemocao()
                                        }

                                        // Reinicializar as propriedades de animação
                                        v.translationX = -v.width.toFloat()
                                    }

                                    override fun onAnimationRepeat(animation: Animation?) {}
                                })
                                v.startAnimation(it)
                            }

                            // Clicado ou arrastado para a direita
                        } else {
                            val position = viewHolder.adapterPosition
                            onItemClicked(getItem(position))
                        }

                        v.performClick()
                    }
                }
                true
            }

            // Caso seja uma pesquisa apenas permite onClick
        } else{
            viewHolder.itemView.setOnClickListener {
                val position = viewHolder.adapterPosition
                onItemClicked(getItem(position))
            }
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: ItemListaComprasViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getPosicaoItem(itemId: Int): Int {
        for (index in 0 until currentList.size) {
            if (currentList[index].idItem == itemId) {
                return index
            }
        }
        return RecyclerView.NO_POSITION
    }

    // Método que mostra uma avis para se desfazer a ação de remover item
    private fun mostrarSnackbarDesfazerRemocao() {
        val snackbar = Snackbar.make(
            viewParent.rootView,
            viewParent.context.getString(R.string.removed_item),
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
                    viewModel.inserirNovoItem(it.first)
                    viewModel.inserirItemEmLista(listaId, it.first.idItem, it.second)
                    notifyDataSetChanged()

                    // O EventBus garante que o card do item reposto é apresentado na UI
                    EventBus.getDefault().post(ItemEliminadoEvento())
                    removedItem = null
                }
            }
        }

        snackbar.show()
    }

}
