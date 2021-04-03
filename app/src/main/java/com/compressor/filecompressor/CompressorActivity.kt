package com.compressor.filecompressor

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.compressor.filecompressor.databinding.ActivityCompressorBinding
import kotlinx.coroutines.*
import java.io.File


class CompressorActivity : AppCompatActivity() {

    private lateinit var managePermissions: ManagePermissions
    private lateinit var mBinding: ActivityCompressorBinding
    private val mJob = Job()
    private val mCoroutineScope = CoroutineScope(Dispatchers.Main + mJob)
    private val mMethodHelper: MethodHelper by lazy { MethodHelper() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = DataBindingUtil.setContentView<ActivityCompressorBinding>(
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
                        val outputFile = inputFile?.let { mMethodHelper.getCompressedOutputFile(it) }

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
                        val outputFile = inputFile?.let { mMethodHelper.getDeCompressedOutputFile(it) }

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
                        val outputFile = inputFile?.let { mMethodHelper.getCompressedImageOutputFile(it) }

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
                    mMethodHelper.browseFiles(this)
                }
                return
            }
        }
    }

    companion object {
        // Request code for selecting files and security permissions
        const val PICK_COMPRESS_FILE = 111
        const val PICK_DECOMPRESS_FILE = 222
        const val PICK_IMAGES = 333
        const val PERMISSIONS_REQUEST_CODE = 444

        const val TAG = "CompressActivity"
    }
}