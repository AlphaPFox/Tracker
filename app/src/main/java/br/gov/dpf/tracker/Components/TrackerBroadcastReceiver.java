package br.gov.dpf.tracker.Components;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import br.gov.dpf.tracker.DetailActivity;

public class TrackerBroadcastReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent) {

        //If intent is a dismiss notification event
        if(intent.getAction() != null)
        {
            //Instantiate singleton notification builder
            NotificationController notificationController = NotificationController.getInstance(context);

             if(intent.hasExtra("DismissGroup"))
            {
                //Dismiss notification group (summary)
                notificationController.dismissNotificationGroup(intent.getAction());
            }
            else if (intent.hasExtra("DismissSingle"))
            {
                //Dismiss single notification
                notificationController.dismissNotification(intent.getStringExtra("GroupKey"), intent.getIntExtra("DismissSingle", 0));
            }
            else if(intent.hasExtra("Tracker"))
            {
                //Dismiss notification group (summary)
                notificationController.dismissNotificationGroup(intent.getStringExtra("GroupKey"));

                //Change intent from broadcast receiver, to activity
                intent.setClass(context, DetailActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                //Start detail activity
                context.startActivity(intent);
            }
        }
    }
}
