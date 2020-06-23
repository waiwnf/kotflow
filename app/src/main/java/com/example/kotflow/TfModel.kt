package com.example.kotflow

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import timber.log.Timber


class TfModel(
    context: Context,
    filePath: String,
    private val imageProcessor: ImageProcessor? = null,
    numThreads: Int = -1,
    backend: Backend = Backend.CPU
) : NnModel {
    enum class Backend { CPU, GPU, NNAPI }

    private val interpreter: Interpreter

    // TODO support multiple inputs/outputs.
    val inImage: TensorImage
    val outBuffer: TensorBuffer

    init {
        val model = FileUtil.loadMappedFile(context, filePath)

        val options = Interpreter.Options()
        options.setNumThreads(numThreads)

        when (backend) {
            Backend.GPU -> {
                val gpuOptions = GpuDelegate.Options()
                gpuOptions.setPrecisionLossAllowed(true)
                gpuOptions.setInferencePreference(
                    GpuDelegate.Options.INFERENCE_PREFERENCE_SUSTAINED_SPEED
                )
                options.addDelegate(GpuDelegate(gpuOptions))
            }
            Backend.NNAPI -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val nnApiDelegate = NnApiDelegate()
                    options.addDelegate(nnApiDelegate)
                } else {
                    Timber.w("NNAPI Tensorflow Lite delegate is not supported on Android API ${Build.VERSION.SDK_INT}.")
                }
            }
            Backend.CPU -> {
            }
        }

        interpreter = Interpreter(model, options)
        require(interpreter.inputTensorCount == 1)
        require(interpreter.outputTensorCount == 1)

        inImage = TensorImage(interpreter.getInputTensor(0).dataType())

        val outTensor = interpreter.getOutputTensor(0)
        outBuffer = TensorBuffer.createFixedSize(outTensor.shape(), outTensor.dataType())
    }

    val inShape: IntArray
        get() = interpreter.getInputTensor(0).shape()

    override val inWidth: Int
        get() = inShape[2]

    override val inHeight: Int
        get() = inShape[1]

    override fun inferBitmap(bmp: Bitmap): Unit {
        inImage.load(bmp)
        imageProcessor?.process(inImage)
        interpreter.run(inImage.buffer, outBuffer.buffer.rewind())
    }
}
