package com.fs.freedom.basic.listener

import android.net.Uri

/**
 * 获取系统铃声列表回调
 */
interface CommonResultListener <T> {

    //正在加载
    fun onLoading() {}

    //加载完成
    fun onHideLoading() {}

    //加载成功，回调map
    fun onSuccess(result: Map<String, Uri>) {}

    //加载成功，回调指定泛型对象
    fun onSuccess(result: T) {}

    //加载成功，回调指定泛型列表
    fun onSuccess(result: List<T>) {}

    //加载进度回调
    fun onProgress(currentProgress: Float) {}

    //回调结果为空
    fun onEmpty() {}

    //加载失败
    fun onError(message: String) {}

}