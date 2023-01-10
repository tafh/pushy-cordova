package me.pushy.sdk;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.content.res.Resources;
import android.media.RingtoneManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.pm.ApplicationInfo;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import android.util.Log;

import me.pushy.sdk.cordova.internal.config.PushyIntentExtras;
import me.pushy.sdk.cordova.internal.util.PushyPersistence;

public class PushReceiver extends BroadcastReceiver {

    private int defaultNotificationColor;

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getApplicationContext().getPackageName(), PackageManager.GET_META_DATA);
            defaultNotificationColor = ContextCompat.getColor(context, ai.metaData.getInt("com.google.firebase.messaging.default_notification_color"));
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("PushyReceiver", "Failed to load meta-data", e);
        } catch(Resources.NotFoundException e) {
            Log.d("PushyReceiver", "Failed to load notification color", e);
        }
        // Notification title and text
        String notificationTitle = getAppName(context);
        String notificationText = "";

        // Attempt to extract the notification text from the "message" property of the data payload
        if (intent.getStringExtra("message") != null) {
            notificationText = intent.getStringExtra("message");
        }

        // Attempt to extract the notification title from the "title" property of the data payload (defaults to the app name if not present)
        if (intent.getStringExtra("title") != null) {
            notificationTitle = intent.getStringExtra("title");
        }

        // Prepare a notification with vibration and sound
        Notification.Builder builder = new Notification.Builder(context)
                .setAutoCancel(true)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setVibrate(new long[]{0, 400, 250, 400})
                .setSmallIcon(getNotificationIcon(context))
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(getMainActivityPendingIntent(context, intent))
                .setColor(defaultNotificationColor);

        // Get an instance of the NotificationManager service
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);

        // Automatically configure a Notification Channel for devices running Android O+
        Pushy.setNotificationChannel(builder, context);

        // Build the notification and display it
        notificationManager.notify(1, builder.build());
    }

    private int getNotificationIcon(Context context) {
        // Attempt to fetch icon name from SharedPreferences
        String icon = PushyPersistence.getNotificationIcon(context);

        // Did we configure a custom icon?
        if (icon != null) {
            // Cache app resources
            Resources resources = context.getResources();

            // Cache app package name
            String packageName = context.getPackageName();

            // Look for icon in drawable folders
            int iconId = resources.getIdentifier(icon, "drawable", packageName);

            // Found it?
            if (iconId != 0) {
                return iconId;
            }

            // Look for icon in mipmap folders
            iconId = resources.getIdentifier(icon, "mipmap", packageName);

            // Found it?
            if (iconId != 0) {
                return iconId;
            }
        }

        // Fallback to generic icon
        return android.R.drawable.ic_dialog_info;
    }

    private static String getAppName(Context context) {
        // Attempt to determine app name via package manager
        return context.getPackageManager().getApplicationLabel(context.getApplicationInfo()).toString();
    }

    private PendingIntent getMainActivityPendingIntent(Context context, Intent receiverIntent) {
        // Convert intent extras to JSON
        String json = PushyPersistence.getJSONObjectFromIntentExtras(receiverIntent).toString();

        // Get launcher activity intent
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getApplicationContext().getPackageName());

        // Make sure to update the activity if it exists
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Pass payload data into PendingIntent
        launchIntent.putExtra(PushyIntentExtras.NOTIFICATION_CLICKED, true);
        launchIntent.putExtra(PushyIntentExtras.NOTIFICATION_PAYLOAD, json);

        // Convert intent into pending intent
        return PendingIntent.getActivity(context, json.hashCode(), launchIntent, PendingIntent.FLAG_IMMUTABLE);
    }
}
