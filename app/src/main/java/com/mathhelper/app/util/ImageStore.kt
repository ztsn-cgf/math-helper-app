package com.mathhelper.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageStore {

    /** 把位图存到应用私有目录，返回绝对路径。 */
    fun save(context: Context, bitmap: Bitmap): String {
        val dir = File(context.filesDir, "mistakes").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        return file.absolutePath
    }

    /** 从相册 Uri 读取位图（可能较大，MVP 未做降采样）。 */
    fun load(context: Context, uri: Uri): Bitmap? =
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
}
