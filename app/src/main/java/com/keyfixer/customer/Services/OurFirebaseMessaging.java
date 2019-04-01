package com.keyfixer.customer.Services;

import android.os.Looper;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import android.os.Handler;
import android.widget.Toast;

public class OurFirebaseMessaging extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(final RemoteMessage remoteMessage) {
        //vì ở đây đang ở ngoài luồng của main, nên nếu muốn dùng toast thì phải dùng tới handler
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(OurFirebaseMessaging.this, "" + remoteMessage.getNotification().getBody(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
