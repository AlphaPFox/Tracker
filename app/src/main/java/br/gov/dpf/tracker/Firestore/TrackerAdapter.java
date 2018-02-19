package br.gov.dpf.tracker.Firestore;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.Image;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
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
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.maps.android.ui.IconGenerator;

import java.util.Date;
import java.util.Map;

import br.gov.dpf.tracker.Components.CircleProgressBar;
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
        holder.txtTrackerModel.setText(tracker.formatTrackerModel());

        //Set user defined color
        holder.imageView.setCircleBackgroundColor(Color.parseColor(tracker.getBackgroundColor()));

        //Set model item image
        holder.imageView.setImageDrawable(mActivity.getResources().getDrawable(mActivity.getResources().getIdentifier("model_" + tracker.getModel().toLowerCase(), "drawable", mActivity.getPackageName())));

        //Change color to default loading animation
        holder.indeterminateProgress.getIndeterminateDrawable().setColorFilter(holder.imageView.getCircleBackgroundColor(), android.graphics.PorterDuff.Mode.SRC_IN);

        //Disable click on map area (opens google map app)
        holder.mapView.setClickable(false);

        //If tracker has coordinates available
        if (tracker.getLastCoordinate() != null)
        {
            //Load map
            holder.mapView.onCreate(null);
            holder.mapView.onResume();
            holder.mapView.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(final GoogleMap googleMap) {

                    //Get coordinates
                    GeoPoint dbCoordinates = (GeoPoint) tracker.getLastCoordinate().get("location");

                    //Initialize map
                    MapsInitializer.initialize(mActivity);
                    holder.googleMap = googleMap;
                    holder.googleMap.getUiSettings().setMapToolbarEnabled(false);

                    //Define icon settings
                    IconGenerator iconFactory = new IconGenerator(mActivity);
                    iconFactory.setColor(Color.parseColor(tracker.getBackgroundColor()));
                    iconFactory.setTextAppearance(R.style.Marker);

                    //Get central coordinates
                    LatLng coordinates = new LatLng(dbCoordinates.getLatitude(), dbCoordinates.getLongitude());

                    //Set camera position
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinates, 14));

                    //If coordinates come from a GSM tower cell
                    if(tracker.getLastCoordinate().get("type") == "GSM" && sharedPreferences.getBoolean("ShowCircle", true))
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
                            icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(formatTime((Date) tracker.getLastCoordinate().get("datetime"))))).
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
                                ((View) holder.indeterminateProgress.getParent()).animate().setDuration(500).alpha(0f);
                            }
                        }
                    });
                }
            });
        }

        //Get last configuration and last coordinates
        Map<String, Object> configuration = tracker.getLastConfiguration();
        Map<String, Object> coordinates = tracker.getLastCoordinate();

        //Check if last tracker configuration occurred less than 30 minutes ago
        if(configuration != null && (coordinates == null || ((Date)configuration.get("datetime")).getTime() + 300000 > ((Date) coordinates.get("datetime")).getTime()))
        {
            //Hide last coordinate panel to show configuration status
            holder.lastCoordinate.setVisibility(View.GONE);
            holder.lastConfiguration.setVisibility(View.VISIBLE);

            //If configuration is in progress
            if(configuration.get("progress") != null)
            {
                //Get configuration progress
                int progress = Integer.valueOf(configuration.get("progress").toString());

                //Check if configuration started
                if(progress == 0)
                {
                    //If configuration still at 0% set color to red
                    holder.txtProgress.setTextColor(Color.RED);

                    //Show circle progress bar to indicate configuration progress
                    holder.indeterminateProgress.setVisibility(View.VISIBLE);
                    holder.circleProgressBar.setVisibility(View.GONE);

                    //Show configuration progress (%)
                    holder.txtProgress.setText(mActivity.getResources().getString(R.string.txtConfigurating, progress));
                }
                else if(progress < 100)
                {
                    //Configuration stared, show progress in green color
                    holder.txtProgress.setTextColor(Color.parseColor("#3f9d2c"));

                    //Show circle progress bar to indicate configuration progress
                    holder.indeterminateProgress.setVisibility(View.GONE);
                    holder.circleProgressBar.setVisibility(View.VISIBLE);
                    holder.circleProgressBar.setProgress(progress);

                    //Change color to loading animation
                    holder.circleProgressBar.setColor(holder.imageView.getCircleBackgroundColor());

                    //Show configuration progress (%)
                    holder.txtProgress.setText(mActivity.getResources().getString(R.string.txtConfigurating, progress));
                }
                else
                {
                    //Configuration finished, clear text from progress indicator
                    holder.txtProgress.setText("");

                    //Show circle progress bar to indicate configuration progress
                    holder.indeterminateProgress.setVisibility(View.VISIBLE);
                    holder.circleProgressBar.setVisibility(View.GONE);
                }
            }

            //Set configuration status text
            holder.txtConfigDescription.setText(configuration.get("description").toString());
            holder.txtStatus.setText(configuration.get("status").toString());

            //Select image to represent configuration status
            switch (configuration.get("step").toString())
            {
                case "ERROR":
                    //Configuration error
                    holder.imgStatus.setImageResource(R.drawable.status_error);
                    holder.txtProgress.setTextColor(Color.RED);
                    holder.imgStatus.clearAnimation();
                    break;

                case "SUCCESS":
                    //Configuration success
                    holder.imgStatus.setImageResource(R.drawable.status_ok);
                    holder.imgStatus.clearAnimation();
                    break;

                case "CANCELED":
                    //Configuration success
                    holder.imgStatus.setImageResource(R.drawable.status_warning);
                    holder.imgStatus.clearAnimation();
                    break;

                default:
                    //Configuration in progress, create loading animation
                    RotateAnimation rotate = new RotateAnimation(
                            0, 360,
                            Animation.RELATIVE_TO_SELF, 0.5f,
                            Animation.RELATIVE_TO_SELF, 0.5f
                    );

                    //Define animation settings
                    rotate.setDuration(3000);
                    rotate.setRepeatCount(Animation.INFINITE);
                    holder.imgStatus.startAnimation(rotate);

                    //Set loading image
                    holder.imgStatus.setImageResource(R.drawable.ic_settings_grey_40dp);
                    break;
            }
        }
        else
        {
            //Show panel to indicate last coordinate available
            holder.lastCoordinate.setVisibility(View.VISIBLE);
            holder.lastConfiguration.setVisibility(View.GONE);

            //Show last known battery level and signal level
            holder.txtBatteryLevel.setText(tracker.getBatteryLevel());
            holder.txtSignalLevel.setText(tracker.getSignalLevel());

            //If tracker has an coordinate available
            if(tracker.getLastCoordinate() != null)
            {
                //Show last coordinate datetime
                holder.txtLastUpdateValue.setText(formatDateTime((Date) tracker.getLastCoordinate().get("datetime"), false));
            }
            else
            {
                //No data available, show default message
                holder.txtLastUpdateValue.setText(mActivity.getResources().getString(R.string.txtWaitingTitle));
            }
        }


        //Set item click listener
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mActivity != null)
                {
                    //Disable click to avoid double clicking
                    holder.itemView.setClickable(false);

                    //Cal interface method on main activity
                    mActivity.OnTrackerSelected(tracker, holder);
                }
            }
        });

        //Set edit click listener
        holder.imgEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mActivity != null)
                {
                    //Cal interface method on main activity
                    mActivity.OnTrackerEdit(tracker, holder);
                }
            }
        });
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
        ((View) holder.indeterminateProgress.getParent()).animate().setDuration(500).alpha(1f);

        super.onViewRecycled(holder);
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        // each data item is just a string in this case
        View lastCoordinate;
        TextView txtLastUpdateValue, txtBatteryLevel, txtSignalLevel, txtProgress;

        CircleProgressBar circleProgressBar;
        ProgressBar indeterminateProgress;
        MapView mapView;

        //Public layout components (used on detail activity transition)
        public View lastConfiguration;
        public TextView txtTrackerName, txtTrackerModel, txtConfigDescription, txtStatus;
        public CircleImageView imageView;
        public ImageView imgEdit, imgStatus;

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
            txtStatus = itemView.findViewById(R.id.txtStatus);
            txtProgress = itemView.findViewById(R.id.txtProgress);
            txtConfigDescription = itemView.findViewById(R.id.txtConfigDescription);

            //Image views
            imageView = itemView.findViewById(R.id.imgTracker);
            imgStatus = itemView.findViewById(R.id.imgStatus);
            imgEdit = itemView.findViewById(R.id.imgEdit);

            //Layout panels
            lastConfiguration = itemView.findViewById(R.id.vwConfiguration);
            lastCoordinate = itemView.findViewById(R.id.vwLastCoordinate);

            //Progress bars
            indeterminateProgress = itemView.findViewById(R.id.indeterminateProgress);
            circleProgressBar = itemView.findViewById(R.id.circleProgressBar);

            //Google maps view
            mapView = itemView.findViewById(R.id.googleMap);
        }
    }
}
