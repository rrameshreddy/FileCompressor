package com.compressor.filecompressor.videocompressor

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread

/**
 * Created by Ramesh Reddy
 */
interface CompressionListener {
    @MainThread
    fun onStart()

    @MainThread
    fun onSuccess()

    @MainThread
    fun onFailure(failureMessage: String)

    @WorkerThread
    fun onProgress(percent: Float)

    @WorkerThread
    fun onCancelled()
}

interface CompressionProgressListener {
    fun onProgressChanged(percent: Float)
    fun onProgressCancelled()
}