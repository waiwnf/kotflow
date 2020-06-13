package com.example.kotflow

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.SystemClock
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.Type
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.kotflow.databinding.LayoutCamBinding
import com.example.kotflow.rs.NnInputPipeline
import kotlinx.android.synthetic.main.layout_cam.*
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

// All NN setup to helper.
// Figure out orientations.
// Add segmentation model.
// Make hamburger with options.


class CameraFragment : Fragment() {
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private val inVideoSize = Size(640, 480)
    private val blurRadius = 1F

    private lateinit var cameraController: CameraController

    // NN configs -> to json / options.
    private val tfModelPath = "mobilenet-v2.tflite"
    private val tfBackend = TfModel.Backend.CPU

    private val ptModelPath = "mobilenet-v2.pt"

    private val inferenceThreads = 4

    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(0F, 255F))
        .build()

    private lateinit var model: NnModel

    private lateinit var inferenceInBitmap: Bitmap

    lateinit var rs: RenderScript
    lateinit var nnVideoPreproc: NnInputPipeline

    lateinit var outViewAlloc: Allocation

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: LayoutCamBinding = DataBindingUtil.inflate(
            inflater, R.layout.layout_cam, container, false
        )

        // Set the lifecycleOwner so DataBinding can observe LiveData
        binding.lifecycleOwner = viewLifecycleOwner

        // Switch between the TF and Pytorch models here by commenting/uncommenting.
        model = TfModel(context!!, tfModelPath, imageProcessor, inferenceThreads, tfBackend)
//        model = PyTorchModel(context!!.assets, ptModelPath, 224, 224, inferenceThreads)
        inferenceInBitmap = Bitmap.createBitmap(model.inWidth, model.inHeight, Bitmap.Config.ARGB_8888)

        rs = RenderScript.create(context)

        nnVideoPreproc = NnInputPipeline(
            rs,
            inVideoSize,
            Size(model.inWidth, model.inHeight),
            blurRadius
        )
        outViewAlloc = Allocation.createTyped(
            rs, Type.createXY(rs, Element.RGBA_8888(rs), model.inWidth, model.inHeight),
            Allocation.USAGE_SCRIPT or Allocation.USAGE_IO_OUTPUT
        )

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val manager = activity!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraController = CameraController(manager)
        maybeStartCamera()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                maybeStartCamera()
            } else {
                Toast.makeText(context, "Permissions not granted by the user.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun maybeStartCamera() {
        if (!allPermissionsGranted()) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        } else if (!viewFinder.isAvailable) {
            viewFinder.surfaceTextureListener =
                LambdaAvailableTextureListener { maybeStartCamera() }
        } else {
            startCamera()
        }
    }

    private fun allPermissionsGranted() = context.let { ctx ->
        ctx != null && REQUIRED_PERMISSIONS.all { perm ->
            ctx.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission", "LogNotTimber")
    private fun startCamera() {
        Timber.d("Start camera stub.")

        val texture: SurfaceTexture = viewFinder.surfaceTexture
        texture.setDefaultBufferSize(model.inWidth, model.inHeight)
        outViewAlloc.surface = Surface(texture)

        var frameCount = 0
        var startTime = -1L
        var inferenceTime = 0L

        nnVideoPreproc.inAlloc.setOnBufferAvailableListener { a ->
            if (startTime < 0) {
                startTime = SystemClock.uptimeMillis()
            }

            a.ioReceive()

            // TODO farm out heavy image processing to a separate thread to skip the
            //      accumulating frames quickly.
            nnVideoPreproc.run()
            nnVideoPreproc.outAlloc.copyTo(inferenceInBitmap)

            val inferenceStart = SystemClock.uptimeMillis()

            model.inferBitmap(inferenceInBitmap)

            inferenceTime += SystemClock.uptimeMillis() - inferenceStart

            outViewAlloc.copyFrom(inferenceInBitmap)
            outViewAlloc.ioSend()

            val walltimeSinceStarted = SystemClock.uptimeMillis() - startTime

            frameCount++
            if (frameCount % 100 == 0) {
                Log.i("CameraFragment",
                    "Frame $frameCount took $walltimeSinceStarted ms. " +
                            "Inference wall time $inferenceTime.")
            }
        }

        cameraController.start(nnVideoPreproc.inAlloc.surface)
    }
}

class LambdaAvailableTextureListener(val block: () -> Unit) : TextureView.SurfaceTextureListener {
    override fun onSurfaceTextureAvailable(texture: SurfaceTexture?, width: Int, height: Int) =
        block()

    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture?, width: Int, height: Int) {}
    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture?) = true
    override fun onSurfaceTextureUpdated(texture: SurfaceTexture?) {}
}

/**
 * Copies specified asset to the file in /files app directory and returns this file absolute path.
 *
 * @return absolute file path
 */
@Throws(IOException::class)
fun assetFilePath(
    context: Context,
    assetName: String
): String? {
    val file = File(context.filesDir, assetName)
    if (file.exists() && file.length() > 0) {
        return file.absolutePath
    }
    context.assets.open(assetName).use { `is` ->
        FileOutputStream(file).use { os ->
            val buffer = ByteArray(4 * 1024)
            var read: Int
            while (`is`.read(buffer).also { read = it } != -1) {
                os.write(buffer, 0, read)
            }
            os.flush()
        }
        return file.absolutePath
    }
}