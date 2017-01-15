package com.xhq.dloaddemo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.File;

/**
 * Created by XHQ on 2017/1/15.
 */

public class DownloadService extends Service{
    private DownloadTask downloadTask;
    private String downloadUrl;

    private DownloadListener listener = new DownloadListener() {
        @Override
        public void onProgress(int progress) {
            getNotificationManager().notify(1,getNotification("downloading...",progress));
        }

        @Override
        public void onSuccess() {
            downloadTask = null;
            stopForeground(true);
            getNotificationManager().notify(1,getNotification("download success",-1));
            Toast.makeText(DownloadService.this,"download success",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed() {
            downloadTask = null;
            stopForeground(true);
            getNotificationManager().notify(1,getNotification("download failed",-1));
            Toast.makeText(DownloadService.this,"download failed",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPaused() {
            downloadTask = null;
            Toast.makeText(DownloadService.this,"download paused" ,Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancled() {
            downloadTask = null;
            stopForeground(true);
            Toast.makeText(DownloadService.this,"download canceled",Toast.LENGTH_SHORT).show();
        }
    };

    class DownloadBinder extends Binder{
        public void startDownload(String url){
            if (downloadTask == null){
                downloadUrl = url;
                downloadTask = new DownloadTask(listener);
                downloadTask.execute(downloadUrl);
                startForeground(1,getNotification("downlading...",0));
                Toast.makeText(DownloadService.this,"downloading...",Toast.LENGTH_SHORT).show();
            }
        }

        public void pausedDownload(){
            if (downloadTask == null){
                return;
            }
            downloadTask.pauseDoenload();
        }

        public void cancelDownload(){
            if (downloadTask != null){
                downloadTask.cancelDoenload();
            }else{
                //取消下载将文件删除，并关闭通知
                String finleName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                File file = new File(directory + finleName);
                if (file.exists()){
                    file.delete();
                }
                getNotificationManager().cancel(1);
                stopForeground(true);
                Toast.makeText(DownloadService.this,"downloading canceled...",Toast.LENGTH_SHORT).show();
            }

        }
    }

    private NotificationManager getNotificationManager(){
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }
    private Notification getNotification(String title,int progress){
        Intent intent = new Intent(this,MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent,0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));
        builder.setContentIntent(pendingIntent);
        builder.setContentTitle(title);
        if (progress > 0){
            builder.setContentText(progress + "%");
            builder.setProgress(100,progress,false);
        }
        return builder.build();
    }

    private DownloadBinder mBInder = new DownloadBinder();
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBInder;
    }
}
