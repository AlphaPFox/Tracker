package br.gov.dpf.tracker.Entities;

import android.app.NotificationManager;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

//Class representing a group of notifications
public class NotificationGroup {

    //Identifiers
    private int groupID;
    private String groupKey;

    //Notification content
    public ArrayList<NotificationMessage> notifications = new ArrayList<>();

    //Tracker object representing this group
    public Tracker tracker = null;

    //Create a notification group and add it's first notification
    public NotificationGroup(int id, String key)
    {
        //Save notification group ID (to build unique notifications to this group)
        groupID = id;

        //Save notification group key
        groupKey = key;

        //Get tracker object from DB
        getTrackerData(key);
    }

    //Get notification with that specific ID
    public NotificationMessage findNotification(int notificationID)
    {
        for(NotificationMessage notification : notifications)
        {
            if(notification.getNotificationID() == notificationID)
            {
                return notification;
            }
        }

        return null;
    }

    //Add notification to topic
    public void addNotification(NotificationMessage newNotification, NotificationManagerCompat notificationManager)
    {
        //For each existing notification on this group
        for(NotificationMessage notification : notifications)
        {
            //If it is from the same topic
            if(notification.getTopic().equals((newNotification.getTopic())))
            {
                //Remove old notification (will be replaced)
                notifications.remove(notification);

                //Cancel old notification
                notificationManager.cancel(notification.getNotificationID());
            }
        }

        //Add new notification
        notifications.add(newNotification);
    }

    //Get tracker from FireStore DB
    private void getTrackerData(String key)
    {
        try
        {
            //Try to get tracker data from firestore DB (wait for operation to finish)
            DocumentSnapshot result = Tasks.await(FirebaseFirestore.getInstance().collection("Tracker").document(key).get());

            //Parse result to a tracker object
            tracker = result.toObject(Tracker.class);
        }
        catch (Exception ex)
        {
            //Log data
            Log.e("NotificationController", "Error getting tracker data", ex);
        }
    }

    public int getGroupID()
    {
        return groupID;
    }

    public String getGroupKey()
    {
        return groupKey;
    }
}

