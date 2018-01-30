package is.xyz.mpv;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

/*
    All this service does is
    - Discourage Android from killing mpv while it's in background
    - Update the persistent notification (which we're forced to display)
 */

public class BackgroundPlaybackService extends Service implements EventObserver {
    @Override
    public void onCreate() {
        MPVLib.addObserver(this);
        MPVLib.observeProperty("media-title", MPVLib.mpvFormat.MPV_FORMAT_STRING);
    }

    private Notification buildNotification(String contentText) {
        Intent notificationIntent = new Intent(this, MPVActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification.Builder builder =
            new Notification.Builder(this)
                    .setPriority(Notification.PRIORITY_LOW)
                    .setContentTitle(getText(R.string.mpv_activity))
                    .setContentText(contentText)
                    .setSmallIcon(R.drawable.ic_play_arrow_black_24dp)
                    .setContentIntent(pendingIntent);
        return builder.build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "BackgroundPlaybackService: starting");

        // create notification and turn this into a "foreground service"

        Notification notification = buildNotification(MPVLib.getPropertyString("media-title"));
        startForeground(NOTIFICATION_ID, notification);

        // resume playback (audio-only)

        MPVLib.setPropertyString("vid", "no");
        MPVLib.setPropertyBoolean("pause", false);

        return START_NOT_STICKY; // Android can't restart this service on its own
    }

    @Override
    public void onDestroy() {
        MPVLib.removeObserver(this);

        Log.v(TAG, "BackgroundPlaybackService: destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    /* Event observers */

    @Override
    public void eventProperty(@NotNull String property) {}

    @Override
    public void eventProperty(@NotNull String property, long value) {}

    @Override
    public void eventProperty(@NotNull String property, boolean value) {}

    @Override
    public void eventProperty(@NotNull String property, @NotNull String value) {
        if (property.equals("media-title")) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_ID, buildNotification(value));
        }
    }

    @Override
    public void event(int eventId) {
        if (eventId == MPVLib.mpvEventId.MPV_EVENT_IDLE)
            stopSelf();
    }

    private static final int NOTIFICATION_ID = 12345; // TODO: put this into resource file
    private static final String TAG = "mpv";
}
