package com.compressor.filecompressor

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.compressor.filecompressor.databinding.ActivityCompressorBinding
import com.compressor.filecompressor.videocompressor.CompressionListener
import com.compressor.filecompressor.videocompressor.VideoCompressor
import com.compressor.filecompressor.videocompressor.VideoQuality
import com.github.angads25.filepicker.model.DialogConfigs
import com.github.angads25.filepicker.model.DialogProperties
import com.github.angads25.filepicker.view.FilePickerDialog
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException


class CompressorActivity : AppCompatActivity() {

    private lateinit var managePermissions: ManagePermissions
    private lateinit var mBinding: ActivityCompressorBinding
    private val mJob = Job()
    private val mCoroutineScope = CoroutineScope(Dispatchers.Main + mJob)
    private val mMethodHelper: MethodHelper by lazy { MethodHelper() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_compressor
        )

        // Initialize a list of required permissions to request runtime
        val list = listOf<String>(
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        // Initialize a new instance of ManagePermissions class
        managePermissions = ManagePermissions(this, list, PERMISSIONS_REQUEST_CODE)

        mBinding.compressImage.setOnClickListener(View.OnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (managePermissions.isPermissionsGranted() == PackageManager.PERMISSION_GRANTED) {
                    mMethodHelper.browseImages(this)
                } else {
                    managePermissions.checkPermissions()
                }

            } else {
                mMethodHelper.browseImages(this)
            }
        })
        mBinding.compressVideo.setOnClickListener(View.OnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (managePermissions.isPermissionsGranted() == PackageManager.PERMISSION_GRANTED) {
                    mMethodHelper.browseVideos(this)
                } else {
                    managePermissions.checkPermissions()
                }

            } else {
                mMethodHelper.browseVideos(this)
            }
        })

        mBinding.compressZip.setOnClickListener(View.OnClickListener {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (managePermissions.isPermissionsGranted() == PackageManager.PERMISSION_GRANTED) {
                    filePicker()
                } else {
                    managePermissions.checkPermissions()
                }

            } else {
                filePicker()
            }
        })
        mBinding.compressUnzip.setOnClickListener(View.OnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (managePermissions.isPermissionsGranted() == PackageManager.PERMISSION_GRANTED) {
                    mMethodHelper.browseZipFile(this)
                } else {
                    managePermissions.checkPermissions()
                }

            } else {
                mMethodHelper.browseZipFile(this)
            }
        })
        mBinding.gzipCompress.setOnClickListener(View.OnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (managePermissions.isPermissionsGranted() == PackageManager.PERMISSION_GRANTED) {
                    mMethodHelper.browseFiles(this)
                } else {
                    managePermissions.checkPermissions()
                }

            } else {
                mMethodHelper.browseFiles(this)
            }
        })
        mBinding.gzipDecompress.setOnClickListener(View.OnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (managePermissions.isPermissionsGranted() == PackageManager.PERMISSION_GRANTED) {
                    mMethodHelper.browseGzipFiles(this)
                } else {
                    managePermissions.checkPermissions()
                }

            } else {
                mMethodHelper.browseGzipFiles(this)
            }
        })

    }

    override fun onDestroy() {
        super.onDestroy()
        mJob.cancel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {

                PICK_COMPRESS_FILE -> {
                    // The result data contains a URI for the document or directory that
                    // the user selected.
                    resultData?.data?.also { uri ->
                        // Perform operations on the document using its URI.
                        val inputFile = mMethodHelper.getRealPath(this, uri)
                        val outputFile =
                            inputFile?.let { mMethodHelper.getCompressedOutputFile(it) }

                        mBinding.inputFile.text =
                            "Input File: $inputFile : ${(File(inputFile).length() / 1024).toDouble() / 1024} MB"
                        if (inputFile != null && outputFile != null) {
                            mCoroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    mMethodHelper.compressGzipFile(
                                        inputFile,
                                        outputFile.absoluteFile.path
                                    )
                                }
                                withContext(Dispatchers.Main) {
                                    mBinding.outputFile.text =
                                        "Output File: ${outputFile.absolutePath} : ${(outputFile.length() / 1024).toDouble() / 1024} MB"
                                }
                            }

                        }

                    }
                }
                PICK_DECOMPRESS_FILE -> {
                    // The result data contains a URI for the document or directory that
                    // the user selected.
                    resultData?.data?.also { uri ->
                        // Perform operations on the document using its URI.
                        val inputFile = mMethodHelper.getRealPath(this, uri)
                        val outputFile =
                            inputFile?.let { mMethodHelper.getDeCompressedOutputFile(it) }

                        mBinding.inputFile.text =
                            "Input File: $inputFile : ${(File(inputFile).length() / 1024).toDouble() / 1024} MB"
                        if (inputFile != null && outputFile != null) {
                            mCoroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    mMethodHelper.compressGzipFile(
                                        inputFile,
                                        outputFile.absoluteFile.path
                                    )
                                }
                                withContext(Dispatchers.Main) {
                                    mBinding.outputFile.text =
                                        "Output File: ${outputFile.absolutePath} : ${(outputFile.length() / 1024).toDouble() / 1024} MB"
                                }
                            }

                        }

                    }
                }

                PICK_IMAGES -> {
                    // The result data contains a URI for the document or directory that
                    // the user selected.
                    resultData?.data?.also { uri ->
                        // Perform operations on the document using its URI.
                        val inputFile = mMethodHelper.getRealPath(this, uri)
                        val outputFile =
                            inputFile?.let { mMethodHelper.getCompressedImageOutputFile(it) }

                        mBinding.inputFile.text =
                            "Input File: $inputFile : ${(File(inputFile).length() / 1024).toDouble() / 1024} MB"
                        if (inputFile != null && outputFile != null) {
                            mCoroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    mMethodHelper.compressImage(
                                        inputFile,
                                        outputFile.absoluteFile.path
                                    )
                                }
                                withContext(Dispatchers.Main) {
                                    mBinding.outputFile.text =
                                        "Output File: ${outputFile.absolutePath} : ${(outputFile.length() / 1024).toDouble() / 1024} MB"
                                }
                            }

                        }
                    }
                }

                PICK_VIDEOS -> {
                    // The result data contains a URI for the document or directory that
                    // the user selected.
                    resultData?.data?.also { uri ->
                        processVideo(uri)
                    }
                }
                PICK_ZIP_FILE -> {
                    resultData?.data?.also { uri ->
                        val inputFile = mMethodHelper.getRealPath(this, uri)
                        val outputFile =mMethodHelper.getUnZipOutputFile()

                        ZipManager.unzip(inputFile,outputFile.absolutePath)
                    }
                }
            }
        }
    }

    // Receive the permissions request result
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                val isPermissionsGranted = managePermissions
                    .processPermissionsResult(grantResults)
                if (isPermissionsGranted) {
                    //mMethodHelper.browseFiles(this)
                }
                return
            }
        }
    }

    private fun processVideo(uri: Uri?) {
        uri?.let {

            val inputFile = mMethodHelper.getRealPath(this, uri)

            mBinding.inputFile.text =
                "Input File: $inputFile : ${(File(inputFile).length() / 1024).toDouble() / 1024} MB"
            mCoroutineScope.launch {
                // run in background as it can take a long time if the video is big,
                // this implementation is not the best way to do it,
                val desFile = saveVideoFile(inputFile)

                desFile?.let {
                    var time = 0L
                    inputFile?.let { it1 ->
                        VideoCompressor.start(
                            it1,
                            desFile.path,
                            object : CompressionListener {
                                override fun onProgress(percent: Float) {

                                }

                                override fun onStart() {

                                }

                                override fun onSuccess() {
                                    mBinding.outputFile.text =
                                        "Output File: ${desFile.absolutePath} : ${(desFile.length() / 1024).toDouble() / 1024} MB"
                                }

                                override fun onFailure(failureMessage: String) {

                                    Log.wtf("failureMessage", failureMessage)
                                }

                                override fun onCancelled() {
                                    Log.wtf("TAG", "compression has been cancelled")
                                    // make UI changes, cleanup, etc
                                }
                            },
                            VideoQuality.MEDIUM,
                            isMinBitRateEnabled = true,
                            keepOriginalResolution = false,
                        )
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun saveVideoFile(filePath: String?): File? {
        filePath?.let {
            val videoFile = File(filePath)
            val videoFileName = "${System.currentTimeMillis()}_${videoFile.name}"
            //val folderName = Environment.DIRECTORY_MOVIES
            val folderName = mMethodHelper.getCompressedVideoOutputFile().absolutePath
            if (Build.VERSION.SDK_INT >= 30) {

                val values = ContentValues().apply {

                    put(
                        MediaStore.Images.Media.DISPLAY_NAME,
                        videoFileName
                    )
                    put(MediaStore.Images.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Images.Media.RELATIVE_PATH, folderName)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val collection =
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

                val fileUri = applicationContext.contentResolver.insert(collection, values)

                fileUri?.let {
                    application.contentResolver.openFileDescriptor(fileUri, "rw")
                        .use { descriptor ->
                            descriptor?.let {
                                FileOutputStream(descriptor.fileDescriptor).use { out ->
                                    FileInputStream(videoFile).use { inputStream ->
                                        val buf = ByteArray(4096)
                                        while (true) {
                                            val sz = inputStream.read(buf)
                                            if (sz <= 0) break
                                            out.write(buf, 0, sz)
                                        }
                                    }
                                }
                            }
                        }

                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    applicationContext.contentResolver.update(fileUri, values, null, null)

                    return File(mMethodHelper.getRealPath(applicationContext, fileUri))
                }
            } else {
                val downloadsPath =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val desFile = File(downloadsPath, videoFileName)

                if (desFile.exists())
                    desFile.delete()

                try {
                    desFile.createNewFile()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                return desFile
            }
        }
        return null
    }

    private fun initFilePicker(): DialogProperties {
        val properties = DialogProperties()

        properties.selection_mode = DialogConfigs.MULTI_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = File(DialogConfigs.DEFAULT_DIR);
        properties.error_dir = File(DialogConfigs.DEFAULT_DIR);
        properties.offset = File(DialogConfigs.DEFAULT_DIR);
        properties.extensions = null;
        return properties
    }

    private fun filePicker(){
        val dialog = FilePickerDialog(this, initFilePicker())
        dialog.setTitle("Select a File")
        dialog.show()
        dialog.setDialogSelectionListener {
            //files is the array of the paths of files selected by the Application User.
            ZipManager.zip(it,mMethodHelper.getZipOutputFile().absolutePath)

        }
    }

    companion object {
        // Request code for selecting files and security permissions
        const val PICK_COMPRESS_FILE = 111
        const val PICK_DECOMPRESS_FILE = 222
        const val PICK_IMAGES = 333
        const val PICK_VIDEOS = 444
        const val PICK_ZIP_FILE = 555
        const val PERMISSIONS_REQUEST_CODE = 666

        const val TAG = "CompressActivity"
    }
}