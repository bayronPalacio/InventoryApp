package com.example.inventoryapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.inventoryapp.barcode.BarcodeScanningActivity
import com.example.inventoryapp.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val cameraPermissionRequestCode = 1
    private lateinit var binding: ActivityMainBinding
    private lateinit var scanType: ScanType

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        tvCompany.text = "Welcome\n" + intent.getStringExtra("company")

        binding.cardBarcode.setOnClickListener {
            scanType = ScanType.Barcode
            startScanning()
        }

        binding.cardQR.setOnClickListener {
            scanType = ScanType.QR
            startScanning()
        }

        binding.cardProdRec.setOnClickListener{
            scanType = ScanType.ProdRec
            startScanning()
        }
    }

    private fun startScanning() {
        if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
        ) {
            openCameraWithScanner()
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    cameraPermissionRequestCode
            )
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraPermissionRequestCode && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCameraWithScanner()
            } else if (!ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.CAMERA
                    )
            ) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivityForResult(intent, cameraPermissionRequestCode)
            }
        }
    }

    private fun openCameraWithScanner() {
        when (scanType) {
            ScanType.Barcode, ScanType.QR -> BarcodeScanningActivity.start(this, scanType)
            ScanType.ProdRec -> OcrProdRecActivity.start(this, scanType)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == cameraPermissionRequestCode) {
            if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
            ) {
                openCameraWithScanner()
            }
        }
    }

    enum class ScanType{
        Barcode,
        QR,
        ProdRec
    }
}