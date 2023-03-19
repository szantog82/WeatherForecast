package com.example.szantog.weatherforecast;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.Calendar;

public class AlarmWidgetProvider extends AppWidgetProvider {

    private SharedPreferences prefs;

    private static final String OPEN = "open";
    private static final String START = "start";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        updateDisplay(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        updateDisplay(context);
        if (intent.getAction().equals(START)) {
            Intent alarmIntent = new Intent(context, AlarmService.class);
            try {
                context.stopService(alarmIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }
            prefs = context.getApplicationContext().getSharedPreferences(MainActivity.SHAREDPREFS, 0);
            String hour = prefs.getString(MainActivity.PREFERREDHOUR_KEY, "");
            String minute = prefs.getString(MainActivity.PREFERREDMINUTE_KEY, "");
            if (!hour.equals("") && !minute.equals("")) {
                String formattedMinute = minute.length() < 2 ? "0" + minute : minute;
                Calendar calendar = Calendar.getInstance();
                int actualHour = calendar.get(Calendar.HOUR_OF_DAY);
                int actualMinute = calendar.get(Calendar.MINUTE);
                Long timeFromNow;
                String timeLeft;
                if (Integer.parseInt(hour) > actualHour || (Integer.parseInt(hour) == actualHour && Integer.parseInt(minute) > actualMinute)) {
                    timeFromNow = (long) (Integer.parseInt(hour) * 3600 + Integer.parseInt(minute) * 60 - actualHour * 3600 - actualMinute * 60) * 1000;
                    timeLeft = WFTools.convertToTime(timeFromNow);
                } else {
                    timeFromNow = (long) (0 + (24 * 3600 * 1000 - (actualHour * 3600 + actualMinute * 60 - Integer.parseInt(hour) * 3600 - Integer.parseInt(minute) * 60) * 1000));
                    timeLeft = WFTools.convertToTime(timeFromNow);
                }
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong(MainActivity.TIMEFROMNOW_KEY, timeFromNow);
                editor.putString(MainActivity.ALARMTIME_KEY, hour + ":" + formattedMinute);
                editor.apply();
                context.startService(alarmIntent);
                Toast.makeText(context, "Ébresztés: " + hour + ":" + formattedMinute + ", hátralévő idő: " + timeLeft, Toast.LENGTH_SHORT).show();
            }
        } else if (intent.getAction().equals(OPEN)) {
            Intent startActivityIntent = new Intent(context, MainActivity.class);
            startActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(startActivityIntent);

        }
    }

    private void updateDisplay(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, AlarmWidgetProvider.class));
        int count = appWidgetIds.length;
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.alarmwidget_layout);
        for (int i = 0; i < count; i++) {
            prefs = context.getApplicationContext().getSharedPreferences(MainActivity.SHAREDPREFS, 0);
            String alarmTime = prefs.getString(MainActivity.ALARMTIME_KEY, null);
            if (alarmTime != null) {
                remoteViews.setTextViewText(R.id.widget_alarmtime, alarmTime);
            }
            remoteViews.setOnClickPendingIntent(R.id.widget_alarm_title, getPendingSelfIntent(context, OPEN));
            remoteViews.setOnClickPendingIntent(R.id.widget_alarm_start, getPendingSelfIntent(context, START));
            appWidgetManager.updateAppWidget(appWidgetIds[i], remoteViews);
        }
    }

    protected PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
