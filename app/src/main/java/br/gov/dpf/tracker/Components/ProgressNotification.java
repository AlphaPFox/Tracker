package br.gov.dpf.tracker.Components;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.ScrollView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import br.gov.dpf.tracker.Entities.Configuration;
import br.gov.dpf.tracker.Entities.NotificationMessage;
import br.gov.dpf.tracker.Entities.Tracker;
import br.gov.dpf.tracker.MainActivity;
import br.gov.dpf.tracker.R;

public class ProgressNotification implements EventListener<DocumentSnapshot>, Runnable, OnFailureListener
{
    private ListenerRegistration listener;
    private FirebaseFirestore mFireStoreDB;
    private Tracker mTracker;
    private NotificationController notificationController;
    private NotificationMessage notification;

    public ProgressNotification(Context context, Tracker tracker)
    {
        //Get notification controller
        notificationController = NotificationController.getInstance(context);

        //Initialize firestore instance
        mFireStoreDB = FirebaseFirestore.getInstance();

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

        //Save progressNotification (to allow dismiss)
        notification.progressNotification = ProgressNotification.this;

        //Get tracker document reference
        DocumentReference trackerRef = mFireStoreDB.document("Tracker/" + mTracker.getIdentification());

        //Listen for changes on tracker document
        listener = trackerRef.addSnapshotListener(ProgressNotification.this);
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
            if (configuration != null)
            {
                //Set configuration progress
                notification.setProgress(Integer.parseInt(configuration.get("progress").toString()));

                //Set notification title
                notification.setTitle(configuration.get("description").toString());

                //Set description
                notification.setContent(notification.getProgress() + "%");

                // Displays the progress bar update
                notificationController.updateNotification(notification);

                //If configuration finished (no longer pending)
                if (!configuration.get("step").equals("PENDING"))
                {
                    //Remove listener
                    listener.remove();
                }
            }
        }
    }

    //Cancel progress indicator
    void dismissProgress()
    {
        //Check if listener is active
        if(listener != null)
        {
            //Remove listener to avoid updates
            listener.remove();
        }
    }

    void cancelConfiguration()
    {
        //Create a configuration array
        final Map<String, Object> configuration = new HashMap<>();

        //Set pending configuration status
        configuration.put("step", "CANCELED");
        configuration.put("status", "Procedimento cancelado às " + new SimpleDateFormat("hh:mm - dd/MM", Locale.getDefault()).format(new Date()));
        configuration.put("description", "Solicitação cancelada com sucesso");
        configuration.put("pending", 0);
        configuration.put("progress", 100);
        configuration.put("datetime", new Date());

        //Update tracker object
        mTracker.setLastConfiguration(configuration);

        //Create a transaction
        final WriteBatch transaction = mFireStoreDB.batch();

        //Set to update tracker configuration
        transaction.set(mFireStoreDB.document("Tracker/" + mTracker.getIdentification()), mTracker);

        //Update notification status
        notification.setTitle("Interrompendo processo...");
        notification.setProgress(0);

        // Displays the progress bar update
        notificationController.updateNotification(notification);

        //Get each configuration from tracker
        mFireStoreDB
                .collection("Tracker/" + mTracker.getIdentification() + "/Configurations")
                .whereEqualTo("status.finished", false)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot documentSnapshots)
                    {

                        //For each configuration available
                        for(DocumentSnapshot document : documentSnapshots)
                        {
                            //Transform document snapshot to configuration object
                            Configuration config = document.toObject(Configuration.class);

                            // Update to cancel notification
                            config.getStatus().put("step", "CANCELED");
                            config.getStatus().put("finished", true);
                            config.getStatus().put("description", configuration.get("status").toString());
                            config.getStatus().put("datetime", new Date());

                            //Add to transaction
                            transaction.set(document.getReference(), config);
                        }

                        //Commit transaction
                        transaction.commit().addOnFailureListener(ProgressNotification.this);

                    }
                })
                .addOnFailureListener(this);
    }

    @Override
    public void onFailure(@NonNull Exception e)
    {
        //Set notification title
        notification.setTitle("Erro ao cancelar solicitação");

        // Displays the progress bar update
        notificationController.updateNotification(notification);
    }
}
