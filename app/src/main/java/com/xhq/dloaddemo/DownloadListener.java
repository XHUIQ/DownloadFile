package com.xhq.dloaddemo;

/**
 * Created by XHQ on 2017/1/15.
 * 监听下载的回调类
 */

public interface DownloadListener {
    void onProgress(int progress);
    void onSuccess();
    void onFailed();
    void onPaused();
    void onCancled();
}
