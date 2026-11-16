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
 *  sap theo ten file de thu tu on dinh.
 *
 *  Meo trung nhau (cung 1 anh them 2 lan) se khien ke nhin nhu du 3 con
 *  giong nhau nhung that ra khac loai nen khong bien mat. Vi vay luc them
 *  meo moi phai kiem tra bang bam anh (aHash) va tu choi anh trung. */
object CatStore {
    const val BUNDLED_TYPES = 13

    private const val MANIFEST_URL =
        "https://raw.githubusercontent.com/duyhien135/meo-xep-ke/main/remote_cats.json"

    /** Khoang cach Hamming <= nguong nay thi coi la cung 1 anh. */
    private const val DUPLICATE_THRESHOLD = 5

    enum class ImportResult { ADDED, FAILED, DUPLICATE }

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

    /** Them nhieu anh tu gallery lam meo moi. Tra ve (so con them duoc, so con trung da bo). */
    fun importCats(context: Context, uris: List<Uri>): Pair<Int, Int> {
        val existing = existingHashes(context).toMutableList()
        var added = 0
        var dup = 0
        for (uri in uris) {
            when (importOne(context, uri, existing)) {
                ImportResult.ADDED -> added++
                ImportResult.DUPLICATE -> dup++
                ImportResult.FAILED -> {}
            }
        }
        return added to dup
    }

    private fun importOne(
        context: Context,
        uri: Uri,
        existingHashes: MutableList<Long>,
    ): ImportResult {
        return try {
            val bmp = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            } ?: return ImportResult.FAILED
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
            val hash = averageHash(scaled)
            if (existingHashes.any { hamming(it, hash) <= DUPLICATE_THRESHOLD }) {
                if (scaled != bmp) bmp.recycle()
                return ImportResult.DUPLICATE
            }
            val dir = customDir(context)
            val next = (dir.listFiles()?.size ?: 0) + 1
            val out = File(dir, "custom_%03d.jpg".format(next))
            FileOutputStream(out).use { stream ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            }
            existingHashes.add(hash)
            if (scaled != bmp) bmp.recycle()
            ImportResult.ADDED
        } catch (e: Exception) {
            ImportResult.FAILED
        }
    }

    /** Xoa het meo nguoi choi tu them (khong xoa meo tai tu mang). */
    fun clearCustomCats(context: Context) {
        customDir(context).listFiles()?.forEach { it.delete() }
    }

    /** Xoa cac meo tu them trung voi meo da co (goc, tai ve, hoac tu them truoc do).
     *  Tra ve so con da xoa. */
    fun removeDuplicateCustomCats(context: Context): Int {
        val custom = customCatFiles(context)
        if (custom.isEmpty()) return 0
        return try {
            val kept = hashesOfDrawables(context).toMutableList()
            kept.addAll(remoteCatFiles(context).mapNotNull { hashOfFile(it) })
            var removed = 0
            for (file in custom) {
                val hash = hashOfFile(file) ?: continue
                if (kept.any { hamming(it, hash) <= DUPLICATE_THRESHOLD }) {
                    file.delete()
                    removed++
                } else {
                    kept.add(hash)
                }
            }
            removed
        } catch (e: Exception) {
            0
        }
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

    /** Bam anh trung binh (aHash 8x8): 2 anh giong nhau co bam gan nhau. */
    private fun averageHash(bmp: Bitmap): Long {
        val small = Bitmap.createScaledBitmap(bmp, 8, 8, true)
        val pixels = IntArray(64)
        small.getPixels(pixels, 0, 8, 0, 0, 8, 8)
        val gray = IntArray(64)
        var sum = 0
        for (i in pixels.indices) {
            val p = pixels[i]
            val g = ((p shr 16 and 0xFF) * 3 + (p shr 8 and 0xFF) * 4 + (p and 0xFF) * 2) / 9
            gray[i] = g
            sum += g
        }
        val avg = sum / 64
        var hash = 0L
        for (i in gray.indices) {
            if (gray[i] > avg) hash = hash or (1L shl i)
        }
        if (small != bmp) small.recycle()
        return hash
    }

    private fun hamming(a: Long, b: Long): Int = (a xor b).countOneBits()

    private fun hashOfFile(file: File): Long? {
        val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
        val bmp = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
        return averageHash(bmp).also { bmp.recycle() }
    }

    private fun hashesOfDrawables(context: Context): List<Long> {
        val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
        return CAT_DRAWABLES.mapNotNull { resId ->
            BitmapFactory.decodeResource(context.resources, resId, opts)?.let { bmp ->
                averageHash(bmp).also { bmp.recycle() }
            }
        }
    }

    private fun existingHashes(context: Context): List<Long> {
        val hashes = hashesOfDrawables(context).toMutableList()
        hashes.addAll(customCatFiles(context).mapNotNull { hashOfFile(it) })
        hashes.addAll(remoteCatFiles(context).mapNotNull { hashOfFile(it) })
        return hashes
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
