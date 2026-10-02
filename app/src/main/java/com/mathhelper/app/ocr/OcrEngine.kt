package com.mathhelper.app.ocr

import com.google.mlkit.vision.common.InputImage

interface OcrEngine {
    suspend fun recognize(image: InputImage): String
}
