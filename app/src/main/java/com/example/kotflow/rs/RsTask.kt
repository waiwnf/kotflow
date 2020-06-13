package com.example.kotflow.rs

import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.Type
import android.util.Size

abstract class RsTask(
    rs: RenderScript,
    element: Element,
    outSize: Size,
    usage: Int = Allocation.USAGE_SCRIPT
) {
    val outAlloc: Allocation = Allocation.createTyped(
        rs, Type.createXY(rs, element, outSize.width, outSize.height), usage
    )

    abstract fun run(): Unit
}