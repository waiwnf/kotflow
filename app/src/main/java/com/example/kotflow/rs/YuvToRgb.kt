package com.example.kotflow.rs

import android.renderscript.*
import android.util.Size

// TODO factor out _outAlloc stuff
class YuvToRgb(
    rs: RenderScript,
    inAlloc: Allocation,
    outElement: Element,
    size: Size,
    usage: Int = Allocation.USAGE_SCRIPT
) : RsTask(rs, outElement, size, usage) {
    private val script = ScriptIntrinsicYuvToRGB.create(rs, outElement)

    init {
        script.setInput(inAlloc)
    }

    override fun run() {
        script.forEach(outAlloc)
    }
}