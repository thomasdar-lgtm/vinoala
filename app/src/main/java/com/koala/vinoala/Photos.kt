package com.koala.vinoala

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Photo prête à enregistrer : miniature (liste) + grande (fiche), en JPEG base64. */
data class Photo(val thumb: String, val full: String)

object Photos {
    fun newCaptureUri(ctx: Context): Uri {
        val dir = File(ctx.cacheDir, "photos").apply { mkdirs() }
        val f = File(dir, "capture.jpg").apply { delete() }
        return FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)
    }

    suspend fun fromUri(ctx: Context, uri: Uri): Photo? = withContext(Dispatchers.IO) {
        val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
        build(bytes)
    }

    suspend fun fromUrl(url: String): Photo? = withContext(Dispatchers.IO) {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000; c.readTimeout = 15000
        c.setRequestProperty("User-Agent", "Vinoala/1.0 (Android)")
        try {
            if (c.responseCode != 200) null else build(c.inputStream.readBytes())
        } finally {
            c.disconnect()
        }
    }

    private fun build(bytes: ByteArray): Photo? {
        val bmp = decode(bytes, 1280) ?: return null
        return Photo(encode(scale(bmp, 220), 60), encode(scale(bmp, 1024), 72))
    }

    private fun decode(bytes: ByteArray, max: Int): Bitmap? {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        var sample = 1
        while (o.outWidth / (sample * 2) >= max || o.outHeight / (sample * 2) >= max) sample *= 2
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null
        val rot = runCatching {
            when (ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)
        if (rot == 0f) return bmp
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(rot) }, true)
    }

    private fun scale(b: Bitmap, max: Int): Bitmap {
        val big = maxOf(b.width, b.height)
        if (big <= max) return b
        val r = max.toFloat() / big
        return Bitmap.createScaledBitmap(b, (b.width * r).toInt().coerceAtLeast(1), (b.height * r).toInt().coerceAtLeast(1), true)
    }

    private fun encode(b: Bitmap, q: Int): String {
        val out = ByteArrayOutputStream()
        b.compress(Bitmap.CompressFormat.JPEG, q, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    fun toBitmap(b64: String): Bitmap? = runCatching {
        val bytes = Base64.decode(b64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}
