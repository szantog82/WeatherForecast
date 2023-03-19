package com.example.szantog.weatherforecast;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYou;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class WidgetProvider extends AppWidgetProvider implements DownloadHandler.TaskListener {

    private DatabaseHandler databaseHandler;
    private Context receiveContext;
    private SharedPreferences prefs;
    private SharedPreferences.Editor prefsEditor;
    public static final String KEY = "key";
    public static final String CLICK = "ClickCount";
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMMM dd. HH:mm");
    private SharedPreferences sharedPrefs;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        prefs = context.getApplicationContext().getSharedPreferences(KEY, 0);

        databaseHandler = new DatabaseHandler(context.getApplicationContext());

        onUpdateViews(context);
    }

    protected PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    @Override
    public void onReceive(final Context context, final Intent intent) {
        super.onReceive(context, intent);
        receiveContext = context;
        prefs = context.getApplicationContext().getSharedPreferences(KEY, 0);
        int clickCount = prefs.getInt(CLICK, 0);
        prefsEditor = prefs.edit();
        prefsEditor.putInt(CLICK, clickCount + 1);

        if (intent.getAction().equals("click")) {
            if (prefs.getInt(CLICK, 0) < 1) {
                Runnable mRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (prefs.getInt(CLICK, 0) == 1) {
                            DownloadHandler handler = new DownloadHandler(context, WidgetProvider.this, null);
                            handler.execute("");
                        } else if (prefs.getInt(CLICK, 0) == 2) {
                            Intent activityIntent = new Intent(context, MainActivity.class);
                            activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(activityIntent);
                        }
                        prefsEditor = prefs.edit();
                        prefsEditor.putInt(CLICK, 0);
                        prefsEditor.commit();
                    }
                };
                Handler handler = new Handler();
                handler.postDelayed(mRunnable, 250);
            }
            clickCount = prefs.getInt(CLICK, 0);
            prefsEditor = prefs.edit();
            prefsEditor.putInt(CLICK, clickCount + 1);
            prefsEditor.commit();
        }
    }

    private void onUpdateViews(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, WidgetProvider.class));
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        final int count = appWidgetIds.length;
        for (int i = 0; i < count; i++) {
            updateForecasts(context, remoteViews, appWidgetIds, databaseHandler.getAllForecastData());
            remoteViews.setOnClickPendingIntent(R.id.widget_root_layout, getPendingSelfIntent(context, "click"));
            sharedPrefs = context.getSharedPreferences(MainActivity.SHAREDPREFS, 0);
            long lastUpdated = Long.parseLong(sharedPrefs.getString(MainActivity.PREF_KEY, "0"));
            remoteViews.setTextViewText(R.id.widget_timeupdated_text, simpleDateFormat.format(new Date(lastUpdated)));
            remoteViews.setTextViewText(R.id.widget_city_name, sharedPrefs.getString(MainActivity.CITYSEARCH_KEY, ""));
            appWidgetManager.updateAppWidget(appWidgetIds[i], remoteViews);
        }
    }


    private void updateForecasts(Context context, RemoteViews remoteViews,
                                 int[] appWidgetIds, ArrayList<WeatherItem> items) {
        if (items.size() > 0) {
            try {
                String[] splits = items.get(0).getDate().split(" ");
                if (splits[2].substring(0, 1).toLowerCase().equals("s") || splits[2].substring(0, 1).toLowerCase().equals("c")) {
                    remoteViews.setTextViewText(R.id.widget_date1, splits[2].substring(0, 3));
                } else {
                    remoteViews.setTextViewText(R.id.widget_date1, splits[2].substring(0, 1));
                }
            } catch (Exception e) {
                remoteViews.setTextViewText(R.id.widget_date1, items.get(0).getDate());
            }
            try {
               // GlideToVectorYou.justLoadImage(this, Uri.parse(items.get(0).getIcon()), R.id.widget_image1);
                Picasso.get().load(items.get(0).getIcon()).into(remoteViews, R.id.widget_image1, appWidgetIds);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (items.get(0).getMinTemp() == -999) {
                remoteViews.setTextViewText(R.id.widget_temp1, String.valueOf(items.get(0).getMaxTemp()) + "°C");
            } else {
                remoteViews.setTextViewText(R.id.widget_temp1, String.valueOf(items.get(0).getMaxTemp()) + "/" + String.valueOf(items.get(0).getMinTemp()) + "°C");
            }
        }

        if (items.size() > 1) {
            try {
                String[] splits = items.get(1).getDate().split(" ");
                if (splits[2].substring(0, 1).toLowerCase().equals("s") || splits[2].substring(0, 1).toLowerCase().equals("c")) {
                    remoteViews.setTextViewText(R.id.widget_date2, splits[2].substring(0, 3));
                } else {
                    remoteViews.setTextViewText(R.id.widget_date2, splits[2].substring(0, 1));
                }
            } catch (Exception e) {
                remoteViews.setTextViewText(R.id.widget_date2, items.get(1).getDate());
            }
            try {
                Picasso.get().load(items.get(1).getIcon()).into(remoteViews, R.id.widget_image2, appWidgetIds);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (items.get(1).getMinTemp() == -999) {
                remoteViews.setTextViewText(R.id.widget_temp2, String.valueOf(items.get(1).getMaxTemp()) + "°C");
            } else {
                remoteViews.setTextViewText(R.id.widget_temp2, String.valueOf(items.get(1).getMaxTemp()) + "/" + String.valueOf(items.get(1).getMinTemp()) + "°C");
            }
        }

        if (items.size() > 2) {
            try {
                String[] splits = items.get(2).getDate().split(" ");
                if (splits[2].substring(0, 1).toLowerCase().equals("s") || splits[2].substring(0, 1).toLowerCase().equals("c")) {
                    remoteViews.setTextViewText(R.id.widget_date3, splits[2].substring(0, 3));
                } else {
                    remoteViews.setTextViewText(R.id.widget_date3, splits[2].substring(0, 1));
                }
            } catch (Exception e) {
                remoteViews.setTextViewText(R.id.widget_date3, items.get(2).getDate());
            }
            try {
                Picasso.get().load(items.get(2).getIcon()).into(remoteViews, R.id.widget_image3, appWidgetIds);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (items.get(2).getMinTemp() == -999) {
                remoteViews.setTextViewText(R.id.widget_temp3, String.valueOf(items.get(2).getMaxTemp()) + "°C");
            } else {
                remoteViews.setTextViewText(R.id.widget_temp3, String.valueOf(items.get(2).getMaxTemp()) + "/" + String.valueOf(items.get(2).getMinTemp()) + "°C");
            }
        }

        if (items.size() > 3) {
            try {
                String[] splits = items.get(3).getDate().split(" ");
                if (splits[2].substring(0, 1).toLowerCase().equals("s") || splits[2].substring(0, 1).toLowerCase().equals("c")) {
                    remoteViews.setTextViewText(R.id.widget_date4, splits[2].substring(0, 3));
                } else {
                    remoteViews.setTextViewText(R.id.widget_date4, splits[2].substring(0, 1));
                }
            } catch (Exception e) {
                remoteViews.setTextViewText(R.id.widget_date4, items.get(3).getDate());
            }
            try {
                Picasso.get().load(items.get(3).getIcon()).into(remoteViews, R.id.widget_image4, appWidgetIds);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (items.get(3).getMinTemp() == -999) {
                remoteViews.setTextViewText(R.id.widget_temp4, String.valueOf(items.get(3).getMaxTemp()) + "°C");
            } else {
                remoteViews.setTextViewText(R.id.widget_temp4, String.valueOf(items.get(3).getMaxTemp()) + "/" + String.valueOf(items.get(3).getMinTemp()) + "°C");
            }
        }

        if (items.size() > 4) {
            try {
                String[] splits = items.get(4).getDate().split(" ");
                if (splits[2].substring(0, 1).toLowerCase().equals("s") || splits[2].substring(0, 1).toLowerCase().equals("c")) {
                    remoteViews.setTextViewText(R.id.widget_date5, splits[2].substring(0, 3));
                } else {
                    remoteViews.setTextViewText(R.id.widget_date5, splits[2].substring(0, 1));
                }
            } catch (Exception e) {
                remoteViews.setTextViewText(R.id.widget_date5, items.get(4).getDate());
            }
            try {
                Picasso.get().load(items.get(4).getIcon()).into(remoteViews, R.id.widget_image5, appWidgetIds);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (items.get(4).getMinTemp() == -999) {
                remoteViews.setTextViewText(R.id.widget_temp5, String.valueOf(items.get(4).getMaxTemp()) + "°C");
            } else {
                remoteViews.setTextViewText(R.id.widget_temp5, String.valueOf(items.get(4).getMaxTemp()) + "/" + String.valueOf(items.get(4).getMinTemp()) + "°C");
            }
        }

        if (items.size() > 5) {
            try {
                String[] splits = items.get(5).getDate().split(" ");
                if (splits[2].substring(0, 1).toLowerCase().equals("s") || splits[2].substring(0, 1).toLowerCase().equals("c")) {
                    remoteViews.setTextViewText(R.id.widget_date6, splits[2].substring(0, 3));
                } else {
                    remoteViews.setTextViewText(R.id.widget_date6, splits[2].substring(0, 1));
                }
            } catch (Exception e) {
                remoteViews.setTextViewText(R.id.widget_date6, items.get(5).getDate());
            }
            try {
                Picasso.get().load(items.get(5).getIcon()).into(remoteViews, R.id.widget_image6, appWidgetIds);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (items.get(5).getMinTemp() == -999) {
                remoteViews.setTextViewText(R.id.widget_temp6, String.valueOf(items.get(5).getMaxTemp()) + "°C");
            } else {
                remoteViews.setTextViewText(R.id.widget_temp6, String.valueOf(items.get(5).getMaxTemp()) + "/" + String.valueOf(items.get(5).getMinTemp()) + "°C");
            }
        }
    }

    @Override
    public void OnCompletition(Boolean successfulDownload, ArrayList<WeatherItem> tomorrowList, ArrayList<WeatherItem> forecastList, Context context, String actualTemp, String actualTempIcon, String backgroundImageLink) {
        databaseHandler = new DatabaseHandler(receiveContext);

        if (successfulDownload) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            remoteViews.setTextViewText(R.id.widget_timeupdated_text, simpleDateFormat.format(new Date(System.currentTimeMillis())));

            sharedPrefs = receiveContext.getSharedPreferences(MainActivity.SHAREDPREFS, 0);
            SharedPreferences.Editor sharedEditor = sharedPrefs.edit();
            sharedEditor.putString(MainActivity.PREF_KEY, String.valueOf(System.currentTimeMillis()));
            sharedEditor.putString(MainActivity.ACTUALTEMP_KEY, actualTemp);
            sharedEditor.putString(MainActivity.ACTUALTEMPICON_KEY, actualTempIcon);
            sharedEditor.putString(MainActivity.ACTUALBACKGROUND_KEY, backgroundImageLink);
            sharedEditor.apply();
            databaseHandler.replaceTomorrowData(tomorrowList);
            databaseHandler.replaceForecastData(forecastList);

            onUpdateViews(context);
            Toast.makeText(receiveContext, "Adatok frissítve", Toast.LENGTH_SHORT).show();

        } else {
            onUpdateViews(context);
            Toast.makeText(receiveContext, "Adatok frissítése sikertelen", Toast.LENGTH_SHORT).show();
        }
    }
}

