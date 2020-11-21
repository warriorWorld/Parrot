package com.harbinger.parrot.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.harbinger.parrot.MainActivity;
import com.harbinger.parrot.R;


public class RecordService extends Service {
    private final String TAG = "RecordService";
    private NotificationCompat.Builder notificationBuilder;
    private RemoteViews remoteViews;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotification(this);
        startForeground(10, notificationBuilder.build());
    }

    private void createNotification(Context context) {
        try {
            notificationBuilder = new NotificationCompat.Builder(context);
            remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_record);
            notificationManager = (NotificationManager) context.getSystemService
                    (context.NOTIFICATION_SERVICE);
            notificationBuilder.setSmallIcon(R.drawable.ic_parrot4);
            notificationBuilder.setContent(remoteViews);
            notificationBuilder.setDefaults(Notification.DEFAULT_SOUND);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel mChannel = new NotificationChannel("parrot_channel", "record", NotificationManager.IMPORTANCE_HIGH);
                notificationManager.createNotificationChannel(mChannel);
                notificationBuilder.setChannelId("parrot_channel");
            }
            notificationManager.notify(10, notificationBuilder.build());

            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            notificationBuilder.setContentIntent(pendingIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateNotification() {
        try {
            //TODO 转起来
            notificationManager.notify(10, notificationBuilder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}