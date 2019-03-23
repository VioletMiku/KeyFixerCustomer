package com.keyfixer.customer.Helper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.keyfixer.customer.R;

public class CustomInfoWindow implements GoogleMap.InfoWindowAdapter {

    View myView;

    public CustomInfoWindow(Context context){
        myView = LayoutInflater.from(context).inflate(R.layout.custom_customer_info_window,null);
    }

    @Override
    public View getInfoWindow(Marker marker) {
        TextView txtFixTitle = ((TextView) myView.findViewById(R.id.txt_fixinfo));
        txtFixTitle.setText(marker.getTitle());

        return myView;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }
}
