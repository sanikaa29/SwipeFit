package com.swipefit.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.ByteArrayOutputStream

object ImageUtils {
    private const val MAX_SIDE = 1280
    private const val JPEG_QUALITY = 85

    /**
     * Decodes the picked image (ImageDecoder applies EXIF rotation), shrinks it so the longest side is
     * at most 1280 px, and returns JPEG bytes (typically a few hundred KB). Call from a background thread.
     */
    fun compressToJpeg(resolver: ContentResolver, uri: Uri): ByteArray {
        val bitmap = try {
            val source = ImageDecoder.createSource(resolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val w = info.size.width
                val h = info.size.height
                val scale = MAX_SIDE.toFloat() / maxOf(w, h)
                if (scale < 1f) {
                    decoder.setTargetSize(
                        (w * scale).toInt().coerceAtLeast(1),
                        (h * scale).toInt().coerceAtLeast(1)
                    )
                }
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Couldn't read that image. Try a different photo.")
        }
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }
}
