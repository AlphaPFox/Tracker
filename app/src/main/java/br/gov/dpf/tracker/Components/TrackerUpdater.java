package br.gov.dpf.tracker.Components;

import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import br.gov.dpf.tracker.Entities.NotificationMessage;
import br.gov.dpf.tracker.Entities.Tracker;

public class TrackerUpdater
{
    private boolean dismissUpdate = false;
    private int updateStep = 0;

    public void requestUpdate(final View rootView, final Tracker tracker)
    {
        //Flag indicating to dismiss the update or not
        dismissUpdate = false;

        // Start a lengthy operation in a background thread
        new Thread(
                new Runnable() {
                    @Override
                    public void run()
                    {
                        //Create notification data object
                        Map<String, String> notificationData = new HashMap<>();

                        //Save notification settings
                        notificationData.put("id", tracker.getIdentification());
                        notificationData.put("title", "Realizando solicitação...");
                        notificationData.put("progress", "5");
                        notificationData.put("datetime", String.valueOf(new Date().getTime()));

                        //Get notification controller
                        final NotificationController notificationController = NotificationController.getInstance(rootView.getContext());

                        //Build notification
                        final NotificationMessage notification = notificationController.showNotification(notificationData);

                        //Save updater (to allow dismiss)
                        notification.updater = TrackerUpdater.this;

                        //Get Firestore DB instance
                        FirebaseFirestore mFireStoreDB = FirebaseFirestore.getInstance();

                        //Get current date time
                        Date currentDate = new Date();

                        //Reset update step counter
                        updateStep = 0;

                        //Update lastCheck to force server update on tracker
                        mFireStoreDB.document("Tracker/" + tracker.getIdentification())
                                .update("lastCheck", null)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid)
                                    {
                                        //Update notification
                                        notification.setProgress(10);
                                        notification.setTitle("Aguardando resposta do servidor...");

                                        //Update notification step
                                        updateStep(1);
                                    }
                                });

                        //Listen for changes on SMS sent
                        ListenerRegistration smsSentListener = mFireStoreDB.collection("Tracker/" + tracker.getIdentification() + "/SMS_Sent")
                                .whereGreaterThan("sentTime", currentDate)
                                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                    @Override
                                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {

                                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                                            if (dc.getType() == DocumentChange.Type.ADDED) {

                                                //Server sent SMS to tracker, update status
                                                notification.setProgress(30);
                                                notification.setTitle("Entrando em contato com rastreador...");

                                                //Update notification step
                                                updateStep(2);
                                            }
                                            else if (dc.getType() == DocumentChange.Type.MODIFIED)
                                            {
                                                //Updated sms received
                                                notification.setProgress(45);
                                                notification.setTitle("Aguardando resposta do rastreador...");

                                                //Update notification step
                                                updateStep(3);
                                            }
                                        }

                                    }
                                });

                        //Listen for changes on SMS received
                        ListenerRegistration smsReceivedListener = mFireStoreDB.collection("Tracker/" + tracker.getIdentification() + "/SMS_Received")
                                .whereGreaterThan("receivedTime", currentDate)
                                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                    @Override
                                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {

                                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                                            if (dc.getType() == DocumentChange.Type.ADDED) {

                                                //Get text available on SMS received from tracker
                                                String smsText = dc.getDocument().get("text").toString();

                                                //If contains position data
                                                if(smsText.contains("lat") || smsText.contains("lac")) {

                                                    //SMS received from tracker, update notification
                                                    notification.setProgress(80);
                                                    notification.setTitle("Resposta recebida: processando dados...");

                                                    //Update notification step
                                                    updateStep(4);
                                                }
                                            }
                                        }

                                    }
                                });

                        //Listen for changes on Coordinates
                        ListenerRegistration coordinatesUpdateListener = mFireStoreDB.collection("Tracker/" + tracker.getIdentification() + "/Coordinates")
                                .whereGreaterThan("lastDatetime", currentDate)
                                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                    @Override
                                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {

                                        for (DocumentChange dc : snapshots.getDocumentChanges())
                                        {
                                            if (dc.getType() == DocumentChange.Type.ADDED) {

                                                //End of request, update status
                                                notification.setProgress(100);
                                                notification.setTitle("Solicitação finalizada com sucesso.");

                                                //Update notification step
                                                updateStep(5);
                                            }
                                        }
                                    }
                                });

                        //Listen for changes on Coordinates
                        ListenerRegistration coordinatesInsertListener = mFireStoreDB.collection("Tracker/" + tracker.getIdentification() + "/Coordinates")
                                .whereGreaterThan("datetime", currentDate)
                                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                    @Override
                                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {

                                        for (DocumentChange dc : snapshots.getDocumentChanges())
                                        {
                                            if (dc.getType() == DocumentChange.Type.ADDED) {

                                                //End of request, update status
                                                notification.setProgress(100);
                                                notification.setTitle("Solicitação finalizada com sucesso.");

                                                //Update notification step
                                                updateStep(5);
                                            }
                                        }
                                    }
                                });

                        //While waiting for notification to end
                        while(notification.getProgress() < 100 && !dismissUpdate)
                        {
                            // Sets the progress indicator to a max value, the
                            notification.setProgress(notification.getProgress() + 1);

                            try
                            {
                                // Sleep for 2 seconds
                                Thread.sleep(2000);

                                // If user did not requested dismiss during thread sleep
                                if(!dismissUpdate)
                                {
                                    // Displays the progress bar for the first time.
                                    notificationController.updateNotification(notification);
                                }

                            }
                            catch (InterruptedException e)
                            {
                                Log.d("UpdateRequest", "sleep failure");
                            }
                        }

                        //If user canceled operation
                        if(dismissUpdate)
                        {
                            //Dismiss notification
                            notificationController.dismissNotification(notification.getGroupKey(), notification.getNotificationID());
                        }
                        else
                        {
                            switch (updateStep)
                            {
                                case 0:
                                    //Step 0: Could not update tracker.lastCheck value
                                    notification.setProgress(0);
                                    notification.setTitle("Solicitação não concluída");
                                    notification.setContent("Servidor de controle não acessível");
                                    break;

                                case 1:
                                    //Step 1: SMS not sent by server
                                    notification.setProgress(0);
                                    notification.setTitle("Solicitação não concluída");
                                    notification.setContent("Servidor não respondeu a solicitação");
                                    break;

                                case 2:
                                    //Step 2: Tracker did not receive SMS sent by server
                                    notification.setProgress(0);
                                    notification.setTitle("Solicitação não concluída");
                                    notification.setContent("Rastreador não confirmou recebimento do SMS");
                                    break;

                                case 3:
                                    //Step 3: Tracker did not respond to SMS command
                                    notification.setProgress(0);
                                    notification.setTitle("Solicitação não concluída");
                                    notification.setContent("Rastreador não respondeu ao comando solicitado");
                                    break;

                                case 4:
                                    //Step 4: Could not parse tracker response
                                    notification.setProgress(0);
                                    notification.setTitle("Solicitação não concluída");
                                    notification.setContent("Resposta do rastreador não processada corretamente");
                                    break;
                            }

                            // Displays the progress bar update
                            notificationController.updateNotification(notification);
                        }

                        //Remove database change listeners
                        smsSentListener.remove();
                        smsReceivedListener.remove();
                        coordinatesInsertListener.remove();
                        coordinatesUpdateListener.remove();
                    }
                }
                // Starts the thread by calling the run() method in its Runnable
        ).start();

        //Create snack bar to show feed back to user
        Snackbar.make(rootView, "Solicitação em andamento...", Snackbar.LENGTH_LONG)
                .setAction("CANCELAR", new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        dismissUpdate = true;
                        //Show message to user
                        Snackbar.make(rootView, "Solicitação cancelada com sucesso.", Snackbar.LENGTH_LONG).show();
                    }
                }).show();

    }

    //Cancel update execution
    public void dismissUpdate()
    {
        //Change flag value
        dismissUpdate = true;
    }

    private void updateStep(int currentStep)
    {
        //Check if current step
        if(currentStep > updateStep)
        {
            //Update value
            updateStep = currentStep;
        }
    }
}
