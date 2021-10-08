package com.hawkxeye.online.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;

import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import com.hawkxeye.online.ui.main.activities.MainActivity;
import com.hawkxeye.online.R;

import java.lang.reflect.Field;
import java.util.ArrayList;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by osama on 24/04/2017.
 */

public class OnlineNotificationManager {
    private static OnlineNotificationManager m_OnlineNotificationManager;
    public static final int MVIEW_NOTIFICATION_ID = 2;
    public static final int FOREGROUND_NOTIFICATION_ID = 3;
    public static final String NOTIFICATION_CHANNEL_ID = "mview_channel";
    public static final String NOTIFICATION_CHANNEL_NAME = "MView Foregrounding Channel";
    public static final String NOTIFICATION_CHANNEL_DESC = "This channel foregrounds the background operation.";
    private NotificationCompat.Builder mBuilder;
    private Notification mNotification;
    private static Context mcontext;
    private NotificationManager mNotificationManager;
    private RemoteViews remoteViews;

    private OnlineNotificationManager() {
    }

    public static OnlineNotificationManager getInstance(Context context) {
        if (m_OnlineNotificationManager == null) {
            m_OnlineNotificationManager = new OnlineNotificationManager();
        }
        mcontext = context;

        return m_OnlineNotificationManager;
    }

    public Notification getLiveNotification() {
        InitializeCustomActionNotification();
//        InitializeCustomNotification();
//        InitializNotification();
        return mNotification;
    }

    private void InitializeCustomNotification() {
        if (mBuilder == null) {
            remoteViews = new RemoteViews(mcontext.getPackageName(), R.layout.cutom_noti_layout);
            remoteViews.setImageViewResource(R.id.ibStream, R.drawable.ic_intercom_foreground);
            mNotificationManager = (NotificationManager) mcontext.getSystemService(NOTIFICATION_SERVICE);
            mBuilder = new NotificationCompat.Builder(mcontext, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? R.drawable.ic_intercom_foreground : R.mipmap.ic_intercom)
                    .setCustomContentView(remoteViews)
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                    .setAutoCancel(false)
                    .setShowWhen(true);
            mNotification = mBuilder.build();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
                channel.setDescription(NOTIFICATION_CHANNEL_DESC);
                mNotificationManager.createNotificationChannel(channel);
            }
            mNotification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        }

    }

    public void UpdateCustomNotification(String content, PendingIntent streamPendingIntent, boolean isStreaming) {
        InitializeCustomNotification();
        remoteViews.setTextViewText(R.id.tvContent, content);
        if (isStreaming)
            remoteViews.setImageViewResource(R.id.ibStream, R.drawable.ic_intercom_foreground);
        else
            remoteViews.setImageViewResource(R.id.ibStream, R.drawable.ic_intercom_background);
        remoteViews.setOnClickPendingIntent(R.id.ibStream, streamPendingIntent);
        mNotificationManager = (NotificationManager) mcontext.getSystemService(NOTIFICATION_SERVICE);
        mBuilder.setStyle(new NotificationCompat.DecoratedCustomViewStyle());
        mBuilder.setWhen(System.currentTimeMillis());
        mBuilder.setPriority(Notification.PRIORITY_MAX);
        mNotification = mBuilder.build();
        mNotificationManager.notify(MVIEW_NOTIFICATION_ID, mNotification);
    }

    private void InitializeCustomActionNotification() {
        if (mBuilder == null) {
            mNotificationManager = (NotificationManager) mcontext.getSystemService(NOTIFICATION_SERVICE);
            mBuilder = new NotificationCompat.Builder(mcontext, NOTIFICATION_CHANNEL_ID)
                    .setLargeIcon(BitmapFactory.decodeResource(mcontext.getResources(), R.mipmap.ic_intercom))
                    .setSmallIcon(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? R.drawable.ic_intercom_foreground : R.drawable.ic_intercom_background)
                    .setContentText("Searching...")
                    .setAutoCancel(false)
                    .setShowWhen(true);
            mNotification = mBuilder.build();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                        NOTIFICATION_CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_LOW);
                channel.setDescription(NOTIFICATION_CHANNEL_DESC);
                mNotificationManager.createNotificationChannel(channel);
            }
            mNotification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        }

    }

    public void UpdateCustomActionNotification(String content, PendingIntent streamPendingIntent, boolean isStreaming, PendingIntent contentIntent) {
        InitializeCustomActionNotification();
        int iconRes = isStreaming ? R.drawable.ic_intercom_foreground : R.drawable.ic_intercom_background;
        String text = isStreaming ? "Stop Streaming" : "Start Streaming";

        NotificationCompat.Action action = new NotificationCompat.Action(
                iconRes,
                text,
                streamPendingIntent
        );
        mNotificationManager = (NotificationManager) mcontext.getSystemService(NOTIFICATION_SERVICE);
        mBuilder.setWhen(System.currentTimeMillis());
        mBuilder.setPriority(Notification.PRIORITY_MAX);
        mBuilder.setContentText(content);
        mBuilder.setContentIntent(contentIntent);

        clearActions(mBuilder);
        if (mcontext != null)
            mBuilder.addAction(action);
        mNotification = mBuilder.build();
        mNotificationManager.notify(MVIEW_NOTIFICATION_ID, mNotification);
    }

    private void clearActions(NotificationCompat.Builder builder) {
        try {
            //Use reflection clean up old actions
            Field f = builder.getClass().getDeclaredField("mActions");
            f.setAccessible(true);
            f.set(builder, new ArrayList<NotificationCompat.Action>());
        } catch (NoSuchFieldException e) {
            // no fielld
        } catch (IllegalAccessException e) {
            // wrong types
        }
    }

    private void InitializNotification() {
        if (mBuilder == null) {

            mNotificationManager = (NotificationManager) mcontext.getSystemService(NOTIFICATION_SERVICE);
            mBuilder = new NotificationCompat.Builder(mcontext, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? R.drawable.ic_intercom_foreground : R.drawable.ic_intercom_background)
                    .setContentTitle("MView")
                    .setTicker("TICKER")
                    .setPriority(Notification.PRIORITY_MAX)
                    .setShowWhen(true);

            mBuilder.setContentIntent(PendingIntent.getActivity(mcontext,
                    0,
                    new Intent(mcontext, MainActivity.class),
                    PendingIntent.FLAG_UPDATE_CURRENT));
            mNotification = mBuilder.build();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
                channel.setDescription(NOTIFICATION_CHANNEL_DESC);
                mNotificationManager.createNotificationChannel(channel);
            }
            mNotification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        }

    }

    public void UpdateNotification(String content) {
        InitializNotification();
        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(content));
        mBuilder.setWhen(System.currentTimeMillis());
        mNotification = mBuilder.build();
        mNotificationManager.notify(MVIEW_NOTIFICATION_ID, mNotification);
    }

    public void DisplayNotification() {
        InitializNotification();
        mNotificationManager.notify(MVIEW_NOTIFICATION_ID, mNotification);
    }

    public void CancelNotification() {
        if (mNotificationManager != null) {
            mNotificationManager.cancel(MVIEW_NOTIFICATION_ID);
            mBuilder = null;
            InitializeCustomActionNotification();
        }
    }

}