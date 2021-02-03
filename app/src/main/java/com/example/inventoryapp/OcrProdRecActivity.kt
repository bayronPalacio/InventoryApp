package com.example.inventoryapp

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.inventoryapp.Barcode.ScannerResultDialog
import com.example.inventoryapp.databinding.ActivityOcrProdRecBinding
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import kotlinx.android.synthetic.main.activity_ocr_prod_rec.*
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

const val ARG_SCAN_TYPE = "scanType"

class OcrProdRecActivity : AppCompatActivity() {

    companion object {
        @JvmStatic
        fun start(context: Context, scanType: MainActivity.ScanType) {
            val starter = Intent(context, OcrProdRecActivity::class.java).apply {
                putExtra(ARG_SCAN_TYPE, scanType)
            }
            context.startActivity(starter)
        }
    }

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var binding: ActivityOcrProdRecBinding

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService
    private var flashEnabled = false
    private var imageCapture: ImageCapture? = null
    private var scanType: MainActivity.ScanType = MainActivity.ScanType.Ocr

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOcrProdRecBinding.inflate(layoutInflater)
        setContentView(binding.root)
        scanType = intent?.getSerializableExtra(ARG_SCAN_TYPE) as MainActivity.ScanType

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(this))

        // Set up the listener for take pic btn
        takePic.setOnClickListener { takePhoto() }
        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()
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

        imageCapture = ImageCapture.Builder()
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

        preview.setSurfaceProvider(binding.cameraPreview.surfaceProvider)

        val camera = cameraProvider?.bindToLifecycle(this, cameraSelector, preview, imageCapture)

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

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return
        imageCapture.takePicture(cameraExecutor, object :
            ImageCapture.OnImageCapturedCallback() {
            @ExperimentalGetImage
            override fun onCaptureSuccess(image: ImageProxy) {
                val mediaImage = image.image
                val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
                if(scanType.equals(MainActivity.ScanType.Ocr)){
                    ocrAnalyzer(inputImage)
                }else{
                    prodRecAnalyser(inputImage)
                }
                super.onCaptureSuccess(image)
                image.close()
            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
            }
        })
    }

    private fun ocrAnalyzer(imageInt: InputImage) {

        val image: InputImage = imageInt
        try {
            val recognizer = TextRecognition.getClient()

            val result = recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    // Task completed successfully
                    var text = ""
                    for (block in visionText.textBlocks) {
                        text += block.text + "\n"
                    }
                    Log.e("OCR", text);
                    ScannerResultDialog.newInstance(
                        text,
                        object : ScannerResultDialog.DialogDismissListener {
                            override fun onDismiss() {
                                bindPreview(cameraProviderFuture.get())
                            }
                        })
                        .show(supportFragmentManager, ScannerResultDialog::class.java.simpleName)
//                    val products: ArrayList<Product> = arrayListOf()
//                    var isStartProduct: Boolean = false
//                    var i = 0
//                    while (i < visionText.textBlocks.size - 4) {
//                        val text = visionText.textBlocks
//                        Log.e(
//                            "Test",text[i].text)
//                        if (text[i].text.equals("1"))
//                            isStartProduct = true
//                        if (text[i].text.contains("NOTE"))
//                            break;
//                        if (isStartProduct) {
//                            val prod = Product(text[i + 1].text, text[i + 2].text, text[i + 3].text)
//                            products.add(prod)
//                            Log.e(
//                                "Test",
//                                text[i + 1].text + " " + text[i + 2].text + " " + text[i + 3].text
//                            )
//                            i += 3
//                        }
//
//                        i++
//
//                    }
//                    for (a in 0..products.size) {
//                        Log.e("Test", products[a].barcode + " " + products[a].quantity)
//                    }
                }
                .addOnFailureListener { e ->
                    // Task failed with an exception
                    // ...
                    Log.e("OCR", "Error OCR")
                }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun prodRecAnalyser(imageInt: InputImage){
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        labeler.process(imageInt)
            .addOnSuccessListener { labels ->
                val text = labels[0].text
                for (label in labels) {
                    val confidence = label.confidence
                    val index = label.index
                    Log.e("Image", label.text + "," + confidence + "," + index)
                }

                ScannerResultDialog.newInstance(
                    text,
                    object : ScannerResultDialog.DialogDismissListener {
                        override fun onDismiss() {
                            bindPreview(cameraProviderFuture.get())
                        }
                    })
                    .show(supportFragmentManager, ScannerResultDialog::class.java.simpleName)
            }
            .addOnFailureListener{e-> Log.e("ProdRec", "Error Product Recognition")}
    }

    override fun onDestroy() {
        super.onDestroy()
        // Shut down our background executor
        cameraExecutor.shutdown()
    }

}