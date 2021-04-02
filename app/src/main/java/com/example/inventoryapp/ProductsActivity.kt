package com.example.inventoryapp

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.inventoryapp.barcode.BarcodeScanningActivity
import com.example.inventoryapp.databinding.ActivityProductsBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.fragment_scanner_result_dialog_list_dialog.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader

const val ARG_PRODUCTS_LIST = "productsList"

class ProductsActivity : AppCompatActivity() {
    companion object {
        @JvmStatic
        fun start(context: Context, productsList: ArrayList<Product>) {
            val starter = Intent(context, ProductsActivity::class.java).apply {
                putParcelableArrayListExtra(ARG_PRODUCTS_LIST, productsList)
            }
            context.startActivity(starter)
        }
    }

    private lateinit var binding: ActivityProductsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var productsList = intent?.getSerializableExtra(ARG_PRODUCTS_LIST) as ArrayList<Product>

        binding.rvProducts.layoutManager = LinearLayoutManager(this)
        binding.rvProducts.adapter = ProductAdapter(productsList)

        binding.tbnSubmitProds.setOnClickListener {
            sendProductsList(productsList)
        }

        // Create the PartAdapter
        // 1st parameter: our generated testData
        // 2nd parameter: item click handler function (implemented below) as function parameter
//        binding.rvProducts.adapter = ProductAdapter(testData) { partItem: Product ->
//            partItemClicked(
//                partItem
//            )
//        }

    }

//    private fun partItemClicked(partItem : PartData) {
//        Toast.makeText(this, "Clicked: ${partItem.itemName}", Toast.LENGTH_LONG).show()
//
//        // Launch second activity, pass part ID as string parameter
//        val showDetailActivityIntent = Intent(this, PartDetailActivity::class.java)
//        showDetailActivityIntent.putExtra(Intent.EXTRA_TEXT, partItem.id.toString())
//        startActivity(showDetailActivityIntent)
//    }

    private fun sendProductsList(productsList: ArrayList<Product>) {
        val fileReader: BufferedReader =
            this.applicationContext.assets.open("url.txt")?.bufferedReader()
        val url = fileReader.readLine()
        val urlPath = "$url/addProductsList"

        //Retrieve from SharedPreference
        val preference = this.getSharedPreferences(
            resources.getString(R.string.app_name),
            Context.MODE_PRIVATE
        )
        val company = preference?.getString(getString(R.string.companyName), "")

        val products = JSONObject()
        val prodString = Gson().toJson(productsList)
        products.put("products", prodString)
        products.put("company", company)

        val que = Volley.newRequestQueue(this)
        val req = JsonObjectRequest(
            Request.Method.POST, urlPath, products,
            { response ->
                Log.e("res", response["responseServer"].toString())
                if (response["responseServer"].toString().equals("Qty added")) {
                    Toast.makeText(
                        this,
                        "Quantity has been updated",
                        Toast.LENGTH_LONG
                    ).show()
                } else if (response["responseServer"].toString()
                        .equals("Problem adding qty")
                ) {
                    Toast.makeText(
                        this,
                        "Problem adding qty, Please try again",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Product doesn't exists on DB, Please report it",
                        Toast.LENGTH_LONG
                    ).show()
                }
                BarcodeScanningActivity.start(this, MainActivity.ScanType.QR)
                println("Response from server -> " + response["responseServer"])
            }, {
                println("Error from server")
            }
        )
        que.add(req)
    }

}