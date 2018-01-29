package br.gov.dpf.tracker.Components;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import br.gov.dpf.tracker.Entities.NotificationGroup;
import br.gov.dpf.tracker.Entities.NotificationMessage;
import br.gov.dpf.tracker.R;

public class NotificationController
{
    private static final String NOTIFICATION_ID = "br.gov.dpf.tracker.NOTIFICATION_ID";
    private static final int SUMMARY_ID = 0;

    private final Context context;
    private final NotificationManagerCompat notificationManager;
    private final SharedPreferences sharedPreferences;

    //Simple way to track text for notifications that have already been issued
    private static HashMap<String, NotificationGroup> notificationGroups = new HashMap<>();

    private static NotificationController _instance;

    private NotificationController(Context context)
    {
        this.context = context.getApplicationContext();
        this.notificationManager = NotificationManagerCompat.from(context);
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public synchronized static NotificationController getInstance(Context context)
    {
        if (_instance == null)
        {
            _instance = new NotificationController(context);
        }
        return _instance;
    }

    //Show notification to user (groups notification from the same tracker)
    public NotificationMessage showNotification(Map<String, String> notificationData)
    {
        //Notification group object
        NotificationGroup notificationGroup;

        //Create notification object
        NotificationMessage notification = new NotificationMessage(getNotificationId(), notificationData);

        //If this group already exists (created on a previous notification)
        if(notificationGroups.containsKey(notification.getGroupKey()))
        {
            //Get notification group corresponding to this tracker
            notificationGroup = notificationGroups.get(notification.getGroupKey());
        }
        else
        {
            //Create a notification group
            notificationGroup = new NotificationGroup(notificationGroups.size(), notification.getGroupKey());

            //Add group on notification list
            notificationGroups.put(notification.getGroupKey(), notificationGroup);
        }

        //Add notification to group
        notificationGroup.notifications.add(notification);

        //Check if tracker was successfully retrieved from FireStore DB
        if(notificationGroup.tracker != null)
        {
            //Check how many notifications are available and device android version (pré-Nougat devices do not support bundled notifications)
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            {
                //Build single notification
                notificationManager.notify(notification.getNotificationID(), buildNotification(notification, notificationGroup));

                //Build notification summary
                notificationManager.notify(notificationGroup.getGroupID(), buildSummary(notificationGroup));
            }
            else if(notificationGroup.notifications.size() == 1)
            {
                //Build single notification
                notificationManager.notify(notification.getNotificationID(), buildNotification(notification, notificationGroup));
            }
            else
            {
                //Build notification summary
                notificationManager.notify(notificationGroup.getGroupID(), buildSummary(notificationGroup));
            }
        }

        //Return created notification
        return notification;
    }

    void updateNotification(NotificationMessage notification)
    {
        //Get notification group corresponding to this tracker
        NotificationGroup notificationGroup = notificationGroups.get(notification.getGroupKey());

        //Build single notification
        notificationManager.notify(notification.getNotificationID(), buildNotification(notification, notificationGroup));
    }

    private Notification buildNotification(NotificationMessage notification, NotificationGroup notificationGroup) {

        //Build single notification style (shown if there is only one notification)
        NotificationCompat.Builder single_notification;

        // If android version is 7.0 or superior
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        {

            //Build notification using new features (bundle notifications)
            single_notification = new NotificationCompat.Builder(context, "CHANEL")
                    .setSmallIcon(R.drawable.ic_tracker_notification_24dp)
                    .setGroup(notificationGroup.getGroupKey())
                    .setContentTitle(notification.getTitle())
                    .setContentInfo(notificationGroup.tracker.getName() + " (" + notificationGroup.tracker.getModel() + ")")
                    .setSubText(notificationGroup.tracker.getName() + " (" + notificationGroup.tracker.getModel() + ")")
                    .setContentText(notification.getContent())
                    .setWhen(notification.getDatetime())
                    .setAutoCancel(true)
                    .setShowWhen(true);
        }
        else
        {
            //Build notification without using new features
            single_notification = new NotificationCompat.Builder(context, "CHANEL")
                    .setSmallIcon(R.drawable.ic_tracker_notification_24dp)
                    .setContentTitle(notificationGroup.tracker.getName() + " (" + notificationGroup.tracker.getModel() + ")")
                    .setContentText(notification.getTitle())
                    .setWhen(notification.getDatetime())
                    .setAutoCancel(true)
                    .setShowWhen(true);
        }

        //Check if notification has coordinates available
        if(notification.getCoordinates() != null)
        {
            //Change notification priority
            single_notification.setPriority(Notification.PRIORITY_HIGH);

            try
            {
                //Build google maps static API URL
                String mapsURL = "https://maps.googleapis.com/maps/api/staticmap?" +
                        "center=" + notification.getCoordinates() +
                        "&zoom=14" +
                        "&size=512x512" +
                        "&scale=2" +
                        "&maptype=roadmap" +
                        "&markers=color:0x" + notificationGroup.tracker.getBackgroundColor().substring(3)+ "%7C" + notification.getCoordinates() +
                        "&key=" + context.getResources().getString(R.string.google_maps_static_key);

                //Perform download from URL
                InputStream in = new java.net.URL(mapsURL).openStream();

                //Decode downloaded map to bitmap image
                Bitmap mapImage = BitmapFactory.decodeStream(in);

                //Append to notification
                single_notification.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(mapImage).setSummaryText(notification.getContent()));
            }
            catch (Exception e)
            {
                //Log error
                Log.e("NotificationController", "Error downloading map", e);
            }
        }
        else if(notification.getExpandedContent() != null)
        {
            //Append expanded text to notification
            single_notification.setStyle(new NotificationCompat.BigTextStyle().bigText(notification.getExpandedContent()));
        }

        //If notification has progress indicator
        if(notification.getProgress() > 0)
        {
            //Set progress to notification
            single_notification.setPriority(Notification.PRIORITY_MAX);
            single_notification.setProgress(100, notification.getProgress(), false);
            single_notification.setAutoCancel(false);
        }

        //Set tracker color on notification
        single_notification.setColor(Color.parseColor(notificationGroup.tracker.getBackgroundColor()));

        //File path to model image
        File imgFile = new File(context.getFilesDir(), notificationGroup.tracker.getModel());

        //Check if image was already downloaded
        if(imgFile.exists())
        {
            //Return image disk path
            single_notification.setLargeIcon(createRoundedBitmapDrawableWithBorder(BitmapFactory.decodeFile(imgFile.getAbsolutePath()), single_notification.getColor()));
        }

        //Create dismiss event intent
        Intent dismissIntent = new Intent(context, TrackerBroadcastReceiver.class);

        //Set intent action with group key and notification ID
        dismissIntent.setAction(notificationGroup.getGroupKey() + "_dismiss_" + notification.getNotificationID());
        dismissIntent.putExtra("GroupKey", notificationGroup.getGroupKey());
        dismissIntent.putExtra("DismissSingle", notification.getNotificationID());

        //Apply it to notification builder
        single_notification.setDeleteIntent(PendingIntent.getBroadcast(context, 0, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        //Create dismiss event intent
        Intent clickIntent = new Intent(context, TrackerBroadcastReceiver.class);

        //Set intent action with group key
        clickIntent.setAction(notificationGroup.getGroupKey() + "_click_" + notification.getNotificationID());

        // Put tracker data on intent
        clickIntent.putExtra("GroupKey", notificationGroup.getGroupKey());
        clickIntent.putExtra("Tracker", notificationGroup.tracker);

        //Apply it to notification builder
        single_notification.setContentIntent(PendingIntent.getBroadcast(context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        //Return built notification
        return single_notification.build();
    }

    private Notification buildSummary(NotificationGroup notificationGroup) {

        //Build and issue the group summary. Use inbox style so that all messages are displayed
        NotificationCompat.Builder summaryBuilder = new NotificationCompat.Builder(context, "CHANEL")
                .setSmallIcon(R.drawable.ic_tracker_notification_24dp)
                .setShowWhen(true)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setGroup(notificationGroup.getGroupKey());

        //Create notification style
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        //Set notification title
        summaryBuilder.setContentTitle(notificationGroup.tracker.getName() + " (" + notificationGroup.tracker.getModel() + ")");
        inboxStyle.setBigContentTitle(notificationGroup.tracker.getName() + " (" + notificationGroup.tracker.getModel() + ")");

        //Set notification color
        summaryBuilder.setColor(Color.parseColor(notificationGroup.tracker.getBackgroundColor()));

        //If there is only one notification
        if(notificationGroup.notifications.size() == 1)
        {
            //Get single notification
            NotificationMessage notification = notificationGroup.notifications.get(0);

            //Set title text
            summaryBuilder.setContentText(notification.getStyledMessage());

            //Set summary text
            inboxStyle.setSummaryText(notificationGroup.tracker.getName() + " (" + notificationGroup.tracker.getModel() + ")");
        }
        else if(notificationGroup.notifications.size() == 2)
        {
            //Get single notification
            NotificationMessage first_notification = notificationGroup.notifications.get(0);
            NotificationMessage second_notification = notificationGroup.notifications.get(1);

            //Set title text
            summaryBuilder.setContentText(first_notification.getTitle() + " / " + second_notification.getTitle());

            //Set summary text
            inboxStyle.setSummaryText(notificationGroup.tracker.getName() + " (" + notificationGroup.tracker.getModel() + ")");
        }
        else
        {
            //Get single notification
            NotificationMessage first_notification = notificationGroup.notifications.get(0);

            //Set title text
            summaryBuilder.setContentText(first_notification.getTitle() + " / + " + (notificationGroup.notifications.size() - 1) + " notificações");

            //Set summary text
            inboxStyle.setSummaryText(notificationGroup.tracker.getName() + " (" + notificationGroup.tracker.getModel() + ")");
        }

        //File path to model image
        File imgFile = new File(context.getFilesDir(), notificationGroup.tracker.getModel());

        //Check if image was already downloaded
        if(imgFile.exists())
        {
            //Return image disk path
            summaryBuilder.setLargeIcon(createRoundedBitmapDrawableWithBorder(BitmapFactory.decodeFile(imgFile.getAbsolutePath()), summaryBuilder.getColor()));
        }

        //Create dismiss event intent
        Intent dismissIntent = new Intent(context, TrackerBroadcastReceiver.class);

        //Set intent action with group key
        dismissIntent.setAction(notificationGroup.getGroupKey());
        dismissIntent.putExtra("DismissGroup", notificationGroup.getGroupKey());

        //Apply it to notification builder
        summaryBuilder.setDeleteIntent(PendingIntent.getBroadcast(context, 0, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        //Create dismiss event intent
        Intent clickIntent = new Intent(context, TrackerBroadcastReceiver.class);

        //Set intent action with group key
        clickIntent.setAction(notificationGroup.getGroupKey() + "_click");

        // Put tracker data on intent
        clickIntent.putExtra("GroupKey", notificationGroup.getGroupKey());
        clickIntent.putExtra("Tracker", notificationGroup.tracker);

        //Apply it to notification builder
        summaryBuilder.setContentIntent(PendingIntent.getBroadcast(context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        //Add notification title
        for (NotificationMessage notificationMessage : notificationGroup.notifications)
        {
            //Check if it is a progress notification
            if(notificationMessage.updater == null)
            {
                //Add to summary
                inboxStyle.addLine(notificationMessage.getStyledMessage());
            }
        }

        //Set notification style
        summaryBuilder.setStyle(inboxStyle);

        // If android version is lower than 7.0 (do not support bundled notifications)
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
        {
            //Change summary text (tracker name is already displayed on inbox style title)
            inboxStyle.setSummaryText("Clique para obter mais informações.");

            //For each single notification available
            for(NotificationMessage notification : notificationGroup.notifications)
            {
                //Remove from notification bar, show only summary
                notificationManager.cancel(notification.getNotificationID());
            }
        }

        //Build notification and return
        return summaryBuilder.build();
    }

    void dismissNotificationGroup(String groupKey)
    {
        //Get notification group
        NotificationGroup notificationGroup = notificationGroups.get(groupKey);

        //If group successfully retrieved
        if(notificationGroup != null)
        {
            //For each existing notification
            for(NotificationMessage notification : notificationGroup.notifications) {

                //Dismiss notification
                notificationManager.cancel(notification.getNotificationID());

                //Check if there is an update related to this notification
                if (notification.updater != null)
                {
                    //Cancel update
                    notification.updater.dismissUpdate();
                }
            }

            //Dismiss summary
            notificationManager.cancel(notificationGroup.getGroupID());

            //Erase existing notifications
            notificationGroup.notifications.clear();
        }
    }

    public void dismissNotification(String groupKey, int notificationID)
    {
        //Get notification group
        NotificationGroup notificationGroup = notificationGroups.get(groupKey);

        //If group successfully retrieved
        if(notificationGroup != null)
        {
            //Find notification by id
            NotificationMessage notification = notificationGroup.findNotification(notificationID);

            //Check if there is an update related to this notification
            if(notification != null && notification.updater != null)
            {
                //Cancel update
                notification.updater.dismissUpdate();
            }

            //Remove notification from group
            notificationGroup.notifications.remove(notification);

            //If there are more notifications
            if(notificationGroup.notifications.size() > 0)
            {
                //Rebuild summary
                notificationManager.notify(notificationGroup.getGroupID(), buildSummary(notificationGroup));
            }
        }
    }

    private int getNotificationId() {
        int id = sharedPreferences.getInt(NOTIFICATION_ID, SUMMARY_ID) + 1;
        while (id == SUMMARY_ID) {
            id++;
        }
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(NOTIFICATION_ID, id);
        editor.apply();
        return id;
    }

    private Bitmap createRoundedBitmapDrawableWithBorder(Bitmap bitmap, int color){

        // Create bitmap
        Bitmap roundedBitmap = Bitmap.createBitmap(bitmap.getWidth(),bitmap.getHeight(),Bitmap.Config.ARGB_8888);

        // Initialize a new Canvas to draw empty bitmap
        Canvas canvas = new Canvas(roundedBitmap);

        // Draw a solid color to canvas
        canvas.drawColor(color);

        // Now draw the bitmap to canvas.
        canvas.drawBitmap(bitmap, 0, 0, null);

        // Create circular bitmap drawable
        RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(context.getResources(),roundedBitmap);

        // Set circular option
        roundedBitmapDrawable.setCircular(true);

        // Enable anti-aliasing for this drawable.
        roundedBitmapDrawable.setAntiAlias(true);

        // Create resulting bitmap
        Bitmap output = Bitmap.createBitmap(roundedBitmapDrawable.getIntrinsicWidth(), roundedBitmapDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

        // Create a resulting canvas
        canvas = new Canvas(output);

        //Set canvas bounds
        roundedBitmapDrawable.setBounds(20, 20, canvas.getWidth() - 20, canvas.getHeight() - 20);

        // Draw on canvas the rounded bitmap
        roundedBitmapDrawable.draw(canvas);

        // Return the rounded bitmap
        return output;
    }
}
