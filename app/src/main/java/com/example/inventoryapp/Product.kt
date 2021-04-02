package com.example.inventoryapp

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Product(
    var barcode: String,
    var name: String,
    var price: Double,
    var quantity: Int,
    var total: Double
) : Parcelable