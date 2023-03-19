package com.example.szantog.weatherforecast;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class DownloadHandler extends AsyncTask {

    public interface TaskListener {
        public void OnCompletition(Boolean successfulDownload, ArrayList<WeatherItem> tomorrowList,
                                   ArrayList<WeatherItem> forecastList, Context context,
                                   String actualTemp, String actualTempIcon, String backgroundImageLink);
    }

    private final String WEBSITE_ROOT = "https://www.idokep.hu/idojaras/";
    private final String WEBSITETEXTUAL_ROOT = "https://www.idokep.hu/elorejelzes/";
    private String website;
    private String websiteForTextual;
    private String inputCityName;
    private String foundCityName;
    private StringBuilder textualForecast = new StringBuilder();
    private SharedPreferences prefs;

    private ArrayList<String> tomorrowtitles = new ArrayList<String>();
    private ArrayList<Integer> tomorrowTemps = new ArrayList<Integer>();
    private ArrayList<String> tomorrowIcons = new ArrayList<String>();
    private String actualTemp = "";
    private String backgroundImageLink = "";
    private String actualTempIcon = "";

    private ArrayList<WeatherItem> forecastList;
    private ArrayList<WeatherItem> tomorrowList;
    private Boolean successfulDownload;

    private Context context;
    private final TaskListener taskListener;

    public DownloadHandler(Context context, TaskListener taskListener, String inputCityName) {
        this.context = context;
        this.taskListener = taskListener;
        this.inputCityName = inputCityName;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        successfulDownload = false;

        if (inputCityName == null) {
            prefs = context.getSharedPreferences(MainActivity.SHAREDPREFS, 0);
            String prefCityName = prefs.getString(MainActivity.CITYSEARCH_KEY, "Budapest");
            website = WEBSITE_ROOT + Uri.encode(prefCityName, "ISO-8859-1");
            websiteForTextual = WEBSITETEXTUAL_ROOT + Uri.encode(prefCityName, "ISO-8859-1");
        } else {
            website = WEBSITE_ROOT + Uri.encode(inputCityName, "ISO-8859-1");
            websiteForTextual = WEBSITETEXTUAL_ROOT + Uri.encode(inputCityName, "ISO-8859-1");
        }
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        URL url;
        try {
            tomorrowList = new ArrayList<WeatherItem>();
            forecastList = new ArrayList<WeatherItem>();
            url = new URL(website);
            HttpsURLConnection connection = (HttpsURLConnection) url
                    .openConnection();
            connection.setRequestMethod("GET");
            connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:56.0) Gecko/20100101 Firefox/56.0");
            connection.connect();
            BufferedReader bReader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));

            String line;
            Boolean maxval = false;
            String date = "";
            int maxTemp = 0;
            int minTemp = 1000;
            String icon = "";

            Boolean firstData = true;
            Boolean cityFound = false;

            int lineCount = 0;
            int actualIconMarker = 0;
            int forecastIconLineCounter = 0;
            int lineAnchorDate = 99990;
            int lineAnchorImg = 99990;
            int lineAnchorMaxTemp = 99990;
            int lineAnchorMinTemp = 99990;

            while ((line = bReader.readLine()) != null) {
                if (minTemp != 1000) {
                    WeatherItem timeStamp = new WeatherItem(date, maxTemp, minTemp,
                            icon);
                    forecastList.add(timeStamp);
                    date = "";
                    maxTemp = 0;
                    minTemp = 1000;
                    icon = "";
                }

                if (line.contains("current-weather-icon")) {
                    actualIconMarker = lineCount;
                }

                if (line.contains("oszlop jelenlegi")
                        && line.contains("background-image")) {
                    int index = line.indexOf("background-image");
                    int index2 = line.indexOf(")\">");
                    try {
                        backgroundImageLink = "https://www.idokep.hu" + line.substring(index + 21, index2);
                    } catch (Exception e) {
                        backgroundImageLink = "";
                    }
                }

                if (actualIconMarker != 0 && lineCount == actualIconMarker + 2) {
                    int index = line.lastIndexOf("src=");
                    int index2 = line.indexOf(".svg");
                    actualTempIcon = "https://www.idokep.hu" + line.substring(index + 5, index2 + 4);
                    actualIconMarker = 0;
                }

                if (line.contains("current-temperature")) {
                    int index = line.indexOf("temperature");
                    String textFound = (line.substring(index + 13)).toLowerCase();
                    String numbersToParse = "";
                    int i = 0;
                    while ("0123456789-".contains(textFound.substring(i, i + 1))) {
                        numbersToParse += textFound.charAt(i);
                        i++;
                    }
                    actualTemp = numbersToParse + "°C";
                }

                if (line.contains("ik dailyForecastCol")) {
                    lineAnchorDate = lineCount;
                }
                if (line.contains(" </div>\"><img class=\"ik forecast-icon\"")) {
                    lineAnchorImg = lineCount;
                }
                if (line.contains("ik max")) {
                    lineAnchorMaxTemp = lineCount;
                }
                if (line.contains("ik min\"")) {
                    lineAnchorMinTemp = lineCount;
                }


                if (lineCount == lineAnchorDate + 10) {
                    int index = line.indexOf("\"");
                    int index2 = line.lastIndexOf("\"");
                    date = line.substring(index + 1, index2);
                    date = date.replace("<br>", " ");
                }
                if (lineCount == lineAnchorImg + 1) {
                    int index = line.indexOf("src=");
                    int index2 = line.indexOf(".svg");
                    icon = "https://www.idokep.hu" + line.substring(index + 5, index2 + 4);
                }
                if (lineCount == lineAnchorMaxTemp + 11) {
                    String data = line.replaceAll("[^0-9\\-]+", "");
                    maxTemp = Integer.parseInt(data);
                }
                if (lineCount == lineAnchorMinTemp + 12) {
                    String data = line.replaceAll("[^0-9\\-]+", "");
                    minTemp = Integer.parseInt(data);
                }


                /*
                if (forecastIconLineCounter != 0 && lineCount == forecastIconLineCounter + 10) {
                    int index = line.lastIndexOf("title");
                    date = line.substring(index + 7, line.length() - 1).replace("<br>", " ");
                } else if (forecastIconLineCounter != 0 && lineCount == forecastIconLineCounter + 13) {
                    int index = line.lastIndexOf("src");
                    int index2 = line.lastIndexOf(".svg");
                    icon = "https://www.idokep.hu" + line.substring(index + 5, index2 + 4);
                } else if (forecastIconLineCounter != 0 && line.contains("max-daily-temp") && lineCount > forecastIconLineCounter && !maxval) {
                    int index = line.lastIndexOf("max-daily-temp");
                    String data = line.substring(index + 16).replaceAll("[^0-9\\-]+", "");
                    maxTemp = Integer.parseInt(data);
                    maxval = true;
                } else if (forecastIconLineCounter != 0 && line.contains("max-daily-temp") && lineCount > forecastIconLineCounter && maxval) {
                    int index = line.lastIndexOf("max-daily-temp");
                    String data = line.substring(index + 16).replaceAll("[^0-9\\-]+", "");
                    minTemp = Integer.parseInt(data);
                    maxval = false;
                    forecastIconLineCounter = 0;
                }*/

                if (line.contains("ik hourly-forecast-hour")) {
                    int index = line.lastIndexOf("hour");
                    int index2 = line.lastIndexOf("</div>");
                    tomorrowtitles.add(line.substring(index + 6, index2));
                }
                if (line.contains("ik forecast-icon") && line.contains("forecast-icons")) {
                    int index = line.indexOf("icon");
                    int index2 = line.lastIndexOf(".svg");
                    tomorrowIcons.add("https://www.idokep.hu" + line.substring(index + 11, index2 + 4));
                }

                if (line.contains("Várható hőmérséklet")) {
                    int index = line.lastIndexOf(":");
                    int index2 = line.lastIndexOf("&deg");
                    tomorrowTemps.add(Integer.parseInt(line.substring(index + 2, index2 - 1)));
                }

                if (line.contains("fas fa-location-arrow")) {
                    int index = line.lastIndexOf("arrow");
                    int index2 = line.lastIndexOf("</a>");
                    foundCityName = line.substring(index + 11, index2);
                }
                lineCount++;
            }
            connection.disconnect();

            //for textual forecast
            URL url2 = new URL(websiteForTextual);
            HttpsURLConnection connection2 = (HttpsURLConnection) url2.openConnection();
            connection2.setRequestMethod("GET");
            connection2.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:56.0) Gecko/20100101 Firefox/56.0");
            connection2.connect();
            BufferedReader bReader2 = new BufferedReader(new InputStreamReader(
                    connection2.getInputStream()));

            String line2;
            int lineIndex = 0;
            int actualIndex = 0;
            boolean stop = false;

            while ((line2 = bReader2.readLine()) != null) {
                lineIndex++;
                if (line2.contains("<small>") && actualIndex != 0) {
                    stop = true;
                }

                if (line2.contains("<h2>") && !stop) {
                    textualForecast.append(line2.trim());
                    actualIndex = lineIndex;
                }

                if (!stop && actualIndex != 0 && lineIndex > actualIndex) {
                    textualForecast.append(line2.trim());
                }
            }
            connection2.disconnect();

            successfulDownload = true;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);

        for (int i = 0; i < tomorrowtitles.size(); i++) {
            try {
                String title = tomorrowtitles.get(i);
                int maxTemp = tomorrowTemps.get(i);
                String icon = tomorrowIcons.get(i);
                tomorrowList.add(new WeatherItem(title, maxTemp, -999, icon));
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }

        }

        prefs = context.getSharedPreferences(MainActivity.SHAREDPREFS, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(MainActivity.TEXTUALFORECAST_KEY, textualForecast.toString());
        editor.apply();

        if (foundCityName != null && foundCityName.length() > 0) {
            editor.putString(MainActivity.CITYSEARCH_KEY, foundCityName);
            String cityList = prefs.getString(MainActivity.CITYLIST_KEY, null);
            if (cityList == null) {
                editor.putString(MainActivity.CITYLIST_KEY, foundCityName);
            } else {
                List<String> cities = Arrays.asList(cityList.split(MainActivity.DIVIDER));
                if (!cities.contains(foundCityName)) {
                    editor.putString(MainActivity.CITYLIST_KEY, cityList + MainActivity.DIVIDER + foundCityName);
                }
            }
            editor.apply();
        }

        this.taskListener.OnCompletition(successfulDownload, tomorrowList, forecastList, context, actualTemp, actualTempIcon, backgroundImageLink);
    }
}
