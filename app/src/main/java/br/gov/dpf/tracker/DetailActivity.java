package br.gov.dpf.tracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.maps.android.ui.IconGenerator;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import br.gov.dpf.tracker.Components.ImageDownloader;
import br.gov.dpf.tracker.Entities.Coordinates;
import br.gov.dpf.tracker.Firestore.CoordinatesAdapter;
import de.hdodenhof.circleimageview.CircleImageView;

public class DetailActivity
        extends AppCompatActivity
        implements OnMapReadyCallback, CoordinatesAdapter.OnCoordinatesSelectedListener {


    private SharedPreferences sharedPreferences;
    private SlidingUpPanelLayout mLayout;

    private GoogleMap mMap;

    private LinearLayout mLoadingBackground;
    private RecyclerView mRecyclerView;

    private FirebaseFirestore mFireStoreDB;
    private CoordinatesAdapter mAdapter;
    private Query mQuery;

    private List<Marker> markers;

    @Override
    protected void onStart() {
        super.onStart();
        mAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAdapter.stopListening();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        final Toolbar mToolbar = findViewById(R.id.main_toolbar);
        Intent intent = getIntent();

        setSupportActionBar(mToolbar);

        getSupportActionBar().setDisplayShowTitleEnabled(false);

        //Get shared preferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //Get sliding layout
        mLayout = findViewById(R.id.sliding_layout);

        //Initialize db instance
        mFireStoreDB = FirebaseFirestore.getInstance();

        //Define db search query
        buildQuery(intent);

        //if extra is not null
        if (intent.hasExtra("DetailActivity_TrackerID"))
        {
            CircleImageView imgToolbarIcon = mToolbar.findViewById(R.id.imgToolbarIcon);
            TextView txtToolbarTitle = mToolbar.findViewById(R.id.txtToolbarTitle);
            TextView txtToolbarSubtitle = mToolbar.findViewById(R.id.txtToolbarSubtitle);
            ProgressBar progressBar = findViewById(R.id.progressBar);
            mLoadingBackground = findViewById(R.id.loadingBackground);

            imgToolbarIcon.setCircleBackgroundColor(Color.parseColor(intent.getStringExtra("DetailActivity_TrackerColor")));

            //Set model item image
            ImageDownloader modelIcon = new ImageDownloader(imgToolbarIcon, intent.getStringExtra("DetailActivity_TrackerModel"));

            //Execute image search from disk or URL
            modelIcon.execute();

            //Change loading color
            progressBar.getIndeterminateDrawable().setColorFilter(imgToolbarIcon.getCircleBackgroundColor(), android.graphics.PorterDuff.Mode.SRC_IN);

            //Set toolbar texts
            txtToolbarTitle.setText(intent.getStringExtra("DetailActivity_TrackerName"));
            txtToolbarSubtitle.setText(intent.getStringExtra("DetailActivity_TrackerModel"));

            //Check if device supports shared element transition
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            {
                //Check if loading animation was visible on previous activity (MainActivity)
                if(intent.getIntExtra("DetailActivity_BackgroundTransition", R.id.loadingBackground) == R.id.loadingBackground)
                {
                    //Perform transition with loading animation on this activity
                    mLoadingBackground.setTransitionName(getString(R.string.transition_drawable));
                }
                else
                {
                    //Else, hide loading animation
                    mLoadingBackground.setVisibility(View.GONE);

                    //Perform transition with map component
                    findViewById(R.id.map).setTransitionName(getString(R.string.transition_drawable));
                }

                //Components in toolbar for transition
                imgToolbarIcon.setTransitionName(getString(R.string.transition_icon));
                txtToolbarTitle.setTransitionName(getString(R.string.transition_title));
                txtToolbarSubtitle.setTransitionName(getString(R.string.transition_subtitle));
            }
        }

        // Load coordinates data
        loadDataset();

        // Lookup the recycler view in activity layout
        mRecyclerView = findViewById(R.id.DetailRecycler);

        // Attach the adapter to the recycler view to populate items
        mRecyclerView.setAdapter(mAdapter);

        // Set linear layout for orientation
        LinearLayoutManager layoutManager = new LinearLayoutManager(mRecyclerView.getContext());

        // Set layout manager to position the items
        mRecyclerView.setLayoutManager(layoutManager);

        // Set item divider settings
        mRecyclerView.addItemDecoration(new DividerItemDecoration(mRecyclerView.getContext(), layoutManager.getOrientation()));

        // Initialize google map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    //Initialize FireStore data adapter
    public void loadDataset()
    {
        // RecyclerView
        mAdapter = new CoordinatesAdapter(this, mQuery)
        {
            @Override
            protected void onDataChanged()
            {
                //Get how many items in recycler view
                if(getItemCount() == 0)
                {
                    //No items to display, hide recycler view
                    mRecyclerView.setVisibility(View.GONE);

                    //Show empty message layout
                    findViewById(R.id.vwEmptyRecycler).setVisibility(View.VISIBLE);

                    //Set corresponding panel height
                    mLayout.setPanelHeight(getResources().getDimensionPixelSize(R.dimen.panel_empty_height));
                }
                else
                {
                    //Items available to display, show recycler view
                    mRecyclerView.setVisibility(View.VISIBLE);

                    //Hide empty message
                    findViewById(R.id.vwEmptyRecycler).setVisibility(View.GONE);

                    //Check how many items available
                    if(getItemCount() == 1)
                    {
                        //If only one item, set panel height to show it
                        mLayout.setPanelHeight(getResources().getDimensionPixelSize(R.dimen.panel_single_row_height));
                    }
                    else
                    {
                        //Else, show the first two items
                        mLayout.setPanelHeight(getResources().getDimensionPixelSize(R.dimen.panel_multiple_row_height));
                    }

                    //Check if map is already loaded
                    if(mMap != null)
                    {

                        //Clear any existing data from map
                        mMap.clear();
                    }

                    //Initialize markers array list
                    markers = new ArrayList<>();

                    //Define marker factory settings
                    IconGenerator iconFactory = new IconGenerator(getApplicationContext());
                    iconFactory.setTextAppearance(R.style.Marker);

                    //For each coordinate
                    for(DocumentSnapshot snapshot : mSnapshots)
                    {
                        //Decode data
                        Coordinates coordinates = snapshot.toObject(Coordinates.class);

                        //Get coordinates
                        LatLng position = new LatLng(coordinates.getPosition().getLatitude(), coordinates.getPosition().getLongitude());

                        //If coordinates contains tower cell data (GSM location) and user selected display option
                        if (coordinates.getCellID() != null && sharedPreferences.getBoolean("ShowCircle", true))
                        {
                            //Check if tower cell circle is already added in this position
                            boolean is_added = false;

                            //For each circle already added
                            for(Marker marker : markers)
                            {
                                //Check if marker is tagged (means it has a circle around)
                                if(marker.getTag() != null && marker.getPosition().equals(position))

                                    //Already drawn a circle in this position
                                    is_added = true;
                            }

                            //Change marker color (remove transparency)
                            iconFactory.setColor(Color.parseColor("#" + getIntent().getStringExtra("DetailActivity_TrackerColor").substring(3)));

                            //If circle is not added on this position yet
                            if(!is_added)
                            {
                                //Add circle representing cell tower coverage area
                                mMap.addCircle(new CircleOptions()
                                        .center(position)
                                        .radius(500)
                                        .strokeWidth(getResources().getDimensionPixelSize(R.dimen.map_circle_width))
                                        .strokeColor(Color.parseColor("#88" + getIntent().getStringExtra("DetailActivity_TrackerColor").substring(3)))
                                        .fillColor(Color.parseColor("#55" + getIntent().getStringExtra("DetailActivity_TrackerColor").substring(3))));

                                //Define marker settings
                                MarkerOptions markerOptions = new MarkerOptions()
                                        .icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(String.valueOf(mSnapshots.size() - markers.size()))))
                                        .position(position)
                                        .zIndex(mSnapshots.size() - markers.size())
                                        .anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV());

                                //Add marker on map
                                Marker marker = mMap.addMarker(markerOptions);

                                //Add marker tag
                                marker.setTag("GSM Marker");

                                //Add marker on markers list
                                markers.add(marker);
                            }
                            else
                            {
                                //Add marker on map
                                markers.add(mMap.addMarker(new MarkerOptions()
                                        .icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(String.valueOf(mSnapshots.size() - markers.size()))))
                                        .position(position)
                                        .zIndex(mSnapshots.size() - markers.size())
                                        .anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV())));
                            }
                        }
                        else
                        {
                            //Set default color
                            iconFactory.setColor(Color.parseColor(getIntent().getStringExtra("DetailActivity_TrackerColor")));

                            //Add marker on map
                            markers.add(mMap.addMarker(new MarkerOptions()
                                    .icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(String.valueOf(mSnapshots.size() - markers.size()))))
                                    .position(position)
                                    .zIndex(mSnapshots.size() - markers.size())
                                    .anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV())));
                        }
                    }



                    //Center map on most recent available position
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markers.get(0).getPosition(), 14));

                    //Check if user selected option to display polyline
                    if(sharedPreferences.getBoolean("ShowPolyline", true))
                    {
                        // Add a polyline to the map.
                        PolylineOptions options = new PolylineOptions()
                                .width(getResources().getDimensionPixelSize(R.dimen.map_polyline_width))
                                .color(Color.parseColor(getIntent().getStringExtra("DetailActivity_TrackerColor")))
                                .pattern(Arrays.asList(new Dash(getResources().getDimensionPixelSize(R.dimen.map_polyline_dash)), new Gap(getResources().getDimensionPixelSize(R.dimen.map_polyline_gap))))
                                .geodesic(true);

                        // For each marker on map
                        for (Marker marker : markers)

                            //Add way point to path
                            options.add(marker.getPosition());

                        //Add polyline to map
                        mMap.addPolyline(options);
                    }

                    //Define map loaded callback
                    mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                        @Override
                        public void onMapLoaded()
                        {
                            //Hide loading animation
                            mLoadingBackground.animate().setDuration(500).alpha(0f);
                        }
                    });
                }
            }

            @Override
            protected void onError(FirebaseFirestoreException e) {

                // Show a snack bar on errors
                Snackbar.make(findViewById(android.R.id.content),
                        "Error: check logs for info.", Snackbar.LENGTH_LONG).show();
            }
        };
    }
    @Override
    public void OnCoordinatesSelected(DocumentSnapshot coordinates, View viewRoot) {

        // Go to the details page for the selected restaurant
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(markers.get(mAdapter.mSnapshots.indexOf(coordinates)).getPosition(), 14));

        // Reset z-index for each marker
        for(int i = 0; i < markers.size(); i++)
            markers.get(i).setZIndex(markers.size() - i);

        // Set higher Z-index to this current marker be visible above others
        markers.get(mAdapter.mSnapshots.indexOf(coordinates)).setZIndex(1000);

        // Return panel to collapsed state
        mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
    }


    @Override
    protected void onActivityResult (int requestCode, int resultCode, final Intent intent)
    {
        // Collect data from the intent and use it
        if (resultCode == RESULT_OK)
        {
            //Rebuild query using filter settings
            buildQuery(intent);

            //Check if map is already loaded
            if(mMap != null)
            {
                //Clear any existing data from map
                mMap.clear();
            }

            //Inform new query to adapter
            mAdapter.setQuery(mQuery);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.detail, menu);
        MenuItem item = menu.findItem(R.id.action_toggle);
        if (mLayout != null) {
            if (mLayout.getPanelState() == PanelState.HIDDEN) {
                item.setTitle(R.string.action_show);
            } else {
                item.setTitle(R.string.action_hide);
            }
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_toggle:
            {
                if (mLayout != null)
                {
                    if (mLayout.getPanelState() != PanelState.HIDDEN)
                    {
                        mLayout.setPanelState(PanelState.HIDDEN);
                        item.setTitle(R.string.action_show);
                    }
                    else
                    {
                        mLayout.setPanelState(PanelState.COLLAPSED);
                        item.setTitle(R.string.action_hide);
                    }
                }
                return true;
            }
            case R.id.action_edit:
            {

            }
            case R.id.action_filter:
            {
                Intent intent = getIntent();
                intent.setClass(this, FilterActivity.class);
                startActivityForResult(intent, 0);

                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mLayout != null &&
                (mLayout.getPanelState() == PanelState.EXPANDED)) {
            mLayout.setPanelState(PanelState.COLLAPSED);
        } else {
            setResult(RESULT_OK, getIntent());
            finish();
        }
    }

    public void buildQuery(Intent intent)
    {
        //Get shared preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //Get data limit
        int limit = sharedPreferences.getInt("CoordinatesNumber", 5);

        //Check if intent has date filter
        if(intent.hasExtra("StartDate") && intent.hasExtra("EndDate"))
        {
            //Parse datetime
            Date startDate = parseDateTime(intent.getStringExtra("StartDate") + " 00:00");
            Date endDate = parseDateTime(intent.getStringExtra("EndDate") + " 23:59");

            //Save intent
            setIntent(intent);

            //Define date relative search query
            mQuery = mFireStoreDB.collection("Tracker/" + intent.getStringExtra("DetailActivity_TrackerID") + "/Coordinates")
                    .whereGreaterThan("datetime", startDate)
                    .whereLessThan("datetime", endDate)
                    .orderBy("datetime", Query.Direction.DESCENDING)
                    .limit(limit);

            //Change text from empty view
            ((TextView) findViewById(R.id.txtWaitingTitle)).setText(getResources().getString(R.string.txtFilterResult));
            ((TextView) findViewById(R.id.txtWaitingSubtitle)).setText(getResources().getString(R.string.txtFilterResultSubtitle));
        }
        else
        {
            //Define search query
            mQuery = mFireStoreDB.collection("Tracker/" + intent.getStringExtra("DetailActivity_TrackerID") + "/Coordinates")
                    .orderBy("datetime", Query.Direction.DESCENDING)
                    .limit(limit);
        }
    }

    public Date parseDateTime(String datetime)
    {
        //Create datetime format
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        try
        {
            //Return formatted text
            return format.parse(datetime);
        }
        catch (Exception e)
        {
            //Error formatting, return nothing
            return null;
        }
    }
}
