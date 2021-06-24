package com.androidexam.printshare.utilities;

import android.content.Context;

import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import java.util.concurrent.TimeUnit;

public class BackgroundNotify {
    private final WorkManager manager;

    public BackgroundNotify(Context c){
        manager = WorkManager.getInstance(c);
    }

    public void start(){
        WorkRequest request = new PeriodicWorkRequest.Builder(CheckNotifyWork.class,PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)
                                    .addTag("Notify")
                                    .build();
        manager.enqueue(request);
    }

    public void stop(){
        manager.cancelAllWorkByTag("Notify");
    }

    public void sync(){
        WorkRequest request = new OneTimeWorkRequest.Builder(CheckNotifyWork.class)
                .addTag("Notify")
                .build();
        manager.enqueue(request);

    }
}
