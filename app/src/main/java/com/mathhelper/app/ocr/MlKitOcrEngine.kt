package com.mathhelper.app.ocr

import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ML Kit 中文离线 OCR。
 */
class MlKitOcrEngine : OcrEngine {

    private val recognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    override suspend fun recognize(image: InputImage): String = withContext(Dispatchers.IO) {
        runCatching {
            Tasks.await(recognizer.process(image)).text
        }.getOrDefault("")
    }
}
