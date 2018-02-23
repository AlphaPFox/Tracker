package br.gov.dpf.tracker;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannedString;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.config.GoogleDirectionConfiguration;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.model.Route;
import com.akexorcist.googledirection.model.Step;
import com.akexorcist.googledirection.request.DirectionOriginRequest;
import com.akexorcist.googledirection.request.DirectionRequest;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.appolica.interactiveinfowindow.InfoWindow;
import com.appolica.interactiveinfowindow.InfoWindowManager;
import com.appolica.interactiveinfowindow.fragment.MapInfoWindowFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.google.maps.android.ui.IconGenerator;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import br.gov.dpf.tracker.Components.CircleProgressBar;
import br.gov.dpf.tracker.Components.InfoFragment;
import br.gov.dpf.tracker.Components.ProgressNotification;
import br.gov.dpf.tracker.Entities.Configuration;
import br.gov.dpf.tracker.Entities.Coordinates;
import br.gov.dpf.tracker.Entities.Tracker;
import br.gov.dpf.tracker.Firestore.CoordinatesAdapter;
import de.hdodenhof.circleimageview.CircleImageView;

import static br.gov.dpf.tracker.MainActivity.REQUEST_UPDATE;

public class DetailActivity
        extends AppCompatActivity
        implements
        OnMapReadyCallback,
        DirectionCallback,
        GoogleMap.OnMarkerClickListener,
        EventListener<DocumentSnapshot>,
        NavigationView.OnNavigationItemSelectedListener {

    //Activities request types (RegisterActivity or FilterActivity)
    final static int REQUEST_FILTER = 1;
    final static int REQUEST_PERMISSION = 2;

    //Object containing user preferences
    private SharedPreferences sharedPreferences;

    //Layout components
    private SlidingUpPanelLayout mLayout;
    private InfoWindowManager infoWindowManager;
    private LinearLayout mLoadingBackground;
    private RecyclerView mRecyclerView;
    private CircleProgressBar circleProgressBar;
    private ProgressBar indeterminateProgressBar;
    private DisplayMetrics mMetrics;

    //Menu item used to refresh settings
    private MenuItem refreshMenu;

    //Bottom panel layout components
    private TextView txtConfigDescription, txtConfigStatus;
    private View vwEmptyPanel, vwConfigPanel;
    private ImageView imgConfigStatus;

    //Database components
    private FirebaseFirestore mFireStoreDB;
    private CoordinatesAdapter mCoordinatesAdapter;
    private ListenerRegistration listener;
    private Query mQuery;

    //Object containing tracker data
    public Tracker tracker;

    //Boolean flag to indicate configuration process
    private boolean configPending;

    //Google maps components
    private GoogleMap mMap;
    private List<Marker> markers;

    @Override
    protected void onStart() {
        super.onStart();
        mCoordinatesAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCoordinatesAdapter.stopListening();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Set activity layout
        setContentView(R.layout.activity_detail);

        //Get intent from previous activity
        Intent intent = getIntent();

        //Get shared preferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //Initialize db instance
        mFireStoreDB = FirebaseFirestore.getInstance();

        //Get display metrics
        mMetrics = Resources.getSystem().getDisplayMetrics();

        //Get tracker data from intent
        tracker = intent.getParcelableExtra("Tracker");

        //Get last configuration and last coordinate data from this tracker
        Map<String, Object> configuration = tracker.getLastConfiguration();
        Map<String, Object> coordinates = tracker.getLastCoordinate();

        //Check if last configuration update is more recent than last coordinate available
        configPending = configuration != null && (coordinates == null || ((Date) configuration.get("datetime")).getTime() + 300000 > ((Date) coordinates.get("datetime")).getTime());

        // Load layout elements using tracker data
        loadLayout(intent);

        // Load coordinates data
        loadDataset();

        // Initialize google map
        MapInfoWindowFragment mapFragment = (MapInfoWindowFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Initialize info window manager
        infoWindowManager = mapFragment.infoWindowManager();
        infoWindowManager.setHideOnFling(true);

        //If tracker has any configuration pending
        if (configPending && configuration != null) {
            //Call method to show update progress
            monitorConfiguration(configuration.get("step").toString(), configuration.get("description").toString(), configuration.get("status").toString());
        }
    }

    public void loadLayout(Intent intent) {
        //Get sliding layout
        mLayout = findViewById(R.id.sliding_layout);

        //Set toolbar element
        final Toolbar mToolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(mToolbar);

        //Check if action bar is enabled
        if (getSupportActionBar() != null) {
            //Disable title
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        //Set drawer layout components
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, drawer, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        //Set drawer toggle
        drawer.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        //Initialize navigation view
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //Update navigation view menu
        updateNavigationMenu(navigationView);

        //Get navigation view header
        View header = navigationView.getHeaderView(0);

        //Update layout color
        header.getBackground().setColorFilter(Color.parseColor(tracker.getBackgroundColor()), android.graphics.PorterDuff.Mode.SRC_IN);

        //Update header text fields
        ((TextView) header.findViewById(R.id.txtName)).setText(tracker.formatName());
        ((TextView) header.findViewById(R.id.txtModel)).setText(Html.fromHtml(String.format(Html.toHtml(new SpannedString(getText(R.string.txtModel))), tracker.formatTrackerModel())));

        //Update header image
        ((CircleImageView) header.findViewById(R.id.imgModel)).setBorderColor(Color.parseColor("#" + tracker.getBackgroundColor().substring(3)));
        ((CircleImageView) header.findViewById(R.id.imgModel)).setImageDrawable(getResources().getDrawable(getResources().getIdentifier("model_" + tracker.getModel().toLowerCase(), "drawable", getPackageName())));

        //Define db search query
        buildQuery(intent);

        //Get layout elements
        CircleImageView imgToolbarIcon = mToolbar.findViewById(R.id.imgModel);
        TextView txtToolbarTitle = mToolbar.findViewById(R.id.txtToolbarTitle);
        TextView txtToolbarSubtitle = mToolbar.findViewById(R.id.txtToolbarSubtitle);
        indeterminateProgressBar = findViewById(R.id.progressBar);
        mLoadingBackground = findViewById(R.id.loadingBackground);
        circleProgressBar = findViewById(R.id.circleProgressBar);
        txtConfigDescription = findViewById(R.id.txtConfigDescription);
        txtConfigStatus = findViewById(R.id.txtConfigStatus);
        imgConfigStatus = findViewById(R.id.imgStatus);
        vwEmptyPanel = findViewById(R.id.vwBottomPanel);
        vwConfigPanel = findViewById(R.id.vwConfigPanel);

        // Lookup the recycler view in activity layout
        mRecyclerView = findViewById(R.id.DetailRecycler);

        //Set circle image background color
        imgToolbarIcon.setCircleBackgroundColor(Color.parseColor(tracker.getBackgroundColor()));

        //Set model item image
        imgToolbarIcon.setImageDrawable(getResources().getDrawable(getResources().getIdentifier("model_" + tracker.getModel().toLowerCase(), "drawable", getPackageName())));

        //Change loading color
        indeterminateProgressBar.getIndeterminateDrawable().setColorFilter(imgToolbarIcon.getCircleBackgroundColor(), android.graphics.PorterDuff.Mode.SRC_IN);
        circleProgressBar.setColor(imgToolbarIcon.getCircleBackgroundColor());

        //Set toolbar texts
        txtToolbarTitle.setText(tracker.formatName());
        txtToolbarSubtitle.setText(tracker.formatTrackerModel());

        //Check if device supports shared element transition
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //Check if loading animation was visible on previous activity (MainActivity)
            if (intent.getIntExtra("DetailActivity_BackgroundTransition", R.id.loadingBackground) == R.id.loadingBackground) {
                //Perform transition with loading animation on this activity
                mLoadingBackground.setTransitionName(getString(R.string.transition_drawable));
            } else {
                //Else, hide loading animation
                mLoadingBackground.setVisibility(View.GONE);

                //Perform transition with map component
                findViewById(R.id.map).setTransitionName(getString(R.string.transition_drawable));
            }

            //If configuration pending on this tracker
            if (configPending) {
                //Set transition for configuration layout components
                txtConfigDescription.setTransitionName(getString(R.string.transition_config_description));
                txtConfigStatus.setTransitionName(getString(R.string.transition_config_status));
                imgConfigStatus.setTransitionName(getString(R.string.transition_config_image));
            }

            //Components in toolbar for transition
            imgToolbarIcon.setTransitionName(getString(R.string.transition_icon));
            txtToolbarTitle.setTransitionName(getString(R.string.transition_title));
            txtToolbarSubtitle.setTransitionName(getString(R.string.transition_subtitle));
        }
    }

    private void updateNavigationMenu(NavigationView navigationView) {
        //For each tracker model
        switch (tracker.getModel()) {
            case "tk102b":
                //Show location and status commands
                navigationView.getMenu().findItem(R.id.menu_request_position).setVisible(true);
                navigationView.getMenu().findItem(R.id.menu_request_status).setVisible(true);
                break;

            case "st940":
                //Show location and turn off commands
                navigationView.getMenu().findItem(R.id.menu_request_position).setVisible(true);
                navigationView.getMenu().findItem(R.id.menu_turn_off).setVisible(true);
                break;

            case "spot":
                //This tracker does not support commands
                navigationView.getMenu().findItem(R.id.menu_tracker_commands).setVisible(false);
                break;
        }

        //Select user preferred map type
        switch (sharedPreferences.getInt("UserMapType", GoogleMap.MAP_TYPE_NORMAL)) {
            case GoogleMap.MAP_TYPE_NORMAL:
                navigationView.setCheckedItem(R.id.map_default);
                break;
            case GoogleMap.MAP_TYPE_SATELLITE:
                navigationView.setCheckedItem(R.id.map_satellite);
                break;
            case GoogleMap.MAP_TYPE_TERRAIN:
                navigationView.setCheckedItem(R.id.map_terrain);
                break;
            case GoogleMap.MAP_TYPE_HYBRID:
                navigationView.setCheckedItem(R.id.map_hybrid);
                break;
        }
    }

    public void monitorConfiguration(String step, String description, String status) {
        //Get tracker document reference
        DocumentReference trackerRef = FirebaseFirestore.getInstance().document("Tracker/" + tracker.getIdentification());

        //Update message
        txtConfigDescription.setText(description);
        txtConfigStatus.setText(status);

        //Check configuration progress
        switch (step) {
            case "ERROR":
                //Configuration error
                imgConfigStatus.setImageResource(R.drawable.status_error);
                imgConfigStatus.clearAnimation();
                break;

            case "SUCCESS":
                //Configuration success
                imgConfigStatus.setImageResource(R.drawable.status_ok);
                imgConfigStatus.clearAnimation();
                break;

            default:

                //Listen for changes on tracker document
                listener = trackerRef.addSnapshotListener(this);

                //Configuration in progress, create loading animation
                RotateAnimation rotate = new RotateAnimation(
                        0, 360,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f
                );

                //Define animation settings
                rotate.setDuration(3000);
                rotate.setRepeatCount(Animation.INFINITE);
                imgConfigStatus.startAnimation(rotate);
                //imgConfigStatus.setColorFilter(Color.parseColor(tracker.getBackgroundColor()), android.graphics.PorterDuff.Mode.SRC_IN);

                //Set loading image
                imgConfigStatus.setImageResource(R.drawable.ic_settings_grey_40dp);
                break;
        }
    }

    @Override
    public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
        //Check if tracker is available
        if (documentSnapshot.exists()) {
            //Parse DB result to tracker object
            tracker = documentSnapshot.toObject(Tracker.class);

            //Try to get configuration status
            Map<String, Object> configuration = tracker.getLastConfiguration();

            //Check if configuration has not started yet
            if (configuration != null) {
                int progress = Integer.parseInt(configuration.get("progress").toString());

                //Check if progress started
                if (progress > 0 && progress < 100) {
                    //Hide indeterminate progress bar
                    indeterminateProgressBar.setVisibility(View.GONE);

                    //Show circle progress bar
                    circleProgressBar.setVisibility(View.VISIBLE);

                    //Set configuration progress
                    circleProgressBar.setProgressWithAnimation(progress, 5000);

                    //Set notification title
                    txtConfigDescription.setText(String.format(Locale.getDefault(), "%s (%d%%)", configuration.get("description").toString(), progress));
                } else {
                    //Show indeterminate progress bar
                    indeterminateProgressBar.setVisibility(View.VISIBLE);

                    //Hide circle progress bar
                    circleProgressBar.setVisibility(View.GONE);

                    //Set notification title
                    txtConfigDescription.setText(configuration.get("description").toString());
                }

                //Set description
                txtConfigStatus.setText(configuration.get("status").toString());

                //If configuration finished (no longer pending)
                if (!configuration.get("step").equals("PENDING")) {
                    //Remove listener
                    listener.remove();

                    //Select image to represent configuration status
                    switch (configuration.get("step").toString()) {
                        case "ERROR":
                            //Configuration error
                            imgConfigStatus.setImageResource(R.drawable.status_error);
                            imgConfigStatus.clearAnimation();
                            break;

                        case "SUCCESS":
                            //Configuration success
                            imgConfigStatus.setImageResource(R.drawable.status_ok);
                            imgConfigStatus.clearAnimation();
                            break;

                        case "CANCELED":
                            //Configuration success
                            imgConfigStatus.setImageResource(R.drawable.status_warning);
                            imgConfigStatus.clearAnimation();
                            break;
                    }
                }
            }
        }
    }

    //Initialize FireStore data adapter
    public void loadDataset() {
        // RecyclerView
        mCoordinatesAdapter = new CoordinatesAdapter(this, mQuery) {
            @Override
            protected void onDataChanged() {
                //Get how many coordinates are currently available
                int coordinatesCount = getItemCount();

                //Load bottom panel layout
                loadBottomPanel(coordinatesCount);

                //If any coordinates available
                if (coordinatesCount > 0) {
                    //Load map data
                    loadMapComponents(mSnapshots);
                }

                //If loading indicator is available
                if (refreshMenu != null) {
                    //Cancel loading indicator
                    refreshMenu.setActionView(null);
                }
            }

            @Override
            protected void onError(FirebaseFirestoreException e) {

                // Show a snack bar on errors
                Snackbar.make(findViewById(android.R.id.content),
                        "Error: check logs for info.", Snackbar.LENGTH_LONG).show();
            }
        };

        // Attach the adapter to the recycler view to populate items
        mRecyclerView.setAdapter(mCoordinatesAdapter);

        // Set linear layout for orientation
        LinearLayoutManager layoutManager = new LinearLayoutManager(mRecyclerView.getContext());

        // Set layout manager to position the items
        mRecyclerView.setLayoutManager(layoutManager);

        // Set item divider settings
        mRecyclerView.addItemDecoration(new DividerItemDecoration(mRecyclerView.getContext(), layoutManager.getOrientation()));
    }

    public void loadBottomPanel(int coordinatesCount) {
        //If no coordinates available
        if (coordinatesCount == 0) {
            //No items to display, hide recycler view
            mRecyclerView.setVisibility(View.GONE);

            //Check if tracker have pending configurations
            if (configPending) {
                //Show config panel, hide empty message panel
                vwConfigPanel.setVisibility(View.VISIBLE);
                vwEmptyPanel.setVisibility(View.GONE);

                //Set corresponding panel height
                mLayout.setPanelHeight(getResources().getDimensionPixelSize(R.dimen.panel_single_row_height));
            } else {
                //Else, show empty message panel
                vwEmptyPanel.setVisibility(View.VISIBLE);
                vwConfigPanel.setVisibility(View.GONE);

                //Set corresponding panel height
                mLayout.setPanelHeight(getResources().getDimensionPixelSize(R.dimen.panel_empty_height));
            }
        } else {
            //Items available to display, show recycler view
            mRecyclerView.setVisibility(View.VISIBLE);

            //Hide bottom panel layout if tracker has no pending configurations
            vwEmptyPanel.setVisibility(View.GONE);

            //Check if only one item available to display (and no pending configuration) or if screen is small
            if (coordinatesCount == 1 || configPending || mMetrics.heightPixels / mMetrics.density < 530) {
                //Set panel height to show only one item
                mLayout.setPanelHeight(getResources().getDimensionPixelSize(R.dimen.panel_single_row_height));
            } else {
                //Else, show the first two items
                mLayout.setPanelHeight(getResources().getDimensionPixelSize(R.dimen.panel_multiple_row_height));
            }

            //Show config panel
            vwConfigPanel.setVisibility(configPending ? View.VISIBLE : View.GONE);
        }
    }

    //Load map markers, lines and other components based on coordinates available
    public void loadMapComponents(ArrayList<DocumentSnapshot> mCoordinateSnapshots) {

        //Check if map is already loaded
        if (mMap != null)
        {
            //Clear any existing data from map
            mMap.clear();

            //Initialize markers array list
            markers = new ArrayList<>();

            //Initialize markers array list
            List<Circle> gsmTowers = new ArrayList<>();

            //Define marker factory settings
            IconGenerator iconFactory = new IconGenerator(getApplicationContext());
            iconFactory.setTextAppearance(R.style.Marker);

            //Get marker offset (info window requirement)
            final InfoWindow.MarkerSpecification markerSpec = new InfoWindow.MarkerSpecification((int) getResources().getDimension(R.dimen.marker_offset_x), (int) getResources().getDimension(R.dimen.marker_offset_y));

            //For each coordinate
            for (DocumentSnapshot snapshot : mCoordinateSnapshots) {
                //Decode data
                Coordinates coordinates = snapshot.toObject(Coordinates.class);

                //Get coordinates
                LatLng position = new LatLng(coordinates.getPosition().getLatitude(), coordinates.getPosition().getLongitude());

                //Set default color
                iconFactory.setColor(Color.parseColor(tracker.getBackgroundColor()));

                //If coordinates contains tower cell data (GSM location) and user selected display option
                if (coordinates.getCellID() != null && sharedPreferences.getInt("Map_Radius", 3) > 0) {
                    //Check if tower cell circle is already added in this position
                    boolean is_added = false;

                    //For each circle already added
                    for (Circle gsmTower : gsmTowers) {
                        //Check if marker is tagged (means it has a circle around)
                        if (gsmTower.getCenter().equals(position))

                            //Already drawn a circle in this position
                            is_added = true;
                    }

                    //If circle is not added on this position yet
                    if (!is_added) {
                        //Add circle representing cell tower coverage area
                        Circle gsmTower = mMap.addCircle(new CircleOptions()
                                .center(position)
                                .strokeWidth(getResources().getDimensionPixelSize(R.dimen.map_circle_width))
                                .strokeColor(Color.parseColor("#88" + tracker.getBackgroundColor().substring(3)))
                                .fillColor(Color.parseColor("#55" + tracker.getBackgroundColor().substring(3))));

                        //Get user option
                        switch (sharedPreferences.getInt("Map_Radius", 3)) {
                            case 1:
                                //Preference: 200m
                                gsmTower.setRadius(200);
                                break;
                            case 2:
                                //Preference: 500m
                                gsmTower.setRadius(500);
                                break;
                            case 3:
                                //Preference: 1km
                                gsmTower.setRadius(1000);
                                break;
                            case 4:
                                //Preference: 2km
                                gsmTower.setRadius(2000);
                                break;
                            case 5:
                                //Preference: 5km
                                gsmTower.setRadius(5000);
                                break;
                        }

                        //Save gsm tower on list (avoid duplicate circles on same position)
                        gsmTowers.add(gsmTower);

                        //Change marker color (remove transparency)
                        iconFactory.setColor(Color.parseColor("#" + tracker.getBackgroundColor().substring(3)));
                    }
                }

                //Add marker on map
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(String.valueOf(mCoordinateSnapshots.size() - markers.size()))))
                        .position(position)
                        .zIndex(mCoordinateSnapshots.size() - markers.size())
                        .anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV()));

                //Create info window fragment
                InfoFragment infoFragment = new InfoFragment();

                //Define arguments to fragment
                Bundle args = new Bundle();

                //Set arguments
                args.putInt("ID", mCoordinateSnapshots.size() - markers.size());
                args.putInt("ItemCount", mCoordinateSnapshots.size());
                args.putString("CellID", coordinates.getCellID());
                args.putString("Address", coordinates.getAddress());
                args.putString("BatteryLevel", coordinates.getStringBatteryLevel());
                args.putString("SignalLevel", coordinates.getStringSignalLevel());
                args.putString("TrackerName", tracker.getName());
                args.putString("TrackerModel", tracker.getModel());
                args.putString("TrackerColor", tracker.getBackgroundColor());

                // Check if coordinate has an updated time
                if (coordinates.getLastDatetime() != null)
                {
                    //Specify period of time
                    args.putString("Datetime", mCoordinatesAdapter.formatDateTime(coordinates.getDatetime(), coordinates.getLastDatetime()));
                }
                else
                {
                    //Single time
                    args.putString("Datetime", mCoordinatesAdapter.formatDateTime(coordinates.getDatetime(), false, false));
                }

                //Add to fragment
                infoFragment.setArguments(args);

                //Add info window to map
                marker.setTag(new InfoWindow(marker, markerSpec, infoFragment));

                //Add marker to list
                markers.add(marker);
            }

            //Center map on most recent available position
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markers.get(0).getPosition(), 14));

            //Get user preference on path between coordinates
            int map_path = sharedPreferences.getInt("Map_Path", 2);

            //Check if user selected option to display polyline
            if (map_path == 1 || map_path == 2)
            {
                // Add a polyline to the map.
                PolylineOptions polylineSettings = new PolylineOptions()
                        .width(getResources().getDimensionPixelSize(R.dimen.map_polyline_width))
                        .color(Color.parseColor(tracker.getBackgroundColor()))
                        .geodesic(true);

                //If option is dashed
                if (map_path == 2) {
                    //Set pattern
                    polylineSettings.pattern(Arrays.asList(new Dash(getResources().getDimensionPixelSize(R.dimen.map_polyline_dash)), new Gap(getResources().getDimensionPixelSize(R.dimen.map_polyline_gap))));
                }

                // For each marker on map
                for (Marker marker : markers)

                    //Add way point to path
                    polylineSettings.add(marker.getPosition());

                //Add polyline to map
                mMap.addPolyline(polylineSettings);
            }
            else if (map_path == 3 && markers.size() > 1)
            {
                //Enable logging
                GoogleDirectionConfiguration.getInstance().setLogEnabled(true);

                //Build way point list
                ArrayList<LatLng> waypoints = new ArrayList<>();

                //Remove first and last points
                for (int i = 1; i < markers.size() - 1; i++)
                {
                    //Add location
                    waypoints.add(markers.get(i).getPosition());
                }

                //Build request
                GoogleDirection.withServerKey(getString(R.string.google_maps_directions_key))
                        .from(markers.get(0).getPosition())
                        .and(waypoints)
                        .to(markers.get(markers.size() - 1).getPosition())
                        .transportMode(TransportMode.DRIVING)
                        .execute(this);
            }

            //Check user preference to display zoom controls
            if(sharedPreferences.getInt("Map_Zoom", 0) > 0)
            {
                //Show controls
                mMap.getUiSettings().setZoomControlsEnabled(true);
            }
            else
            {
                //Hide controls
                mMap.getUiSettings().setZoomControlsEnabled(false);
            }

            //Check user preference to display device location
            if(sharedPreferences.getInt("Map_MyLocation", 0) > 0)
            {
                //Check permission
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                {
                    //User wants to display and granted permission, display device location
                    mMap.setMyLocationEnabled(true);
                    mMap.getUiSettings().setMyLocationButtonEnabled(true);
                }
                else
                {
                    //User wants to display, but no permission, request from user
                    ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION }, REQUEST_PERMISSION);
                }
            }
            else
            {
                //User don't want to show current location
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
            }

            //Define map loaded callback
            mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {

                @Override
                public void onMapLoaded() {

                    //Hide loading animation
                    mLoadingBackground.animate().setDuration(500).alpha(0f);
                }
            });
        }
    }

    @Override
    public void onDirectionSuccess(Direction direction, String rawBody) {
        //Check if direction between coordinates is correctly calculated
        if (direction.isOK()) {
            //Get first route to path
            Route route = direction.getRouteList().get(0);

            //For each leg from this step
            for (int index = 0; index < route.getLegList().size(); index++) {
                //Get list of steps from this leg
                List<Step> stepList = route.getLegList().get(index).getStepList();

                //Create polyline list
                ArrayList<PolylineOptions> polylineOptionList = DirectionConverter.createTransitPolyline(this, stepList, 3, Color.RED, 2, Color.BLUE);

                //For each polyline
                for (PolylineOptions polylineOption : polylineOptionList) {
                    //Add to map
                    mMap.addPolyline(polylineOption);
                }
            }
        } else {
            //Failed to calculate directions
            Snackbar.make(mLayout, "Não foi possível calcular caminho provável", Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDirectionFailure(Throwable t) {
        Snackbar.make(mLayout, "Erro calculando caminho provável: " + t.getMessage(), Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        // Reset z-index for each marker
        for (int i = 0; i < markers.size(); i++)
            markers.get(i).setZIndex(markers.size() - i);

        // Set higher Z-index to this current marker be visible above others
        marker.setZIndex(1000);

        //Retrieve info window associated with this marker
        final InfoWindow markerInfo = (InfoWindow) marker.getTag();

        //Check if successfully retrieved
        if (markerInfo != null) {
            //Center map on marker
            double center = mMap.getCameraPosition().target.latitude;
            double southMap = mMap.getProjection().getVisibleRegion().latLngBounds.southwest.latitude;
            double newLat = marker.getPosition().latitude + (center - southMap) / 1.2;

            //Move map camera to show info window properly
            mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(newLat, marker.getPosition().longitude)), 500, new GoogleMap.CancelableCallback() {
                @Override
                public void onFinish() {
                    //Show or hide info window after map move animation ended
                    infoWindowManager.toggle(markerInfo, true);
                }

                @Override
                public void onCancel() {
                    //Show or hide info window after user canceled update
                    infoWindowManager.toggle(markerInfo, true);
                }
            });
        }

        return true;
    }

    public void OnInfoWindowClick(int markerPosition, int nextPosition) {
        //Get next (+1) or previous (-1) marker (markers are added on reversed order, since data are ordered by date descending)
        Marker marker = markers.get(markers.size() - markerPosition + nextPosition * -1);

        //Call method to show marker info window
        onMarkerClick(marker);
    }

    public void OnCoordinatesSelected(final DocumentSnapshot coordinates) {
        // Return panel to collapsed state
        mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);

        // Call method on marker click
        onMarkerClick(markers.get(mCoordinatesAdapter.mSnapshots.indexOf(coordinates)));
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent intent) {
        // Collect data from the intent and use it
        if (resultCode == RESULT_OK) {
            //Check if returning from filter activity
            if (requestCode == REQUEST_FILTER) {
                //Rebuild query using filter settings
                buildQuery(intent);

                //Check if map is already loaded
                if (mMap != null) {
                    //Clear any existing data from map
                    mMap.clear();
                }

                //Inform new query to adapter
                refreshMenu.setActionView(new ProgressBar(this));
                mCoordinatesAdapter.disablePersistence();
                mCoordinatesAdapter.setQuery(mQuery);
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        //Save google map instance
        mMap = googleMap;

        //Set padding
        mMap.setPadding(0, getResources().getDimensionPixelSize(R.dimen.map_top_padding), 0, 0);

        //Set map type from user preferences
        mMap.setMapType(sharedPreferences.getInt("UserMapType", GoogleMap.MAP_TYPE_NORMAL));

        //Set map click event listener
        mMap.setOnMarkerClickListener(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.detail, menu);

        //Show loading indicator
        refreshMenu = menu.findItem(R.id.action_refresh);

        //Show loading view on beginning
        refreshMenu.setActionView(new ProgressBar(this));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Single menu item
        if (item.getItemId() == R.id.action_filter) {
            //Get current activity intent
            Intent intent = getIntent();

            //Set class to FilterActivity
            intent.setClass(this, FilterActivity.class);

            //Call FilterActivity and wait for result
            startActivityForResult(intent, REQUEST_FILTER);

            //End method
            return true;

        } else if (item.getItemId() == R.id.action_refresh) {
            //Show loading indicator
            refreshMenu.setActionView(new ProgressBar(this));

            //Stop current snapshot listener
            mCoordinatesAdapter.stopListening();

            //Disable local cache results
            mCoordinatesAdapter.disablePersistence();

            //Start listening again, but now without cached data
            mCoordinatesAdapter.startListening();

            //End method
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        //Go back to main activity
        NavUtils.navigateUpFromSameTask(this);
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        //Edit tracker group
        if (item.getGroupId() == R.id.edit_tracker) {
            // Create intent to edit tracker settings
            Intent editIntent = new Intent();

            //Put tracker data on intent
            editIntent.putExtra("Tracker", tracker);

            //Define request intent to update an existing tracker
            editIntent.putExtra("Request", REQUEST_UPDATE);

            switch (item.getItemId()) {
                case R.id.menu_default_settings:

                    //Editing default settings
                    editIntent.setClass(this, DefaultSettingsActivity.class);
                    break;

                case R.id.menu_notification_settings:

                    //Define request intent to update an existing tracker
                    editIntent.putExtra("UpdateNotifications", true);

                case R.id.menu_tracker_settings:

                    //Editing default settings
                    editIntent.setClass(this, TrackerSettingsActivity.class);
            }

            //Start edit activity
            startActivityForResult(editIntent, REQUEST_UPDATE);
        } else if (item.getGroupId() == R.id.tracker_commands) {
            //Create empty configuration
            final Configuration command = new Configuration("", "", null, true, Configuration.PRIORITY_MAX);

            //Create empty alert strings
            String alertTitle = "", alertDescription = "";

            //Get command id
            switch (item.getItemId()) {
                case R.id.menu_reset:

                    //Command to reset current tracker configurations
                    showAlert("Reiniciar configurações", "Deseja reiniciar as configurações deste dispositivo?", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //Create intent to call next activity (Tracker Configurations)
                            Intent intent = new Intent(DetailActivity.this, TrackerSettingsActivity.class);

                            //Put tracker data on intent
                            intent.putExtra("Tracker", tracker);

                            //Inform activity intention: update existing tracker
                            intent.putExtra("Request", MainActivity.REQUEST_UPDATE);

                            //Inform activity intention: reset configurations
                            intent.putExtra("ResetConfig", true);

                            //Start next activity
                            startActivityForResult(intent, REQUEST_UPDATE);
                        }
                    });

                    //End method
                    return true;

                case R.id.menu_request_position:

                    //Define request position command
                    command.setName("Location");
                    command.setDescription("Solicitando localização atual");

                    //Define alert strings
                    alertTitle = "Solicitar localização";
                    alertDescription = "Deseja solicitar a localização atual do dispositivo?";
                    break;

                case R.id.menu_request_status:

                    //Define request position command
                    command.setName("StatusCheck");
                    command.setDescription("Solicitando status do dispositivo");

                    //Define alert strings
                    alertTitle = "Solicitar atualização de status";
                    alertDescription = "Deseja solicitar o status (nível de sinal GSM e percentual de bateria) do dispositivo?";
                    break;

                case R.id.menu_turn_off:

                    //Define request position command
                    command.setName("TempOff");
                    command.setDescription("Solicitando desligamento temporário");
                    command.setValue("10");

                    //Define alert strings
                    alertTitle = "Solicitar o desligamento temporário";
                    alertDescription = "Deseja solicitar o desligamento temporário deste dispositivo?";
                    break;
            }

            //Show alert to request confirmation from user
            showAlert(alertTitle, alertDescription, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //If user confirmed, send command to tracker
                    sendCommand(command);
                }
            });
        } else if (item.getGroupId() == R.id.menu_map_layers && mMap != null) {
            //Get shared preferences editor
            SharedPreferences.Editor editor = sharedPreferences.edit();

            //Initialize using default map
            int mapType = GoogleMap.MAP_TYPE_NORMAL;

            //Check map type chosen by user
            switch (item.getItemId()) {
                case R.id.map_default:
                    mapType = GoogleMap.MAP_TYPE_NORMAL;
                    break;
                case R.id.map_satellite:
                    mapType = GoogleMap.MAP_TYPE_SATELLITE;
                    break;
                case R.id.map_terrain:
                    mapType = GoogleMap.MAP_TYPE_TERRAIN;
                    break;
                case R.id.map_hybrid:
                    mapType = GoogleMap.MAP_TYPE_HYBRID;
                    break;
            }

            //Change current map type
            mMap.setMapType(mapType);

            //Change user preference
            editor.putInt("UserMapType", mapType);

            //Save user preference
            editor.apply();
        }
        else if (item.getGroupId() == R.id.menu_map_components && mMap != null)
        {
            //Get map component
            switch (item.getItemId())
            {
                case R.id.menu_map_path:

                    //Vibrate options
                    final String[] path_options = {"Nenhum", "Linha", "Pontilhado", "Caminho provável"};

                    //Show options to user
                    showOptions("Caminho entre coordenadas", path_options, sharedPreferences.getInt("Map_Path", 2), new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i)
                        {
                            //Get user option
                            int option = ((AlertDialog) dialogInterface).getListView().getCheckedItemPosition();

                            //Save on shared preferences
                            sharedPreferences.edit().putInt("Map_Path", option).apply();

                            //Reload map
                            loadMapComponents(mCoordinatesAdapter.mSnapshots);
                        }
                    });
                    break;

                case R.id.menu_map_gsm:

                    //Radius options
                    final String[] radius_options = {"Nenhum", "200m", "500m", "1km", "2km", "5km"};

                    //Show options to user
                    showOptions("Raio de alcance de coordenadas por GSM", radius_options, sharedPreferences.getInt("Map_Radius", 3), new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i)
                        {
                            //Get user option
                            int option = ((AlertDialog) dialogInterface).getListView().getCheckedItemPosition();

                            //Save on shared preferences
                            sharedPreferences.edit().putInt("Map_Radius", option).apply();

                            //Reload map
                            loadMapComponents(mCoordinatesAdapter.mSnapshots);
                        }
                    });
                    break;

                case R.id.menu_map_location:

                    //Vibrate options
                    final String[] location_options = {"Não exibir", "Exibir minha localização"};

                    //Show options to user
                    showOptions("Acesso à localização deste dispositivo", location_options, sharedPreferences.getInt("Map_MyLocation", 0), new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //Get user option
                            int option = ((AlertDialog) dialogInterface).getListView().getCheckedItemPosition();

                            //Save on shared preferences
                            sharedPreferences.edit().putInt("Map_MyLocation", option).apply();

                            //If user wants to show location
                            if (option > 0)
                            {
                                //Check for permission
                                if (ActivityCompat.checkSelfPermission(DetailActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                                {
                                    //If user has not granted permission yet, request user
                                    ActivityCompat.requestPermissions(DetailActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION);
                                }
                                else
                                {
                                    //Show user location
                                    mMap.setMyLocationEnabled(true);
                                    mMap.getUiSettings().setMyLocationButtonEnabled(true);
                                }

                            }
                            else
                            {
                                //Hide user location
                                mMap.setMyLocationEnabled(false);
                                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                            }
                        }
                    });
                    break;

                case R.id.menu_map_zoom:

                    //Vibrate options
                    final String[] zoom_options = {"Não exibir", "Exibir controles"};

                    //Show options to user
                    showOptions("Exibição de botões de zoom", zoom_options, sharedPreferences.getInt("Map_Zoom", 0), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i)
                        {
                            //Get user option
                            int option = ((AlertDialog) dialogInterface).getListView().getCheckedItemPosition();

                            //Save on shared preferences
                            sharedPreferences.edit().putInt("Map_Zoom", option).apply();

                            //If user wants to display
                            if(option > 0)
                            {
                                //Show zoom controls
                                mMap.getUiSettings().setZoomControlsEnabled(true);
                            }
                            else
                            {
                                //Hide controls
                                mMap.getUiSettings().setZoomControlsEnabled(false);
                            }
                        }
                    });
            }
        }

        //Close drawer
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults)
    {
        switch (requestCode)
        {
            case REQUEST_PERMISSION:
            {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    //Check permission for fine location
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    {
                        //User didn't allowed permission to my location
                        sharedPreferences.edit().putInt("Map_MyLocation", 1).apply();

                        //Permission was granted
                        mMap.setMyLocationEnabled(true);
                        mMap.getUiSettings().setMyLocationButtonEnabled(true);
                    }
                }
                else
                {
                    //User didn't allowed permission to my location
                    sharedPreferences.edit().putInt("Map_MyLocation", 0).apply();

                    //Disable on map
                    mMap.setMyLocationEnabled(false);
                    mMap.getUiSettings().setMyLocationButtonEnabled(false);
                }
            }
        }
    }

    public void showAlert(String alertTitle, String alertMessage, DialogInterface.OnClickListener positiveResult)
    {
        //Create confirmation dialog
        new AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_Dialog_Alert)
                .setIcon(R.drawable.ic_settings_grey_40dp)
                .setTitle(alertTitle)
                .setMessage(alertMessage)
                .setPositiveButton("Sim", positiveResult)
                .setNegativeButton("Não", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        //Dismiss dialog, no action
                        dialog.cancel();
                    }
                })
                .show();
    }

    public void showOptions(String alertTitle, String[] options, int defaultOption, DialogInterface.OnClickListener positiveResult)
    {
        //Create single choice selection dialog
        new AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_Dialog_Alert)
                .setIcon(R.drawable.ic_settings_grey_40dp)
                .setTitle(alertTitle)
                .setSingleChoiceItems(options, defaultOption, null)
                .setPositiveButton("Confirmar", positiveResult)
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        //Dismiss dialog, no action
                        dialog.cancel();
                    }
                })
                .show();
    }

    public void sendCommand(Configuration command)
    {
        //Initialize transaction
        WriteBatch transaction = mFireStoreDB.batch();

        //Save configuration on DB
        transaction.set(mFireStoreDB.document("Tracker/" + tracker.getIdentification() + "/Configurations/" + command.getName()), command);

        //Request new update to this tracker
        transaction.update(mFireStoreDB.document("Tracker/" + tracker.getIdentification()), "lastConfiguration", null);

        //Create message to give user a feedback
        txtConfigDescription.setText(getString(R.string.txtConfigRequest));
        txtConfigStatus.setText(getString(R.string.txtConfigStatus));
        imgConfigStatus.setImageResource(R.drawable.ic_settings_grey_40dp);

        //Flag pending configuration
        configPending = true;

        //Load config panel
        loadBottomPanel(mCoordinatesAdapter.getItemCount());

        //Commit transaction
        transaction.commit().addOnSuccessListener(new OnSuccessListener<Void>()
        {
            @Override
            public void onSuccess(Void aVoid)
            {

                //Initialize progressNotification
                ProgressNotification updateIndicator = new ProgressNotification(DetailActivity.this, tracker);

                //Request configuration update
                updateIndicator.initialize();

                //Call method to show configuration process
                DetailActivity.this.monitorConfiguration("PENDING", "Solicitação registrada com sucesso", "Aguardando resposta do servidor");

            }
        }).addOnFailureListener(new OnFailureListener()
        {
            @Override
            public void onFailure(@NonNull Exception e)
            {

                //Error message
                Snackbar.make(findViewById(android.R.id.content), "Erro: Não foi possível concluir solicitação", Snackbar.LENGTH_LONG).show();
            }
        });
    }

    public void buildQuery(Intent intent)
    {
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
            mQuery = mFireStoreDB.collection("Tracker/" + tracker.getIdentification() + "/Coordinates")
                    .whereGreaterThan("datetime", startDate)
                    .whereLessThan("datetime", endDate)
                    .orderBy("datetime", Query.Direction.DESCENDING)
                    .limit(limit);

            //Change text from empty view
            ((TextView) findViewById(R.id.txtBottomTitle)).setText(getResources().getString(R.string.txtFilterResult));
            ((TextView) findViewById(R.id.txtBottomSubtitle)).setText(getResources().getString(R.string.txtFilterResultSubtitle));
        }
        else
        {
            //Define search query
            mQuery = mFireStoreDB.collection("Tracker/" + tracker.getIdentification() + "/Coordinates")
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
