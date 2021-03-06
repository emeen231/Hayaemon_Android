package com.edolfzoku.hayaemon2;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RemoteControlClient;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.un4seen.bass.BASS;

public class ForegroundService extends IntentService {
    private Notification notification;
    private MainActivity mainActivity;

    public void setMainActivity(MainActivity mainActivity) { this.mainActivity = mainActivity; }

    public class ForegroundServiceBinder extends Binder {
        public ForegroundService getService() {
            return ForegroundService.this;
        }
    }

    private final IBinder mBinder = new ForegroundServiceBinder();

    public ForegroundService() {
        super("ForegroundService");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        stopForeground();

        super.onDestroy();
    }

    public void startForeground(String strTitle, String strArtist) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager notificationManager =
                    (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel("default",
                    "ハヤえもんによる音声の再生",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("ハヤえもんによる音声の再生");
            notificationManager.createNotificationChannel(channel);
        }

        if (Build.VERSION.SDK_INT >= 21) {
            Intent intentRewind = new Intent(getApplicationContext(), ForegroundService.class );
            intentRewind.setAction("action_rewind");
            PendingIntent pendingIntentRewind = PendingIntent.getService(getApplicationContext(), 1, intentRewind, 0);

            Intent intentPause = new Intent(getApplicationContext(), ForegroundService.class );
            intentPause.setAction("action_pause");
            PendingIntent pendingIntentPause = PendingIntent.getService(getApplicationContext(), 1, intentPause, 0);

            Intent intentForward = new Intent(getApplicationContext(), ForegroundService.class );
            intentForward.setAction("action_forward");
            PendingIntent pendingIntentForward = PendingIntent.getService(getApplicationContext(), 1, intentForward, 0);

            notification = new NotificationCompat.Builder(this, "default")
                    .addAction(new NotificationCompat.Action.Builder(R.drawable.ic_rewind, "Previous", pendingIntentRewind).build())
                    .addAction(new NotificationCompat.Action.Builder(R.drawable.ic_pause, "Pause", pendingIntentPause).build())
                    .addAction(new NotificationCompat.Action.Builder(R.drawable.ic_forward, "Next", pendingIntentForward).build())
                    .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                            .setShowActionsInCompactView(0, 1, 2))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher))
                    .setContentTitle(strTitle)
                    .setContentText(strArtist)
                    .build();
        }
        else {
            notification = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher))
                    .setContentTitle(strTitle)
                    .setContentText(strArtist)
                    .build();
        }

        startForeground(1, notification);
    }

    public void stopForeground()
    {
        stopForeground(true);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(intent.getAction() == null) return;

        if(intent.getAction().equals("action_rewind")) {
            final PlaylistFragment playlistFragment = (PlaylistFragment)mainActivity.mSectionsPagerAdapter.getItem(0);
            if(MainActivity.hStream == 0) return;
            EffectFragment effectFragment = (EffectFragment)mainActivity.mSectionsPagerAdapter.getItem(4);
            if(!effectFragment.isReverse() && BASS.BASS_ChannelBytes2Seconds(mainActivity.hStream, BASS.BASS_ChannelGetPosition(mainActivity.hStream, BASS.BASS_POS_BYTE)) > mainActivity.dLoopA + 1.0)
                BASS.BASS_ChannelSetPosition(mainActivity.hStream, BASS.BASS_ChannelSeconds2Bytes(mainActivity.hStream, mainActivity.dLoopA), BASS.BASS_POS_BYTE);
            else if(effectFragment.isReverse() && BASS.BASS_ChannelBytes2Seconds(mainActivity.hStream, BASS.BASS_ChannelGetPosition(mainActivity.hStream, BASS.BASS_POS_BYTE)) < mainActivity.dLoopA - 1.0)
                BASS.BASS_ChannelSetPosition(mainActivity.hStream, BASS.BASS_ChannelSeconds2Bytes(mainActivity.hStream, mainActivity.dLoopB), BASS.BASS_POS_BYTE);
            else {
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        playlistFragment.playPrev();
                    }
                });
            }
        }
        else if(intent.getAction().equals("action_pause")) {
            final PlaylistFragment playlistFragment = (PlaylistFragment)mainActivity.mSectionsPagerAdapter.getItem(0);
            mainActivity.runOnUiThread(new Runnable() {
               @Override
               public void run() {
                   if(BASS.BASS_ChannelIsActive(MainActivity.hStream) == BASS.BASS_ACTIVE_PLAYING)
                       playlistFragment.pause();
                   else if(BASS.BASS_ChannelIsActive(MainActivity.hStream) == BASS.BASS_ACTIVE_PAUSED)
                       playlistFragment.play();
               }
            });
        }
        else if(intent.getAction().equals("action_forward")) {
            final PlaylistFragment playlistFragment = (PlaylistFragment)mainActivity.mSectionsPagerAdapter.getItem(0);
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    playlistFragment.playNext(true);
                }
            });
        }
    }
}
