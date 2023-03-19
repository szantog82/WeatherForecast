package com.example.szantog.weatherforecast;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DatabaseHandler extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "weatherforecast";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_NAME = "datas";
    private static final String DATE = "date";
    private static final String MAX_TEMP = "maxtemp";
    private static final String MIN_TEMP = "mintemp";
    private static final String ICON = "icon";
    private static final String ISTOMORROWDATA = "istomorrowdata";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_NAME + "(ID INTEGER PRIMARY KEY AUTOINCREMENT," + DATE + " TEXT," +
                MAX_TEMP + " TEXT," + MIN_TEMP + " TEXT," + ICON + " TEXT," + ISTOMORROWDATA + " INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_NAME + "(ID INTEGER PRIMARY KEY AUTOINCREMENT," + DATE + " TEXT," +
                MAX_TEMP + " TEXT," + MIN_TEMP + " TEXT," + ICON + " TEXT)");
    }

    public ArrayList<WeatherItem> getAllTomorrowData() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + ISTOMORROWDATA + "=1", null);
        ArrayList<WeatherItem> items = new ArrayList<>();
        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            String date = cursor.getString(cursor.getColumnIndex(DATE));
            int maxTemp = cursor.getInt(cursor.getColumnIndex(MAX_TEMP));
            int minTemp = cursor.getInt(cursor.getColumnIndex(MIN_TEMP));
            String icon = cursor.getString(cursor.getColumnIndex(ICON));
            items.add(new WeatherItem(date, maxTemp, minTemp, icon));
        }
        cursor.close();
        db.close();
        return items;
    }

    public ArrayList<WeatherItem> getAllForecastData() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + ISTOMORROWDATA + "=0", null);
        ArrayList<WeatherItem> items = new ArrayList<>();
        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            String date = cursor.getString(cursor.getColumnIndex(DATE));
            int maxTemp = cursor.getInt(cursor.getColumnIndex(MAX_TEMP));
            int minTemp = cursor.getInt(cursor.getColumnIndex(MIN_TEMP));
            String icon = cursor.getString(cursor.getColumnIndex(ICON));
            items.add(new WeatherItem(date, maxTemp, minTemp, icon));
        }
        cursor.close();
        db.close();
        return items;
    }

    public void replaceTomorrowData(ArrayList<WeatherItem> items) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, ISTOMORROWDATA + "=1", null);
        for (WeatherItem item : items) {
            ContentValues values = new ContentValues();
            values.put(DATE, item.getDate());
            values.put(MAX_TEMP, item.getMaxTemp());
            values.put(MIN_TEMP, item.getMinTemp());
            values.put(ICON, item.getIcon());
            values.put(ISTOMORROWDATA, 1);
            db.insert(TABLE_NAME, null, values);
        }
        db.close();
    }

    public void replaceForecastData(ArrayList<WeatherItem> items) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, ISTOMORROWDATA + "=0", null);
        for (WeatherItem item : items) {
            ContentValues values = new ContentValues();
            values.put(DATE, item.getDate());
            values.put(MAX_TEMP, item.getMaxTemp());
            values.put(MIN_TEMP, item.getMinTemp());
            values.put(ICON, item.getIcon());
            values.put(ISTOMORROWDATA, 0);
            db.insert(TABLE_NAME, null, values);
        }
        db.close();
    }

    public void deleteAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, null, null);
        db.close();
    }
}
