package br.gov.dpf.tracker.Firestore;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.maps.android.ui.IconGenerator;

import br.gov.dpf.tracker.Entities.Tracker;
import br.gov.dpf.tracker.MainActivity;
import br.gov.dpf.tracker.R;
import de.hdodenhof.circleimageview.CircleImageView;

public class TrackerAdapter
        extends BaseAdapter<TrackerAdapter.ViewHolder>  {

    //Linked activity
    private MainActivity mActivity;

    //Get shared preferences
    private SharedPreferences sharedPreferences;

    //Constructor
    protected TrackerAdapter(MainActivity activity, Query query) {
        super(activity, query);

        mActivity = activity;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
    }

    // Create new views (invoked by the layout manager)
    @Override
    public TrackerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        // Inflate the custom layout
        return new ViewHolder(inflater.inflate(R.layout.main_recycler_item, parent, false));
    }

    @Override
    public void onViewAttachedToWindow(ViewHolder holder) {
        super.onViewAttachedToWindow(holder);

        int dimen[] = mActivity.mRecyclerLayoutManager.getOptimalDimension();

        ViewGroup.LayoutParams layout = holder.itemView.getLayoutParams();
        layout.width = dimen[0];
        layout.height = dimen[1];

        holder.itemView.setLayoutParams(layout);
        holder.itemView.requestLayout();
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        //Get tracker using index position
        final Tracker tracker = getSnapshot(position).toObject(Tracker.class);

        // - replace the contents of the view with that element
        holder.txtTrackerName.setText(tracker.formatName());
        holder.txtTrackerModel.setText(tracker.getModel());
        holder.txtBatteryLevel.setText(tracker.formatBatteryLevel());
        holder.txtSignalLevel.setText(tracker.formatSignalLevel());

        //Check if tracker has an coordinate available
        if(tracker.getLastCoordinate() != null)
            holder.txtLastUpdateValue.setText(formatDateTime(tracker.getLastUpdate(), false));
        else
            holder.txtLastUpdateValue.setText(mActivity.getResources().getString(R.string.txtWaitingTitle));

        //Set user defined color
        holder.imageView.setCircleBackgroundColor(Color.parseColor(tracker.getBackgroundColor()));

        //Set model item image
        holder.imageView.setImageDrawable(mActivity.getResources().getDrawable(mActivity.getResources().getIdentifier("model_" + tracker.getModel().toLowerCase(), "drawable", mActivity.getPackageName())));

        //Change color to loading animation
        holder.progressBar.getIndeterminateDrawable().setColorFilter(holder.imageView.getCircleBackgroundColor(), android.graphics.PorterDuff.Mode.SRC_IN);

        //Set item click listener
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mActivity != null)
                {
                    //Disable click to avoid double clicking
                    holder.itemView.setClickable(false);

                    //Cal interface method on main activity
                    mActivity.OnTrackerSelected(tracker, holder.itemView);
                }
            }
        });

        //Set edit click listener
        holder.imgEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mActivity != null)
                {
                    //Disable click to avoid double clicking
                    holder.itemView.setClickable(false);

                    //Cal interface method on main activity
                    mActivity.OnTrackerEdit(tracker, holder.itemView);
                }
            }
        });

        //Disable click on map area (opens google map app)
        holder.mapView.setClickable(false);

        //If tracker has coordinates available
        if (tracker.getLastCoordinate() != null && tracker.getLastUpdate() != null)
        {

            //Load map
            holder.mapView.onCreate(null);
            holder.mapView.onResume();
            holder.mapView.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(final GoogleMap googleMap) {

                    //Initialize map
                    MapsInitializer.initialize(mActivity);
                    holder.googleMap = googleMap;
                    holder.googleMap.getUiSettings().setMapToolbarEnabled(false);

                    //Define icon settings
                    IconGenerator iconFactory = new IconGenerator(mActivity);
                    iconFactory.setColor(Color.parseColor(tracker.getBackgroundColor()));
                    iconFactory.setTextAppearance(R.style.Marker);

                    //Get central coordinates
                    LatLng coordinates = new LatLng(tracker.getLastCoordinate().getLatitude(), tracker.getLastCoordinate().getLongitude());

                    //Set camera position
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinates, 14));

                    //If coordinates come from a GSM tower cell
                    if(tracker.getLastCoordinateType() != null && tracker.getLastCoordinateType().equals("GSM") && sharedPreferences.getBoolean("ShowCircle", true))
                    {
                        //Add circle representing cell tower signal coverage
                        googleMap.addCircle(new CircleOptions()
                                .center(coordinates)
                                .radius(500)
                                .strokeWidth(mActivity.getResources().getDimensionPixelSize(R.dimen.map_circle_width))
                                .strokeColor(Color.parseColor("#88" + tracker.getBackgroundColor().substring(3)))
                                .fillColor(Color.parseColor("#55" + tracker.getBackgroundColor().substring(3))));

                        //Change marker color to be not transparent
                        iconFactory.setColor(Color.parseColor("#" + tracker.getBackgroundColor().substring(3)));
                    }

                    //Define map marker settings
                    MarkerOptions markerOptions = new MarkerOptions().
                            icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(formatTime(tracker.getLastUpdate())))).
                            position(coordinates).
                            anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV());

                    //Add marker on map
                    googleMap.addMarker(markerOptions);

                    //Set map style
                    holder.googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

                    //Define method to call after map is loaded
                    holder.googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                        @Override
                        public void onMapLoaded() {

                            //If coordinates is available
                            if (googleMap.getMapType() != GoogleMap.MAP_TYPE_NONE && tracker.getLastCoordinate() != null)
                            {
                                //Hide loading animation
                                ((View) holder.progressBar.getParent()).animate().setDuration(500).alpha(0f);
                            }
                        }
                    });
                }
            });
        }
    }

    public static void manageTracker(final Intent intent, final View rootView)
    {
        //Get Firestore DB instance
        final FirebaseFirestore mFireStoreDB = FirebaseFirestore.getInstance();

        //Load tracker data from previous activity
        final Tracker tracker = intent.getParcelableExtra("Tracker");

        //Check previous activity intent is to delete this tracker
        if (intent.hasExtra("DeleteTracker"))
        {
            //Create transaction batch
            final WriteBatch batch = mFireStoreDB.batch();

            //Delete operation to be performed if user don't cancel
            batch.delete(mFireStoreDB.collection("Tracker").document(tracker.getIdentification()));

            //Inform user that an delete operation is going to happen
            final Snackbar message = Snackbar
                    .make(rootView, "Exclusão em andamento...", Snackbar.LENGTH_LONG);

            //Set available action
            message.setAction("CANCELAR", new View.OnClickListener()
            {
                @Override
                public void onClick(View view) {

                    //Undo tracker delete
                    message.dismiss();
                }
            });

            //Set dismiss event callback
            message.addCallback(new Snackbar.Callback() {

                @Override
                public void onDismissed(Snackbar snackbar, int event) {

                    //If user did not canceled delete operation
                    if(event == DISMISS_EVENT_TIMEOUT)
                    {
                        // Commit the delete operation
                        batch.commit()
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {

                                        //Update notifications with empty intent to remove subscription to topics
                                        TrackerAdapter.updateTrackerNotifications(new Intent(), tracker.getIdentification(), rootView.getContext());

                                        //Show success message
                                        Snackbar.make(rootView, "Exclusão finalizada com sucesso!", Snackbar.LENGTH_LONG).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener()
                                {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                        //Show error message
                                        Snackbar.make(rootView, "Erro durante a exclusão: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                                    }
                                });
                    }
                }
            });

            //Show snack bar
            message.show();
        }
        else if (intent.hasExtra("InsertTracker") || intent.hasExtra("UpdateTracker"))
        {
            // If not DELETE intent, then this is an INSERT/UPDATE database operation
            mFireStoreDB.collection("Tracker").document(tracker.getIdentification())
                    .set(tracker, SetOptions.merge())
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            //Update notification options
                            TrackerAdapter.updateTrackerNotifications(intent, tracker.getIdentification(), rootView.getContext());

                            //Create snack bar to show feed back to user
                            Snackbar.make(rootView, "Rastreador " + (intent.hasExtra("InsertTracker") ? "cadastrado" : "atualizado") +" com sucesso" , Snackbar.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Snackbar.make(rootView, "Erro na operação: " + e.toString(), Snackbar.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private static void updateTrackerNotifications(Intent i, String TrackerID, Context context)
    {
        //Get shared preferences
        SharedPreferences.Editor sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context).edit();

        //Get firebase messaging instance
        FirebaseMessaging notifications = FirebaseMessaging.getInstance();

        //Save options
        updateNotificationOption(sharedPreferences, notifications, i, TrackerID, "NotifyLowBattery");
        updateNotificationOption(sharedPreferences, notifications, i, TrackerID, "NotifyMovement");
        updateNotificationOption(sharedPreferences, notifications, i, TrackerID, "NotifyStopped");
        updateNotificationOption(sharedPreferences, notifications, i, TrackerID, "NotifyStatus");
        updateNotificationOption(sharedPreferences, notifications, i, TrackerID, "NotifyAvailable");
        updateNotificationOption(sharedPreferences, notifications, i, TrackerID, "NotifySMSResponse");

        //Commit changes on shared preferences
        sharedPreferences.apply();
    }

    private static void updateNotificationOption(SharedPreferences.Editor sharedPreferences, FirebaseMessaging notifications, Intent i, String TrackerID, String topic)
    {
        //If user wants to receive this notification
        if(i.getBooleanExtra(topic, false))
        {
            //Subscribe to notification topic
            notifications.subscribeToTopic(TrackerID + "_" + topic);

            //Save option on shared preferences
            sharedPreferences.putBoolean(TrackerID + "_" + topic, true);
        }
        else
        {
            //Unsubscribe to notification topic
            notifications.unsubscribeFromTopic(TrackerID + "_" + topic);

            //Remove option from shared preferences
            sharedPreferences.putBoolean(TrackerID + "_" + topic, false);
        }
    }

    //Recycling GoogleMap for list item
    @Override
    public void onViewRecycled(ViewHolder holder)
    {
        // Cleanup MapView here
        if (holder.googleMap != null)
        {
            holder.googleMap.clear();
            holder.googleMap.setMapType(GoogleMap.MAP_TYPE_NONE);
        }

        //Show loading animation again
        ((View) holder.progressBar.getParent()).animate().setDuration(500).alpha(1f);

        super.onViewRecycled(holder);
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class ViewHolder
            extends RecyclerView.ViewHolder {

        // each data item is just a string in this case
        TextView txtTrackerName, txtTrackerModel, txtLastUpdateValue, txtBatteryLevel, txtSignalLevel;
        ImageView imgEdit;
        CircleImageView imageView;
        ProgressBar progressBar;
        MapView mapView;

        //Google maps object
        GoogleMap googleMap;

        ViewHolder(View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            //Text fields
            txtTrackerName = itemView.findViewById(R.id.lblTrackerName);
            txtTrackerModel = itemView.findViewById(R.id.txtTrackerModel);
            txtLastUpdateValue = itemView.findViewById(R.id.txtLastUpdate);
            txtBatteryLevel = itemView.findViewById(R.id.lblBatteryLevel);
            txtSignalLevel = itemView.findViewById(R.id.lblSignalLevel);

            //Progress bar
            progressBar = itemView.findViewById(R.id.progressBar);

            //Circle image view
            imageView = itemView.findViewById(R.id.imgTracker);

            //Edit image view
            imgEdit = itemView.findViewById(R.id.imgEdit);

            //Google maps view
            mapView = itemView.findViewById(R.id.googleMap);
        }
    }
}
