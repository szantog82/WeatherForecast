package com.example.szantog.weatherforecast;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;

public class AlarmService extends Service {

    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private final String WAKELOCK_TAG = "wakelock_tag";

    private AlarmManager alarmManager;
    private PendingIntent alarmPendingIntent;
    private BroadcastReceiver alarmBroadcastReceiver;
    private Handler mHandler;
    private final int UPDATE_TIME = 5 * 6 * 1000;
    private Runnable mRunnable;
    private NotificationManager notificationManager;
    private RemoteViews notifView;
    private Notification notification;

    private long alarmTime;

    private final String ALARM_ACTION = "alarm_action";
    private final String CANCEL_ACTION = "cancel_action";
    private final String UPDATE_ACTION = "update_action";

    private final int ALARM_ID = 65387;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction() != null && intent.getAction().equals(CANCEL_ACTION)) {
            wakeLock.release();
            stopSelf();
        } else if (intent.getAction() != null && intent.getAction().equals(UPDATE_ACTION)) {
            notifView.setTextViewText(R.id.notification_text, formatTime(alarmTime - System.currentTimeMillis()));
            notificationManager.notify(ALARM_ID, notification);
        }

        return START_STICKY;
    }

    @SuppressLint("InvalidWakeLockTag")
    @Override
    public void onCreate() {
        super.onCreate();
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG);
        wakeLock.acquire();
        SharedPreferences prefs = getSharedPreferences(MainActivity.SHAREDPREFS, 0);
        alarmTime = System.currentTimeMillis() + prefs.getLong(MainActivity.TIMEFROMNOW_KEY, 0);
        alarmManager = (AlarmManager) getApplication().getSystemService(ALARM_SERVICE);
        final Intent alarmIntent = new Intent(ALARM_ACTION);
        alarmPendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, 0);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime, alarmPendingIntent);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        notifView = new RemoteViews(this.getPackageName(), R.layout.notification_view);
        Intent cancelIntent = new Intent(this, AlarmService.class);
        cancelIntent.setAction(CANCEL_ACTION);
        PendingIntent pendingCancelIntent = PendingIntent.getService(this, 0, cancelIntent, PendingIntent.FLAG_ONE_SHOT);
        notifView.setOnClickPendingIntent(R.id.notification_cancel, pendingCancelIntent);
        Intent updateIntent = new Intent(this, AlarmService.class);
        updateIntent.setAction(UPDATE_ACTION);
        PendingIntent pendingUpdateIntent = PendingIntent.getService(this, 0, updateIntent, 0);
        notifView.setOnClickPendingIntent(R.id.notification_pic, pendingUpdateIntent);

        final Notification.Builder notifBuilder = new Notification.Builder(this);
        notifBuilder.setSmallIcon(R.drawable.alarmclock64x64);
        notifBuilder.setContent(notifView);
        notifBuilder.setAutoCancel(false);
        notifView.setTextViewText(R.id.notification_text, formatTime(alarmTime - System.currentTimeMillis()));
        notification = notifBuilder.build();

        startForeground(ALARM_ID, notification);

        mHandler = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                notifView.setTextViewText(R.id.notification_text, formatTime(alarmTime - System.currentTimeMillis()));
                notificationManager.notify(ALARM_ID, notification);
                mHandler.postDelayed(this, 250000);
            }
        };
        mHandler.postDelayed(mRunnable, 250000);

        alarmBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Intent startServiceIntent = new Intent(getApplicationContext(), AlarmRunOutService.class);
                startServiceIntent.setAction(AlarmRunOutService.ALARMACTIVITY_INTENT_ACTION);
                startServiceIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startService(startServiceIntent);
                stopSelf();
            }
        };
        registerReceiver(alarmBroadcastReceiver, new IntentFilter(ALARM_ACTION));
    }

    private String formatTime(long millis) {
        return String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            wakeLock.release();
        } catch (Exception e) {
            //needed to make sure wakeLock is released
        }
        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }
        unregisterReceiver(alarmBroadcastReceiver);
        alarmManager.cancel(alarmPendingIntent);
        notificationManager.cancel(ALARM_ID);
        Log.e("alarmservice", "destroyed");
    }
}
