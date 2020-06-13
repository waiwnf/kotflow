package com.example.kotflow.rs

import android.renderscript.*
import android.util.Size

class Blur(
    rs: RenderScript,
    inAlloc: Allocation,
    size: Size,
    blurRadius: Float,
    usage: Int = Allocation.USAGE_SCRIPT
): RsTask(rs, inAlloc.element, size, usage) {
    private var script = ScriptIntrinsicBlur.create(rs, inAlloc.element)

    init {
        script.setInput(inAlloc)
        require(blurRadius > 0)
        script.setRadius(blurRadius)
    }

    override fun run() {
        script.forEach(outAlloc)
    }
}