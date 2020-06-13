package com.example.kotflow.rs

import android.renderscript.*
import android.util.Size

class Resize(
    private val rs: RenderScript,
    private val inAlloc: Allocation,
    destSize: Size,
    usage: Int = Allocation.USAGE_SCRIPT
): RsTask(rs, inAlloc.element, destSize, usage) {
    private var script = ScriptIntrinsicResize.create(rs)

    init {
        script.setInput(inAlloc)
    }

    override fun run() {
        script.forEach_bicubic(outAlloc)
    }
}