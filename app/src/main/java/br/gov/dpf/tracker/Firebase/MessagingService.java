package br.gov.dpf.tracker.Firebase;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import br.gov.dpf.tracker.Components.NotificationController;

public class MessagingService extends FirebaseMessagingService {

    //Notification manager class
    private NotificationController notificationController;

    @Override
    public void onCreate()
    {
        //Call parent method
        super.onCreate();

        //Instantiate singleton notification builder
        notificationController = NotificationController.getInstance(this);
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0)
        {
            //Show notification to user
            notificationController.showNotification(remoteMessage.getData());
        }
    }
}
