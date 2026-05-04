package com.voltbody.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.View
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ShareUtils {
    fun shareBitmap(context: Context, bitmap: Bitmap, text: String) {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val stream = FileOutputStream("$cachePath/workout_summary.png")
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val imagePath = File(context.cacheDir, "images")
        val newFile = File(imagePath, "workout_summary.png")
        val contentUri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", newFile)

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setDataAndType(contentUri, context.contentResolver.getType(contentUri))
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_TEXT, text)
            type = "image/png"
        }
        context.startActivity(Intent.createChooser(shareIntent, "Compartir resumen de entreno"))
    }

    fun captureBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }
}
