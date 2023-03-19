package com.example.szantog.weatherforecast;

public class WeatherItem {

    private String date;
    private int maxTemp;
    private int minTemp;
    private String icon;

    public WeatherItem(String date, int maxTemp, int minTemp, String icon) {
        this.date = date;
        this.maxTemp = maxTemp;
        this.minTemp = minTemp;
        this.icon = icon;
    }

    public String getDate() {
        return date;
    }

    public int getMaxTemp() {
        return maxTemp;
    }

    public int getMinTemp() {
        return minTemp;
    }

    public String getIcon() {
        return icon;
    }

}
