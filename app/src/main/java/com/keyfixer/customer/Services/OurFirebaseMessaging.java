package com.keyfixer.customer.Services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.keyfixer.customer.Common.Common;
import com.keyfixer.customer.CustomerCallActivity;
import com.keyfixer.customer.HomeActivity;
import com.keyfixer.customer.R;
import com.keyfixer.customer.RateActivity;

import java.util.Map;

public class OurFirebaseMessaging extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(final RemoteMessage remoteMessage) {
        if (remoteMessage.getData() != null){
            Map<String, String> data = remoteMessage.getData();
            String title = data.get("title");
            final String message = data.get("message");

            try{
                Log.e("title", "" + title);
                if (title.equals("Thông báo!")){
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(OurFirebaseMessaging.this, message, Toast.LENGTH_SHORT).show();
                        }
                    });
                    showCancelNotification(message);
                    Common.isFixDone = true;
                    GoesToHome();
                } else if (title.equals("Đã đến")){
                    showArrivedNotification(message);
                } else if (title.equals("Sửa xong")){
                    openRateActivity(message);
                }
            }catch (Exception ex){
                ex.printStackTrace();
                Log.e("Warning","Something wrong");
                Log.e("Exception", "" + ex.getCause());
            }
        }
    }

    private void openRateActivity(String body) {
        Intent intent = new Intent(this, RateActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void GoesToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        Common.isFixDone = true;
        //set lại false để khi nhấn tìm thợ sửa khóa, nó sẽ không auto gọi activity CallFixer
        Common.isFixerFound = false;
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void showArrivedNotification(String body) {
        PendingIntent contentIntent = PendingIntent.getActivity(getBaseContext(), 0, new Intent(),
                PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getBaseContext());
        builder.setAutoCancel(true).setDefaults(Notification.DEFAULT_LIGHTS|Notification.DEFAULT_SOUND)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Đã đến")
                .setContentText(body)
                .setContentIntent(contentIntent);
        NotificationManager manager = (NotificationManager)getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1, builder.build());
    }

    private void showCancelNotification(String body) {
        PendingIntent contentIntent = PendingIntent.getActivity(getBaseContext(), 0, new Intent(),
                PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getBaseContext());
        builder.setAutoCancel(true).setDefaults(Notification.DEFAULT_LIGHTS|Notification.DEFAULT_SOUND)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Đã hủy")
                .setContentText(body)
                .setContentIntent(contentIntent);
        NotificationManager manager = (NotificationManager)getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1, builder.build());
    }
}
