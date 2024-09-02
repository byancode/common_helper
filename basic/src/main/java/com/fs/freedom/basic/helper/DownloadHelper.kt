package com.fs.freedom.basic.helper

import com.fs.freedom.basic.constant.CommonConstant
import com.fs.freedom.basic.listener.CommonResultListener
import com.fs.freedom.basic.model.DownloadingFileModel
import com.fs.freedom.basic.util.LogUtil
import okhttp3.*
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object DownloadHelper {

    private val mDownloadingList = mutableListOf<DownloadingFileModel>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun downloadFile(
        fileUrl: String?,
        filePath: String?,
        fileName: String?,
        isDeleteOriginalFile: Boolean = true,
        commonResultListener: CommonResultListener<File>
    ) {
        if (fileUrl.isNullOrEmpty()) {
            commonResultListener.onError("downloadApk: download failed, apk url is empty!")
            return
        }
        if (filePath.isNullOrEmpty() || fileName.isNullOrEmpty()) {
            commonResultListener.onError("downloadApk: download failed, filePath or fileName is empty!")
            return
        }

        val downloadingFileModel =
            DownloadingFileModel(fileUrl = fileUrl, fileName = fileName, filePath = filePath)
        if (mDownloadingList.contains(downloadingFileModel)) {
            commonResultListener.onError(CommonConstant.ERROR_SAME_FILE_DOWNLOADED)
            return
        }
        mDownloadingList.add(downloadingFileModel)
        if (isDeleteOriginalFile) {
            val originalFile = File("$filePath/$fileName")
            if (originalFile.exists()) {
                originalFile.delete()
            }
        }

        val request = Request.Builder()
            .url(fileUrl)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mDownloadingList.remove(downloadingFileModel)
                commonResultListener.onError(e.message ?: "Download failed!")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    mDownloadingList.remove(downloadingFileModel)
                    commonResultListener.onError("Download failed with code ${response.code}")
                    return
                }

                val file = File("$filePath/$fileName")
                response.body?.byteStream()?.use { inputStream ->
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                mDownloadingList.remove(downloadingFileModel)
                if (file.exists()) {
                    commonResultListener.onSuccess(file)
                } else {
                    commonResultListener.onError("Download failed, file does not exist")
                }
            }
        })
    }

    fun cancelDownload(tag: String) {
        if (tag.isEmpty()) {
            LogUtil.logE("cancelDownload: tag is empty!")
            return
        }

        // OkHttp does not provide a direct way to cancel a request by tag.
        // A workaround is to keep track of the call objects and cancel them manually if needed.
        // This feature is simpler in OkHttp by using interceptors or storing the Call objects.
        
        // Example pseudo-code for manual management (to be adapted according to your architecture):
        val calls = client.dispatcher.queuedCalls().filter { it.request().tag() == tag }
        calls.forEach { it.cancel() }
        val runningCalls = client.dispatcher.runningCalls().filter { it.request().tag() == tag }
        runningCalls.forEach { it.cancel() }

        val find = mDownloadingList.find { "${it.filePath}/${it.fileName}" == tag }
        if (find != null) {
            mDownloadingList.remove(find)
        } else {
            LogUtil.logE("cancelDownload: current tag $tag, is already canceled")
        }
    }
}
