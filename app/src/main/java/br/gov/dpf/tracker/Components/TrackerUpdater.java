package br.gov.dpf.tracker.Components;

import android.content.Context;
import android.provider.DocumentsProvider;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import br.gov.dpf.tracker.Entities.Configuration;
import br.gov.dpf.tracker.Entities.NotificationMessage;
import br.gov.dpf.tracker.Entities.Tracker;

public class TrackerUpdater implements EventListener<DocumentSnapshot>, Runnable
{
    private ListenerRegistration listener;
    private Tracker mTracker;
    private NotificationController notificationController;
    private NotificationMessage notification;

    public TrackerUpdater(Context context, Tracker tracker)
    {
        //Get notification controller
        notificationController = NotificationController.getInstance(context);

        //Save tracker data
        mTracker = tracker;
    }

    public void initialize()
    {
        new Thread(this).start();
    }

    @Override
    public void run()
    {
        //Create notification data object
        Map<String, String> notificationData = new HashMap<>();

        //Save notification settings
        notificationData.put("id", mTracker.getIdentification());
        notificationData.put("title", "Realizando solicitação...");
        notificationData.put("progress", "0");
        notificationData.put("datetime", String.valueOf(new Date().getTime()));

        //Build notification
        notification = notificationController.showNotification(notificationData, "/topics/" + mTracker.getIdentification() +"_NotifyUpdate");

        //Save updater (to allow dismiss)
        notification.updater = TrackerUpdater.this;

        //Get tracker document reference
        DocumentReference trackerRef = FirebaseFirestore.getInstance().document("Tracker/" + mTracker.getIdentification());

        //Listen for changes on tracker document
        listener = trackerRef.addSnapshotListener(TrackerUpdater.this);
    }

    @Override
    public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e)
    {
        //Check if document was not deleted
        if(documentSnapshot.exists())
        {
            //Parse DB result to tracker object
            Tracker tracker = documentSnapshot.toObject(Tracker.class);

            //Try to get configuration status
            Map<String, Object> configuration = tracker.getLastConfiguration();

            //Check if configuration has not started yet
            if (configuration != null) {
                //Set configuration progress
                notification.setProgress(Integer.parseInt(configuration.get("progress").toString()));

                //Set notification title
                notification.setTitle(configuration.get("description").toString());

                //Set description
                notification.setContent(notification.getProgress() + "%");

                // Displays the progress bar update
                notificationController.updateNotification(notification);

                //If configuration finished (no longer pending)
                if (!configuration.get("step").equals("PENDING")) {
                    //Remove listener
                    listener.remove();
                }
            }
        }
    }
}
