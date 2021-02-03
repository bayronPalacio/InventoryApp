package com.example.inventoryapp.Barcode

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.inventoryapp.databinding.FragmentScannerResultDialogListDialogBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_scanner_result_dialog_list_dialog.*
import org.json.JSONObject
import java.io.BufferedReader

const val ARG_SCANNING_RESULT = "scanning_result"

class ScannerResultDialog(private val listener: DialogDismissListener) : BottomSheetDialogFragment() {

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
        binding.btnSubmit.setOnClickListener{
            val fileReader: BufferedReader = context!!.applicationContext.assets.open("url.txt")?.bufferedReader()
            val url = fileReader.readLine()
            val urlPath = "$url/addProduct"

            val product = JSONObject()
            product.put("Barcode", etBarcode.text.toString())
            product.put("Qty", etQty.text.toString())

            val que = Volley.newRequestQueue(context)
            val req = JsonObjectRequest(
                    Request.Method.POST, urlPath, product,
                    { response ->
                        if (response["responseServer"].toString().equals("Yes")) {
                            Toast.makeText(
                                    context,
                                    "Product has been added",
                                    Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(context, "Error on server", Toast.LENGTH_LONG).show()
                        }
                        println("Response from server -> " + response["responseServer"])
                    }, {
                println("Error from server")
            }
            )
            que.add(req)
        }
    }

    companion object {
        fun newInstance(scanningResult: String, listener: DialogDismissListener): ScannerResultDialog =
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