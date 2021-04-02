package com.example.inventoryapp.barcode

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.inventoryapp.*
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.ActivityBarcodeScanningBinding
import com.google.common.util.concurrent.ListenableFuture
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BarcodeScanningActivity : AppCompatActivity() {

    companion object {
        @JvmStatic
        fun start(context: Context, scanType: MainActivity.ScanType) {
            val starter = Intent(context, BarcodeScanningActivity::class.java).apply {
                putExtra(ARG_SCAN_TYPE, scanType)
            }
            context.startActivity(starter)
        }
    }

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var binding: ActivityBarcodeScanningBinding

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService
    private var flashEnabled = false
    private var scanType: MainActivity.ScanType = MainActivity.ScanType.QR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBarcodeScanningBinding.inflate(layoutInflater)
        setContentView(binding.root)
        scanType = intent?.getSerializableExtra(ARG_SCAN_TYPE) as MainActivity.ScanType

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(this))

        binding.overlay.post {
            binding.overlay.setViewFinder()
        }
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider?) {

        if (isDestroyed || isFinishing) {
            //This check is to avoid an exception when trying to re-bind use cases but user closes the activity.
            //java.lang.IllegalArgumentException: Trying to create use case mediator with destroyed lifecycle.
            return
        }

        cameraProvider?.unbindAll()

        val preview: Preview = Preview.Builder()
            .build()

        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        val orientationEventListener = object : OrientationEventListener(this as Context) {
            override fun onOrientationChanged(orientation: Int) {
                // Monitors orientation values to determine the target rotation value
                val rotation: Int = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }

                imageAnalysis.targetRotation = rotation
            }
        }
        orientationEventListener.enable()

        class ScanningListener : ScanningResultListener {
            override fun onScanned(result: String) {
                runOnUiThread {
                    imageAnalysis.clearAnalyzer()
                    cameraProvider?.unbindAll()
                    if(scanType.equals(MainActivity.ScanType.QR)){
                        val arrayProductType = object : TypeToken<ArrayList<Product>>() {}.type
                        val productsList: ArrayList<Product> = Gson().fromJson(result, arrayProductType)

                        ProductsActivity.start(this@BarcodeScanningActivity, productsList)
                    }else{
                        ScannerResultDialog.newInstance(
                            result,
                            object : ScannerResultDialog.DialogDismissListener {
                                override fun onDismiss() {
                                    bindPreview(cameraProvider)
                                }
                            })
                            .show(supportFragmentManager, ScannerResultDialog::class.java.simpleName)
                    }
                }
            }
        }

        val analyzer: ImageAnalysis.Analyzer = MLKitBarcodeAnalyzer(ScanningListener())

        imageAnalysis.setAnalyzer(cameraExecutor, analyzer)

        preview.setSurfaceProvider(binding.cameraPreview.surfaceProvider)

        val camera = cameraProvider?.bindToLifecycle(this, cameraSelector, imageAnalysis, preview)

        if (camera?.cameraInfo?.hasFlashUnit() == true) {
            binding.ivFlashControl.visibility = View.VISIBLE

            binding.ivFlashControl.setOnClickListener {
                camera.cameraControl.enableTorch(!flashEnabled)
            }

            camera.cameraInfo.torchState.observe(this) {
                it?.let { torchState ->
                    if (torchState == TorchState.ON) {
                        flashEnabled = true
                        binding.ivFlashControl.setImageResource(R.drawable.ic_round_flash_on)
                    } else {
                        flashEnabled = false
                        binding.ivFlashControl.setImageResource(R.drawable.ic_round_flash_off)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Shut down our background executor
        cameraExecutor.shutdown()
    }

}