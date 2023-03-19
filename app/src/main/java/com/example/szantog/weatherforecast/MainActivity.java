package com.example.szantog.weatherforecast;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYou;
import com.squareup.picasso.Picasso;
import com.szantog.filechooserdialog.FileChooserDialogBuilder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity implements DownloadHandler.TaskListener, View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;
    private View dialogView;
    private Spinner alertdialog_hour;
    private Spinner alertdialog_minute;
    private SeekBar alertdialog_seekbar;
    private TextView songNameTextView;
    private ImageView songNameTextViewRemover;
    private long timeFromNow;

    private AlertDialog.Builder forecastDialogBuilder;
    private AlertDialog forecastDialog;
    private View forecastView;
    private TextView forecastTextView;

    public static final String TIMEFROMNOW_KEY = "timefromnow_key";
    public static final String PREFERREDHOUR_KEY = "preferredhour_key";
    public static final String PREFERREDMINUTE_KEY = "preferredminute_key";
    public static final String SONGNAME_KEY = "songname_key";
    public static final String ALARMTIME_KEY = "alarmtime_key";
    public static final String ALARMVOLUME_KEY = "alarmvolume_key";

    private AudioManager audioManager;
    private MediaPlayer mediaPlayer;

    private RelativeLayout mainActRelativeLayout;
    private TextView actualTempTextView;
    private ImageView actualIconImageView;
    private TextView actualDayNameTextView;
    private SimpleDateFormat formatDay = new SimpleDateFormat("EEEE", new Locale("hu"));
    private ImageView alarmClockButton;
    private ImageView textualForecastButton;
    private ImageView citySearchButton;
    private ImageView refreshButton;
    private ImageView changeListButton;
    private AlertDialog citySearchDialog;
    private Boolean isForecastShowing = true;
    private ListView listView;
    private ActivityListAdapter listAdapter;
    private ArrayList<WeatherItem> showItems = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshLayout;

    private DatabaseHandler databaseHandler;
    public static final String SHAREDPREFS = "weatherforecast";
    public static final String PREF_KEY = "update_time";
    public static final String CITYSEARCH_KEY = "citysearch_key";
    public static final String CITYLIST_KEY = "citylist_key";
    public static final String TEXTUALFORECAST_KEY = "textualforecast_key";
    public static final String ACTUALTEMP_KEY = "actualtemp_key";
    public static final String ACTUALTEMPICON_KEY = "actualtempicon_key";
    public static final String ACTUALBACKGROUND_KEY = "actualbackground_key";
    private String textualForecast = "(üres)";
    public static final String DIVIDER = "__D__D__D";
    private SharedPreferences sharedPrefs;
    private SharedPreferences.Editor sharedEditor;
    private TextView lastupdated_text;
    private TextView activity_city_name;
    private SimpleDateFormat lastUpdateDateFormat = new SimpleDateFormat("MMMM dd. HH:mm", new Locale("hu"));

    private Toast t;
    private final String BACK_COUNT_KEY = "com.example.szantog.weatherforecast.back_count_key";

    @Override
    public void onBackPressed() {
        if (!isForecastShowing) {
            showItems.clear();
            showItems.addAll(databaseHandler.getAllForecastData());
            isForecastShowing = true;
            listAdapter.notifyDataSetChanged();
        } else {
            int count = sharedPrefs.getInt(BACK_COUNT_KEY, 0);
            if (count == 0) {
                t = Toast.makeText(MainActivity.this, "'Vissza' még egyszer a kilépéshez", Toast.LENGTH_SHORT);
                t.show();
            }
            sharedEditor.putInt(BACK_COUNT_KEY, ++count);
            sharedEditor.apply();
            Runnable mRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        t.cancel();
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                    if (sharedPrefs.getInt(BACK_COUNT_KEY, 0) == 1) {
                        sharedEditor.putInt(BACK_COUNT_KEY, 0);
                        sharedEditor.apply();
                        //nothing happens...
                    } else if (sharedPrefs.getInt(BACK_COUNT_KEY, 0) > 1) {
                        sharedEditor.putInt(BACK_COUNT_KEY, 0);
                        sharedEditor.apply();
                        finish();
                    }
                }
            };
            Handler mHandler = new Handler();
            mHandler.postDelayed(mRunnable, 500);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        forecastDialog.dismiss();
        dialog.dismiss();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(Html.fromHtml("<small>Időkép időjárás</small>"));


        //General inits

        SharedPreferences widgetPrefs = getSharedPreferences(WidgetProvider.KEY, 0);
        SharedPreferences.Editor prefsEditor = widgetPrefs.edit();
        prefsEditor.putString(SONGNAME_KEY, null);
        prefsEditor.putInt(WidgetProvider.CLICK, 0);
        prefsEditor.apply();


        //
        //FORECAST INITS
        //

        sharedPrefs = getSharedPreferences(SHAREDPREFS, 0);
        sharedEditor = sharedPrefs.edit();

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(true);
                DownloadHandler handler;
                handler = new DownloadHandler(getApplicationContext(), MainActivity.this, null);
                handler.execute("");
            }
        });

        mainActRelativeLayout = findViewById(R.id.mainactivity_relativelayout);
        actualTempTextView = findViewById(R.id.actual_temptext);
        if (sharedPrefs.getString(ACTUALTEMP_KEY, null) != null) {
            actualTempTextView.setText(sharedPrefs.getString(ACTUALTEMP_KEY, null));
        }
        actualIconImageView = findViewById(R.id.actual_icon);

        if (sharedPrefs.getString(ACTUALTEMPICON_KEY, null) != null) {
            try {
                Picasso.get().load(sharedPrefs.getString(ACTUALTEMPICON_KEY, null)).into(actualIconImageView);
            } catch (Exception e) {

            }
        }
        actualDayNameTextView = findViewById(R.id.actual_dayname);
        actualDayNameTextView.setText(formatDay.format(System.currentTimeMillis()));
        alarmClockButton = findViewById(R.id.mainactivity_alarmclock);
        alarmClockButton.setOnClickListener(this);
        textualForecastButton = findViewById(R.id.mainactivity_textualforecast);
        textualForecastButton.setOnClickListener(this);
        citySearchButton = findViewById(R.id.mainactivity_citysearch);
        citySearchButton.setOnClickListener(this);
        refreshButton = findViewById(R.id.mainactivity_refresh);
        refreshButton.setOnClickListener(this);
        changeListButton = findViewById(R.id.mainactivity_change_list);
        changeListButton.setOnClickListener(this);

        listAdapter = new ActivityListAdapter(this, showItems);
        listView = findViewById(R.id.mainactivity_listview);
        listView.setAdapter(listAdapter);
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {

            }

            @Override
            public void onScroll(AbsListView view,
                                 int firstVisibleItem,
                                 int visibleItemCount,
                                 int totalItemCount) {
                if (firstVisibleItem == 0) {
                    swipeRefreshLayout.setEnabled(true);
                } else {
                    swipeRefreshLayout.setEnabled(false);
                }

            }
        });
        databaseHandler = new DatabaseHandler(this);

        long lastUpdated = Long.parseLong(sharedPrefs.getString(PREF_KEY, "0"));
        lastupdated_text = (TextView) findViewById(R.id.lastupdated_text);
        lastupdated_text.setText(lastUpdateDateFormat.format(new Date(lastUpdated)));
        activity_city_name = (TextView) findViewById(R.id.activity_city_name);
        activity_city_name.setText(sharedPrefs.getString(CITYSEARCH_KEY, ""));

        forecastDialogBuilder = new AlertDialog.Builder(this);
        forecastView = getLayoutInflater().inflate(R.layout.forecast_dialog, null);
        forecastTextView = forecastView.findViewById(R.id.dialog_textualforecast);
        forecastDialogBuilder.setView(forecastView);
        forecastDialog = forecastDialogBuilder.create();
        if (sharedPrefs.getString(TEXTUALFORECAST_KEY, null) != null) {
            textualForecast = sharedPrefs.getString(TEXTUALFORECAST_KEY, null);
        }

        activity_city_name.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return false;
            }
        });

        if (databaseHandler.getAllForecastData() != null && databaseHandler.getAllForecastData().size() > 0) {
            showItems.addAll(databaseHandler.getAllForecastData());
            listAdapter.notifyDataSetChanged();
        }


        //
        //ALARMCLOCK INITS
        //
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        dialogBuilder = new AlertDialog.Builder(this);
        dialogView = getLayoutInflater().inflate(R.layout.alertdialog, null);
        dialogBuilder.setView(dialogView);
        dialog = dialogBuilder.create();
        songNameTextView = dialogView.findViewById(R.id.alertdialog_songname);
        songNameTextViewRemover = dialogView.findViewById(R.id.alertdialog_songnameremover);
        songNameTextViewRemover.setOnClickListener(this);

        String[] hourArray = new String[24];
        for (int i = 0; i < 24; i++) {
            hourArray[i] = String.valueOf(i);
        }
        ArrayAdapter hourAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, hourArray);
        alertdialog_hour = dialogView.findViewById(R.id.alertdialog_hour);
        alertdialog_hour.setAdapter(hourAdapter);

        String[] minuteArray = new String[60];
        for (int i = 0; i < 60; i++) {
            minuteArray[i] = String.valueOf(i);
        }
        ArrayAdapter minuteAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, minuteArray);
        alertdialog_minute = dialogView.findViewById(R.id.alertdialog_minute);
        alertdialog_minute.setAdapter(minuteAdapter);

        Button alertdialog_filechooser = dialogView.findViewById(R.id.alertdialog_chooser);
        alertdialog_filechooser.setOnClickListener(this);
        Button alertdialog_savebtn = dialogView.findViewById(R.id.alertdialog_save);
        alertdialog_savebtn.setOnClickListener(this);
        Button alertdialog_cancelbtn = dialogView.findViewById(R.id.alertdialog_cancel);
        alertdialog_cancelbtn.setOnClickListener(this);
        alertdialog_seekbar = dialogView.findViewById(R.id.alertdialog_seekbar);
        alertdialog_seekbar.setOnSeekBarChangeListener(this);
    }

    @Override
    public void OnCompletition(Boolean successfulDownload, ArrayList<WeatherItem> tomorrowList,
                               ArrayList<WeatherItem> forecastList,
                               Context context, String actualTemp, String actualTempIcon, String backgroundImageLink) {
        swipeRefreshLayout.setRefreshing(false);
        if (successfulDownload) {
            sharedEditor.putString(PREF_KEY, String.valueOf(System.currentTimeMillis()));
            sharedEditor.putString(ACTUALTEMP_KEY, actualTemp);
            sharedEditor.putString(ACTUALTEMPICON_KEY, actualTempIcon);
            sharedEditor.putString(ACTUALBACKGROUND_KEY, backgroundImageLink);
            sharedEditor.apply();
            lastupdated_text.setText(lastUpdateDateFormat.format(new Date(System.currentTimeMillis())));
            activity_city_name.setText(sharedPrefs.getString(CITYSEARCH_KEY, ""));
            Toast.makeText(this, "Adatok frissítve: " + sharedPrefs.getString(CITYSEARCH_KEY, ""), Toast.LENGTH_SHORT).show();
            actualTempTextView.setText(actualTemp);
            actualDayNameTextView.setText(formatDay.format(System.currentTimeMillis()));
            try {
                GlideToVectorYou.justLoadImage(this, Uri.parse(actualTempIcon), actualIconImageView);
                // Picasso.get().load(actualTempIcon).into(actualIconImageView);
            } catch (Exception e) {

            }
            showItems.clear();
            if (isForecastShowing) {
                showItems.addAll(forecastList);
            } else {
                showItems.addAll(tomorrowList);
            }
            listAdapter.notifyDataSetChanged();

            databaseHandler.replaceTomorrowData(tomorrowList);
            databaseHandler.replaceForecastData(forecastList);

            this.textualForecast = sharedPrefs.getString(TEXTUALFORECAST_KEY, "");
        } else {
            Toast.makeText(this, "Adatok frissítése sikertelen", Toast.LENGTH_SHORT).show();
        }
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(MainActivity.this, WidgetProvider.class));
                new WidgetProvider().onUpdate(MainActivity.this, appWidgetManager, appWidgetIds);
            }
        });
        thread.start();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.mainactivity_alarmclock) {
            int savedVolume = sharedPrefs.getInt(ALARMVOLUME_KEY, -999);
            if (savedVolume != -999) {
                alertdialog_seekbar.setProgress(savedVolume);
            }
            String selectedSongFile = sharedPrefs.getString(SONGNAME_KEY, null);
            if (selectedSongFile != null) {
                songNameTextView.setText(selectedSongFile);
            }
            if (sharedPrefs.getString(PREFERREDHOUR_KEY, null) != null) {
                alertdialog_hour.setSelection(Integer.parseInt(sharedPrefs.getString(PREFERREDHOUR_KEY, null)));
            }
            if (sharedPrefs.getString(PREFERREDMINUTE_KEY, null) != null) {
                alertdialog_minute.setSelection(Integer.parseInt(sharedPrefs.getString(PREFERREDMINUTE_KEY, null)));
            }
            dialog.show();

        } else if (view.getId() == R.id.mainactivity_textualforecast) {
            forecastTextView.setText(Html.fromHtml(textualForecast));
            forecastDialog.show();
        } else if (view.getId() == R.id.mainactivity_citysearch) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final AutoCompleteTextView autoCompleteTextView = new AutoCompleteTextView(this);
            autoCompleteTextView.setHint("Város neve...");
            autoCompleteTextView.setImeOptions(EditorInfo.IME_ACTION_DONE);
            autoCompleteTextView.setInputType(InputType.TYPE_CLASS_TEXT);
            final String cityList = sharedPrefs.getString(CITYLIST_KEY, null);
            ArrayAdapter adapter;
            if (cityList != null) {
                adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, cityList.split(DIVIDER));
            } else {
                adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1);
            }
            autoCompleteTextView.setAdapter(adapter);
            autoCompleteTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                    String inputText = autoCompleteTextView.getText().toString();
                    if (inputText.length() > 1) {
                        autoCompleteTextView.setText("");
                        citySearchDialog.dismiss();
                        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(autoCompleteTextView.getWindowToken(), 0);
                        swipeRefreshLayout.setRefreshing(true);
                        DownloadHandler handler = new DownloadHandler(getApplicationContext(), MainActivity.this,
                                inputText);
                        handler.execute("");
                    }
                    return true;
                }
            });
            final LinearLayout linlay = new LinearLayout(this);
            ListView cityListView = new ListView(this);
            linlay.setOrientation(LinearLayout.VERTICAL);
            linlay.addView(autoCompleteTextView);
            linlay.addView(cityListView);
            builder.setView(linlay);
            builder.setTitle("Írj be egy várost");
            citySearchDialog = builder.create();
            final ArrayAdapter cityListAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1);
            if (cityList != null) {
                cityListAdapter.addAll(cityList.split(DIVIDER));
            }
            cityListView.setAdapter(cityListAdapter);
            autoCompleteTextView.setText("");
            citySearchDialog.show();
            cityListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    String selectedCity = (String) adapterView.getItemAtPosition(i);
                    autoCompleteTextView.setText("");
                    citySearchDialog.dismiss();
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(autoCompleteTextView.getWindowToken(), 0);
                    swipeRefreshLayout.setRefreshing(true);
                    DownloadHandler handler = new DownloadHandler(getApplicationContext(), MainActivity.this,
                            selectedCity);
                    handler.execute("");
                }
            });
            cityListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(final AdapterView<?> adapterView, View view, final int pos, final long l) {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                    builder1.setTitle("Megerősítés...");
                    builder1.setMessage("Biztos, hogy töröljük?");
                    builder1.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            String[] list = cityList.split(DIVIDER);
                            ArrayList<String> arr = new ArrayList<>(Arrays.asList(list));
                            arr.remove(adapterView.getItemAtPosition(pos));
                            String newCityList = "";
                            for (String str : arr) {
                                newCityList += str + DIVIDER;
                            }
                            cityListAdapter.clear();
                            cityListAdapter.addAll(arr);
                            cityListAdapter.notifyDataSetChanged();
                            sharedEditor.putString(CITYLIST_KEY, newCityList);
                            sharedEditor.apply();
                        }
                    });
                    builder1.setNegativeButton("Mégse", null);
                    builder1.show();
                    return true;
                }
            });
        } else if (view.getId() == R.id.mainactivity_refresh) {
            swipeRefreshLayout.setRefreshing(true);
            DownloadHandler handler;
            handler = new DownloadHandler(getApplicationContext(), MainActivity.this, null);
            handler.execute("");
        } else if (view.getId() == R.id.mainactivity_change_list) {
            showItems.clear();
            if (isForecastShowing) {
                showItems.addAll(databaseHandler.getAllTomorrowData());
                isForecastShowing = false;
            } else {
                showItems.addAll(databaseHandler.getAllForecastData());
                isForecastShowing = true;
            }
            listAdapter.notifyDataSetChanged();
        } else if (view.getId() == R.id.alertdialog_songnameremover) {
            songNameTextView.setText("(alapértelmezett)");
            sharedEditor.putString(SONGNAME_KEY, null);
            sharedEditor.apply();
        } else if (view.getId() == R.id.alertdialog_chooser) {
            FileChooserDialogBuilder fileChooser = new FileChooserDialogBuilder(MainActivity.this, ".mp3", ".wav");
            fileChooser.setOnItemSelectedListener(new FileChooserDialogBuilder.OnItemSelectedListener() {
                @Override
                public void OnItemSelected(File file) {
                    if (songNameTextView != null) {
                        songNameTextView.setText(file.getName());
                        sharedEditor.putString(SONGNAME_KEY, file.getAbsolutePath());
                        sharedEditor.apply();
                    }
                }
            });
        } else if (view.getId() == R.id.alertdialog_save) {
            String hour = alertdialog_hour.getSelectedItem().toString();
            String minute = alertdialog_minute.getSelectedItem().toString();
            String formattedMinute = minute.length() < 2 ? "0" + minute : minute;
            Calendar calendar = Calendar.getInstance();
            int actualHour = calendar.get(Calendar.HOUR_OF_DAY);
            int actualMinute = calendar.get(Calendar.MINUTE);
            String timeLeft;
            if (Integer.parseInt(hour) > actualHour || (Integer.parseInt(hour) == actualHour && Integer.parseInt(minute) > actualMinute)) {
                timeFromNow = (long) (Integer.parseInt(hour) * 3600 + Integer.parseInt(minute) * 60 - actualHour * 3600 - actualMinute * 60) * 1000;
                timeLeft = WFTools.convertToTime(timeFromNow);
            } else {
                timeFromNow = (long) (0 + (24 * 3600 * 1000 - (actualHour * 3600 + actualMinute * 60 - Integer.parseInt(hour) * 3600 - Integer.parseInt(minute) * 60) * 1000));
                timeLeft = WFTools.convertToTime(timeFromNow);
            }
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putLong(TIMEFROMNOW_KEY, timeFromNow);
            editor.putString(ALARMTIME_KEY, hour + ":" + formattedMinute);
            editor.putString(PREFERREDHOUR_KEY, alertdialog_hour.getSelectedItem().toString());
            editor.putString(PREFERREDMINUTE_KEY, alertdialog_minute.getSelectedItem().toString());
            editor.apply();
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(MainActivity.this, AlarmWidgetProvider.class));
            new AlarmWidgetProvider().onUpdate(MainActivity.this, appWidgetManager, appWidgetIds);
            Intent alarmIntent = new Intent(this, AlarmService.class);
            try {
                stopService(alarmIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }
            startService(alarmIntent);
            dialog.dismiss();
            Toast.makeText(this, "Ébresztés: " + hour + ":" + formattedMinute + ", hátralévő idő: " + timeLeft, Toast.LENGTH_SHORT).show();
        } else if (view.getId() == R.id.alertdialog_cancel) {
            dialog.dismiss();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        try {
            mediaPlayer.stop();
            mediaPlayer.reset();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        try {
            mediaPlayer.stop();
            mediaPlayer.reset();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String soundFile = sharedPrefs.getString(SONGNAME_KEY, null);
        int setVolume = seekBar.getProgress();
        final int systemVolumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        float volume = (float) (1 - (Math.log(101 - setVolume) / Math.log(101)));
        if (soundFile == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.alarmbeeps);
        } else {
            mediaPlayer = MediaPlayer.create(this, Uri.parse(soundFile));
        }
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 15, 0);
        mediaPlayer.setVolume(volume, volume);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mediaPlayer.stop();
                mediaPlayer.reset();
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, systemVolumeLevel, 0);
            }
        };
        Handler handler = new Handler();
        handler.postDelayed(runnable, 5000);
        mediaPlayer.start();
        sharedEditor.putInt(ALARMVOLUME_KEY, setVolume);
        sharedEditor.apply();
    }

    public enum Tools {
        TOMORROW, FORECAST
    }

}
