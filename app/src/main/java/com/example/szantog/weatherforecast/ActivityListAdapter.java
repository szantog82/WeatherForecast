package com.example.szantog.weatherforecast;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYou;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by szantog on 2018.04.25..
 */

public class ActivityListAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<WeatherItem> items;

    public ActivityListAdapter(Context context, ArrayList<WeatherItem> items) {
        this.context = context;
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int i) {
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.mainactivity_listitem_layout, viewGroup, false);
        }
        ImageView img = view.findViewById(R.id.listitem_img);
        TextView dayText = view.findViewById(R.id.listitem_dayname);
        TextView dateText = view.findViewById(R.id.listitem_date);
        TextView data = view.findViewById(R.id.listitem_temp);

        String[] splits = items.get(i).getDate().split(" ");

        if (splits.length > 2) {
            //Day text
            dayText.setText(splits[0]);

            //Date text
            dateText.setVisibility(View.VISIBLE);
            dateText.setText(splits[1] + " " + splits[2] + " " + splits[3]);
        } else {
            dayText.setText(items.get(i).getDate());
            dateText.setVisibility(View.GONE);
        }

        //Image
        try {
           // Picasso.get().load(items.get(i).getIcon()).into(img);
            GlideToVectorYou.justLoadImage((Activity) context, Uri.parse(items.get(i).getIcon()), img);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Temp data
        if (items.get(i).getMinTemp() == -999) {
            data.setText(String.valueOf(items.get(i).getMaxTemp()) + "°C");
        } else {
            data.setText(String.valueOf(items.get(i).getMaxTemp()) + "/" + String.valueOf(items.get(i).getMinTemp()) + "°C");
        }

        if (items.get(i).getDate().toLowerCase().contains("szombat") || items.get(i).getDate().toLowerCase().contains("vasárnap")) {
            dayText.setTextColor(Color.parseColor("#AA0000"));
        } else {
            dayText.setTextColor(Color.parseColor("#333333"));
        }

        return view;
    }
}
