package com.example.kotflow.rs

import android.graphics.ImageFormat
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.Type
import android.util.Size

class NnInputPipeline(
    rs: RenderScript,
    inSize: Size,
    outSize: Size,
    blurRadius: Float = 0F,
    inAllocUsage: Int = Allocation.USAGE_IO_INPUT or Allocation.USAGE_SCRIPT
) {
    val inAlloc: Allocation = Allocation.createTyped(
        rs, makeYuvType(rs, inSize.width, inSize.height),
        inAllocUsage
    )

    private val toRgb = YuvToRgb(rs, inAlloc, Element.RGBA_8888(rs), inSize)
    private val blur = if (blurRadius > 0) Blur(rs, toRgb.outAlloc, inSize, blurRadius) else null
    private val resize = Resize(rs, blur?.outAlloc ?: toRgb.outAlloc, outSize)

    val outAlloc: Allocation
        get() = resize.outAlloc

    fun run() {
        toRgb.run()
        blur?.run()
        resize.run()
    }
}

fun makeYuvType(rs: RenderScript, width: Int, height: Int): Type =
    Type.Builder(rs, Element.YUV(rs))
        .setX(width)
        .setY(height)
        .setYuvFormat(ImageFormat.YUV_420_888)
        .create()