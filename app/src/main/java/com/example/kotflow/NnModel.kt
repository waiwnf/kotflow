package com.example.kotflow

import android.graphics.Bitmap

interface NnModel {
    val inWidth: Int
    val inHeight: Int
    fun inferBitmap(bmp: Bitmap): Unit
}