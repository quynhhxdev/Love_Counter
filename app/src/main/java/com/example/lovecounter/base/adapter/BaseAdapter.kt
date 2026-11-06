package com.example.lovecounter.base.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class BaseAdapter<T : Any, VB : ViewBinding> :
    RecyclerView.Adapter<BaseAdapter.BaseViewHolder<VB>>() {

    protected val mItems = mutableListOf<T>()
    var onItemClickListener: ((T, Int) -> Unit)? = null

    abstract fun createBinding(inflater: LayoutInflater, parent: ViewGroup): VB
    abstract fun bind(binding: VB, item: T, position: Int)

    @SuppressLint("NotifyDataSetChanged")
    open fun submitList(newList: List<T>) {
        mItems.clear()
        mItems.addAll(newList)
        notifyDataSetChanged()
    }

    fun getItems(): List<T> = mItems.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<VB> {
        val inflater = LayoutInflater.from(parent.context)
        val binding = createBinding(inflater, parent)
        return BaseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<VB>, position: Int) {
        val item = mItems[position]
        bind(holder.binding, item, position)
        if (onItemClickListener != null) {
            holder.itemView.setOnClickListener {
                onItemClickListener?.invoke(item, position)
            }
        }
    }

    override fun getItemCount(): Int = mItems.size

    class BaseViewHolder<VB : ViewBinding>(val binding: VB) :
        RecyclerView.ViewHolder(binding.root)
}
