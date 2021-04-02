package com.example.inventoryapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.databinding.ProductItemBinding

class ProductAdapter(var productsList: List<Product>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // LayoutInflater: takes ID from layout defined in XML.
        // Instantiates the layout XML into corresponding View objects.
        // Use context from main app -> also supplies theme layout values!
        val inflater = LayoutInflater.from(parent.context)
        // Inflate XML. Last parameter: don't immediately attach new view to the parent view group
        val binding = ProductItemBinding.inflate(inflater, parent, false)
        return PartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // Populate ViewHolder with data that corresponds to the position in the list
        // which we are told to load
//        (holder as PartViewHolder).bind(productsList[position], clickListener)
        (holder as PartViewHolder).bind(productsList[position])
    }

    override fun getItemCount() = productsList.size

    inner class PartViewHolder(private val binding: ProductItemBinding) : RecyclerView.ViewHolder(binding.root) {
//        fun bind(product: Product, clickListener: (Product) -> Unit) {
//            binding.tvBarcode.text = product.barcode
//            binding.tvName.text = product.name
//            binding.tvQty.text = product.quantity.toString()
//            binding.root.setOnClickListener { clickListener(product) }
//        }
        fun bind(product: Product) {
            binding.tvBarcode.text = "Barcode: " + product.barcode
            binding.tvName.text = product.name
            binding.tvQty.text = "Quantity: " + product.quantity.toString()
        }
    }
}