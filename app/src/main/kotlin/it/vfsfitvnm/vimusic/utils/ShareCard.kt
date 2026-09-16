package it.vfsfitvnm.vimusic.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

fun Context.shareNowPlayingCard(
    title: String,
    artist: String,
    artwork: Bitmap?
) {
    val width = 1080
    val height = 1080
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(0xFF16171D.toInt())

    artwork?.let {
        val scaled = Bitmap.createScaledBitmap(it, width, width, true)
        canvas.drawBitmap(scaled, 0f, 0f, null)
        canvas.drawColor(0x66000000)
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 56f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(title.take(40), width / 2f, height - 180f, paint)
    paint.textSize = 40f
    paint.alpha = 200
    canvas.drawText(artist.take(40), width / 2f, height - 110f, paint)
    paint.textSize = 32f
    canvas.drawText("MiMusic", width / 2f, height - 50f, paint)

    val file = File(cacheDir, "share-card.png")
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 90, it) }
    val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, "$title — $artist")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
