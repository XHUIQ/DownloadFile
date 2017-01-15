package com.xhq.dloaddemo;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by XHQ on 2017/1/15.
 */

public class DownloadTask extends AsyncTask<String,Integer,Integer>{
    public static final int TYPE_SUCCESS = 0;
    public static final int TYPE_FAILED = 1;
    public static final int TYPE_PAUSED = 2;
    public static final int TYPE_CANCELED = 3;
    private DownloadListener listener;
    private boolean isCanceled = false;
    private boolean isPausesd = false;
    private int lastProgress;

    public DownloadTask(DownloadListener listener){
        this.listener = listener;
    }

    @Override
    protected Integer doInBackground(String... strings) {
        InputStream is = null;
        RandomAccessFile savedFile = null;
        File file = null;

        try{
            long downloadedLengh = 0;//记录下载文件的长度
            String downloadUrl = strings[0];
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            String directory = Environment.getExternalStoragePublicDirectory
                    (Environment.DIRECTORY_DOWNLOADS).getPath();
            file = new File(directory + fileName);
            if (file.exists()){
                downloadedLengh = file.length();
            }
            long contentLengh = getContentLengh(downloadUrl);//获取文件的总大小
            if (contentLengh == 0){
                return TYPE_FAILED;
            }else if(contentLengh == downloadedLengh){
                return TYPE_SUCCESS;
            }

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .addHeader("RANGE","bytes=" + downloadedLengh + "-")//断点下载，指定下载开始位置
                    .url(downloadUrl)
                    .build();
            Response response = client.newCall(request).execute();
            if (null != response){
                is = response.body().byteStream();
                savedFile = new RandomAccessFile(file,"rw");
                savedFile.seek(downloadedLengh);
                byte[] b = new byte[1024];
                int  total = 0;
                int len;
                while ((len = is.read()) != -1){
                    if (isCanceled){
                        return  TYPE_CANCELED;
                    }else if (isPausesd){
                        return TYPE_PAUSED;
                    }else{
                        total += len;
                        savedFile.write(b,0,len);
                        //计算下载的百分比
                        int progress = (int)((total + downloadedLengh)*100/contentLengh);
                        publishProgress(progress);
                    }
                }
                response.body().close();
                return TYPE_SUCCESS;
            }

        }catch (Exception e){
            e.printStackTrace();;
        }finally {
            try {
                if (is != null){
                    is.close();
                }
                if (savedFile != null){
                    savedFile.close();
                }
                if (isCanceled && file != null){
                    file.delete();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return TYPE_FAILED;
    }

    private long getContentLengh(String downloadUrl) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        Response response = client.newCall(request).execute();
        if (response != null && response.isSuccessful()) {
            long contentLengh = response.body().contentLength();
            response.close();
            return contentLengh;
        }
        return 0;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress = values[0];
        if (progress > lastProgress){
            listener.onProgress(progress);
            lastProgress = progress;
        }
    }

    @Override
    protected void onPostExecute(Integer integer) {
        switch (integer){
            case TYPE_CANCELED:
                listener.onCancled();
                break;
            case TYPE_FAILED:
                listener.onFailed();
                break;
            case TYPE_PAUSED:
                listener.onPaused();
                break;
            case TYPE_SUCCESS:
                listener.onSuccess();
                break;
        }
    }

    public void pauseDoenload(){
        isPausesd = true;
    }

    public void cancelDoenload(){
        isCanceled = true;
    }
}
