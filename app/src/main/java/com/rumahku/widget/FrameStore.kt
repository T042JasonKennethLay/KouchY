package com.rumahku.widget

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException

object FrameStore {
    const val AUTHORITY = "com.rumahku.widget.frames"
    private const val SCALE = 4
    private val NAME = Regex("""frame_\d+_\d+\.png""")

    fun isValidName(name: String) = NAME.matches(name)

    fun dir(ctx: Context) = File(ctx.filesDir, "frames").apply { mkdirs() }

    fun save(ctx: Context, frames: List<Bitmap>): List<Uri> {
        val folder = dir(ctx)
        val stamp = System.currentTimeMillis()
        val names = frames.mapIndexed { i, frame ->
            val name = "frame_${stamp}_$i.png"
            val tmp = File(folder, "$name.tmp")
            val scaled = SceneRenderer.upscale(frame, SCALE)
            tmp.outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
            scaled.recycle()
            tmp.renameTo(File(folder, name))
            name
        }
        folder.listFiles()?.filter { it.name !in names }?.forEach { it.delete() }
        return names.map { Uri.parse("content://$AUTHORITY/$it") }
    }
}

class FrameProvider : ContentProvider() {
    override fun onCreate() = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val name = uri.lastPathSegment ?: throw FileNotFoundException()
        if (!FrameStore.isValidName(name)) throw FileNotFoundException(name)
        val file = File(FrameStore.dir(requireNotNull(context)), name)
        if (!file.exists()) throw FileNotFoundException(name)
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri) = "image/png"

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
}
