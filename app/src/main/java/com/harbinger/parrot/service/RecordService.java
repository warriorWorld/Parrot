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
import com.harbinger.parrot.config.RooboServiceConfig;
import com.harbinger.parrot.config.ShareKeys;
import com.harbinger.parrot.event.BatEvent;
import com.harbinger.parrot.utils.FileUtil;
import com.harbinger.parrot.utils.SharedPreferencesUtils;
import com.harbinger.parrot.vad.IVADRecorder;
import com.harbinger.parrot.vad.VADListener;
import com.harbinger.parrot.vad.VadProcesser;
import com.harbinger.parrot.vad.VadRecorder;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import java.io.File;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;


public class RecordService extends Service {
    public final static String SERVICE_PCK_NAME = "com.harbinger.parrot.service.RecordService";
    private final String TAG = "RecordService";
    private NotificationCompat.Builder notificationBuilder;
    private RemoteViews remoteViews;
    private NotificationManager notificationManager;
    private VadRecorder recorder;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotification(this);
        startForeground(10, notificationBuilder.build());
        initRecorder();
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

    private void refreshUI(boolean isSpeaking) {
        try {
            if (isSpeaking) {
                remoteViews.setImageViewResource(R.id.app_icon_iv, R.drawable.ic_listening);
                EventBus.getDefault().post(new BatEvent(BatEvent.BOS));
            } else {
                remoteViews.setImageViewResource(R.id.app_icon_iv, R.drawable.ic_spy);
                EventBus.getDefault().post(new BatEvent(BatEvent.EOS));
            }
            notificationManager.notify(10, notificationBuilder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initRecorder() {
        recorder = new VadRecorder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        recorder.start(new VadProcesser(
                RooboServiceConfig.INSTANCE.getPersistedPcmPath(),
                new Function0<Unit>() {
                    @Override
                    public Unit invoke() {
                        refreshUI(true);
                        return null;
                    }
                }, new Function0<Unit>() {
            @Override
            public Unit invoke() {
                refreshUI(false);
                return null;
            }
        }, new Function1<File, Unit>() {
            @Override
            public Unit invoke(File file) {
                String wavPath = file.getAbsolutePath().replace(".pcm", ".wav");
                FileUtil.savePcmToWav(file,new File(wavPath));
                FileUtil.deleteFile(file);
                return null;
            }
        }));
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
        recorder.stop();
    }
}