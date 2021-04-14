package com.compressor.filecompressor

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import androidx.loader.content.CursorLoader
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream


class MethodHelper {

    fun browseFiles(activity: Activity) {
        val intent = Intent()
            .setType("*/*")
            .setAction(Intent.ACTION_GET_CONTENT)

        activity.startActivityForResult(
            Intent.createChooser(intent, "Select a file"),
            CompressorActivity.PICK_COMPRESS_FILE
        )
    }

    fun browseZipFile(activity: Activity) {
        val intent = Intent()
            .setType("*/*")
            .setAction(Intent.ACTION_GET_CONTENT)

        activity.startActivityForResult(
            Intent.createChooser(intent, "Select Zip file"),
            CompressorActivity.PICK_ZIP_FILE
        )
    }

    fun browseImages(activity: Activity){
        val intent = Intent()
            .setType("image/*")
            .setAction(Intent.ACTION_GET_CONTENT)

        activity.startActivityForResult(
            Intent.createChooser(intent, "Select a Image"),
            CompressorActivity.PICK_IMAGES
        )
    }
    fun browseVideos(activity: Activity){
        val intent = Intent()
            .setType("video/*")
            .setAction(Intent.ACTION_GET_CONTENT)

        activity.startActivityForResult(
            Intent.createChooser(intent, "Select a Video"),
            CompressorActivity.PICK_VIDEOS)
    }

    fun browseGzipFiles(activity: Activity) {
        val fileintent = Intent(Intent.ACTION_GET_CONTENT)
        fileintent.type = "resource/folder"
        try {
            activity.startActivityForResult(fileintent, CompressorActivity.PICK_DECOMPRESS_FILE)
        } catch (e: ActivityNotFoundException) {
            Log.e(
                CompressorActivity.TAG,
                "No activity can handle picking a file. Showing alternatives."
            )
        }
    }

    fun compressGzipFile(file: String, gzipFile: String) {
        try {
            val fis = FileInputStream(file)
            val fos = FileOutputStream(gzipFile)
            val gzipOS = GZIPOutputStream(fos)
            val buffer = ByteArray(1024)
            var len: Int
            while (fis.read(buffer).also { len = it } != -1) {
                gzipOS.write(buffer, 0, len)
            }
            //close resources
            gzipOS.close()
            fos.close()
            fis.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun decompressGzipFile(gzipFile: String, newFile: String) {
        try {
            val fis = FileInputStream(gzipFile)
            val gis = GZIPInputStream(fis)
            val fos = FileOutputStream(newFile)
            val buffer = ByteArray(1024)
            var len: Int
            while (gis.read(buffer).also { len = it } != -1) {
                fos.write(buffer, 0, len)
            }
            //close resources
            fos.close()
            gis.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun compressImage(inputFilePath: String?, outputFilePath: String?) {
        var scaledBitmap: Bitmap? = null
        val options = BitmapFactory.Options()

//      by setting this field as true, the actual bitmap pixels are not loaded in the memory. Just the bounds are loaded. If
//      you try the use the bitmap here, you will get null.
        options.inJustDecodeBounds = true
        var bmp = BitmapFactory.decodeFile(inputFilePath, options)
        var actualHeight = options.outHeight
        var actualWidth = options.outWidth

//      max Height and width values of the compressed image is taken as 816x612
        val maxHeight = 816.0f
        val maxWidth = 612.0f
        var imgRatio = (actualWidth / actualHeight).toFloat()
        val maxRatio = maxWidth / maxHeight

//      width and height values are set maintaining the aspect ratio of the image
        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            if (imgRatio < maxRatio) {
                imgRatio = maxHeight / actualHeight
                actualWidth = (imgRatio * actualWidth).toInt()
                actualHeight = maxHeight.toInt()
            } else if (imgRatio > maxRatio) {
                imgRatio = maxWidth / actualWidth
                actualHeight = (imgRatio * actualHeight).toInt()
                actualWidth = maxWidth.toInt()
            } else {
                actualHeight = maxHeight.toInt()
                actualWidth = maxWidth.toInt()
            }
        }

//      setting inSampleSize value allows to load a scaled down version of the original image
        options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight)

//      inJustDecodeBounds set to false to load the actual bitmap
        options.inJustDecodeBounds = false

//      this options allow android to claim the bitmap memory if it runs low on memory
        options.inPurgeable = true
        options.inInputShareable = true
        options.inTempStorage = ByteArray(16 * 1024)
        try {
//          load the bitmap from its path
            bmp = BitmapFactory.decodeFile(inputFilePath, options)
        } catch (exception: OutOfMemoryError) {
            exception.printStackTrace()
        }
        try {
            scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888)
        } catch (exception: OutOfMemoryError) {
            exception.printStackTrace()
        }
        val ratioX = actualWidth / options.outWidth.toFloat()
        val ratioY = actualHeight / options.outHeight.toFloat()
        val middleX = actualWidth / 2.0f
        val middleY = actualHeight / 2.0f
        val scaleMatrix = Matrix()
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY)
        val canvas = scaledBitmap?.let { Canvas(it) }
        if (canvas != null) {
            canvas.setMatrix(scaleMatrix)
            canvas.drawBitmap(
                bmp,
                middleX - bmp.width / 2,
                middleY - bmp.height / 2,
                Paint(Paint.FILTER_BITMAP_FLAG)
            )
        }


//      check the rotation of the image and display it properly
        val exif: ExifInterface
        try {
            exif = inputFilePath?.let { ExifInterface(it) }!!
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION, 0
            )
            Log.d("EXIF", "Exif: $orientation")
            val matrix = Matrix()
            if (orientation == 6) {
                matrix.postRotate(90F)
                Log.d("EXIF", "Exif: $orientation")
            } else if (orientation == 3) {
                matrix.postRotate(180F)
                Log.d("EXIF", "Exif: $orientation")
            } else if (orientation == 8) {
                matrix.postRotate(270F)
                Log.d("EXIF", "Exif: $orientation")
            }
            scaledBitmap = Bitmap.createBitmap(
                scaledBitmap!!, 0, 0,
                scaledBitmap.width, scaledBitmap.height, matrix,
                true
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
        var out: FileOutputStream? = null

        try {
            out = FileOutputStream(outputFilePath)

//          write the compressed bitmap at the destination specified by filename.
            scaledBitmap!!.compress(Bitmap.CompressFormat.JPEG, 80, out)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val heightRatio = Math.round(height.toFloat() / reqHeight.toFloat())
            val widthRatio = Math.round(width.toFloat() / reqWidth.toFloat())
            inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
        }
        val totalPixels = (width * height).toFloat()
        val totalReqPixelsCap = (reqWidth * reqHeight * 2).toFloat()
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++
        }
        return inSampleSize
    }

    fun getCompressedOutputFile(inputFile: String): File {
        val sd_main = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM
            ), "Gzip"
        )
        if (!sd_main.exists())
            sd_main.mkdirs()

        val outputFile = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM
            ).absolutePath + "/Gzip/" + File(inputFile).name.split(".")[0] + ".gz"
        )
        return outputFile
    }

    fun getDeCompressedOutputFile(inputFile: String): File {
        val sd_main = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM
            ), "Decompressed"
        )
        if (!sd_main.exists())
            sd_main.mkdirs()

        val outputFile = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM
            ).absolutePath + "/Decompressed/" + File(inputFile).name.split(".")[0] + ".jpg"
        )
        return outputFile
    }

    fun getCompressedImageOutputFile(inputFile: String): File {
        val sd_main = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM
            ), "CompressedImages"
        )
        if (!sd_main.exists())
            sd_main.mkdirs()

        val outputFile = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM
            ).absolutePath + "/CompressedImages/" + File(inputFile).name.split(".")[0] + ".jpg"
        )
        return outputFile
    }

    fun getCompressedVideoOutputFile(): File {
        val sd_main = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM
            ), "CompressedVideos"
        )
        if (!sd_main.exists())
            sd_main.mkdirs()

        return sd_main
    }

    fun getZipOutputFile(): File {
        val zipFile = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM
            ), "Zip"
        )
        if (!zipFile.exists())
            zipFile.mkdirs()
        val sdf = SimpleDateFormat("ddMyyyy_hhmmss")
        val currentDate = sdf.format(Date())

        val outputFile = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM
            ).absolutePath + "/Zip/" + currentDate + ".zip"
        )

        return outputFile
    }

    fun getUnZipOutputFile(): File {
        val unZipFile = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM
            ), "UnZip"
        )
        if (!unZipFile.exists())
            unZipFile.mkdirs()

        return unZipFile
    }

    fun getRealPath(context: Context, fileUri: Uri): String? {
        // SDK >= 11 && SDK < 19
        return if (Build.VERSION.SDK_INT < 19) {
            getRealPathFromURIAPI11to18(context, fileUri)
        } else {
            getRealPathFromURIAPI19(context, fileUri)
        }// SDK > 19 (Android 4.4) and up
    }

    @SuppressLint("NewApi")
    fun getRealPathFromURIAPI11to18(context: Context, contentUri: Uri): String? {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        var result: String? = null

        val cursorLoader = CursorLoader(context, contentUri, proj, null, null, null)
        val cursor = cursorLoader.loadInBackground()

        if (cursor != null) {
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            result = cursor.getString(columnIndex)
            cursor.close()
        }
        return result
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author Niks
     */
    @SuppressLint("NewApi")
    fun getRealPathFromURIAPI19(context: Context, uri: Uri): String? {

        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }
            } else if (isDownloadsDocument(uri)) {
                var cursor: Cursor? = null
                try {
                    cursor = context.contentResolver.query(
                        uri,
                        arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                        null,
                        null,
                        null
                    )
                    cursor!!.moveToNext()
                    val fileName = cursor.getString(0)
                    val path = Environment.getExternalStorageDirectory()
                        .toString() + "/Download/" + fileName
                    if (!TextUtils.isEmpty(path)) {
                        return path
                    }
                } finally {
                    cursor?.close()
                }
                val id = DocumentsContract.getDocumentId(uri)
                if (id.startsWith("raw:")) {
                    return id.replaceFirst("raw:".toRegex(), "")
                }
                val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads"),
                    java.lang.Long.valueOf(id)
                )

                return getDataColumn(context, contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                var contentUri: Uri? = null
                when (type) {
                    "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])

                return getDataColumn(context, contentUri, selection, selectionArgs)
            }// MediaProvider
            // DownloadsProvider
        } else if ("content".equals(uri.scheme!!, ignoreCase = true)) {

            // Return the remote address
            return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(
                context,
                uri,
                null,
                null
            )
        } else if ("file".equals(uri.scheme!!, ignoreCase = true)) {
            return uri.path
        }// File
        // MediaStore (and general)

        return null
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     * @author Niks
     */
    private fun getDataColumn(
        context: Context, uri: Uri?, selection: String?,
        selectionArgs: Array<String>?
    ): String? {

        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)

        try {
            cursor =
                context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }
}