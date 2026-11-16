package com.example.meoxepke

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** Quan ly meo ngoai 13 con mac dinh: meo nguoi choi tu them (custom_cats)
 *  va meo tai tu mang ve (remote_cats). Loai meo 0..12 la anh mac dinh,
 *  loai 13 tro di la file trong 2 thu muc nay (tu them truoc, tai ve sau),
 *  sap theo ten file de thu tu on dinh. */
object CatStore {
    const val BUNDLED_TYPES = 13

    private const val MANIFEST_URL =
        "https://raw.githubusercontent.com/duyhien135/meo-xep-ke/main/remote_cats.json"

    fun customDir(context: Context): File =
        File(context.filesDir, "custom_cats").apply { mkdirs() }

    fun remoteDir(context: Context): File =
        File(context.filesDir, "remote_cats").apply { mkdirs() }

    fun customCatFiles(context: Context): List<File> =
        customDir(context).listFiles()?.filter { it.isFile }?.sortedBy { it.name } ?: emptyList()

    fun remoteCatFiles(context: Context): List<File> =
        remoteDir(context).listFiles()?.filter { it.isFile }?.sortedBy { it.name } ?: emptyList()

    /** Tat ca meo them: tu them truoc, tai ve sau. */
    fun extraCatFiles(context: Context): List<File> =
        customCatFiles(context) + remoteCatFiles(context)

    fun totalTypes(context: Context): Int = BUNDLED_TYPES + extraCatFiles(context).size

    /** Them 1 anh tu gallery lam meo moi. Tra ve true neu them duoc. */
    fun importCat(context: Context, uri: Uri): Boolean {
        return try {
            val bmp = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            } ?: return false
            val longest = maxOf(bmp.width, bmp.height)
            val scaled = if (longest > 512) {
                val scale = 512f / longest
                Bitmap.createScaledBitmap(
                    bmp,
                    (bmp.width * scale).toInt().coerceAtLeast(1),
                    (bmp.height * scale).toInt().coerceAtLeast(1),
                    true,
                )
            } else {
                bmp
            }
            val dir = customDir(context)
            val next = (dir.listFiles()?.size ?: 0) + 1
            val out = File(dir, "custom_%03d.jpg".format(next))
            FileOutputStream(out).use { stream ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            }
            if (scaled != bmp) bmp.recycle()
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Xoa het meo nguoi choi tu them (khong xoa meo tai tu mang). */
    fun clearCustomCats(context: Context) {
        customDir(context).listFiles()?.forEach { it.delete() }
    }

    /** Tai danh sach meo tren mang roi tai cac meo chua co ve may.
     *  Tra ve so meo moi tai duoc (0 neu khong co mang hoac loi). */
    fun syncRemoteCats(context: Context): Int {
        return try {
            val manifestText = httpGetText(MANIFEST_URL) ?: return 0
            val cats = JSONObject(manifestText).getJSONArray("cats")
            val dir = remoteDir(context)
            var added = 0
            for (i in 0 until cats.length()) {
                val entry = cats.getJSONObject(i)
                val file = File(dir, entry.getString("file"))
                if (file.exists() && file.length() > 0) continue
                val data = httpGetBytes(entry.getString("url")) ?: continue
                // Chi giu anh doc duoc.
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(data, 0, data.size, opts)
                if (opts.outWidth <= 0 || opts.outHeight <= 0) continue
                FileOutputStream(file).use { it.write(data) }
                added++
            }
            added
        } catch (e: Exception) {
            0
        }
    }

    private fun httpGetText(url: String): String? {
        val bytes = httpGetBytes(url) ?: return null
        return String(bytes, Charsets.UTF_8)
    }

    private fun httpGetBytes(url: String): ByteArray? {
        var connection: HttpURLConnection? = null
        return try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("User-Agent", "dung-iu-hien/1.7")
            connection.instanceFollowRedirects = true
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            connection.inputStream.use { it.readBytes() }
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}
