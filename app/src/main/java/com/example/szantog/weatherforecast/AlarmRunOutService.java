package com.example.szantog.weatherforecast;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.annotation.Nullable;

public class AlarmRunOutService extends Service {

    private SharedPreferences prefs;

    private MediaPlayer mPlayer;
    private AudioManager manager;
    private String songName;
    private int systemVolumeLevel;
    private PowerManager pm;
    private int gain = 0;

    private Handler mHandler;
    private Runnable mRunnable;

    public static final String ALARMACTIVITY_INTENT_ACTION = "alarmactivity_intent_action";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(ALARMACTIVITY_INTENT_ACTION)) {
            playSound(songName);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.e("alarmrunout", "started");

        manager = (AudioManager) getSystemService(AUDIO_SERVICE);
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");

        prefs = getApplication().getSharedPreferences(MainActivity.SHAREDPREFS, 0);
        songName = prefs.getString(MainActivity.SONGNAME_KEY, null);

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final View layoutView = inflater.inflate(R.layout.alarmactivity_layout, null);

        /*LinearLayout linearLayout = layoutView.findViewById(R.id.alarmactivity_rootlayout);
        linearLayout.dispatchKeyEvent({

        });*/

        final WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.type = WindowManager.LayoutParams.TYPE_TOAST;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

        wm.addView(layoutView, params);

        pm = (PowerManager) getSystemService(POWER_SERVICE);
        @SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "tag");
        wl.acquire();

        final TextView time = layoutView.findViewById(R.id.alarmactivity_actualtime);
        time.setText(simpleDateFormat.format(new Date(System.currentTimeMillis())));
        mHandler = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                time.setText(simpleDateFormat.format(new Date(System.currentTimeMillis())));
                mHandler.postDelayed(this, 60000);
            }
        };
        mHandler.postDelayed(mRunnable, 60000);

        Button stop_btn = layoutView.findViewById(R.id.alarmactivity_stopbtn);

        stop_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wm.removeView(layoutView);
                try {
                    mPlayer.stop();
                    mPlayer.release();
                    mPlayer.reset();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Toast.makeText(getApplicationContext(), "Leállítva", Toast.LENGTH_SHORT).show();
                stopSelf();
            }
        });

    }

    private void playSound(final String soundFile) {
        int setVolumeLevel = prefs.getInt(MainActivity.ALARMVOLUME_KEY, -999);
        if (setVolumeLevel == -999) {
            setVolumeLevel = 5;
        }
        float volume;
        if (setVolumeLevel + gain >= 100) {
            volume = 1.0f;
        } else {
            volume = (float) (1 - (Math.log(101 - (setVolumeLevel + gain)) / Math.log(101)));
        }
        mPlayer = new MediaPlayer();
        try {
            if (soundFile == null) {
                mPlayer = MediaPlayer.create(this, R.raw.alarmbeeps);
                Log.e("MediaPlayer", "alarmbeep is playing...");
            } else {
                mPlayer.setDataSource(soundFile);
                mPlayer.prepare();
                Log.e("MediaPlayer", soundFile + " is playing...");
            }
            systemVolumeLevel = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
            manager.setStreamVolume(AudioManager.STREAM_MUSIC, 15, 0);
            mPlayer.setVolume(volume, volume);
            mPlayer.start();
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    Log.e("Mediaplayer", "onCompletition");
                    gain += 3;
                    playSound(songName);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }
        try {
            manager.setStreamVolume(AudioManager.STREAM_MUSIC, systemVolumeLevel, 0);
            mPlayer.reset();
            mPlayer = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
