package br.gov.dpf.tracker.Firestore;

import android.content.res.Resources;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.Query;
import com.google.maps.android.ui.IconGenerator;

import br.gov.dpf.tracker.Entities.Tracker;
import br.gov.dpf.tracker.MainActivity;
import br.gov.dpf.tracker.R;
import de.hdodenhof.circleimageview.CircleImageView;

public class TrackerAdapter
        extends BaseAdapter<TrackerAdapter.ViewHolder>  {

    //Define interface to event listener
    public interface OnTrackerSelectedListener {

        //Fired when user click on a recycler view item
        void OnTrackerSelected(String tracker_id, Tracker tracker, View viewRoot);

    }

    //Display size info
    private DisplayMetrics mMetrics;

    //Linked activity
    private MainActivity mActivity;

    //Constructor
    public TrackerAdapter(MainActivity activity, Query query) {
        super(activity, query);

        mActivity = activity;
        mMetrics = Resources.getSystem().getDisplayMetrics();
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
        final String trackerID = getSnapshot(position).getId();

        // - replace the contents of the view with that element
        holder.txtTrackerName.setText(mActivity.getString(R.string.TrackerLabel, tracker.getName()));
        holder.txtTrackerModel.setText(tracker.getModel());
        holder.txtBatteryLevel.setText(tracker.getBatteryLevel());
        holder.txtSignalLevel.setText(tracker.getSignalLevel());
        holder.txtLastUpdateValue.setText(formatDateTime(tracker.getLastUpdate()));

        //Set user defined color
        holder.imageView.setCircleBackgroundColor(Color.parseColor(tracker.getBackgroundColor()));
        holder.progressBar.getIndeterminateDrawable().setColorFilter(holder.imageView.getCircleBackgroundColor(), android.graphics.PorterDuff.Mode.SRC_IN);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mActivity != null) {
                    holder.itemView.setClickable(false);
                    mActivity.OnTrackerSelected(trackerID, tracker, holder.itemView);
                }
            }
        });

        holder.mapView.setClickable(false);

        if (tracker.getLastCoordinate() != null && tracker.getLastUpdate() != null)
        {

            holder.mapView.onCreate(null);
            holder.mapView.onResume();
            holder.mapView.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(final GoogleMap googleMap) {


                    MapsInitializer.initialize(mActivity);
                    holder.googleMap = googleMap;
                    holder.googleMap.getUiSettings().setMapToolbarEnabled(false);
                    IconGenerator iconFactory = new IconGenerator(mActivity);
                    iconFactory.setColor(Color.parseColor(tracker.getBackgroundColor()));
                    iconFactory.setTextAppearance(R.style.Marker);

                    LatLng coordinates = new LatLng(tracker.getLastCoordinate().getLatitude(), tracker.getLastCoordinate().getLongitude());
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinates, 14));

                    MarkerOptions markerOptions = new MarkerOptions().
                            icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(formatTime(tracker.getLastUpdate())))).
                            position(coordinates).
                            anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV());

                    googleMap.addMarker(markerOptions);

                    holder.googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

                    holder.googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                        @Override
                        public void onMapLoaded() {
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
    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        // each data item is just a string in this case
        TextView txtTrackerName, txtTrackerModel, txtLastUpdateValue, txtBatteryLevel, txtSignalLevel;
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
            txtBatteryLevel = itemView.findViewById(R.id.txtBatteryLevel);
            txtSignalLevel = itemView.findViewById(R.id.txtSignalLevel);

            //Progress bar
            progressBar = itemView.findViewById(R.id.progressBar);

            //CardView view
            imageView = itemView.findViewById(R.id.imgTracker);

            //Google maps view
            mapView = itemView.findViewById(R.id.googleMap);
        }
    }
}
