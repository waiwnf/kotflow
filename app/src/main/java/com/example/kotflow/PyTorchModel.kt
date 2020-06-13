package com.example.kotflow

import android.content.res.AssetManager
import android.graphics.Bitmap
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.PyTorchAndroid
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils

class PyTorchModel(
    assetManager: AssetManager,
    filePath: String,
    override val inWidth: Int,
    override val inHeight: Int,
    numThreads: Int = -1
) : NnModel {
    private val module: Module = PyTorchAndroid.loadModuleFromAsset(assetManager, filePath)
    var outTensor: Tensor? = null

    init {
        if (numThreads > 0) {
            PyTorchAndroid.setNumThreads(numThreads)
        }
    }

    override fun inferBitmap(bmp: Bitmap): Unit {
        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
            bmp,
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
            TensorImageUtils.TORCHVISION_NORM_STD_RGB
        )
        val ival = IValue.from(inputTensor)
        outTensor = module.forward(ival).toTensor()
    }
}