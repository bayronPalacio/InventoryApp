package com.example.inventoryapp.barcode

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.inventoryapp.MainActivity
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.FragmentScannerResultDialogListDialogBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_scanner_result_dialog_list_dialog.*
import org.json.JSONObject
import java.io.BufferedReader
import java.lang.Exception

const val ARG_SCANNING_RESULT = "scanning_result"

class ScannerResultDialog(private val listener: DialogDismissListener) :
    BottomSheetDialogFragment() {

    private lateinit var binding: FragmentScannerResultDialogListDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentScannerResultDialogListDialogBinding.inflate(
            inflater, container, false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val scannedResult = arguments?.getString(ARG_SCANNING_RESULT)
        binding.etBarcode.setText(scannedResult)
        binding.btnSubmit.setOnClickListener {

            var qty = 0;
            try {
                qty = etQty.text.toString().toInt()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Please verify quantity!",
                    Toast.LENGTH_LONG
                ).show()
            }
            if (qty > 0 && qty < 10000) {
                val fileReader: BufferedReader =
                    context!!.applicationContext.assets.open("url.txt")?.bufferedReader()
                val url = fileReader.readLine()
                val urlPath = "$url/addQuantity"

                //Retrieve from SharedPreference
                val preference = activity?.getSharedPreferences(
                    resources.getString(R.string.app_name),
                    Context.MODE_PRIVATE
                )
                val company = preference?.getString(getString(R.string.companyName), "")

                val product = JSONObject()
                product.put("barcode", etBarcode.text.toString())
                product.put("qty", etQty.text.toString())
                product.put("company", company)

                val que = Volley.newRequestQueue(context)
                val req = JsonObjectRequest(
                    Request.Method.POST, urlPath, product,
                    { response ->
                        Log.e("res", response["responseServer"].toString())
                        if (response["responseServer"].toString().equals("Qty added")) {
                            Toast.makeText(
                                context,
                                "Quantity has been updated",
                                Toast.LENGTH_LONG
                            ).show()
                        } else if (response["responseServer"].toString()
                                .equals("Problem adding qty")
                        ) {
                            Toast.makeText(
                                context,
                                "Problem adding qty, Please try again",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Product doesn't exists on DB, Please report it",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        BarcodeScanningActivity.start(context!!, MainActivity.ScanType.Barcode)
                        println("Response from server -> " + response["responseServer"])
                    }, {
                        println("Error from server")
                    }
                )
                que.add(req)
            } else {
                Toast.makeText(
                    context,
                    "Please verify quantity!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    companion object {
        fun newInstance(
            scanningResult: String,
            listener: DialogDismissListener
        ): ScannerResultDialog =
            ScannerResultDialog(listener).apply {
                arguments = Bundle().apply {
                    putString(ARG_SCANNING_RESULT, scanningResult)
                }
            }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        listener.onDismiss()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        listener.onDismiss()
    }

    interface DialogDismissListener {
        fun onDismiss()
    }
}