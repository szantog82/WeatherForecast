package com.example.szantog.weatherforecast;

public class WFTools {

    public static String convertToTime(Long timeInMillis) {
        long timeInSec = timeInMillis / 1000;
        String hour;
        if (timeInSec > 3600) {
            hour = String.valueOf(timeInSec / 3600);
            timeInSec = timeInSec - Long.parseLong(hour) * 3600;
        } else {
            hour = "0";
        }
        String minute = String.valueOf(timeInSec / 60);
        return minute.length() < 2 ? hour + ":0" + minute : hour + ":" + minute;
    }
}
